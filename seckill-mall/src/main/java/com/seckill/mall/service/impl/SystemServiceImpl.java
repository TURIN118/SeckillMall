package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.mapper.OperationLogMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.service.SystemService;
import com.seckill.mall.vo.DashboardVO;
import com.seckill.mall.vo.OperationLogVO;
import com.seckill.mall.vo.SystemHealthVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemServiceImpl implements SystemService {

    private final UserMapper userMapper;
    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final OperationLogMapper operationLogMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ConnectionFactory rabbitConnectionFactory;

    @Override
    public DashboardVO getDashboard() {
        DashboardVO vo = new DashboardVO();
        vo.setUserCount(userMapper.selectCount(null));

        long orderCount = seckillOrderMapper.selectCount(null);
        vo.setOrderCount(orderCount);

        BigDecimal sales = seckillOrderMapper.sumSalesAmount(List.of(OrderStatus.PAID, OrderStatus.COMPLETED));
        vo.setTotalSales(sales == null ? BigDecimal.ZERO : sales);

        vo.setSeckillCount(seckillGoodsMapper.selectCount(null));
        return vo;
    }

    @Override
    public PageResult<OperationLogVO> getOperationLogs(OperationLogQueryRequest req) {
        int pageNum = req.getPageNum() == null || req.getPageNum() < 1 ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null || req.getPageSize() < 1 ? 10 : req.getPageSize();

        Page<OperationLogVO> page = new Page<>(pageNum, pageSize);
        IPage<OperationLogVO> result = operationLogMapper.selectOperationLogVOPage(
                page, req.getModule(), req.getOperatorId());
        List<OperationLogVO> list = result.getRecords() == null
                ? Collections.emptyList() : result.getRecords();
        return PageResult.of(list, result.getTotal(), result.getCurrent(), result.getSize());
    }

    @Override
    public SystemHealthVO getSystemHealth() {
        SystemHealthVO vo = new SystemHealthVO();
        vo.setRedis(checkRedis());
        vo.setDatabase(checkDatabase());
        vo.setMq(checkMq());
        return vo;
    }

    private String checkRedis() {
        try {
            RedisConnection connection = stringRedisTemplate.getConnectionFactory().getConnection();
            try {
                String pong = connection.ping();
                return "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN";
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("Redis 健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkDatabase() {
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return one != null && one == 1 ? "UP" : "DOWN";
        } catch (Exception e) {
            log.warn("数据库健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkMq() {
        try {
            Connection connection = rabbitConnectionFactory.createConnection();
            try {
                return connection.isOpen() ? "UP" : "DOWN";
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("RabbitMQ 健康检查失败: {}", e.getMessage());
            return "DOWN";
        }
    }
}
