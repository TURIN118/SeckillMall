package com.seckill.mall.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 充值卡生成视图对象
 * <p>
 * H17 修复：专用于"生成充值卡"接口的返回对象，显式暴露 cardPassword 一次。
 * 普通查询/列表接口应使用 {@link RechargeCardVO}，其 cardPassword 被 @JsonIgnore 屏蔽。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RechargeCardGenerateVO.java
 * 邮箱：nj651217@163.com
 */
@Data
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.ALWAYS)
public class RechargeCardGenerateVO extends RechargeCardVO {

    /**
     * 卡密明文（仅在生成接口返回一次）。
     * 此字段在父类中被 @JsonIgnore 屏蔽，这里通过覆盖 getter 显式开启序列化。
     */
    @Override
    public String getCardPassword() {
        return super.getCardPassword();
    }
}