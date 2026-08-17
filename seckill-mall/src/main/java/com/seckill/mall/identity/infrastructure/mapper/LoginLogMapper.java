package com.seckill.mall.identity.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.identity.infrastructure.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 登录日志 Mapper。
 *
 * <p>从 {@code com.seckill.mall.mapper.LoginLogMapper} 迁移至 {@code identity.infrastructure.mapper}。
 * 仅在 identity 模块 infrastructure 层内部使用，不对外暴露。
 *
 * @author WNJ
 * @since Phase I.3
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

    List<LoginLog> selectByUserId(@Param("userId") Long userId);
}