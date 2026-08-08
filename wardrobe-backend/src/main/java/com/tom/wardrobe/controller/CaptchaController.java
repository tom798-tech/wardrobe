package com.tom.wardrobe.controller;

import com.tom.wardrobe.util.RedisUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * 验证码控制器（纯 Java 实现，不依赖第三方库）
 */
@RestController
public class CaptchaController {

    private static final Logger log = LoggerFactory.getLogger(CaptchaController.class);

    private final RedisUtils redisUtils;

    /**
     * 验证码有效期（秒）
     */
    private static final int CAPTCHA_EXPIRE_SECONDS = 120;

    /**
     * 验证码字符集
     */
    private static final String CHAR_SET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 验证码长度
     */
    private static final int CAPTCHA_LENGTH = 4;

    /**
     * 图片宽度
     */
    private static final int IMAGE_WIDTH = 150;

    /**
     * 图片高度
     */
    private static final int IMAGE_HEIGHT = 50;

    public CaptchaController(RedisUtils redisUtils) {
        this.redisUtils = redisUtils;
    }

    /**
     * 获取验证码图片
     * 返回格式：{ "captchaId": "xxx", "image": "base64..." }
     */
    @GetMapping("/captcha")
    public Map<String, Object> getCaptcha() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 生成验证码文字
            String captchaText = generateCaptchaText();
            
            // 生成验证码 ID
            String captchaId = UUID.randomUUID().toString().replace("-", "");
            
            // 保存到 Redis
            redisUtils.set("captcha:" + captchaId, captchaText, CAPTCHA_EXPIRE_SECONDS);
            
            // 生成图片并转换为 Base64
            BufferedImage image = generateCaptchaImage(captchaText);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(image, "jpg", outputStream);
            String base64Image = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
            
            result.put("captchaId", captchaId);
            result.put("image", base64Image);
            return result;
        } catch (Exception e) {
            log.error("生成验证码失败", e);
            result.put("error", "生成验证码失败");
            return result;
        }
    }

    /**
     * 验证验证码
     */
    @GetMapping("/captcha/verify")
    public Map<String, Object> verifyCaptcha(@RequestParam String captchaId, @RequestParam String code) {
        Map<String, Object> result = new HashMap<>();
        
        if (captchaId == null || captchaId.isBlank() || code == null || code.isBlank()) {
            result.put("success", false);
            result.put("message", "验证码不能为空");
            return result;
        }
        
        String storedCode = (String) redisUtils.get("captcha:" + captchaId);
        
        if (storedCode == null) {
            result.put("success", false);
            result.put("message", "验证码已过期，请刷新");
            return result;
        }
        
        if (storedCode.equalsIgnoreCase(code)) {
            redisUtils.del("captcha:" + captchaId);
            result.put("success", true);
            result.put("message", "验证通过");
        } else {
            result.put("success", false);
            result.put("message", "验证码错误");
        }
        
        return result;
    }

    /**
     * 生成随机验证码文字
     */
    private String generateCaptchaText() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < CAPTCHA_LENGTH; i++) {
            sb.append(CHAR_SET.charAt(random.nextInt(CHAR_SET.length())));
        }
        return sb.toString();
    }

    /**
     * 生成验证码图片
     */
    private BufferedImage generateCaptchaImage(String captchaText) {
        BufferedImage image = new BufferedImage(IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        
        // 设置背景
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
        
        // 设置字体
        Font font = new Font("Arial", Font.BOLD, 30);
        g2d.setFont(font);
        
        Random random = new Random();
        
        // 绘制每个字符
        for (int i = 0; i < captchaText.length(); i++) {
            char c = captchaText.charAt(i);
            
            // 随机颜色
            Color color = new Color(random.nextInt(100) + 50, random.nextInt(100) + 50, random.nextInt(100) + 50);
            g2d.setColor(color);
            
            // 随机位置和旋转
            int x = 20 + i * 30 + random.nextInt(10);
            int y = 35 + random.nextInt(10);
            
            // 随机旋转角度
            double angle = (random.nextDouble() - 0.5) * 0.5;
            g2d.rotate(angle, x, y);
            g2d.drawString(String.valueOf(c), x, y);
            g2d.rotate(-angle, x, y);
        }
        
        // 添加干扰线
        for (int i = 0; i < 5; i++) {
            g2d.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g2d.drawLine(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT),
                    random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT));
        }
        
        // 添加干扰点
        for (int i = 0; i < 20; i++) {
            g2d.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g2d.fillOval(random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT), 2, 2);
        }
        
        g2d.dispose();
        return image;
    }
}