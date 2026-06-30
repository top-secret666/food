package by.vstu.zamok.order.dto;

import lombok.Data;

@Data
public class DishDto {
    private Long id;
    private String name;
    private String description;
    private int price;
    private String imageUrl;
    private Long restaurantId;
}
