package com.carrental.controller;

import com.carrental.entity.Driver;
import com.carrental.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    @Autowired
    private DriverService driverService;

    @GetMapping("/list")
    public Map<String, Object> list() {
        return Map.of("code", 200, "data", driverService.listAll());
    }

    @GetMapping("/available")
    public Map<String, Object> available() {
        return Map.of("code", 200, "data", driverService.listAvailable());
    }

    @GetMapping("/detail/{id}")
    public Map<String, Object> detail(@PathVariable Long id) {
        Driver driver = driverService.getById(id);
        if (driver == null) {
            return Map.of("code", 404, "message", "司机不存在");
        }
        return Map.of("code", 200, "data", driver);
    }

    @PostMapping("/add")
    public Map<String, Object> add(@RequestBody Driver driver) {
        driverService.save(driver);
        return Map.of("code", 200, "message", "添加成功");
    }

    @PutMapping("/update")
    public Map<String, Object> update(@RequestBody Driver driver) {
        driverService.updateById(driver);
        return Map.of("code", 200, "message", "修改成功");
    }

    @PostMapping("/status/{id}")
    public Map<String, Object> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        if (driverService.updateStatus(id, status)) {
            return Map.of("code", 200, "message", "状态更新成功");
        }
        return Map.of("code", 500, "message", "更新失败");
    }

    @DeleteMapping("/delete/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        driverService.removeById(id);
        return Map.of("code", 200, "message", "删除成功");
    }
}
