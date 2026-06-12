package com.carrental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.carrental.entity.Order;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    @Select("SELECT o.*, c.brand, c.model, c.image, c.price_per_day, c.category, c.seats, u.username " +
            "FROM orders o " +
            "LEFT JOIN cars c ON o.car_id = c.id " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "ORDER BY o.create_time DESC")
    @Results(id = "orderWithCarAndUser", value = {
            @Result(property = "car.brand", column = "brand"),
            @Result(property = "car.model", column = "model"),
            @Result(property = "car.image", column = "image"),
            @Result(property = "car.pricePerDay", column = "price_per_day"),
            @Result(property = "car.category", column = "category"),
            @Result(property = "car.seats", column = "seats"),
            @Result(property = "username", column = "username")
    })
    List<Order> selectAllWithCarAndUser();

    @Select("SELECT o.*, c.brand, c.model, c.image, c.price_per_day, c.category, c.seats, u.username " +
            "FROM orders o " +
            "LEFT JOIN cars c ON o.car_id = c.id " +
            "LEFT JOIN users u ON o.user_id = u.id " +
            "WHERE o.user_id = #{userId} " +
            "ORDER BY o.create_time DESC")
    @ResultMap("orderWithCarAndUser")
    List<Order> selectByUserId(@Param("userId") Long userId);
}
