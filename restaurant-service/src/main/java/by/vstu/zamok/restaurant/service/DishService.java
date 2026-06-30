package by.vstu.zamok.restaurant.service;

import by.vstu.zamok.restaurant.dto.DishDto;
import by.vstu.zamok.restaurant.entity.Dish;
import by.vstu.zamok.restaurant.exception.ResourceNotFoundException;
import by.vstu.zamok.restaurant.mapper.DishMapper;
import by.vstu.zamok.restaurant.repository.DishRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class DishService {
    private final DishRepository dishRepository;
    private final DishMapper dishMapper;

    public List<DishDto> findAll() {
        return dishRepository.findAll().stream().map(dishMapper::toDto).collect(Collectors.toList());
    }

    public List<DishDto> findByRestaurantId(Long restaurantId) {
        return dishRepository.findByRestaurantId(restaurantId).stream().map(dishMapper::toDto).collect(Collectors.toList());
    }

    public DishDto findById(Long id) {
        return dishRepository.findById(id).map(dishMapper::toDto).orElse(null);
    }

    public DishDto findByIdOrThrow(Long id) {
        return dishRepository.findById(id)
                .map(dishMapper::toDto)
                .orElseThrow(() -> new ResourceNotFoundException("Dish not found: " + id));
    }

    public DishDto findByIdOrThrow(Long id) {
        return dishRepository.findById(id)
                .map(dishMapper::toDto)
                .orElseThrow(() -> new by.vstu.zamok.restaurant.exception.ResourceNotFoundException("Dish not found: " + id));
    }

    public DishDto save(DishDto dishDto) {
        Dish dish = dishMapper.toEntity(dishDto);
        return dishMapper.toDto(dishRepository.save(dish));
    }

    public void deleteById(Long id) {
        dishRepository.deleteById(id);
    }
}
