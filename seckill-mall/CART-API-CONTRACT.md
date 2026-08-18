# Cart 模块 API 契约 (CART-API-CONTRACT)

> 生成时间：2026-08-18
> 阶段：Phase C.2 - Cart API 契约层定义
> 依据：CART-MIGRATION-PLAN.md + ProductApi 契约风格
> 说明：本文件定义 Cart 模块对外暴露的 API 契约，一经发布只增不改不删。

---

## 1. CartApi 接口

**全限定名**：`com.seckill.mall.cart.api.CartApi`

**实现类**：`com.seckill.mall.cart.application.CartApplicationService`

**设计原则**：
- 方法用业务语言命名，禁止 CRUD 命名
- 入参用 Command/Query 对象，禁止裸露多参数
- 出参用 Result/DTO/Snapshot，禁止暴露 Entity/Mapper/PO
- 异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈
- 向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

```java
package com.seckill.mall.cart.api;

public interface CartApi {

    /** 获取指定用户的购物车列表（含商品展示信息） */
    List<CartItemDTO> getCartList(Long userId);

    /** 添加商品到购物车（同商品累加，否则新建；同步递增 product.cart_count） */
    void addToCart(AddToCartCommand command);

    /** 修改购物车项数量（校验归属当前用户） */
    void updateQuantity(UpdateCartQuantityCommand command);

    /** 删除购物车项（逻辑删除，校验归属；同步递减 product.cart_count） */
    void removeFromCart(RemoveCartItemCommand command);

    /** 清空指定用户的购物车（逻辑删除全部项；同步递减 product.cart_count） */
    void clearCart(Long userId);

    /** 更新单个购物车项的选中状态（校验归属） */
    void updateSelected(UpdateSelectedCommand command);

    /** 批量更新购物车项的选中状态（校验归属） */
    void batchUpdateSelected(BatchUpdateSelectedCommand command);

    /** 获取指定用户购物车中的商品种类数量 */
    int getCartCount(Long userId);

    /**
     * 按 ID 批量查询购物车项（跨模块只读访问，返回 DTO 而非 Entity）。
     * 供 OrderServiceImpl.createOrderFromCart 跨模块调用。
     */
    List<CartItemDTO> getCartItemsByIds(List<Long> cartIds);

    /**
     * 批量逻辑删除购物车项（@TableLogic 自动 set is_deleted=1）。
     * 供 OrderServiceImpl.createOrderFromCart 结算后删除已结算购物车项。
     */
    void deleteCartItemsByIds(List<Long> cartIds);
}
```

---

## 2. DTO 定义

### 2.1 CartItemDTO

**全限定名**：`com.seckill.mall.cart.api.dto.CartItemDTO`

**用途**：替代 `Cart` Entity 跨模块传递；替代 `CartItemVO` 作为 Application 层返回值。

**来源映射**：`Cart` Entity → `CartItemDTO`（由 `CartApiConverter.toDTO` 转换）；`CartItemVO` → `CartItemDTO`（由 `CartApiConverter.toDTOFromVO` 转换）。

| 字段 | 类型 | 说明 | 来源 |
|---|---|---|---|
| `id` | `Long` | 购物车项主键 ID | `Cart.id` |
| `userId` | `Long` | 用户 ID | `Cart.userId` |
| `productId` | `Long` | 商品 ID | `Cart.productId` |
| `skuId` | `Long` | SKU ID（0 表示无规格） | `Cart.skuId` |
| `quantity` | `Integer` | 加购数量 | `Cart.quantity` |
| `selected` | `Boolean` | 是否选中（Entity 层 0/1 → DTO 层 true/false） | `Cart.selected` |

> **注意**：`CartItemDTO` 仅包含跨模块传递所需的核心字段，**不含**商品展示信息（productName/mainImage/originalPrice/stock/subtotal 等），这些字段属于前端展示关注点，保留在 `CartItemVO`。

> **OrderServiceImpl 使用字段**：`userId`、`productId`、`skuId`、`quantity`，全部覆盖。

```java
package com.seckill.mall.cart.api.dto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {
    /** 购物车项主键 ID */
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 商品 ID */
    private Long productId;
    /** SKU ID，0 表示无规格 */
    private Long skuId;
    /** 加购数量 */
    private Integer quantity;
    /** 是否选中（Entity 层 0/1 → DTO 层 true/false） */
    private Boolean selected;
}
```

---

## 3. Command 定义

### 3.1 AddToCartCommand

**全限定名**：`com.seckill.mall.cart.api.command.AddToCartCommand`

**业务语义**：添加商品到购物车。若已存在相同商品（同 productId + skuId）则数量累加，否则新建购物车项；同步递增 `t_product.cart_count`。

