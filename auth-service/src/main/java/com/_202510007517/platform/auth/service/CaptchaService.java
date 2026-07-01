package com._202510007517.platform.auth.service;

import com._202510007517.platform.auth.config.AuthProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
public class CaptchaService {

    private static final char[] CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(2);

    private final AuthProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();

    public CaptchaService(AuthProperties properties, StringRedisTemplate redisTemplate) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public CaptchaImage createCaptcha() {
        String key = UUID.randomUUID().toString();
        String code = randomCode();
        redisTemplate.opsForValue().set(captchaKey(key), code, CAPTCHA_TTL);
        return new CaptchaImage(key, render(code));
    }

    public boolean verify(String captchaKey, String input) {
        if (captchaKey == null || captchaKey.isBlank() || input == null || input.isBlank()) {
            return false;
        }
        String key = captchaKey(captchaKey);
        String expected = redisTemplate.opsForValue().get(key);
        if (expected == null) {
            return false;
        }
        boolean matched = expected.equalsIgnoreCase(input.trim());
        if (matched) {
            redisTemplate.delete(key);
        }
        return matched;
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            builder.append(CHARS[random.nextInt(CHARS.length)]);
        }
        return builder.toString();
    }

    private byte[] render(String code) {
        BufferedImage image = new BufferedImage(120, 42, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(245, 247, 250));
            g.fillRect(0, 0, 120, 42);
            g.setColor(new Color(45, 55, 72));
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 28));
            g.drawString(code, 18, 30);
            for (int i = 0; i < 8; i++) {
                g.setColor(new Color(140 + random.nextInt(80), 140 + random.nextInt(80), 140 + random.nextInt(80)));
                g.drawLine(random.nextInt(120), random.nextInt(42), random.nextInt(120), random.nextInt(42));
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "jpg", output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("验证码图片生成失败", ex);
        }
    }

    private String captchaKey(String key) {
        return properties.getCaptchaPrefix() + key;
    }

    public record CaptchaImage(String key, byte[] bytes) {
    }
}
