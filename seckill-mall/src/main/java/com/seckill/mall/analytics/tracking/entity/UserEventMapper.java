package com.seckill.mall.analytics.tracking.entity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户行为埋点 Mapper
 * <p>依赖 {@code @Mapper} 注解被 MyBatis-Plus 自动配置扫描（与现有
 * {@code com.seckill.mall.mapper.*Mapper} 同机制，主启动类无 @MapperScan）。
 * <p>批量落库采用循环 {@link #insert(Object)} 方案：MyBatis-Plus 单条 insert 会自动
 * 填充雪花 ID（{@code IdType.ASSIGN_ID}）与 {@code createTime}（MetaObjectHandler），
 * 无需自定义 XML，更简单可靠。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：UserEventMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface UserEventMapper extends BaseMapper<UserEvent> {
}