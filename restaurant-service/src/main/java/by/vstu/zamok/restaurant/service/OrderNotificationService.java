package by.vstu.zamok.restaurant.service;

import by.vstu.zamok.restaurant.entity.RestaurantOrderNotification;
import by.vstu.zamok.restaurant.event.OrderCreatedEvent;
import by.vstu.zamok.restaurant.event.OrderStatusChangedEvent;
import by.vstu.zamok.restaurant.repository.RestaurantOrderNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderNotificationService {

    private final RestaurantOrderNotificationRepository repository;

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        if (event.getOrderId() == null || event.getRestaurantId() == null) {
            log.warn("Skipping order-created event with missing ids: {}", event);
            return;
        }

        RestaurantOrderNotification notification = repository.findByOrderId(event.getOrderId())
                .orElseGet(RestaurantOrderNotification::new);

        notification.setOrderId(event.getOrderId());
        notification.setRestaurantId(event.getRestaurantId());
        notification.setStatus("PENDING");
        LocalDateTime now = LocalDateTime.now();
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(now);
        }
        notification.setUpdatedAt(now);

        repository.save(notification);
        log.info("Stored order notification for orderId={} restaurantId={}", event.getOrderId(), event.getRestaurantId());
    }

    @Transactional
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.getOrderId() == null || event.getStatus() == null) {
            log.warn("Skipping order-status-changed event with missing data: {}", event);
            return;
        }

        repository.findByOrderId(event.getOrderId()).ifPresent(notification -> {
            notification.setStatus(event.getStatus());
            notification.setUpdatedAt(LocalDateTime.now());
            repository.save(notification);
            log.info("Updated order notification status orderId={} status={}", event.getOrderId(), event.getStatus());
        });
    }
}
