package com.seckill.mall.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 架构规则测试（Phase 1：阻止架构继续恶化）
 *
 * <p>本测试使用 ArchUnit 的 freeze 模式：
 * <ul>
 *   <li>首次运行时，自动将当前所有违规记录到 {@code src/test/resources/archunit-frozen-violations/} 目录</li>
 *   <li>后续运行只检测<b>新增</b>违规，已冻结的违规不会导致测试失败</li>
 *   <li>当某条违规被重构消除后，freeze 文件会自动同步缩减（不可逆，再次引入会失败）</li>
 * </ul>
 *
 * <p>配置见 {@code src/test/resources/archunit.properties}。
 *
 * <p>规则分三组：
 * <ol>
 *   <li><b>分层依赖规则</b>：约束 controller / service / mapper / entity / vo / dto 之间的依赖方向</li>
 *   <li><b>AI 模块保护规则</b>：保护 ai.gateway / ai.track 等已有良好结构</li>
 *   <li><b>基础设施泄露规则</b>：禁止业务层直接依赖 RedisTemplate / RabbitTemplate 等基础设施组件</li>
 * </ol>
 */
@AnalyzeClasses(packages = "com.seckill.mall")
class ArchitectureRulesTest {

    // ============================================================
    // A. 分层依赖规则（基础层规则）
    // ============================================================

