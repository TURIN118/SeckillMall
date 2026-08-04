package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.service.SystemService;
import com.seckill.mall.vo.DashboardVO;
import com.seckill.mall.vo.OperationLogVO;
import com.seckill.mall.vo.OrderStatusDistributionVO;
import com.seckill.mall.vo.OrderTrendVO;
import com.seckill.mall.vo.SystemHealthVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemController.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Tag(name = "系统管理", description = "仪表盘/日志/健康检查")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemController {

    private static final DateTimeFormatter EXPORT_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SystemService systemService;

    @Operation(summary = "仪表盘统计")
    @GetMapping("/dashboard")
    public Result<DashboardVO> dashboard() {
        return Result.success(systemService.getDashboard());
    }

    @Operation(summary = "操作日志列表（分页+模块筛选）")
    @GetMapping("/operation-logs")
    public Result<PageResult<OperationLogVO>> operationLogs(OperationLogQueryRequest req) {
        return Result.success(systemService.getOperationLogs(req));
    }

    @Operation(summary = "导出操作日志为 Excel（最多 10000 条）")
    @OperationLog(module = "LOG", action = "EXPORT", targetType = "LOG")
    @GetMapping("/operation-logs/export")
    public void exportLogs(
            @RequestParam(required = false) String module,
            HttpServletResponse response) throws IOException {

        // 安全修复（M10）：限制单次导出条数上限，防止超大结果集导致 OOM/DoS。
        // Service 层 listAllForExport 内部已限制最多 10000 条；此处再次校验作为兜底，
        // 并在超过阈值时仅导出前 10000 条，避免恶意条件触发全表扫描。
        final int EXPORT_LIMIT = 10000;

        // 1. 查询所有符合条件的日志（不分页，最多 10000 条）
        List<OperationLogVO> logs = systemService.listAllForExport(module);
        if (logs.size() > EXPORT_LIMIT) {
            logs = logs.subList(0, EXPORT_LIMIT);
        }

        // 2. 设置响应头
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=operation_logs.xlsx");

        // 3. 生成 Excel
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");

            // 3.1 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // 3.2 创建表头
            String[] headers = {"操作人", "模块", "操作", "目标", "IP地址", "操作时间"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 3.3 填充数据
            for (int i = 0; i < logs.size(); i++) {
                Row row = sheet.createRow(i + 1);
                OperationLogVO logVo = logs.get(i);
                row.createCell(0).setCellValue(nullToEmpty(logVo.getOperatorName()));
                row.createCell(1).setCellValue(nullToEmpty(logVo.getModule()));
                row.createCell(2).setCellValue(nullToEmpty(logVo.getAction()));
                row.createCell(3).setCellValue(formatTarget(logVo));
                row.createCell(4).setCellValue(nullToEmpty(logVo.getIpAddress()));
                row.createCell(5).setCellValue(logVo.getOperationTime() == null
                        ? "" : logVo.getOperationTime().format(EXPORT_DATETIME_FORMATTER));
            }

            // 3.4 自动列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // 3.5 冻结首行
            sheet.createFreezePane(0, 1);

            // 3.6 自动筛选（含表头行）
            int lastRow = Math.max(0, logs.size());
            sheet.setAutoFilter(new CellRangeAddress(0, lastRow, 0, headers.length - 1));

            // 4. 写出
            workbook.write(response.getOutputStream());
        }
    }

    /** 安全转字符串，null 转空串 */
    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 格式化目标列：优先 detail，其次 targetType (ID:targetId)，再次 ID:targetId */
    private static String formatTarget(OperationLogVO logVo) {
        if (logVo.getDetail() != null && !logVo.getDetail().isEmpty()) {
            return logVo.getDetail();
        }
        String targetId = logVo.getTargetId();
        String targetType = logVo.getTargetType();
        if (targetId != null && !targetId.isEmpty() && targetType != null && !targetType.isEmpty()) {
            return targetType + " (ID:" + targetId + ")";
        }
        if (targetId != null && !targetId.isEmpty()) {
            return "ID:" + targetId;
        }
        return "—";
    }

    @Operation(summary = "系统健康检查")
    @GetMapping("/system/health")
    public Result<SystemHealthVO> systemHealth() {
        return Result.success(systemService.getSystemHealth());
    }

    @Operation(summary = "近N天订单趋势（按日期分组统计订单数与销售额）")
    @GetMapping("/dashboard/order-trend")
    public Result<OrderTrendVO> orderTrend(@RequestParam(required = false) Integer days) {
        return Result.success(systemService.getOrderTrend(days));
    }

    @Operation(summary = "订单状态分布（按状态分组统计数量与占比）")
    @GetMapping("/dashboard/order-status-distribution")
    public Result<OrderStatusDistributionVO> orderStatusDistribution(
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return Result.success(systemService.getOrderStatusDistribution(startTime, endTime));
    }
}
