package com._202510007517.platform.auth.controller;

import com._202510007517.platform.auth.service.CaptchaService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class PublicCaptchaCompatibilityController {

    private final CaptchaService captchaService;

    public PublicCaptchaCompatibilityController(CaptchaService captchaService) {
        this.captchaService = captchaService;
    }

    @GetMapping("/captcha")
    public ResponseEntity<byte[]> captcha() {
        CaptchaService.CaptchaImage captcha = captchaService.createCaptcha();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.noStore())
                .header("X-Captcha-Key", captcha.key())
                .body(captcha.bytes());
    }
}
