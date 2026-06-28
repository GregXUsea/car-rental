package com.carrental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.carrental.entity.Driver;
import com.carrental.mapper.DriverMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DriverService extends ServiceImpl<DriverMapper, Driver> {

    public List<Driver> listAvailable() {
        return list(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getStatus, 0)
                .orderByDesc(Driver::getRating));
    }

    public List<Driver> listAll() {
        return list(new LambdaQueryWrapper<Driver>()
                .orderByDesc(Driver::getRating));
    }

    public Driver getAvailableDriver(Long id) {
        Driver driver = getById(id);
        if (driver != null && driver.getStatus() == 0) {
            return driver;
        }
        return null;
    }

    public boolean updateStatus(Long id, Integer status) {
        Driver driver = getById(id);
        if (driver != null) {
            driver.setStatus(status);
            return updateById(driver);
        }
        return false;
    }

    public boolean incrementServiceCount(Long id) {
        Driver driver = getById(id);
        if (driver != null) {
            driver.setServiceCount(driver.getServiceCount() + 1);
            return updateById(driver);
        }
        return false;
    }
}