    /** 1. Controller 不应直接依赖 Mapper（Controller 应通过 Service 访问数据） */
    @ArchTest
    static final ArchRule controller_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..mapper..")
            .as("Controller 不应直接依赖 Mapper，应通过 Service 访问数据"));

    /** 2. Controller 不应直接依赖 Entity（Controller 应通过 Service 返回 VO/DTO） */
    @ArchTest
    static final ArchRule controller_should_not_depend_on_entity =
        freeze(noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..entity..")
            .as("Controller 不应直接依赖 Entity，应通过 Service 返回 VO/DTO"));

    /** 3a. Entity 不应依赖 Service（数据对象不依赖业务） */
    @ArchTest
    static final ArchRule entity_should_not_depend_on_service =
        freeze(noClasses().that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAPackage("..service..")
            .as("Entity 不应依赖 Service，数据对象不应依赖业务层"));

    /** 3b. Entity 不应依赖 Controller（数据对象不依赖业务） */
    @ArchTest
    static final ArchRule entity_should_not_depend_on_controller =
        freeze(noClasses().that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .as("Entity 不应依赖 Controller，数据对象不应依赖表现层"));

    /** 3c. Entity 不应依赖 Mapper（数据对象不依赖数据访问层） */
    @ArchTest
    static final ArchRule entity_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAPackage("..mapper..")
            .as("Entity 不应依赖 Mapper，数据对象不应依赖数据访问层"));

    /** 4a. Entity 不应依赖 VO（数据库对象不依赖输出对象） */
    @ArchTest
    static final ArchRule entity_should_not_depend_on_vo =
        freeze(noClasses().that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAPackage("..vo..")
            .as("Entity 不应依赖 VO，数据库对象不应依赖输出对象"));

    /** 4b. Entity 不应依赖 DTO（数据库对象不依赖输入对象） */
    @ArchTest
    static final ArchRule entity_should_not_depend_on_dto =
        freeze(noClasses().that().resideInAPackage("..entity..")
            .should().dependOnClassesThat().resideInAPackage("..dto..")
            .as("Entity 不应依赖 DTO，数据库对象不应依赖输入对象"));

    /** 5. VO 不应依赖 Entity（输出对象不依赖数据库对象） */
    @ArchTest
    static final ArchRule vo_should_not_depend_on_entity =
        freeze(noClasses().that().resideInAPackage("..vo..")
            .should().dependOnClassesThat().resideInAPackage("..entity..")
            .as("VO 不应依赖 Entity，输出对象不应依赖数据库对象"));

    /** 6. DTO 不应依赖 Entity（输入对象不依赖数据库对象） */
    @ArchTest
    static final ArchRule dto_should_not_depend_on_entity =
        freeze(noClasses().that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAPackage("..entity..")
            .as("DTO 不应依赖 Entity，输入对象不应依赖数据库对象"));

    /** 7a. Mapper 不应依赖 Service（数据层不依赖业务层） */
    @ArchTest
    static final ArchRule mapper_should_not_depend_on_service =
        freeze(noClasses().that().resideInAPackage("..mapper..")
            .should().dependOnClassesThat().resideInAPackage("..service..")
            .as("Mapper 不应依赖 Service，数据访问层不应依赖业务层"));

    /** 7b. Mapper 不应依赖 Controller（数据层不依赖表现层） */
    @ArchTest
    static final ArchRule mapper_should_not_depend_on_controller =
        freeze(noClasses().that().resideInAPackage("..mapper..")
            .should().dependOnClassesThat().resideInAPackage("..controller..")
            .as("Mapper 不应依赖 Controller，数据访问层不应依赖表现层"));

    // ============================================================
    // B. AI 模块保护规则（保护已有的良好结构）
    // ============================================================

    /** 8a. ai.gateway 不应依赖 ai.assistant（Gateway 是底层，不依赖上层 AI 业务） */
    @ArchTest
    static final ArchRule ai_gateway_should_not_depend_on_ai_assistant =
        freeze(noClasses().that().resideInAPackage("..ai.gateway..")
            .should().dependOnClassesThat().resideInAPackage("..ai.assistant..")
            .as("ai.gateway 不应依赖 ai.assistant，Gateway 是底层不应依赖上层 AI 业务"));

    /** 8b. ai.gateway 不应依赖 ai.customerservice（Gateway 是底层，不依赖上层 AI 业务） */
    @ArchTest
    static final ArchRule ai_gateway_should_not_depend_on_ai_customerservice =
        freeze(noClasses().that().resideInAPackage("..ai.gateway..")
            .should().dependOnClassesThat().resideInAPackage("..ai.customerservice..")
            .as("ai.gateway 不应依赖 ai.customerservice，Gateway 是底层不应依赖上层 AI 业务"));

    /** 8c. ai.gateway 不应依赖 ai.aigc（Gateway 是底层，不依赖上层 AI 业务） */
    @ArchTest
    static final ArchRule ai_gateway_should_not_depend_on_ai_aigc =
        freeze(noClasses().that().resideInAPackage("..ai.gateway..")
            .should().dependOnClassesThat().resideInAPackage("..ai.aigc..")
            .as("ai.gateway 不应依赖 ai.aigc，Gateway 是底层不应依赖上层 AI 业务"));

    /** 9. analytics.tracking 不应依赖 service.impl（Tracking 是旁路，不依赖业务实现） */
    @ArchTest
    static final ArchRule analytics_tracking_should_not_depend_on_service_impl =
        freeze(noClasses().that().resideInAPackage("..analytics.tracking..")
            .should().dependOnClassesThat().resideInAPackage("..service.impl..")
            .as("analytics.tracking 不应依赖 service.impl，Tracking 是旁路不应依赖业务实现"));

    // ============================================================
    // C. 基础设施泄露规则
    // ============================================================

    /** 10. Service 不应直接依赖 RabbitTemplate（应通过 mq.producer 封装） */
    @ArchTest
    static final ArchRule service_should_not_depend_on_rabbit_template =
        freeze(noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.amqp.core.RabbitTemplate")
            .as("Service 不应直接依赖 RabbitTemplate，应通过 mq.producer 封装"));

    /** 10b. Service 不应直接依赖 StringRedisTemplate（应通过 CachePort/RedisService 封装） */
    @ArchTest
    static final ArchRule service_should_not_depend_on_string_redis_template =
        freeze(noClasses().that().resideInAPackage("..service..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.data.redis.core.StringRedisTemplate")
            .as("Service 不应直接依赖 StringRedisTemplate，应通过 CachePort/RedisService 封装"));

    /** 11a. Controller 不应直接依赖 RedisTemplate（应通过 Service） */
    @ArchTest
    static final ArchRule controller_should_not_depend_on_redis_template =
        freeze(noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.data.redis.core.RedisTemplate")
            .as("Controller 不应直接依赖 RedisTemplate，应通过 Service 访问缓存"));

    /** 11b. Controller 不应直接依赖 StringRedisTemplate（应通过 Service） */
    @ArchTest
    static final ArchRule controller_should_not_depend_on_string_redis_template =
        freeze(noClasses().that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
            .haveFullyQualifiedName("org.springframework.data.redis.core.StringRedisTemplate")
            .as("Controller 不应直接依赖 StringRedisTemplate，应通过 Service 访问缓存"));

    // ============================================================
    // D. Shared Kernel 保护规则
    // ============================================================

    /** 12. shared.kernel 不应依赖业务模块（service/controller/mapper/entity/dto/vo）
     *      Shared Kernel 是最稳定的共享内核，不能被业务污染 */
    @ArchTest
    static final ArchRule shared_kernel_should_not_depend_on_business =
        freeze(noClasses().that().resideInAPackage("..shared.kernel..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..mapper..", "..entity..", "..dto..", "..vo..")
            .as("shared.kernel 不应依赖业务模块，Shared Kernel 是最稳定的共享内核不能被业务污染"));

    /** 13. shared.kernel.port 不应依赖 Spring/Redis/RabbitMQ 基础设施
     *      Port 接口必须保持技术中立 */
    @ArchTest
    static final ArchRule shared_kernel_port_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..shared.kernel.port..")
            .should().dependOnClassesThat().resideInAnyPackage("..spring..", "..redis..", "..amqp..")
            .as("shared.kernel.port 不应依赖 Spring/Redis/RabbitMQ 基础设施，Port 接口必须保持技术中立"));

    // ============================================================
    // E. Order 模块包边界规则（Phase 3.7 新增）
    // ============================================================

    /** 14. order.api 不应依赖 order.infrastructure（API 契约层不应依赖基础设施层） */
    @ArchTest
    static final ArchRule order_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..order.api..")
            .should().dependOnClassesThat().resideInAPackage("..order.infrastructure..")
            .as("order.api 不应依赖 order.infrastructure，API 契约层不应依赖基础设施层"));

    /** 15. order.application 不应直接依赖 Mapper（Application 层应通过 Service/Repository 访问数据） */
    @ArchTest
    static final ArchRule order_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..order.application..")
            .should().dependOnClassesThat().resideInAPackage("..order.infrastructure.persistence.mapper..")
            .as("order.application 不应直接依赖 Mapper，应通过 Service/Repository 访问数据"));

    /** 16. order.interfaces 不应直接依赖 Mapper（接口层应通过 ApplicationService 访问数据） */
    @ArchTest
    static final ArchRule order_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..order.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..order.infrastructure.persistence.mapper..")
            .as("order.interfaces 不应直接依赖 Mapper，应通过 ApplicationService 访问数据"));

    // ============================================================
    // F. Product 模块包边界规则（Phase P.6 新增）
    // ============================================================

    /** 17. product.api 不应依赖 product.infrastructure（API 契约层不应依赖基础设施层） */
    @ArchTest
    static final ArchRule product_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..product.api..")
            .should().dependOnClassesThat().resideInAPackage("..product.infrastructure..")
            .as("product.api 不应依赖 product.infrastructure，API 契约层不应依赖基础设施层"));

    /** 18. product.application 不应直接依赖 Mapper（Application 层应通过 Service/Repository 访问数据） */
    @ArchTest
    static final ArchRule product_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..product.application..")
            .should().dependOnClassesThat().resideInAPackage("..product.infrastructure.mapper..")
            .as("product.application 不应直接依赖 Mapper，应通过 Service/Repository 访问数据"));

    /** 19. product.interfaces 不应直接依赖 Mapper（接口层应通过 ApplicationService 访问数据） */
    @ArchTest
    static final ArchRule product_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..product.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..product.infrastructure.mapper..")
            .as("product.interfaces 不应直接依赖 Mapper，应通过 ApplicationService 访问数据"));

    // ============================================================
    // G. Identity 模块包边界规则（Phase I.6 新增）
    // ============================================================

    /** 20. identity.api 不应依赖 identity.infrastructure（API 契约层不应依赖基础设施层） */
    @ArchTest
    static final ArchRule identity_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..identity.api..")
            .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure..")
            .as("identity.api 不应依赖 identity.infrastructure，API 契约层不应依赖基础设施层"));

    /** 21. identity.application 不应直接依赖 Mapper（Application 层应通过 Service/Repository 访问数据） */
    @ArchTest
    static final ArchRule identity_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..identity.application..")
            .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure.mapper..")
            .as("identity.application 不应直接依赖 Mapper，应通过 Service/Repository 访问数据"));

    /** 22. identity.interfaces 不应直接依赖 Mapper（接口层应通过 ApplicationService 访问数据） */
    @ArchTest
    static final ArchRule identity_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..identity.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure.mapper..")
            .as("identity.interfaces 不应直接依赖 Mapper，应通过 ApplicationService 访问数据"));

    // ============================================================
    // H. Cart 模块包边界规则（Phase C.6 新增）
    // ============================================================

    /** 23. cart.api 不应依赖 cart.infrastructure（API 契约层不应依赖基础设施层） */
    @ArchTest
    static final ArchRule cart_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..cart.api..")
            .should().dependOnClassesThat().resideInAPackage("..cart.infrastructure..")
            .as("cart.api 不应依赖 cart.infrastructure，API 契约层不应依赖基础设施层"));

    /** 24. cart.application 不应直接依赖 Mapper（Application 层应通过 Service/Repository 访问数据） */
    @ArchTest
    static final ArchRule cart_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..cart.application..")
            .should().dependOnClassesThat().resideInAPackage("..cart.infrastructure.mapper..")
            .as("cart.application 不应直接依赖 Mapper，应通过 Service/Repository 访问数据"));

    /** 25. cart.interfaces 不应直接依赖 Mapper（接口层应通过 ApplicationService 访问数据） */
    @ArchTest
    static final ArchRule cart_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..cart.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..cart.infrastructure.mapper..")
            .as("cart.interfaces 不应直接依赖 Mapper，应通过 ApplicationService 访问数据"));

    // ============================================================
    // I. Coupon 模块包边界规则（Phase CP.6 新增）
    // ============================================================

    /** 26. coupon.api 不应依赖 coupon.infrastructure（API 契约层不应依赖基础设施层） */
    @ArchTest
    static final ArchRule coupon_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..coupon.api..")
            .should().dependOnClassesThat().resideInAPackage("..coupon.infrastructure..")
            .as("coupon.api 不应依赖 coupon.infrastructure，API 契约层不应依赖基础设施层"));

    /** 27. coupon.application 不应直接依赖 Mapper（Application 层应通过 Service/Repository 访问数据） */
    @ArchTest
    static final ArchRule coupon_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..coupon.application..")
            .should().dependOnClassesThat().resideInAPackage("..coupon.infrastructure.mapper..")
            .as("coupon.application 不应直接依赖 Mapper，应通过 Service/Repository 访问数据"));

    /** 28. coupon.interfaces 不应直接依赖 Mapper（接口层应通过 ApplicationService 访问数据） */
    @ArchTest
    static final ArchRule coupon_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..coupon.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..coupon.infrastructure.mapper..")
            .as("coupon.interfaces 不应直接依赖 Mapper，应通过 ApplicationService 访问数据"));

    // ============================================================
    // J. Payment 模块包边界规则（Phase PM.6 新增）
    // ============================================================

    /** 29. payment.api 不应依赖 payment.infrastructure（API 契约层不应依赖基础设施层） */
    @ArchTest
    static final ArchRule payment_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..payment.api..")
            .should().dependOnClassesThat().resideInAPackage("..payment.infrastructure..")
            .as("payment.api 不应依赖 payment.infrastructure，API 契约层不应依赖基础设施层"));

    /** 30. payment.application 不应直接依赖 Mapper（Application 层应通过 Service/Repository 访问数据） */
    @ArchTest
    static final ArchRule payment_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..payment.application..")
            .should().dependOnClassesThat().resideInAPackage("..payment.infrastructure.mapper..")
            .as("payment.application 不应直接依赖 Mapper，应通过 Service/Repository 访问数据"));

    /** 31. payment.interfaces 不应直接依赖 Mapper（接口层应通过 ApplicationService 访问数据） */
    @ArchTest
    static final ArchRule payment_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..payment.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..payment.infrastructure.mapper..")
            .as("payment.interfaces 不应直接依赖 Mapper，应通过 ApplicationService 访问数据"));

    // ============================================================
    // K. Seckill 模块包边界规则（Phase SK.6 新增）
    // ============================================================

    /** 32. seckill.api 不应依赖 seckill.infrastructure（API 契约层不应依赖基础设施层） */
    @ArchTest
    static final ArchRule seckill_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..seckill.api..")
            .should().dependOnClassesThat().resideInAPackage("..seckill.infrastructure..")
            .as("seckill.api 不应依赖 seckill.infrastructure，API 契约层不应依赖基础设施层"));

    /** 33. seckill.application 不应直接依赖 Mapper（Application 层应通过 Service/Repository 访问数据） */
    @ArchTest
    static final ArchRule seckill_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..seckill.application..")
            .should().dependOnClassesThat().resideInAPackage("..seckill.infrastructure.mapper..")
            .as("seckill.application 不应直接依赖 Mapper，应通过 Service/Repository 访问数据"));

    /** 34. seckill.interfaces 不应直接依赖 Mapper（接口层应通过 ApplicationService 访问数据） */
    @ArchTest
    static final ArchRule seckill_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..seckill.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..seckill.infrastructure.mapper..")
            .as("seckill.interfaces 不应直接依赖 Mapper，应通过 ApplicationService 访问数据"));

    // ============================================================
    // L. Stats 模块包边界规则（Phase ST.6 新增）
    // ============================================================

    /** 35. stats.api 不应依赖 stats.infrastructure（API 契约层不应依赖基础设施层，保持接口纯洁性） */
    @ArchTest
    static final ArchRule stats_api_should_not_depend_on_infrastructure =
        freeze(noClasses().that().resideInAPackage("..stats.api..")
            .should().dependOnClassesThat().resideInAPackage("..stats.infrastructure..")
            .as("stats.api 不应依赖 stats.infrastructure，API 契约层不应依赖基础设施层"));

    /** 36. stats.application 不应直接依赖 Mapper（Application 层应通过 API 接口访问数据） */
    @ArchTest
    static final ArchRule stats_application_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..stats.application..")
            .should().dependOnClassesThat().resideInAPackage("..stats.infrastructure.persistence.mapper..")
            .as("stats.application 不应直接依赖 Mapper，应通过 API 接口访问数据"));

    /** 37. stats.interfaces 不应直接依赖 Mapper（接口层应通过 API 接口访问数据） */
    @ArchTest
    static final ArchRule stats_interfaces_should_not_depend_on_mapper =
        freeze(noClasses().that().resideInAPackage("..stats.interfaces..")
            .should().dependOnClassesThat().resideInAPackage("..stats.infrastructure.persistence.mapper..")
            .as("stats.interfaces 不应直接依赖 Mapper，应通过 API 接口访问数据"));

    /**
     * 用 FreezingArchRule 包装规则，启用 freeze 模式。
     *
     * <p>freeze 行为：
     * <ul>
     *   <li>首次运行：记录当前所有违规到 {@code archunit-frozen-violations/} 目录，测试通过</li>
     *   <li>后续运行：只检测新增违规，已冻结的违规不会导致失败</li>
     *   <li>违规被消除后：freeze 文件自动同步缩减，再次引入会失败</li>
     * </ul>
     */
    private static ArchRule freeze(ArchRule rule) {
        return FreezingArchRule.freeze(rule);
    }
}