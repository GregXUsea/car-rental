package com.carrental.controller;

import com.carrental.dto.Result;
import com.carrental.entity.User;
import com.carrental.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private UserMapper userMapper;

    /**
     * 上传头像：仅支持图片格式，限制 5MB，上传后自动更新用户头像
     */
    @PostMapping("/user/upload-avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }

        // 限制文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error("仅支持图片格式");
        }

        // 限制文件大小（5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            return Result.error("文件大小不能超过5MB");
        }

        try {
            // 创建存储目录：转为绝对路径，避免 Tomcat 临时目录问题
            Path dirPath = Paths.get(uploadDir, "avatars").toAbsolutePath().normalize();
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }

            // 生成唯一文件名：userId_uuid_原扩展名
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String newFileName = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            // 保存文件
            Path filePath = dirPath.resolve(newFileName);
            file.transferTo(filePath.toFile());

            // 更新用户头像
            String avatarUrl = "/uploads/avatars/" + newFileName;
            User user = userMapper.selectById(userId);
            if (user != null) {
                user.setAvatar(avatarUrl);
                userMapper.updateById(user);
            }

            return Result.success(avatarUrl);
        } catch (IOException e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}
