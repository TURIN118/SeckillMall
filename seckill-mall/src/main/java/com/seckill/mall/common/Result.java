package com.seckill.mall.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：Result.java
 * 邮箱：nj651217@163.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private int code;
    private String message;
    private T data;
    private String timestamp;

    private static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static <T> Result<T> success() {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null, now());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, now());
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ErrorCode.SUCCESS.getCode(), message, data, now());
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null, now());
    }

    public static <T> Result<T> error(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null, now());
    }
}
