package by.vstu.zamok.order.dto;

import lombok.Data;

@Data
public class RestaurantDto {
    private Long id;
    private String name;
    private String cuisine;
    private String address;
}
