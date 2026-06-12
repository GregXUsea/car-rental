package com.carrental.controller;

import com.carrental.dto.Result;
import com.carrental.entity.Car;
import com.carrental.service.CarService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cars")
public class CarController {

    @Autowired
    private CarService carService;

    @GetMapping("/list")
    public Result<List<Car>> listAll() {
        return Result.success(carService.listAll());
    }

    @GetMapping("/available")
    public Result<List<Car>> listAvailable() {
        return Result.success(carService.listAvailable());
    }

    @GetMapping("/detail/{id}")
    public Result<Car> detail(@PathVariable Long id) {
        Car car = carService.getById(id);
        return car != null ? Result.success(car) : Result.error("车辆不存在");
    }

    @GetMapping("/search")
    public Result<List<Car>> search(@RequestParam String keyword) {
        return Result.success(carService.search(keyword));
    }
}
