package com.product.controller;

import com.product.common.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
//@RequestMapping("/api") // 移除该注解，接口直接为/upload
@CrossOrigin
public class UploadController {

    @Value("${app.upload-path:upload}")
    private String uploadDir;

    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }
        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String ext = originalFilename != null && originalFilename.contains(".") ? originalFilename.substring(originalFilename.lastIndexOf('.')) : "";
        String fileName = UUID.randomUUID().toString().replaceAll("-", "") + ext;
        // 保存路径 - 移除开头的斜杠以使用相对路径
        String normalizedUploadDir = uploadDir.startsWith("/") ? uploadDir.substring(1) : uploadDir;
        File dir = new File(normalizedUploadDir);
        if (!dir.exists()) dir.mkdirs();
        File dest = new File(dir, fileName);
        try {
            // 使用getBytes而不是transferTo来避免临时目录问题
            byte[] bytes = file.getBytes();
            java.nio.file.Files.write(dest.toPath(), bytes);
            // 返回图片访问URL - 确保路径格式正确
            String url = "/upload/" + fileName;
            return Result.success(url);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("上传失败: " + e.getMessage());
        }
    }
}