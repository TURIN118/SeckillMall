package com.seckill.mall.converter;

import com.seckill.mall.entity.RechargeCard;
import com.seckill.mall.vo.RechargeCardVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 充值卡 entity ↔ VO 转换器（MapStruct）
 * <p>
 * M-D5 修复：启用 MapStruct 替代 {@code RechargeCardServiceImpl.toVO} 手工 setXxx。
 * <ul>
 *   <li>列表查询场景：使用 {@link #toVO}，{@code cardPassword} 显式忽略（脱敏）</li>
 *   <li>生成场景：使用 {@link com.seckill.mall.vo.RechargeCardGenerateVO}，由 Service 手工填充明文卡密</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardConverter.java
 * 邮箱：nj651217@163.com
 */
@Mapper(componentModel = "spring")
public interface RechargeCardConverter {

    /** 便捷的静态访问器（与 Spring Bean 共存，便于单元测试） */
    RechargeCardConverter INSTANCE = Mappers.getMapper(RechargeCardConverter.class);

    /**
     * entity → VO（列表查询场景，脱敏：不映射 cardPassword）
     * <p>
     * {@code status} 枚举通过 MapStruct 内置的枚举→String 转换处理，
     * 此处需自定义映射以调用 {@code getCode()}。
     */
    @Mapping(target = "cardPassword", ignore = true)
    @Mapping(target = "status", expression = "java(entity.getStatus() == null ? null : entity.getStatus().getCode())")
    RechargeCardVO toVO(RechargeCard entity);
}