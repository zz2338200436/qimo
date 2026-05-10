package com._202510007517.major_assignment.controller;

import com._202510007517.major_assignment.entity.EarlyWarning;
import com._202510007517.major_assignment.entity.dto.ResponseResult;
import com._202510007517.major_assignment.entity.dto.WarningStatsDTO;
import com._202510007517.major_assignment.entity.dto.PageResult;
import com._202510007517.major_assignment.entity.dto.WarningStatusUpdateDTO;
import com._202510007517.major_assignment.service.EarlyWarningService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class EarlyWarningController extends BaseController {
    
    @Autowired
    private EarlyWarningService earlyWarningService;
    
    @GetMapping("/early-warnings/teacher/pending")
    public ResponseResult<List<EarlyWarning>> getUnresolvedWarnings(HttpSession session) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long teacherId = getCurrentUserId(session);
        List<EarlyWarning> warnings = earlyWarningService.getUnresolvedWarnings(teacherId);
        
        return ResponseResult.success(warnings, "获取未处理预警数据成功", 200);
    }
    
    @GetMapping("/early-warnings/teacher/stats")
    public ResponseResult<WarningStatsDTO> getWarningStats(HttpSession session,
                                                          @RequestParam(required = false) Long classId,
                                                          @RequestParam(required = false) Long courseId) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long teacherId = getCurrentUserId(session);
        WarningStatsDTO stats = earlyWarningService.getWarningStats(teacherId, classId, courseId);
        
        return ResponseResult.success(stats, "获取预警统计数据成功", 200);
    }
    
    @GetMapping("/early-warnings/teacher/list")
    public ResponseResult<PageResult<EarlyWarning>> getWarningList(HttpSession session,
                                                                 @RequestParam(required = false) Long classId,
                                                                 @RequestParam(required = false) Long courseId,
                                                                 @RequestParam(required = false) String warningType,
                                                                 @RequestParam(required = false) String status,
                                                                 @RequestParam(defaultValue = "1") Integer page,
                                                                 @RequestParam(defaultValue = "10") Integer size) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long teacherId = getCurrentUserId(session);
        PageResult<EarlyWarning> result = earlyWarningService.getWarningList(teacherId, classId, courseId,
                                                                           warningType, status, page, size);
        
        return ResponseResult.success(result, "获取预警列表成功", 200);
    }
    
    @GetMapping("/early-warnings/teacher/detail/{warningId}")
    public ResponseResult<EarlyWarning> getWarningDetail(HttpSession session,
                                                        @PathVariable Long warningId) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        EarlyWarning warning = earlyWarningService.getWarningDetail(warningId);
        if (warning == null) {
            return ResponseResult.failure("预警不存在", 404);
        }
        
        return ResponseResult.success(warning, "获取预警详情成功", 200);
    }
    
    @GetMapping("/teacher/early-warnings/course/{courseId}")
    public ResponseResult<List<EarlyWarning>> getCourseEarlyWarnings(HttpSession session,
                                                                    @PathVariable Long courseId,
                                                                    @RequestParam(required = false) String warningType,
                                                                    @RequestParam(required = false) String warningLevel,
                                                                    @RequestParam(required = false) Boolean isResolved) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        List<EarlyWarning> warnings = earlyWarningService.getByCourseId(courseId, warningType, warningLevel, isResolved);
        return ResponseResult.success(warnings, "获取课程学情预警列表成功", 200);
    }
    
    @PutMapping("/early-warnings/teacher/status/{warningId}")
    public ResponseResult<Void> updateWarningStatus(HttpSession session,
                                                  @PathVariable Long warningId,
                                                  @RequestBody WarningStatusUpdateDTO updateDTO) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long userId = getCurrentUserId(session);
        boolean success = earlyWarningService.updateWarningStatus(warningId, updateDTO, userId);
        if (success) {
            return ResponseResult.success(null, "更新预警状态成功", 200);
        } else {
            return ResponseResult.failure("更新预警状态失败", 500);
        }
    }
    
    @PutMapping("/teacher/early-warnings/{warningId}/resolve")
    public ResponseResult<Void> resolveWarning(HttpSession session,
                                             @PathVariable Long warningId,
                                             @RequestBody WarningStatusUpdateDTO updateDTO) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        Long userId = getCurrentUserId(session);
        // 设置状态为已处理
        updateDTO.setStatus("resolved");
        boolean success = earlyWarningService.updateWarningStatus(warningId, updateDTO, userId);
        if (success) {
            return ResponseResult.success(null, "处理学情预警成功", 200);
        } else {
            return ResponseResult.failure("处理学情预警失败", 500);
        }
    }
    
    @PostMapping("/early-warnings/teacher")
    public ResponseResult<EarlyWarning> addEarlyWarning(HttpSession session, @RequestBody EarlyWarning earlyWarning) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        // 设置创建者ID为当前登录用户ID
        Long teacherId = getCurrentUserId(session);
        earlyWarning.setTeacherId(teacherId);
        
        boolean success = earlyWarningService.addEarlyWarning(earlyWarning);
        if (success) {
            return ResponseResult.success(earlyWarning, "添加预警成功", 201);
        } else {
            return ResponseResult.failure("添加预警失败", 500);
        }
    }
    
    @DeleteMapping("/early-warnings/teacher/{warningId}")
    public ResponseResult<Void> deleteEarlyWarning(HttpSession session, @PathVariable Long warningId) {
        if (!isLoggedIn(session)) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        
        boolean success = earlyWarningService.deleteEarlyWarning(warningId);
        if (success) {
            return ResponseResult.success(null, "删除预警成功", 200);
        } else {
            return ResponseResult.failure("删除预警失败", 500);
        }
    }
    
    @GetMapping("/early-warnings/teacher/export")
    public void exportWarnings(HttpSession session,
                             @RequestParam(required = false) Long classId,
                             @RequestParam(required = false) Long courseId,
                             @RequestParam(required = false) String warningType,
                             @RequestParam(required = false) String status,
                             HttpServletResponse response) throws IOException {
        if (!isLoggedIn(session)) {
            response.sendError(401, "未授权，请重新登录");
            return;
        }
        
        Long teacherId = getCurrentUserId(session);
        List<EarlyWarning> warnings = earlyWarningService.getWarningsForExport(teacherId, classId, courseId,
                                                                             warningType, status);
        
        // 创建Excel工作簿
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("预警数据");
        
        // 创建表头
        Row headerRow = sheet.createRow(0);
        String[] headers = {"预警ID", "学生姓名", "课程名称", "预警类型", "预警级别", "预警消息", "触发时间", "状态", "处理备注"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }
        
        // 填充数据
        for (int i = 0; i < warnings.size(); i++) {
            EarlyWarning warning = warnings.get(i);
            Row dataRow = sheet.createRow(i + 1);
            
            dataRow.createCell(0).setCellValue(warning.getId());
            dataRow.createCell(1).setCellValue(warning.getStudentName());
            dataRow.createCell(2).setCellValue(warning.getCourseName());
            dataRow.createCell(3).setCellValue(warning.getWarningType());
            dataRow.createCell(4).setCellValue(warning.getWarningLevel());
            dataRow.createCell(5).setCellValue(warning.getWarningMessage());
            dataRow.createCell(6).setCellValue(warning.getTriggerDate().toString());
            dataRow.createCell(7).setCellValue(warning.getStatus());
            dataRow.createCell(8).setCellValue(warning.getResolvedNote() != null ? warning.getResolvedNote() : "");
        }
        
        // 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=预警数据.xlsx");
        
        // 输出Excel
        workbook.write(response.getOutputStream());
        workbook.close();
    }
}