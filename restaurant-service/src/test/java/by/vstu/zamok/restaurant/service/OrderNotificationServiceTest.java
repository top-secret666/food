package by.vstu.zamok.restaurant.service;

import by.vstu.zamok.restaurant.entity.RestaurantOrderNotification;
import by.vstu.zamok.restaurant.event.OrderCreatedEvent;
import by.vstu.zamok.restaurant.event.OrderItemSnapshot;
import by.vstu.zamok.restaurant.event.OrderStatusChangedEvent;
import by.vstu.zamok.restaurant.repository.RestaurantOrderNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderNotificationServiceTest {

    @Mock
    private RestaurantOrderNotificationRepository repository;

    @InjectMocks
    private OrderNotificationService orderNotificationService;

    @Test
    void handleOrderCreated_persistsNotification() {
        OrderCreatedEvent event = new OrderCreatedEvent(
                10L, 5L, 1L, List.of(new OrderItemSnapshot(1L, 2, 450)));

        when(repository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(repository.save(any(RestaurantOrderNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        orderNotificationService.handleOrderCreated(event);

        ArgumentCaptor<RestaurantOrderNotification> captor = ArgumentCaptor.forClass(RestaurantOrderNotification.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo(10L);
        assertThat(captor.getValue().getRestaurantId()).isEqualTo(1L);
        assertThat(captor.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void handleOrderStatusChanged_updatesExistingNotification() {
        RestaurantOrderNotification notification = new RestaurantOrderNotification();
        notification.setOrderId(10L);
        notification.setStatus("PENDING");

        when(repository.findByOrderId(10L)).thenReturn(Optional.of(notification));
        when(repository.save(any(RestaurantOrderNotification.class))).thenAnswer(inv -> inv.getArgument(0));

        orderNotificationService.handleOrderStatusChanged(new OrderStatusChangedEvent(10L, "COMPLETED"));

        verify(repository).save(notification);
        assertThat(notification.getStatus()).isEqualTo("COMPLETED");
    }
}
