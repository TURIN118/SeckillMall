package com.seckill.mall.exception;

import com.seckill.mall.common.ErrorCode;
import lombok.Getter;

/**
 * 业务异常。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BusinessException.java
 * 邮箱：nj651217@163.com
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}