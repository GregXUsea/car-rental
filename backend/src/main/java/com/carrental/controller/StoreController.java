package com.carrental.controller;

import com.carrental.dto.Result;
import com.carrental.entity.Store;
import com.carrental.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 门店控制器
 */
@RestController
@RequestMapping("/api/stores")
public class StoreController {

    @Autowired
    private StoreService storeService;

    /**
     * 获取所有城市
     */
    @GetMapping("/cities")
    public Result<List<String>> getCities() {
        return Result.success(storeService.getAllCities());
    }

    /**
     * 按城市获取门店列表
     */
    @GetMapping("/list")
    public Result<List<Store>> getStoresByCity(@RequestParam(required = false) String city) {
        if (city != null && !city.isEmpty()) {
            return Result.success(storeService.getStoresByCity(city));
        }
        return Result.success(storeService.getActiveStores());
    }

    /**
     * 获取门店详情
     */
    @GetMapping("/{id}")
    public Result<Store> getStore(@PathVariable Long id) {
        Store store = storeService.getStoreById(id);
        if (store == null) return Result.error("门店不存在");
        return Result.success(store);
    }
}
