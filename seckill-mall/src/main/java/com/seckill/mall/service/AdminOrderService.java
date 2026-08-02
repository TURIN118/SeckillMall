package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.AdminOrderQueryRequest;
import com.seckill.mall.vo.AdminOrderVO;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AdminOrderService.java
 * 邮箱：nj651217@163.com
 */
public interface AdminOrderService {

    /**
     * 后台订单高级筛选分页查询
     *
     * @param req 查询条件
     * @return 分页结果
     */
    PageResult<AdminOrderVO> getAdminOrderList(AdminOrderQueryRequest req);
}