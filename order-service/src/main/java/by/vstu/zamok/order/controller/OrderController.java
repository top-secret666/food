package by.vstu.zamok.order.controller;

import by.vstu.zamok.order.dto.OrderRequestDto;
import by.vstu.zamok.order.dto.OrderResponseDto;
import by.vstu.zamok.order.dto.UpdateOrderStatusRequest;
import by.vstu.zamok.order.entity.Order;
import by.vstu.zamok.order.entity.OrderStatus;
import by.vstu.zamok.order.mapper.OrderMapper;
import by.vstu.zamok.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponseDto placeOrder(@RequestBody @Valid OrderRequestDto orderRequestDto, JwtAuthenticationToken authentication) {
        Order order = orderService.placeOrder(orderRequestDto, authentication);
        return orderMapper.toDto(order);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public List<OrderResponseDto> getAllOrders(JwtAuthenticationToken authentication) {
        return orderService.getAllOrders(authentication).stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public OrderResponseDto getOrderById(@PathVariable Long id, JwtAuthenticationToken authentication) {
        Order order = orderService.getOrderById(id, authentication);
        return orderMapper.toDto(order);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public OrderResponseDto updateOrderStatus(@PathVariable Long id, @RequestBody @Valid UpdateOrderStatusRequest request) {
        Order order = orderService.updateOrderStatus(id, request.getStatus());
        return orderMapper.toDto(order);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public OrderResponseDto cancelOrder(@PathVariable Long id, JwtAuthenticationToken authentication) {
        Order order = orderService.cancelOrder(id, authentication);
        return orderMapper.toDto(order);
    }

    @GetMapping("/analytics/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public Object analyticsSummary(@RequestParam(required = false) String from,
                                   @RequestParam(required = false) String to) {
        return orderService.analyticsSummary(from, to);
    }
}
