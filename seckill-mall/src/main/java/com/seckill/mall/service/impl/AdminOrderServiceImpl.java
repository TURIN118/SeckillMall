package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.AdminOrderQueryRequest;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.service.AdminOrderService;
import com.seckill.mall.vo.AdminOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminOrderServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 排序字段白名单：请求值 → 实际列名
     */
    private static final Map<String, String> SORT_BY_WHITELIST = Map.of(
            "createTime", "create_time",
            "payTime", "pay_time",
            "totalAmount", "total_amount");

    private static final Set<String> SORT_ORDER_WHITELIST = Set.of("asc", "desc");

    private final SeckillOrderMapper seckillOrderMapper;

    @Override
    public PageResult<AdminOrderVO> getAdminOrderList(AdminOrderQueryRequest req) {
        // 参数归一化
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : req.getPageSize();

        // 排序字段白名单校验：非白名单回退 create_time
        String sortBy = req.getSortBy();
        String sortByColumn = (sortBy == null)
                ? "create_time"
                : SORT_BY_WHITELIST.getOrDefault(sortBy, "create_time");
        req.setSortByColumn(sortByColumn);

        // 排序方向白名单校验：非白名单回退 desc
        String sortOrder = req.getSortOrder();
        String sortOrderNorm = (sortOrder != null && SORT_ORDER_WHITELIST.contains(sortOrder.toLowerCase()))
                ? sortOrder.toLowerCase()
                : "desc";
        req.setSortOrder(sortOrderNorm);

        // 时间字符串解析为 LocalDateTime（解析失败忽略该条件）
        req.setStartLdt(parseDateTimeQuiet(req.getStartTime()));
        req.setEndLdt(parseDateTimeQuiet(req.getEndTime()));
        req.setPayStartLdt(parseDateTimeQuiet(req.getPayStartTime()));
        req.setPayEndLdt(parseDateTimeQuiet(req.getPayEndTime()));

        // M35 配套：date 字段(yyyy-MM-dd)归一化为 [date 00:00:00, date+1 00:00:00) 范围，
        // 覆盖 startLdt/endLdt，使 Mapper 可走索引范围查询而非 DATE(create_time) 函数扫描
        if (req.getDate() != null && !req.getDate().isBlank()) {
            try {
                LocalDate ld = LocalDate.parse(req.getDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                req.setStartLdt(ld.atStartOfDay());
                req.setEndLdt(ld.plusDays(1).atStartOfDay());
            } catch (Exception e) {
                log.warn("date 解析失败，忽略该筛选条件: date={}, err={}", req.getDate(), e.getMessage());
            }
        }

        // 任务#54：startDate/endDate 日期范围筛选（导出弹窗时间范围）
        // 仅当 date 为空时生效（date 单日筛选优先级更高，避免覆盖冲突）
        // 注意：date 为空时 Mapper 对 endLdt 使用 <= (闭区间)，故 endDate 解析为当天 23:59:59
        if ((req.getDate() == null || req.getDate().isBlank())
                && (req.getStartDate() != null || req.getEndDate() != null)) {
            try {
                if (req.getStartDate() != null && !req.getStartDate().isBlank()) {
                    LocalDate startLd = LocalDate.parse(req.getStartDate(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    req.setStartLdt(startLd.atStartOfDay());
                }
                if (req.getEndDate() != null && !req.getEndDate().isBlank()) {
                    LocalDate endLd = LocalDate.parse(req.getEndDate(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    // endDate 含当天，date 为空时 Mapper 用 <= 闭区间，故设为当天 23:59:59
                    req.setEndLdt(endLd.atTime(23, 59, 59));
                }
            } catch (Exception e) {
                log.warn("startDate/endDate 解析失败，忽略该筛选条件: startDate={}, endDate={}, err={}",
                        req.getStartDate(), req.getEndDate(), e.getMessage());
            }
        }

        Page<AdminOrderVO> page = new Page<>(pageNum, pageSize);
        IPage<AdminOrderVO> result = seckillOrderMapper.selectAdminOrderPage(page, req);
        List<AdminOrderVO> list = result.getRecords() == null
                ? Collections.emptyList() : result.getRecords();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    /**
     * 静默解析时间字符串，失败返回 null（忽略该筛选条件）
     */
    private LocalDateTime parseDateTimeQuiet(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(text, DATETIME_FORMATTER);
        } catch (Exception e) {
            log.warn("时间解析失败，忽略该筛选条件: text={}, err={}", text, e.getMessage());
            return null;
        }
    }
}