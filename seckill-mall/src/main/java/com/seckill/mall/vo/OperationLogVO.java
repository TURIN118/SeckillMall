package com.seckill.mall.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OperationLogVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class OperationLogVO {

    private Long id;

    private Long operatorId;

    private String operatorName;

    private String module;

    private String action;

    private String targetId;

    private String targetType;

    private String detail;

    private String ipAddress;

    private LocalDateTime operationTime;
}
