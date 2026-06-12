-- 汽车租赁系统数据库初始化脚本
CREATE DATABASE IF NOT EXISTS car_rental DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE car_rental;

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL UNIQUE,
  `password` VARCHAR(100) NOT NULL,
  `nickname` VARCHAR(50) DEFAULT NULL,
  `phone` VARCHAR(20) DEFAULT NULL,
  `email` VARCHAR(100) DEFAULT NULL,
  `avatar` VARCHAR(255) DEFAULT '/img/default-avatar.png',
  `role` TINYINT NOT NULL DEFAULT 0 COMMENT '0普通用户 1管理员',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 车辆表
DROP TABLE IF EXISTS `cars`;
CREATE TABLE `cars` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `brand` VARCHAR(50) NOT NULL,
  `model` VARCHAR(50) NOT NULL,
  `color` VARCHAR(20) DEFAULT NULL,
  `seats` INT NOT NULL DEFAULT 5,
  `price_per_day` DECIMAL(10,2) NOT NULL,
  `deposit` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `image` VARCHAR(255) DEFAULT '/img/default-car.png',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0空闲 1已租 2已预约 3维护中',
  `mileage` INT NOT NULL DEFAULT 0,
  `last_maintain_date` DATE DEFAULT NULL,
  `description` TEXT,
  `category` VARCHAR(30) DEFAULT '轿车' COMMENT '轿车/SUV/MPV/新能源',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单表
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL UNIQUE,
  `user_id` BIGINT NOT NULL,
  `car_id` BIGINT NOT NULL,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `actual_return_time` DATETIME DEFAULT NULL,
  `total_cost` DECIMAL(10,2) DEFAULT 0,
  `deposit` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待支付 1在租 2已完成 3已取消 4预约中',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_car_id` (`car_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 维护记录表
