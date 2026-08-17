/**
 * Identity 应用服务层 - ApplicationService 实现 API 接口。
 *
 * <p>采用 Strangler Pattern 委托旧 Service，逐步将业务逻辑迁移到领域层。
 * 供 interfaces 层调用，协调领域对象与基础设施。
 *
 * @author wnj
 * @since Phase I.0
 */
package com.seckill.mall.identity.application;