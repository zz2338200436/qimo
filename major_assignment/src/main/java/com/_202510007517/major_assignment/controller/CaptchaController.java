package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.constants.CacheConstants;
import com.google.code.kaptcha.Constants;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Duration;
import java.util.Properties;

@RestController
@RequestMapping("/api/public")
public class CaptchaController {

    private final DefaultKaptcha captchaProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public CaptchaController() {
        // 配置验证码生成器 - 更清晰的配置
        Properties properties = new Properties();
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "105,179,90");
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        properties.setProperty("kaptcha.image.width", "125");
        properties.setProperty("kaptcha.image.height", "45");
        properties.setProperty("kaptcha.textproducer.font.size", "30");
        properties.setProperty("kaptcha.session.key", Constants.KAPTCHA_SESSION_KEY);
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        properties.setProperty("kaptcha.noise.color", "black");
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise"); // 去除噪点
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy"); // 更换为阴影效果
        properties.setProperty("kaptcha.background.clear.from", "white"); // 白色背景
        properties.setProperty("kaptcha.background.clear.to", "white"); // 白色背景
        
        Config config = new Config(properties);
        captchaProducer = new DefaultKaptcha();
        captchaProducer.setConfig(config);
    }

    @GetMapping("/captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 设置响应头
        response.setDateHeader("Expires", 0);
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.addHeader("Cache-Control", "post-check=0, pre-check=0");
        response.setHeader("Pragma", "no-cache");
        response.setContentType("image/jpeg");

        // 生成验证码文本
        String captchaText = captchaProducer.createText();

        // 将验证码文本写入 Redis，命名空间为 CacheConstants.CAPTCHA_NAMESPACE ("CAPTCHA:IMG:")。
        // 与业务会话前缀 "SESSION:" 严格隔离（满足 R2.5 不变量）。
        // 同时以 HttpSession 的 sessionKey 作为 Redis key 的唯一组成部分，保证同一浏览器标签页内可复验。
        HttpSession session = request.getSession(true);
        String redisKey = CacheConstants.CAPTCHA_NAMESPACE + session.getId();
        redisTemplate.opsForValue().set(redisKey, captchaText, Duration.ofSeconds(CacheConstants.CAPTCHA_TTL));

        // 生成验证码图片
        BufferedImage captchaImage = captchaProducer.createImage(captchaText);
        // 输出验证码图片
        ImageIO.write(captchaImage, "jpg", response.getOutputStream());
    }
}
