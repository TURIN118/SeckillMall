package com.seckill.mall.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：PageResult.java
 * 邮箱：nj651217@163.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {

    private List<T> list;
    private long total;
    private long pageNum;
    private long pageSize;
    private long pages;

    public static <T> PageResult<T> of(List<T> list, long total, long pageNum, long pageSize) {
        long pages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
        return new PageResult<>(list, total, pageNum, pageSize, pages);
    }
}
