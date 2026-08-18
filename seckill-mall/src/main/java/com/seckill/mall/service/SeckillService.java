package com.seckill.mall.service;

import com.seckill.mall.seckill.interfaces.vo.SeckillResultVO;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillService.java
 * 邮箱：nj651217@163.com
 */
public interface SeckillService {

    SeckillResultVO doSeckill(Long seckillId, String seckillToken);

    SeckillResultVO getSeckillResult(Long seckillId, String requestId);
}
