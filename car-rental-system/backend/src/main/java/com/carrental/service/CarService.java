package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.entity.Car;
import com.carrental.mapper.CarMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarService {

    @Autowired
    private CarMapper carMapper;

    public List<Car> listAll() {
        return carMapper.selectList(null);
    }

    public List<Car> listAvailable() {
        return carMapper.selectList(
                new LambdaQueryWrapper<Car>().eq(Car::getStatus, 0));
    }

    /** 可租用车辆：排除维护中(3) */
    public List<Car> listRentable() {
        return carMapper.selectList(
                new LambdaQueryWrapper<Car>().ne(Car::getStatus, 3));
    }

    public Car getById(Long id) {
        return carMapper.selectById(id);
    }

    public List<Car> search(String keyword) {
        return carMapper.selectList(new LambdaQueryWrapper<Car>()
                .like(Car::getBrand, keyword)
                .or().like(Car::getModel, keyword)
                .or().like(Car::getCategory, keyword)
                .or().like(Car::getDescription, keyword));
    }

    public void updateStatus(Long carId, Integer status) {
        Car car = new Car();
        car.setId(carId);
        car.setStatus(status);
        carMapper.updateById(car);
    }

    public List<Car> listByStatus(Integer status) {
        return carMapper.selectList(
                new LambdaQueryWrapper<Car>().eq(Car::getStatus, status));
    }

    public void updateMileage(Long carId, int mileage) {
        Car car = new Car();
        car.setId(carId);
        car.setMileage(mileage);
        carMapper.updateById(car);
    }
}