DROP TABLE IF EXISTS `maintenance_records`;
CREATE TABLE `maintenance_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `car_id` BIGINT NOT NULL,
  `mileage_at_maintenance` INT NOT NULL,
  `maintenance_type` VARCHAR(50) NOT NULL,
  `description` TEXT,
  `cost` DECIMAL(10,2) DEFAULT 0,
  `maintenance_date` DATE NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_car_id` (`car_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 注意：用户账号由应用启动时自动创建，无需在此手动插入
-- 默认账号: admin/admin123(管理员), zhangsan/user123(普通用户), lisi/user123(普通用户)

-- 插入车辆数据
INSERT INTO `cars` (`brand`, `model`, `color`, `seats`, `price_per_day`, `deposit`, `image`, `status`, `mileage`, `last_maintain_date`, `description`, `category`) VALUES
('丰田', '卡罗拉 2024款', '白色', 5, 158.00, 2000.00, '/img/corolla.jpg', 0, 32000, '2025-12-01', '经典家用轿车，省油耐用，适合日常通勤和短途出行', '轿车'),
('本田', '雅阁 2024款', '黑色', 5, 198.00, 3000.00, '/img/accord.jpg', 0, 28000, '2025-11-15', '中级轿车，空间宽敞，动力充沛，商务家用皆宜', '轿车'),
('大众', '帕萨特 2024款', '银色', 5, 188.00, 3000.00, '/img/passat.jpg', 1, 45000, '2025-10-20', '德系品质，底盘扎实，高速行驶稳定性出色', '轿车'),
('宝马', '3系 2024款', '蓝色', 5, 358.00, 5000.00, '/img/bmw3.jpg', 0, 18000, '2026-01-10', '豪华运动轿车，操控精准，驾驶乐趣十足', '轿车'),
('奔驰', 'C级 2024款', '白色', 5, 388.00, 5000.00, '/img/benzc.jpg', 0, 22000, '2025-12-20', '豪华舒适轿车，内饰精致，乘坐体验一流', '轿车'),
('丰田', 'RAV4荣放', '红色', 5, 228.00, 3000.00, '/img/rav4.jpg', 0, 35000, '2025-11-01', '紧凑型SUV，四驱系统，适合城市和轻度越野', 'SUV'),
('本田', 'CR-V 2024款', '白色', 5, 238.00, 3000.00, '/img/crv.jpg', 2, 41000, '2025-10-15', '热销SUV，空间大，油耗低，家庭出行首选', 'SUV'),
('奥迪', 'Q5L 2024款', '黑色', 5, 428.00, 6000.00, '/img/q5l.jpg', 0, 15000, '2026-02-01', '豪华中型SUV，quattro四驱，科技配置丰富', 'SUV'),
('别克', 'GL8 2024款', '黑色', 7, 328.00, 4000.00, '/img/gl8.jpg', 0, 52000, '2025-09-20', '商务MPV标杆，空间超大，接待出游两相宜', 'MPV'),
('比亚迪', '汉EV 2024款', '灰色', 5, 268.00, 4000.00, '/img/hanev.jpg', 0, 20000, '2026-01-05', '纯电轿车，续航长，加速快，智能配置高', '新能源'),
('特斯拉', 'Model 3 2024款', '白色', 5, 298.00, 5000.00, '/img/model3.jpg', 3, 38000, '2025-08-10', '纯电轿车，自动驾驶辅助，极简内饰设计', '新能源'),
('蔚来', 'ES6 2024款', '蓝色', 5, 368.00, 5000.00, '/img/es6.jpg', 0, 12000, '2026-03-01', '纯电SUV，换电服务，智能座舱体验出色', '新能源'),
('丰田', '赛那 2024款', '白色', 7, 358.00, 4000.00, '/img/sienna.jpg', 1, 29000, '2025-11-25', '混动MPV，油耗极低，全家出行神器', 'MPV'),
('理想', 'L7 2024款', '银色', 6, 398.00, 5000.00, '/img/l7.jpg', 0, 16000, '2026-02-15', '增程式SUV，大空间，家庭旗舰，长途无焦虑', '新能源');

-- 插入维护记录
INSERT INTO `maintenance_records` (`car_id`, `mileage_at_maintenance`, `maintenance_type`, `description`, `cost`, `maintenance_date`) VALUES
(1, 10000, '常规保养', '更换机油机滤，检查刹车片', 680.00, '2025-03-15'),
(1, 20000, '常规保养', '更换机油机滤、空气滤芯', 850.00, '2025-07-20'),
(1, 30000, '大保养', '更换机油三滤、刹车油、火花塞', 1800.00, '2025-12-01'),
(3, 15000, '常规保养', '更换机油机滤', 720.00, '2025-04-10'),
(3, 30000, '常规保养', '更换机油机滤、空调滤芯', 900.00, '2025-08-15'),
(3, 45000, '大保养', '更换机油三滤、变速箱油、刹车片', 2200.00, '2025-10-20'),
(7, 10000, '常规保养', '更换机油机滤', 650.00, '2025-02-20'),
(7, 20000, '常规保养', '更换机油三滤', 880.00, '2025-06-10'),
(7, 40000, '大保养', '全车检查、更换刹车油防冻液', 1500.00, '2025-10-15'),
(11, 10000, '常规检查', '检查电池状态、更换空调滤芯', 400.00, '2025-04-01'),
(11, 20000, '常规保养', '更换刹车油、检查底盘', 600.00, '2025-08-10'),
(11, 35000, '轮胎更换', '更换四条轮胎', 3200.00, '2025-12-20');

-- 插入测试订单
INSERT INTO `orders` (`order_no`, `user_id`, `car_id`, `start_time`, `end_time`, `actual_return_time`, `total_cost`, `deposit`, `status`) VALUES
('ORD202601010001', 2, 3, '2026-01-05 09:00:00', '2026-01-08 09:00:00', NULL, 564.00, 3000.00, 1),
('ORD202601020001', 3, 7, '2026-01-10 10:00:00', '2026-01-15 10:00:00', NULL, 1190.00, 3000.00, 4),
('ORD202512200001', 2, 13, '2025-12-20 08:00:00', '2025-12-23 08:00:00', '2025-12-23 07:30:00', 1074.00, 4000.00, 2),
('ORD202512150001', 3, 11, '2025-12-15 09:00:00', '2025-12-18 09:00:00', '2025-12-18 08:45:00', 894.00, 5000.00, 2);
