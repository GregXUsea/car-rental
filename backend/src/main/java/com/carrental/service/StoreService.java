package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.entity.Store;
import com.carrental.mapper.StoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店服务
 */
@Service
public class StoreService {

    @Autowired
    private StoreMapper storeMapper;

    /**
     * 获取所有营业中的门店
     */
    public List<Store> getActiveStores() {
        return storeMapper.selectList(
                new LambdaQueryWrapper<Store>().eq(Store::getStatus, 1)
        );
    }

    /**
     * 按城市获取门店
     */
    public List<Store> getStoresByCity(String city) {
        return storeMapper.selectList(
                new LambdaQueryWrapper<Store>()
                        .eq(Store::getCity, city)
                        .eq(Store::getStatus, 1)
        );
    }

    /**
     * 获取所有城市列表
     */
    public List<String> getAllCities() {
        return storeMapper.selectList(
                new LambdaQueryWrapper<Store>()
                        .select(Store::getCity)
                        .eq(Store::getStatus, 1)
                        .groupBy(Store::getCity)
        ).stream().map(Store::getCity).distinct().toList();
    }

    /**
     * 根据ID获取门店
     */
    public Store getStoreById(Long id) {
        return storeMapper.selectById(id);
    }
}
