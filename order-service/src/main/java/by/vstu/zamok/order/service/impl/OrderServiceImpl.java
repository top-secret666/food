package by.vstu.zamok.order.service.impl;

import by.vstu.zamok.order.client.RestaurantServiceClient;
import by.vstu.zamok.order.client.UserServiceClient;
import by.vstu.zamok.order.dto.DishDto;
import by.vstu.zamok.order.dto.OrderRequestDto;
import by.vstu.zamok.order.entity.Order;
import by.vstu.zamok.order.entity.OrderItem;
import by.vstu.zamok.order.entity.OrderStatus;
import by.vstu.zamok.order.entity.Payment;
import by.vstu.zamok.order.event.OrderCreatedEvent;
import by.vstu.zamok.order.event.OrderItemSnapshot;
import by.vstu.zamok.order.event.OrderStatusChangedEvent;
import by.vstu.zamok.order.exception.ResourceNotFoundException;
import by.vstu.zamok.order.mapper.OrderMapper;
import by.vstu.zamok.order.payment.PaymentStrategyFactory;
import by.vstu.zamok.order.repository.OrderRepository;
import by.vstu.zamok.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final UserServiceClient userServiceClient;
    private final RestaurantServiceClient restaurantServiceClient;
    private final PaymentStrategyFactory paymentStrategyFactory;

    @Value("${order.kafka.topic:order-created}")
    private String ORDER_CREATED_TOPIC;

    @Value("${order.kafka.status-topic:order-status-changed}")
    private String ORDER_STATUS_CHANGED_TOPIC;

    @Override
    @Transactional
    public Order placeOrder(OrderRequestDto orderRequestDto, JwtAuthenticationToken authentication) {
        Long userId = userServiceClient.resolveUserId(authentication);
        Long restaurantId = orderRequestDto.getRestaurantId();

        restaurantServiceClient.validateRestaurantExists(restaurantId);

        Order order = orderMapper.toEntity(orderRequestDto);
        order.setStatus(OrderStatus.PENDING);
        order.setUserId(userId);
        order.setOrderDate(LocalDateTime.now());

        int totalPrice = 0;
        List<OrderItemSnapshot> snapshots = new ArrayList<>();

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            for (OrderItem item : order.getOrderItems()) {
                DishDto dish = restaurantServiceClient.getDishForRestaurant(restaurantId, item.getDishId());
                item.setOrder(order);
                item.setPrice(dish.getPrice());
                totalPrice += dish.getPrice() * item.getQuantity();
                snapshots.add(new OrderItemSnapshot(item.getDishId(), item.getQuantity(), dish.getPrice()));
            }
        } else {
            order.setOrderItems(Collections.emptyList());
        }
        order.setTotalPrice(totalPrice);

        Payment payment = new Payment();
        payment.setMethod(orderRequestDto.getPaymentMethod());
        payment.setAmount(totalPrice);
        payment.setOrder(order);
        paymentStrategyFactory.forMethod(orderRequestDto.getPaymentMethod()).apply(payment);
        order.setPayment(payment);

        Order savedOrder = orderRepository.save(order);

        kafkaTemplate.send(ORDER_CREATED_TOPIC, new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getUserId(),
                savedOrder.getRestaurantId(),
                snapshots
        ));

        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders(JwtAuthenticationToken authentication) {
        if (isAdmin(authentication)) {
            return orderRepository.findAll();
        } else {
            Long userId = userServiceClient.resolveUserId(authentication);
            return orderRepository.findByUserId(userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Order getOrderById(Long id, JwtAuthenticationToken authentication) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (isAdmin(authentication)) {
            return order;
        }

        Long userId = userServiceClient.resolveUserId(authentication);

        if (Objects.equals(order.getUserId(), userId)) {
            return order;
        } else {
            throw new AccessDeniedException("You do not have permission to view this order");
        }
    }

    @Override
    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        order.setStatus(status);
        Order saved = orderRepository.save(order);
        kafkaTemplate.send(ORDER_STATUS_CHANGED_TOPIC, new OrderStatusChangedEvent(saved.getId(), saved.getStatus()));
        return saved;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long id, JwtAuthenticationToken authentication) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));

        if (!isAdmin(authentication)) {
            Long userId = userServiceClient.resolveUserId(authentication);
            if (!Objects.equals(order.getUserId(), userId)) {
                throw new AccessDeniedException("You do not have permission to cancel this order");
            }
        }

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            return order;
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        kafkaTemplate.send(ORDER_STATUS_CHANGED_TOPIC, new OrderStatusChangedEvent(saved.getId(), saved.getStatus()));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Object analyticsSummary(String from, String to) {
        LocalDateTime start = Optional.ofNullable(from).map(LocalDateTime::parse).orElse(LocalDateTime.now().minusDays(7));
        LocalDateTime end = Optional.ofNullable(to).map(LocalDateTime::parse).orElse(LocalDateTime.now());
        List<Order> list = orderRepository.findByOrderDateBetween(start, end);
        int total = list.size();
        int revenue = list.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).mapToInt(Order::getTotalPrice).sum();
        Map<String, Long> byStatus = list.stream().collect(java.util.stream.Collectors.groupingBy(o -> o.getStatus().name(), java.util.stream.Collectors.counting()));
        return java.util.Map.of(
                "from", start.toString(),
                "to", end.toString(),
                "totalOrders", total,
                "revenue", revenue,
                "byStatus", byStatus
        );
    }

    private boolean isAdmin(JwtAuthenticationToken authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }
}
