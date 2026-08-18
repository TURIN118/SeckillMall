package com.seckill.mall.seckill.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.seckill.api.command.SeckillPayCommand;
import com.seckill.mall.seckill.api.command.SeckillShipCommand;
import com.seckill.mall.seckill.api.dto.SeckillOrderDTO;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.seckill.api.query.SeckillOrderQuery;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Seckill 模块秒杀订单 API。
 *
 * <p>对外暴露秒杀订单能力（查询/支付/取消/发货/确认/超时/统计），
 * 供 order、payment、review、stats、system 模块调用。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 CRUD 命名</li>
 *     <li>入参用 Command/Query 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * <p>原方法映射参见 SECKILL-API-CONTRACT.md 第 9 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
public interface SeckillOrderApi {

    // === 用户端 ===

    /**
     * 查询用户秒杀订单列表。
     *
     * @param query 查询条件（userId + status + pageNum + pageSize）
     * @return 分页结果（DTO，不含 Entity）
     */
    PageResult<SeckillOrderDTO> listOrders(SeckillOrderQuery query);

    /**
     * 查询秒杀订单详情。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @return 订单 DTO
     */
    SeckillOrderDTO getOrderDetail(Long userId, Long orderId);

    /**
     * 支付秒杀订单。
     *
     * @param command 支付命令（userId + orderId + payMethod）
     * @return 更新后的订单 DTO
     */
    SeckillOrderDTO payOrder(SeckillPayCommand command);

    /**
     * 取消秒杀订单。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @return 更新后的订单 DTO
     */
    SeckillOrderDTO cancelOrder(Long userId, Long orderId);

    /**
     * 查询订单状态（返回字符串而非枚举，避免序列化枚举内部结构）。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @return 订单状态字符串
     */
    String getOrderStatus(Long userId, Long orderId);

    // === 管理端 ===

    /**
     * 超时取消（延迟消费者触发）：UNPAID → TIMEOUT 并回补库存。
     *
     * @param orderId 订单 ID
     * @return true 表示本次执行了超时取消
     */
    boolean timeoutCancel(Long orderId);

    /**
     * 发货：PAID → SHIPPED。
     *
     * @param command 发货命令（userId + orderId + shippingCompany + shippingNo）
     */
    void shipOrder(SeckillShipCommand command);

    /**
     * 确认收货：SHIPPED → COMPLETED。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     */
    void confirmOrder(Long userId, Long orderId);

    // === 跨模块查询 ===

    /**
     * 校验用户是否通过秒杀订单购买了该商品（review 模块评价权限校验用）。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @return true=已购买
     */
    boolean hasUserPurchasedSeckill(Long userId, Long productId);

    /**
     * 查询用户钱包支付的已支付秒杀订单（payment 模块 WalletService 用）。
     *
     * @param userId 用户 ID
     * @return 秒杀订单 DTO 列表
     */
    List<SeckillOrderDTO> getWalletPaidOrdersByUser(Long userId);

    /**
     * 统一订单列表查询（order 模块 OrderQueryService 用）。
     *
     * @param userId 用户 ID
     * @param status 订单状态序号（null=不筛选）
     * @param limit  最大返回条数
     * @return 秒杀订单 DTO 列表
     */
    List<SeckillOrderDTO> getSeckillOrdersForUnifiedList(Long userId, Integer status, int limit);

    /**
     * 逻辑删除秒杀订单（order 模块 deleteOrder 用）。
     *
     * @param orderId 订单 ID
     * @return true 表示删除成功
     */
    boolean logicalDeleteSeckillOrder(Long orderId);

    // === 统计（stats 模块用）===

    /**
     * 订单总数。
     *
     * @return 订单总数
     */
    long countAll();

    /**
     * 销售总额。
     *
     * @param statuses 订单状态集合
     * @return 销售总额
     */
    BigDecimal sumSalesAmount(List<OrderStatus> statuses);

    /**
     * 今日订单数。
     *
     * @param today 今日日期
     * @return 今日订单数
     */
    Long countTodayOrders(LocalDate today);

    /**
     * 订单趋势。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param statuses  订单状态集合
     * @return 趋势数据列表
     */
    List<Map<String, Object>> selectOrderTrend(LocalDate startDate, LocalDate endDate, List<OrderStatus> statuses);

    /**
     * 秒杀排行榜 Top N。
     *
     * @param statuses 订单状态集合
     * @param limit    Top N
     * @return 排行榜列表
     */
    List<SeckillRankingDTO> selectSeckillRanking(List<OrderStatus> statuses, int limit);

    /**
     * 订单状态分布。
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 状态分布列表
     */
    List<Map<String, Object>> selectStatusDistribution(LocalDateTime startTime, LocalDateTime endTime);
}