**原方法**：`CartService.addToCart(Long userId, Long productId, Long skuId, Integer quantity)`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `userId` | `Long` | 是 | - | 用户 ID |
| `productId` | `Long` | 是 | `@NotNull` | 商品 ID |
| `skuId` | `Long` | 否 | - | SKU ID（null 或 0 表示无规格） |
| `quantity` | `Integer` | 是 | `@NotNull` + `@Min(1)` | 加购数量 |

```java
package com.seckill.mall.cart.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartCommand {
    /** 用户 ID（必填） */
    private Long userId;
    /** 商品 ID（必填） */
    private Long productId;
    /** SKU ID（可选，null 或 0 表示无规格商品） */
    private Long skuId;
    /** 加购数量（必填，必须大于 0） */
    private Integer quantity;
}
```

### 3.2 UpdateCartQuantityCommand

**全限定名**：`com.seckill.mall.cart.api.command.UpdateCartQuantityCommand`

**业务语义**：修改购物车项数量（校验归属当前用户）。

**原方法**：`CartService.updateQuantity(Long userId, Long cartId, Integer quantity)`

| 字段 | 类型 | 必填 | 校验 | 说明 |
|---|---|---|---|---|
| `userId` | `Long` | 是 | - | 用户 ID |
| `cartId` | `Long` | 是 | `@NotNull` | 购物车项 ID |
| `quantity` | `Integer` | 是 | `@NotNull` + `@Min(1)` | 新数量 |

```java
package com.seckill.mall.cart.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartQuantityCommand {
    /** 用户 ID（必填） */
    private Long userId;
    /** 购物车项 ID（必填） */
    private Long cartId;
    /** 新数量（必填，必须大于 0） */
    private Integer quantity;
}
```

### 3.3 RemoveCartItemCommand

**全限定名**：`com.seckill.mall.cart.api.command.RemoveCartItemCommand`

**业务语义**：删除购物车项（逻辑删除，校验归属；同步递减 `t_product.cart_count`）。

**原方法**：`CartService.removeFromCart(Long userId, Long cartId)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Long` | 是 | 用户 ID |
| `cartId` | `Long` | 是 | 购物车项 ID |

```java
package com.seckill.mall.cart.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveCartItemCommand {
    /** 用户 ID（必填） */
    private Long userId;
    /** 购物车项 ID（必填） */
    private Long cartId;
}
```

### 3.4 UpdateSelectedCommand

**全限定名**：`com.seckill.mall.cart.api.command.UpdateSelectedCommand`

**业务语义**：更新单个购物车项的选中状态（校验归属）。

**原方法**：`CartService.updateSelected(Long userId, Long cartId, Boolean selected)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Long` | 是 | 用户 ID |
| `cartId` | `Long` | 是 | 购物车项 ID |
| `selected` | `Boolean` | 否 | 是否选中 |

```java
package com.seckill.mall.cart.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSelectedCommand {
    /** 用户 ID（必填） */
    private Long userId;
    /** 购物车项 ID（必填） */
    private Long cartId;
    /** 是否选中 */
    private Boolean selected;
}
```

### 3.5 BatchUpdateSelectedCommand

**全限定名**：`com.seckill.mall.cart.api.command.BatchUpdateSelectedCommand`

**业务语义**：批量更新购物车项的选中状态（校验归属）。

**原方法**：`CartService.batchUpdateSelected(Long userId, List<Long> cartIds, Boolean selected)`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `userId` | `Long` | 是 | 用户 ID |
| `cartIds` | `List<Long>` | 是 | 购物车项 ID 列表 |
| `selected` | `Boolean` | 否 | 是否选中 |

```java
package com.seckill.mall.cart.api.command;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateSelectedCommand {
    /** 用户 ID（必填） */
    private Long userId;
    /** 购物车项 ID 列表（必填） */
    private List<Long> cartIds;
    /** 是否选中 */
    private Boolean selected;
}
```

---

## 4. Query 定义

当前 Cart 模块查询仅按 `userId`，无需复杂 Query 对象。`cart/api/query/` 包预留为空（仅 `package-info.java`），未来若增加分页/筛选查询再补充。

---

## 5. Result 定义

当前 Cart 模块无复杂 Result 对象：
- `getCartList` 返回 `List<CartItemDTO>`
- `getCartCount` 返回 `int`
- 其他方法返回 `void`

`cart/api/result/` 包预留为空（仅 `package-info.java`），未来若增加结算预览等复杂返回再补充。

---

## 6. 转换器（CartApiConverter）

**全限定名**：`com.seckill.mall.cart.application.facade.CartApiConverter`

**职责**：集中存放旧 VO/Entity 与新 API 层 DTO/Command 之间的转换方法，供 `CartApplicationService` 调用。所有方法均为无状态静态方法。

### 6.1 转换方法清单

