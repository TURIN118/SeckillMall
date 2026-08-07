package com.seckill.mall.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 拦截器配置。
 * <p>
 * H-D1 修复：追加 {@link BlockAttackInnerInterceptor}，禁止无 where 条件的
 * update/delete 操作，防止 UpdateWrapper 漏写 eq 条件触发全表更新/删除。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：MybatisPlusConfig.java
 * 邮箱：nj651217@163.com
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件（需指定 MySQL 方言）
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁插件（实体需配合 @Version 注解生效）
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 防全表更新/删除插件：阻止无 where 条件的 update/delete，作为兜底防线
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        return interceptor;
    }
}
