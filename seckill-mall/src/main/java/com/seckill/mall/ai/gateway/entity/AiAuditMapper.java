package com.seckill.mall.ai.gateway.entity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 调用审计 Mapper
 * <p>依赖 {@code @Mapper} 注解被 MyBatis-Plus 自动配置扫描（与现有
 * {@code com.seckill.mall.mapper.*Mapper} 同机制，主启动类无 @MapperScan）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiAuditMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface AiAuditMapper extends BaseMapper<AiAudit> {
}