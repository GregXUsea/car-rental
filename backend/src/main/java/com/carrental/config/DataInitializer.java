package com.carrental.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.entity.User;
import com.carrental.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void run(String... args) {
        // 管理员账号
        createUser("jinhang", "123456", "管理员", "13800000000", "jinhang@carrental.com", 1);
    }

    private void createUser(String username, String password, String nickname, String phone, String email, int role) {
        User exist = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (exist == null) {
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setNickname(nickname);
            user.setPhone(phone);
            user.setEmail(email);
            user.setRole(role);
            userMapper.insert(user);
        }
    }
}
