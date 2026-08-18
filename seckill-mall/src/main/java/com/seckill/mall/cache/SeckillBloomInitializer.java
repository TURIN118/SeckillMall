package com.seckill.mall.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.seckill.infrastructure.entity.SeckillGoods;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillBloomInitializer.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SeckillBloomInitializer implements ApplicationRunner {

    private static final long EXPECTED_ELEMENTS = 10000L;
    private static final double FALSE_POSITIVE_RATE = 0.01;
    /** 分页加载页大小(H14)：避免全表 selectList 导致 OOM */
    private static final int PAGE_SIZE = 1000;

    private final RedissonClient redissonClient;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public void run(ApplicationArguments args) {
        RBloomFilter<Long> bloomFilter =
                redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_BLOOM_GOODS);
        bloomFilter.tryInit(EXPECTED_ELEMENTS, FALSE_POSITIVE_RATE);

        // H14 修复：分页加载且仅查询 ID 字段，避免一次性全表加载导致 OOM
        int pageNum = 1;
        long total = 0;
        try {
            while (true) {
                IPage<SeckillGoods> page = new Page<>(pageNum, PAGE_SIZE);
                LambdaQueryWrapper<SeckillGoods> wrapper = new LambdaQueryWrapper<>();
                wrapper.select(SeckillGoods::getId);
                IPage<SeckillGoods> result = seckillGoodsMapper.selectPage(page, wrapper);
                for (SeckillGoods goods : result.getRecords()) {
                    bloomFilter.add(goods.getId());
                }
                total += result.getRecords().size();
                if (result.getRecords().size() < PAGE_SIZE) {
                    break;
                }
                pageNum++;
            }
            log.info("布隆过滤器初始化完成，加载 {} 个秒杀活动 ID", total);
        } catch (Exception e) {
            log.error("布隆过滤器初始化异常(已加载 {} 个 ID)，继续启动以避免阻塞应用", total, e);
        }
    }
}