| 方法 | 签名 | 用途 |
|---|---|---|
| `toDTO` | `CartItemDTO toDTO(Cart entity)` | Cart Entity → CartItemDTO（跨模块只读） |
| `toDTOList` | `List<CartItemDTO> toDTOList(List<Cart> entities)` | Cart Entity 列表 → CartItemDTO 列表 |
| `toDTOFromVO` | `CartItemDTO toDTOFromVO(CartItemVO vo)` | CartItemVO → CartItemDTO |
| `toDTOListFromVO` | `List<CartItemDTO> toDTOListFromVO(List<CartItemVO> voList)` | CartItemVO 列表 → CartItemDTO 列表 |
| `toVO` | `CartItemVO toVO(CartItemDTO dto)` | CartItemDTO → CartItemVO（Controller 层前端契约适配） |
| `toVOList` | `List<CartItemVO> toVOList(List<CartItemDTO> dtoList)` | CartItemDTO 列表 → CartItemVO 列表 |

### 6.2 转换规则

- **Entity → DTO**：`selected` 字段 `Integer(0/1)` → `Boolean(true/false)`，丢弃 `isDeleted/createTime/updateTime` 等基础设施字段
- **VO → DTO**：仅提取核心字段（id/userId/productId/skuId/quantity/selected），丢弃展示字段（productName/mainImage/originalPrice/stock/subtotal 等）
- **DTO → VO**：仅填充核心字段，展示字段设为 null（由 Controller 层决定是否再查询商品信息填充，当前 `getCartList` 由旧 Service 已填充 VO，转换路径不常用）

---

## 7. ApplicationService 委托映射

`CartApplicationService implements CartApi`，委托 `CartService`：

| CartApi 方法 | 委托 CartService 方法 | 转换 |
|---|---|---|
| `getCartList(userId)` | `cartService.getCartList(userId)` | `Result<List<CartItemVO>>` → `List<CartItemDTO>`（`toDTOListFromVO`） |
| `addToCart(command)` | `cartService.addToCart(userId, productId, skuId, quantity)` | Command 解包 |
| `updateQuantity(command)` | `cartService.updateQuantity(userId, cartId, quantity)` | Command 解包 |
| `removeFromCart(command)` | `cartService.removeFromCart(userId, cartId)` | Command 解包 |
| `clearCart(userId)` | `cartService.clearCart(userId)` | 直接委托 |
| `updateSelected(command)` | `cartService.updateSelected(userId, cartId, selected)` | Command 解包 |
| `batchUpdateSelected(command)` | `cartService.batchUpdateSelected(userId, cartIds, selected)` | Command 解包 |
| `getCartCount(userId)` | `cartService.getCartCount(userId)` | `Result<Integer>` → `int` |
| `getCartItemsByIds(cartIds)` | `cartService.getCartsByIds(cartIds)` | `List<Cart>` → `List<CartItemDTO>`（`toDTOList`） |
| `deleteCartItemsByIds(cartIds)` | `cartService.deleteCartsByIds(cartIds)` | 直接委托 |

---

## 8. 异常契约

CartApi 方法在业务异常时抛出 `BusinessException(ErrorCode)`，不泄露堆栈。主要错误码：

| ErrorCode | 触发场景 |
|---|---|
| `CART_QUANTITY_INVALID` | 加购/修改数量 ≤ 0 |
| `CART_ITEM_NOT_FOUND` | 购物车项不存在或不属于当前用户 |
| `CART_ITEM_FORBIDDEN` | 购物车项不属于当前用户（OrderServiceImpl 校验） |
| `PRODUCT_NOT_FOUND` | 加购时商品不存在 |
| `STOCK_EMPTY` | 加购/结算时库存不足 |
| `PARAM_ERROR` | SKU 不存在/不属于商品、加购超过上限等 |

---

## 9. 向后兼容承诺

1. **方法签名只增不改不删**：CartApi 一经发布，不修改已有方法签名，不删除已有方法
2. **DTO 字段只增不删不改变类型**：CartItemDTO 字段不删除、不重命名、不改变类型
3. **Command 字段只增不删不改变类型**：同上
4. **新增方法/字段保持向后兼容**：新增方法不影响已有调用方，新增字段默认 null 不影响已有逻辑

---

## 10. 引用关系

### 10.1 CartApi 被调用方

| 调用方 | 调用方法 | 用途 |
|---|---|---|
| `cart.interfaces.web.CartController` | 全部方法 | 前端 REST 端点 |
| `order.service.impl.OrderServiceImpl` | `getCartItemsByIds` + `deleteCartItemsByIds` | 购物车结算创建订单 |

### 10.2 CartApi 依赖的契约

| 依赖 | 用途 |
|---|---|
| `product.api.ProductApi` | 加购校验商品、维护 cart_count |
| `product.api.SkuApi` | 加购校验 SKU、组装 VO |
| `identity`（SecurityUtils，仅 Controller 层） | 获取当前用户 ID |