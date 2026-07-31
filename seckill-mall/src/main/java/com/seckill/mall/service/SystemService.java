package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.dto.OperationLogQueryRequest;
import com.seckill.mall.vo.DashboardVO;
import com.seckill.mall.vo.OperationLogVO;
import com.seckill.mall.vo.SystemHealthVO;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SystemService.java
 * 邮箱：nj651217@163.com
 */
public interface SystemService {

    DashboardVO getDashboard();

    PageResult<OperationLogVO> getOperationLogs(OperationLogQueryRequest req);

    SystemHealthVO getSystemHealth();
}
