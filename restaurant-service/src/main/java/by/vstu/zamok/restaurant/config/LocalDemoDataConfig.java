package by.vstu.zamok.restaurant.config;

import by.vstu.zamok.restaurant.entity.Dish;
import by.vstu.zamok.restaurant.entity.Restaurant;
import by.vstu.zamok.restaurant.repository.DishRepository;
import by.vstu.zamok.restaurant.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalDemoDataConfig {

    private final RestaurantRepository restaurantRepository;
    private final DishRepository dishRepository;

    @Bean
    CommandLineRunner seedRestaurants() {
        return args -> {
            if (restaurantRepository.count() > 0) {
                // Ensure image URLs exist even for older in-memory restarts within same JVM (H2 is mem, so usually empty)
                return;
            }

            Restaurant burgers = saveRestaurant("Burger House", "American", "123 Main St, Minsk");
            Restaurant pizza = saveRestaurant("Pizza Roma", "Italian", "45 Lenina Ave, Minsk");
            Restaurant sushi = saveRestaurant("Sushi Zen", "Japanese", "78 Nezavisimosti Blvd, Minsk");
            Restaurant bowls = saveRestaurant("Green Bowl", "Healthy", "12 Kastrychnitskaya, Minsk");

            saveDish(burgers, "Classic Burger", "Beef patty, lettuce, tomato, cheese", 450,
                    "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=600&q=80");
            saveDish(burgers, "Cheese Burger", "Double cheese, beef patty, pickles", 520,
                    "https://images.unsplash.com/photo-1550547660-d9450f859349?w=600&q=80");
            saveDish(burgers, "French Fries", "Crispy golden fries", 180,
                    "https://images.unsplash.com/photo-1573080496689-94e1d5a8d3ed?w=600&q=80");
            saveDish(burgers, "BBQ Bacon Burger", "Smoky BBQ sauce, crispy bacon", 610,
                    "https://images.unsplash.com/photo-1553979459-d2229ba7433b?w=600&q=80");
            saveDish(burgers, "Onion Rings", "Crunchy battered rings", 220,
                    "https://images.unsplash.com/photo-1639024471283-035566936bd2?w=600&q=80");
            saveDish(burgers, "Milkshake", "Vanilla soft-serve shake", 260,
                    "https://images.unsplash.com/photo-1572490122747-3968b75cc699?w=600&q=80");

            saveDish(pizza, "Margherita", "Tomato, mozzarella, basil", 380,
                    "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?w=600&q=80");
            saveDish(pizza, "Pepperoni", "Spicy pepperoni, mozzarella", 420,
                    "https://images.unsplash.com/photo-1628840042765-356cda07504e?w=600&q=80");
            saveDish(pizza, "Quattro Formaggi", "Four cheese blend", 460,
                    "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600&q=80");
            saveDish(pizza, "Diavola", "Spicy salami, chili oil", 490,
                    "https://images.unsplash.com/photo-1604382354936-07c5d9983bd3?w=600&q=80");
            saveDish(pizza, "Funghi", "Mushrooms, garlic, mozzarella", 430,
                    "https://images.unsplash.com/photo-1593560708920-61dd98c46a4e?w=600&q=80");
            saveDish(pizza, "Tiramisu Cup", "Classic espresso dessert", 290,
                    "https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=600&q=80");

            saveDish(sushi, "California Roll", "Crab, avocado, cucumber", 550,
                    "https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?w=600&q=80");
            saveDish(sushi, "Salmon Nigiri", "Fresh salmon over rice", 320,
                    "https://images.unsplash.com/photo-1583623025817-d180a2221d0a?w=600&q=80");
            saveDish(sushi, "Miso Soup", "Traditional miso with tofu", 150,
                    "https://images.unsplash.com/photo-1606491956689-2ea866880067?w=600&q=80");
            saveDish(sushi, "Dragon Roll", "Eel, avocado, unagi sauce", 690,
                    "https://images.unsplash.com/photo-1617196034183-421b4917c92d?w=600&q=80");
            saveDish(sushi, "Edamame", "Steamed soybeans, sea salt", 180,
                    "https://images.unsplash.com/photo-1564834724196-3c527466d516?w=600&q=80");
            saveDish(sushi, "Matcha Ice Cream", "Creamy green tea dessert", 240,
                    "https://images.unsplash.com/photo-1563805042-7684c019e1cb?w=600&q=80");

            saveDish(bowls, "Chicken Poke", "Rice, chicken, edamame, sesame", 540,
                    "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=600&q=80");
            saveDish(bowls, "Avocado Salad", "Greens, avocado, seeds", 390,
                    "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=600&q=80");
            saveDish(bowls, "Quinoa Bowl", "Quinoa, roasted veggies, tahini", 470,
                    "https://images.unsplash.com/photo-1490645935967-10de6ba17061?w=600&q=80");
            saveDish(bowls, "Berry Smoothie", "Berry blend, yogurt", 280,
                    "https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=600&q=80");
        };
    }

    private Restaurant saveRestaurant(String name, String cuisine, String address) {
        Restaurant r = new Restaurant();
        r.setName(name);
        r.setCuisine(cuisine);
        r.setAddress(address);
        return restaurantRepository.save(r);
    }

    private void saveDish(Restaurant restaurant, String name, String description, int price, String imageUrl) {
        Dish dish = new Dish();
        dish.setName(name);
        dish.setDescription(description);
        dish.setPrice(price);
        dish.setImageUrl(imageUrl);
        dish.setRestaurant(restaurant);
        dishRepository.save(dish);
    }
}
