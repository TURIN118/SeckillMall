package com.seckill.mall.cache;

import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private final RedissonClient redissonClient;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public void run(ApplicationArguments args) {
        RBloomFilter<Long> bloomFilter =
                redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_BLOOM_GOODS);
        bloomFilter.tryInit(EXPECTED_ELEMENTS, FALSE_POSITIVE_RATE);

        List<SeckillGoods> all = seckillGoodsMapper.selectList(null);
        for (SeckillGoods goods : all) {
            bloomFilter.add(goods.getId());
        }
        log.info("布隆过滤器初始化完成，加载 {} 个秒杀活动 ID", all.size());
    }
}
