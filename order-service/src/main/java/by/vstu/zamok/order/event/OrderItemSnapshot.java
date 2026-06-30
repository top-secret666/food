package by.vstu.zamok.order.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemSnapshot {
    private Long dishId;
    private Integer quantity;
    private Integer price;
}
