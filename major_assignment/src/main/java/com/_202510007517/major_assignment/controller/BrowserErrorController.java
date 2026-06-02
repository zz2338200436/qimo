package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.BrowserError;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.service.BrowserErrorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * 浏览器错误日志Controller
 */
@RestController
@RequestMapping("/api/errors")
public class BrowserErrorController extends BaseController {

    @Autowired
    private BrowserErrorService browserErrorService;

    /**
     * 上报浏览器错误日志
     * @param browserError 浏览器错误日志实体
     * @param request HttpServletRequest对象，用于获取客户端IP和会话信息
     * @return 响应结果
     */
    @PostMapping("/browser")
    public ResponseResult<Long> reportBrowserError(@RequestBody BrowserError browserError, HttpServletRequest request) {
        // 从请求中获取客户端IP
        String clientIp = getClientIp(request);
        browserError.setClientIp(clientIp);
        
        // 从请求中获取会话ID
        String sessionId = request.getSession().getId();
        browserError.setSessionId(sessionId);
        
        // 保存错误日志
        Long errorId = browserErrorService.saveBrowserError(browserError);
        return ResponseResult.success(errorId, "错误日志上报成功", 200);
    }

    /**
     * 批量上报浏览器错误日志
     * @param browserErrors 浏览器错误日志列表
     * @param request HttpServletRequest对象，用于获取客户端IP和会话信息
     * @return 响应结果
     */
    @PostMapping("/browser/batch")
    public ResponseResult<Integer> batchReportBrowserErrors(@RequestBody List<BrowserError> browserErrors, HttpServletRequest request) {
        // 从请求中获取客户端IP
        String clientIp = getClientIp(request);
        
        // 从请求中获取会话ID
        String sessionId = request.getSession().getId();
        
        // 设置每个错误日志的客户端IP和会话ID
        browserErrors.forEach(error -> {
            error.setClientIp(clientIp);
            error.setSessionId(sessionId);
        });
        
        // 批量保存错误日志
        int count = browserErrorService.batchSaveBrowserErrors(browserErrors);
        return ResponseResult.success(count, "错误日志批量上报成功", 200);
    }

    /**
     * 获取客户端IP地址
     * @param request HttpServletRequest对象
     * @return 客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 根据条件查询浏览器错误日志列表
     * @param params 查询条件
     * @param page 页码
     * @param size 每页数量
     * @return 响应结果
     */
    @GetMapping("/browser")
    public ResponseResult<PageResult<BrowserError>> getBrowserErrorList(@RequestParam Map<String, String> params,
                                                                        @RequestParam(defaultValue = "1") int page,
                                                                        @RequestParam(defaultValue = "10") int size) {
        PageResult<BrowserError> result = browserErrorService.getBrowserErrorList(params, page, size);
        return ResponseResult.success(result, "获取错误日志列表成功", 200);
    }

    /**
     * 根据ID查询浏览器错误日志详情
     * @param id 错误日志ID
     * @return 响应结果
     */
    @GetMapping("/browser/{id}")
    public ResponseResult<BrowserError> getBrowserErrorById(@PathVariable Long id) {
        // 查询错误日志详情
        BrowserError error = browserErrorService.getBrowserErrorById(id);
        if (error == null) {
            return ResponseResult.failure("错误日志不存在");
        }
        return ResponseResult.success(error, "获取错误日志详情成功", 200);
    }
}
