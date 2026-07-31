package com.seckill.mall.dto;

import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OperationLogQueryRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OperationLogQueryRequest {

    private Integer pageNum = 1;

    private Integer pageSize = 10;

    private String module;

    private Long operatorId;
}
