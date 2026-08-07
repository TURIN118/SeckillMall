package com.seckill.mall.converter;

import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.vo.SeckillOrderVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

/**
 * 秒杀订单 entity ↔ VO 转换器（MapStruct）
 * <p>
 * M-D5 修复：启用 MapStruct 替代手工 setXxx，脱敏/枚举描述通过 {@link AfterMapping} 钩子处理。
 * <ul>
 *   <li>{@code isDeleted} 等 entity 内部字段不映射到 VO（屏蔽表结构）</li>
 *   <li>{@code status} 枚举通过 {@link AfterMapping} 转为 code + 中文描述</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderConverter.java
 * 邮箱：nj651217@163.com
 */
@Mapper(componentModel = "spring")
public interface SeckillOrderConverter {

    /** 便捷的静态访问器（与 Spring Bean 共存，便于单元测试） */
    SeckillOrderConverter INSTANCE = Mappers.getMapper(SeckillOrderConverter.class);

    /**
     * entity → VO
     * <p>
     * status 字段在 {@link AfterMapping} 中处理（枚举→code+description），
     * 故此处显式忽略 target.status。
     * <p>
     * entity 的 {@code isDeleted} 字段在 VO 中不存在，MapStruct 自动跳过，
     * 从而屏蔽表结构内部字段。
     */
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "statusDescription", ignore = true)
    SeckillOrderVO toVO(SeckillOrder entity);

    /**
     * 后置映射：将 {@link OrderStatus} 枚举转为 code + 中文描述。
     */
    @AfterMapping
    default void enrichStatus(SeckillOrder entity, @MappingTarget SeckillOrderVO vo) {
        OrderStatus status = entity.getStatus();
        if (status != null) {
            vo.setStatus(status.getCode());
            vo.setStatusDescription(status.getDescription());
        }
    }
}