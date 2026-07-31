package com.seckill.mall.vo;

import lombok.Data;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemHealthVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SystemHealthVO {

    private String redis;

    private String database;

    private String mq;

    public boolean isAllHealthy() {
        return "UP".equals(redis) && "UP".equals(database) && "UP".equals(mq);
    }
}
