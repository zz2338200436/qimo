package com._202510007517.platform.analysis.controller;

import com._202510007517.platform.analysis.service.EarlyWarningCompatibilityService;
import com._202510007517.platform.analysis.controller.dto.EarlyWarningDTO;
import com._202510007517.platform.analysis.controller.dto.EarlyWarningPageResult;
import com._202510007517.platform.analysis.controller.dto.WarningStatsDTO;
import com._202510007517.platform.common.web.CommonTraceConstants;
import com._202510007517.platform.common.web.ResponseResult;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class EarlyWarningCompatibilityController {

    private final EarlyWarningCompatibilityService earlyWarningCompatibilityService;

    public EarlyWarningCompatibilityController(EarlyWarningCompatibilityService earlyWarningCompatibilityService) {
        this.earlyWarningCompatibilityService = earlyWarningCompatibilityService;
    }

    @GetMapping("/early-warnings/teacher/pending")
    public ResponseResult<List<EarlyWarningDTO>> getPendingWarnings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(
                earlyWarningCompatibilityService.listPendingWarnings(resolveTeacherId(userIdHeader)),
                "获取未处理预警数据成功",
                200);
    }

    @GetMapping("/student/early-warnings")
    public ResponseResult<List<EarlyWarningDTO>> getStudentEarlyWarnings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader) {
        return ResponseResult.success(
                earlyWarningCompatibilityService.listStudentWarnings(resolveStudentId(userIdHeader)),
                "获取学情预警列表成功",
                200);
    }

    @GetMapping("/early-warnings/teacher/stats")
    public ResponseResult<WarningStatsDTO> getWarningStats(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId) {
        return ResponseResult.success(
                earlyWarningCompatibilityService.getStats(resolveTeacherId(userIdHeader), classId, courseId),
                "获取预警统计数据成功",
                200);
    }

    @GetMapping("/early-warnings/teacher/list")
    public ResponseResult<EarlyWarningPageResult> getWarningList(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return ResponseResult.success(
                earlyWarningCompatibilityService.listWarnings(
                        resolveTeacherId(userIdHeader),
                        classId,
                        courseId,
                        warningType,
                        status,
                        page,
                        size),
                "获取预警列表成功",
                200);
    }

    @GetMapping("/early-warnings/teacher/detail/{warningId}")
    public ResponseResult<EarlyWarningDTO> getWarningDetail(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long warningId) {
        return ResponseResult.success(
                earlyWarningCompatibilityService.getDetail(resolveTeacherId(userIdHeader), warningId),
                "获取预警详情成功",
                200);
    }

    @GetMapping("/teacher/early-warnings/course/{courseId}")
    public ResponseResult<List<EarlyWarningDTO>> getCourseEarlyWarnings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long courseId,
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) String warningLevel,
            @RequestParam(required = false) Boolean isResolved) {
        return ResponseResult.success(
                earlyWarningCompatibilityService.listCourseWarnings(
                        resolveTeacherId(userIdHeader),
                        courseId,
                        warningType,
                        warningLevel,
                        isResolved),
                "获取课程学情预警列表成功",
                200);
    }

    @PutMapping("/early-warnings/teacher/status/{warningId}")
    public ResponseResult<Void> updateWarningStatus(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long warningId,
            @RequestBody Map<String, Object> body) {
        boolean success = earlyWarningCompatibilityService.updateStatus(
                resolveTeacherId(userIdHeader),
                warningId,
                stringValue(body.get("status")),
                stringValue(body.get("resolvedNote")));
        return success
                ? ResponseResult.success(null, "更新预警状态成功", 200)
                : ResponseResult.failure("更新预警状态失败", 500);
    }

    @PutMapping("/teacher/early-warnings/{warningId}/resolve")
    public ResponseResult<Void> resolveWarning(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long warningId,
            @RequestBody Map<String, Object> body) {
        boolean success = earlyWarningCompatibilityService.updateStatus(
                resolveTeacherId(userIdHeader),
                warningId,
                "resolved",
                stringValue(body.get("resolvedNote")));
        return success
                ? ResponseResult.success(null, "处理学情预警成功", 200)
                : ResponseResult.failure("处理学情预警失败", 500);
    }

    @PostMapping("/early-warnings/teacher")
    public ResponseResult<EarlyWarningDTO> addEarlyWarning(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody Map<String, Object> body) {
        EarlyWarningDTO warning = earlyWarningCompatibilityService.createWarning(
                resolveTeacherId(userIdHeader),
                longValue(body.get("studentId")),
                longValue(body.get("courseId")),
                stringValue(body.get("warningType")),
                stringValue(body.get("warningLevel")),
                stringValue(body.get("warningMessage")),
                stringValue(body.get("assessmentType")),
                longValue(body.get("relatedAssessmentId")));
        return ResponseResult.success(warning, "添加预警成功", 201);
    }

    @DeleteMapping("/early-warnings/teacher/{warningId}")
    public ResponseResult<Void> deleteEarlyWarning(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long warningId) {
        boolean success = earlyWarningCompatibilityService.deleteWarning(resolveTeacherId(userIdHeader), warningId);
        return success
                ? ResponseResult.success(null, "删除预警成功", 200)
                : ResponseResult.failure("删除预警失败", 500);
    }

    @GetMapping("/early-warnings/teacher/export")
    public void exportWarnings(
            @RequestHeader(value = CommonTraceConstants.USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String warningType,
            @RequestParam(required = false) String status,
            HttpServletResponse response) throws IOException {
        List<EarlyWarningDTO> warnings = earlyWarningCompatibilityService.listWarningsForExport(
                resolveTeacherId(userIdHeader),
                classId,
                courseId,
                warningType,
                status);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=early-warnings.xlsx");
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("预警数据");
            writeHeader(workbook, sheet);
            writeRows(sheet, warnings);
            workbook.write(response.getOutputStream());
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult<Void> handleBadRequest(IllegalArgumentException ex) {
        if ("缺少教师身份".equals(ex.getMessage()) || "缺少学生身份".equals(ex.getMessage())) {
            return ResponseResult.failure("未授权，请重新登录", 401);
        }
        return ResponseResult.failure(ex.getMessage(), 400);
    }

    @ExceptionHandler(EarlyWarningCompatibilityService.WarningNotFoundException.class)
    public ResponseResult<Void> handleNotFound() {
        return ResponseResult.failure("预警不存在", 404);
    }

    private static void writeHeader(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {"预警ID", "学生姓名", "课程名称", "预警类型", "预警级别", "预警消息", "触发时间", "状态", "处理备注"};
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        for (int index = 0; index < headers.length; index++) {
            Cell cell = headerRow.createCell(index);
            cell.setCellValue(headers[index]);
            cell.setCellStyle(style);
        }
    }

    private static void writeRows(Sheet sheet, List<EarlyWarningDTO> warnings) {
        for (int index = 0; index < warnings.size(); index++) {
            EarlyWarningDTO warning = warnings.get(index);
            Row row = sheet.createRow(index + 1);
            row.createCell(0).setCellValue(warning.id());
            row.createCell(1).setCellValue(nullToEmpty(warning.studentName()));
            row.createCell(2).setCellValue(nullToEmpty(warning.courseName()));
            row.createCell(3).setCellValue(nullToEmpty(warning.warningType()));
            row.createCell(4).setCellValue(nullToEmpty(warning.warningLevel()));
            row.createCell(5).setCellValue(nullToEmpty(warning.warningMessage()));
            row.createCell(6).setCellValue(warning.triggerDate() == null ? "" : warning.triggerDate().toString());
            row.createCell(7).setCellValue(nullToEmpty(warning.status()));
            row.createCell(8).setCellValue(nullToEmpty(warning.resolvedNote()));
        }
    }

    private static Long resolveTeacherId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少教师身份");
        }
        return Long.valueOf(userIdHeader);
    }

    private static Long resolveStudentId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("缺少学生身份");
        }
        return Long.valueOf(userIdHeader);
    }

    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private static Long longValue(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Long.valueOf(value.toString());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
