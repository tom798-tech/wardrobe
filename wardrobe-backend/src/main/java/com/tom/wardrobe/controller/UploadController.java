package com.tom.wardrobe.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
public class UploadController {

    private static final Logger log = LoggerFactory.getLogger(UploadController.class);

    @Value("${wardrobe.upload.dir:src/main/resources/images}")
    private String uploadDir;

    /**
     * 允许上传的文件类型白名单
     */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    ));

    /**
     * 允许上传的文件类型对应的 Content-Type
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    ));

    /**
     * 文件头魔数校验（防止改后缀绕过）
     */
    private static final byte[] JPG_HEADER = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
    private static final byte[] PNG_HEADER = { (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47 };
    private static final byte[] GIF_HEADER = { (byte) 0x47, (byte) 0x49, (byte) 0x46 };
    private static final byte[] WEBP_HEADER = { (byte) 0x52, (byte) 0x49, (byte) 0x46, (byte) 0x46 };

    /**
     * 最大文件大小（5MB）
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @PostMapping("/upload")
    public String uploadFile(@RequestParam("clothesImage") MultipartFile file) {
        // 1. 检查文件是否为空
        if (file == null || file.isEmpty()) {
            return "文件为空";
        }

        // 2. 检查文件大小
        if (file.getSize() > MAX_FILE_SIZE) {
            return "文件大小超过限制（最大5MB）";
        }

        // 3. 检查文件扩展名
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            return "文件名无效";
        }
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return "不支持的文件类型，仅允许上传图片（jpg/png/gif/webp）";
        }

        // 4. 检查 Content-Type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            return "文件类型不匹配";
        }

        // 5. 校验文件头魔数（防止改后缀绕过）
        try {
            if (!validateFileHeader(file.getBytes(), extension)) {
                return "文件内容与扩展名不匹配，请上传真实图片";
            }
        } catch (IOException e) {
            log.error("读取文件内容失败", e);
            return "文件读取失败";
        }

        // 6. 安全处理文件名（防止路径遍历）
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String safeName = originalFilename.replaceAll("[\\\\/:*?\"<>|]", "_");
        String newFilename = uuid + "_" + safeName;

        try {
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File dest = new File(dir, newFilename);
            file.transferTo(dest);
            return newFilename;
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return "上传失败";
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 校验文件头魔数
     */
    private boolean validateFileHeader(byte[] content, String extension) {
        if (content == null || content.length < 4) {
            return false;
        }

        return switch (extension.toLowerCase()) {
            case "jpg", "jpeg" -> startsWith(content, JPG_HEADER);
            case "png" -> startsWith(content, PNG_HEADER);
            case "gif" -> startsWith(content, GIF_HEADER);
            case "webp" -> startsWith(content, WEBP_HEADER);
            default -> false;
        };
    }

    /**
     * 检查字节数组是否以指定前缀开头
     */
    private boolean startsWith(byte[] content, byte[] prefix) {
        if (content.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (content[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}