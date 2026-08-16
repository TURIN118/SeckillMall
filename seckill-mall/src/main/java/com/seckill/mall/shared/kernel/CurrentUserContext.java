package com.seckill.mall.shared.kernel;

/**
 * 当前用户上下文（统一用户身份获取的抽象）
 * <p>业务代码应依赖此接口而非 SecurityUtils 具体实现，
 * 以解耦业务层与 Spring Security 基础设施。
 * <p>Phase 3 渐进式：先只抽取最基本的两个方法，
 * 后续随 identity 模块建立再扩展。
 */
public interface CurrentUserContext {

    /** 获取当前用户 ID */
    Long getCurrentUserId();

    /** 获取当前用户名 */
    String getCurrentUsername();
}