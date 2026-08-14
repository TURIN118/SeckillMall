package com.seckill.mall.ai.customerservice.entity;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 客服对话 Mapper
 * <p>依赖 {@code @Mapper} 注解被 MyBatis-Plus 自动配置扫描
 * （与现有 {@code com.seckill.mall.ai.track.entity.UserEventMapper} 同机制）。
 * <p>单条 insert 会自动填充雪花 ID（{@code IdType.ASSIGN_ID}）与
 * {@code createTime}/{@code updateTime}（{@code MetaObjectHandler}），
 * 逻辑删除由 {@code @TableLogic} 自动改写 SQL，无需自定义 XML。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiConversationMapper.java
 * 邮箱：nj651217@163.com
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}