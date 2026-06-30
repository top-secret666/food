package by.vstu.zamok.restaurant.controller;

import by.vstu.zamok.restaurant.dto.DishDto;
import by.vstu.zamok.restaurant.service.DishService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dishes")
@AllArgsConstructor
public class DishLookupController {

    private final DishService dishService;

    @GetMapping("/{dishId}")
    public DishDto getDishById(@PathVariable Long dishId) {
        return dishService.findByIdOrThrow(dishId);
    }
}
