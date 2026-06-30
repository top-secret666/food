package by.vstu.zamok.order.client;

import by.vstu.zamok.order.dto.DishDto;
import by.vstu.zamok.order.exception.BadRequestException;
import by.vstu.zamok.order.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class RestaurantServiceClient {

    private final RestTemplate restTemplate;

    @Value("${restaurant.service.url}")
    private String restaurantServiceUrl;

    @Retry(name = "restaurantService", fallbackMethod = "restaurantFallback")
    @CircuitBreaker(name = "restaurantService", fallbackMethod = "restaurantFallback")
    public void validateRestaurantExists(Long restaurantId) {
        String url = restaurantServiceUrl + "/api/restaurants/" + restaurantId;
        try {
            restTemplate.getForEntity(url, by.vstu.zamok.order.dto.RestaurantDto.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Restaurant not found: " + restaurantId, e);
        }
    }

    @Retry(name = "restaurantService", fallbackMethod = "dishFallback")
    @CircuitBreaker(name = "restaurantService", fallbackMethod = "dishFallback")
    public DishDto getDishForRestaurant(Long restaurantId, Long dishId) {
        String url = restaurantServiceUrl + "/api/dishes/" + dishId;
        try {
            ResponseEntity<DishDto> response = restTemplate.getForEntity(url, DishDto.class);
            DishDto dish = response.getBody();
            if (dish == null || dish.getId() == null) {
                throw new ResourceNotFoundException("Dish not found: " + dishId);
            }
            if (!restaurantId.equals(dish.getRestaurantId())) {
                throw new BadRequestException(
                        "Dish " + dishId + " does not belong to restaurant " + restaurantId);
            }
            return dish;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Dish not found: " + dishId, e);
        }
    }

    @SuppressWarnings("unused")
    private void restaurantFallback(Long restaurantId, Throwable t) {
        throw new ResourceNotFoundException("Restaurant-service unavailable or restaurant not found: " + restaurantId, t);
    }

    @SuppressWarnings("unused")
    private DishDto dishFallback(Long restaurantId, Long dishId, Throwable t) {
        throw new ResourceNotFoundException("Restaurant-service unavailable or dish not found: " + dishId, t);
    }
}
