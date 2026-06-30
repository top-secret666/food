package by.vstu.zamok.order.service.impl;

import by.vstu.zamok.order.client.RestaurantServiceClient;
import by.vstu.zamok.order.client.UserServiceClient;
import by.vstu.zamok.order.dto.DishDto;
import by.vstu.zamok.order.dto.OrderItemRequestDto;
import by.vstu.zamok.order.dto.OrderRequestDto;
import by.vstu.zamok.order.entity.Order;
import by.vstu.zamok.order.entity.OrderStatus;
import by.vstu.zamok.order.event.OrderCreatedEvent;
import by.vstu.zamok.order.exception.BadRequestException;
import by.vstu.zamok.order.mapper.OrderMapper;
import by.vstu.zamok.order.payment.PaymentStrategyFactory;
import by.vstu.zamok.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderMapper orderMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private RestaurantServiceClient restaurantServiceClient;
    @Mock
    private PaymentStrategyFactory paymentStrategyFactory;

    @InjectMocks
    private OrderServiceImpl orderService;

    private JwtAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "ORDER_CREATED_TOPIC", "order-created");
        ReflectionTestUtils.setField(orderService, "ORDER_STATUS_CHANGED_TOPIC", "order-status-changed");

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("kc-1")
                .claim("email", "user@example.com")
                .claim("email_verified", true)
                .build();
        authentication = new JwtAuthenticationToken(jwt);
    }

    @Test
    void placeOrder_usesRestaurantPrices() {
        OrderRequestDto request = new OrderRequestDto();
        request.setRestaurantId(1L);
        request.setPaymentMethod("CARD");
        OrderItemRequestDto item = new OrderItemRequestDto();
        item.setDishId(1L);
        item.setQuantity(2);
        request.setItems(List.of(item));

        Order order = new Order();
        order.setRestaurantId(1L);
        var orderItem = new by.vstu.zamok.order.entity.OrderItem();
        orderItem.setDishId(1L);
        orderItem.setQuantity(2);
        order.setOrderItems(List.of(orderItem));

        DishDto dish = new DishDto();
        dish.setId(1L);
        dish.setRestaurantId(1L);
        dish.setPrice(450);

        Order saved = new Order();
        saved.setId(10L);
        saved.setUserId(5L);
        saved.setRestaurantId(1L);
        saved.setTotalPrice(900);
        saved.setStatus(OrderStatus.PENDING);

        when(userServiceClient.resolveUserId(authentication)).thenReturn(5L);
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(restaurantServiceClient.getDishForRestaurant(1L, 1L)).thenReturn(dish);
        when(paymentStrategyFactory.forMethod("CARD")).thenReturn(payment -> payment.setStatus(by.vstu.zamok.order.entity.PaymentStatus.COMPLETED));
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        Order result = orderService.placeOrder(request, authentication);

        assertThat(result.getTotalPrice()).isEqualTo(900);
        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(kafkaTemplate).send(eq("order-created"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getRestaurantId()).isEqualTo(1L);
        assertThat(eventCaptor.getValue().getItems()).hasSize(1);
        assertThat(eventCaptor.getValue().getItems().get(0).getPrice()).isEqualTo(450);
    }

    @Test
    void placeOrder_rejectsDishFromAnotherRestaurant() {
        OrderRequestDto request = new OrderRequestDto();
        request.setRestaurantId(1L);
        request.setPaymentMethod("CARD");
        OrderItemRequestDto item = new OrderItemRequestDto();
        item.setDishId(4L);
        item.setQuantity(1);
        request.setItems(List.of(item));

        Order order = new Order();
        var orderItem = new by.vstu.zamok.order.entity.OrderItem();
        orderItem.setDishId(4L);
        orderItem.setQuantity(1);
        order.setOrderItems(List.of(orderItem));

        when(userServiceClient.resolveUserId(authentication)).thenReturn(5L);
        when(orderMapper.toEntity(request)).thenReturn(order);
        when(restaurantServiceClient.getDishForRestaurant(1L, 4L))
                .thenThrow(new BadRequestException("Dish 4 does not belong to restaurant 1"));

        assertThatThrownBy(() -> orderService.placeOrder(request, authentication))
                .isInstanceOf(BadRequestException.class);
    }
}
