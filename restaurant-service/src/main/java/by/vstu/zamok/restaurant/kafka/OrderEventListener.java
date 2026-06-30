package by.vstu.zamok.restaurant.kafka;

import by.vstu.zamok.restaurant.event.OrderCreatedEvent;
import by.vstu.zamok.restaurant.event.OrderStatusChangedEvent;
import by.vstu.zamok.restaurant.service.OrderNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final OrderNotificationService orderNotificationService;

    @KafkaListener(topics = "${order.kafka.topic:order-created}", groupId = "restaurant-service")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.debug("Received order-created event: {}", event);
        orderNotificationService.handleOrderCreated(event);
    }

    @KafkaListener(topics = "${order.kafka.status-topic:order-status-changed}", groupId = "restaurant-service")
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.debug("Received order-status-changed event: {}", event);
        orderNotificationService.handleOrderStatusChanged(event);
    }
}
