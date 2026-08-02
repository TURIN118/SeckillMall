package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    User findByUsername(@Param("username") String username);

    User findByPhone(@Param("phone") String phone);

    /**
     * 用户注册趋势：按日期分组统计注册数
     *
     * @param startDate 起始日期（含）
     * @param endDate   结束日期（含）
     * @return 每行包含 dt(日期)、cnt(注册数)
     */
    List<Map<String, Object>> selectUserTrend(@Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    /**
     * 今日注册用户数（create_time 落在当天）
     *
     * @param startDate 当天起始日期
     * @return 注册数
     */
    Long countTodayRegistered(@Param("startDate") LocalDate startDate);
}
