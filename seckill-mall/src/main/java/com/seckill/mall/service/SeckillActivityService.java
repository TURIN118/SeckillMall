package com.seckill.mall.service;

import com.seckill.mall.dto.SeckillActivityCreateRequest;
import com.seckill.mall.seckill.interfaces.vo.SeckillActivityVO;

import java.util.List;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillActivityService.java
 * 邮箱：nj651217@163.com
 * <p>
 * 秒杀场次服务：管理场次 CRUD 与场次下商品的批量预热。
 */
public interface SeckillActivityService {

    /** 创建场次（含场次下所有商品），返回场次 VO */
    SeckillActivityVO createActivity(SeckillActivityCreateRequest req);

    /** 查询所有场次列表（含每场次下商品列表） */
    List<SeckillActivityVO> listActivities();

    /** 查询场次详情（含商品列表） */
    SeckillActivityVO getActivityDetail(Long activityId);

    /** 删除场次（逻辑删除，连带商品不强制删除） */
    void deleteActivity(Long activityId);
}