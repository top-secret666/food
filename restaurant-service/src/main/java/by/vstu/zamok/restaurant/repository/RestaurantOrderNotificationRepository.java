package by.vstu.zamok.restaurant.repository;

import by.vstu.zamok.restaurant.entity.RestaurantOrderNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RestaurantOrderNotificationRepository extends JpaRepository<RestaurantOrderNotification, Long> {
    Optional<RestaurantOrderNotification> findByOrderId(Long orderId);
    List<RestaurantOrderNotification> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId);
}
