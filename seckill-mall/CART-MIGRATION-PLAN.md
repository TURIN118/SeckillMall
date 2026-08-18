# Cart 模块迁移计划 (CART-MIGRATION-PLAN)

> 生成时间：2026-08-18
> 阶段：Phase C - Cart 模块迁移（Strangler Pattern 第四阶段）
> 依据：PRODUCT-MIGRATION-PLAN.md + ORDER-MIGRATION-PLAN.md + Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：纯架构设计文档，未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. Cart 当前结构

### 1.1 当前包结构

Cart 相关代码散落在 5 个不同的顶层包中，共 **7 个核心文件**（最小模块）：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `CartController` | `controller` | 购物车 CRUD + 选中状态 + 数量统计（/api/v1/cart） |
| `CartService` | `service` | 购物车业务接口 |
| `CartServiceImpl` | `service.impl` | CartService 实现 |
| `Cart` | `entity` | 购物车 PO（t_cart） |
| `CartMapper` | `mapper` | 购物车 Mapper |
| `CartCheckoutRequest` | `dto` | 从购物车结算创建订单请求 |
| `CartItemVO` | `vo` | 购物车项视图（含商品展示信息） |

### 1.2 Service 方法签名

#### CartService（10 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `getCartList` | `Result<List<CartItemVO>> getCartList(Long userId)` | 获取购物车列表 | ✅ 返回 VO |
| `addToCart` | `Result<Void> addToCart(Long userId, Long productId, Long skuId, Integer quantity)` | 添加商品到购物车 | ⚠️ 裸参数（4 个） |
| `updateQuantity` | `Result<Void> updateQuantity(Long userId, Long cartId, Integer quantity)` | 修改数量 | ⚠️ 裸参数（3 个） |
| `removeFromCart` | `Result<Void> removeFromCart(Long userId, Long cartId)` | 删除购物车项 | ⚠️ 裸参数 |
| `clearCart` | `Result<Void> clearCart(Long userId)` | 清空购物车 | ✅ |
| `updateSelected` | `Result<Void> updateSelected(Long userId, Long cartId, Boolean selected)` | 更新选中状态 | ⚠️ 裸参数 |
| `batchUpdateSelected` | `Result<Void> batchUpdateSelected(Long userId, List<Long> cartIds, Boolean selected)` | 批量更新选中状态 | ⚠️ 裸参数 |
| `getCartCount` | `Result<Integer> getCartCount(Long userId)` | 获取购物车数量 | ✅ |
| `getCartsByIds` | `List<Cart> getCartsByIds(List<Long> cartIds)` | 按 ID 批量查询购物车项实体 | ⚠️ 返回 Entity（被 OrderServiceImpl 跨模块调用） |
| `deleteCartsByIds` | `void deleteCartsByIds(List<Long> cartIds)` | 批量逻辑删除购物车项 | ⚠️ 被 OrderServiceImpl 跨模块调用 |

### 1.3 Controller 端点

| Controller | 基路径 | 端点 | 方法 | 权限 |
|---|---|---|---|---|
| `CartController` | `/api/v1/cart` | `GET /list` | 获取购物车列表 | BUYER/SELLER/ADMIN |
| | | `POST /add` | 添加到购物车 | BUYER/SELLER/ADMIN |
| | | `PUT /{cartId}/quantity` | 修改数量 | BUYER/SELLER/ADMIN |
| | | `DELETE /{cartId}` | 删除购物车项 | BUYER/SELLER/ADMIN |
| | | `DELETE /clear` | 清空购物车 | BUYER/SELLER/ADMIN |
| | | `PUT /{cartId}/selected` | 更新单个选中状态 | BUYER/SELLER/ADMIN |
| | | `PUT /batch-selected` | 批量更新选中状态 | BUYER/SELLER/ADMIN |
| | | `GET /count` | 获取购物车数量 | BUYER/SELLER/ADMIN |

### 1.4 跨模块依赖关系

#### Cart 依赖的模块（出向依赖）

| 被依赖模块 | 依赖方式 | 用途 |
|---|---|---|
| **product** | `ProductApi` + `SkuApi` | 加购校验商品/SKU、组装 VO、维护 cart_count |
| **identity** | `SecurityUtils` | 获取当前登录用户 ID（Controller 层） |

#### 依赖 Cart 的模块（入向依赖）

| 调用方模块 | 依赖方式 | 用途 |
|---|---|---|
| **order** | `CartService.getCartsByIds` + `CartService.deleteCartsByIds` + `Cart` entity | `OrderServiceImpl.createOrderFromCart` 购物车结算创建订单 |

### 1.5 Entity / VO / DTO 字段

#### Cart Entity（t_cart）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 主键（ASSIGN_ID） |
| `userId` | `Long` | 用户 ID |
| `productId` | `Long` | 商品 ID |
| `skuId` | `Long` | SKU ID（0 表示无规格） |
| `quantity` | `Integer` | 加购数量 |
| `selected` | `Integer` | 是否选中（0/1） |
| `isDeleted` | `Integer` | 逻辑删除（@TableLogic） |
| `createTime` | `LocalDateTime` | 创建时间 |
| `updateTime` | `LocalDateTime` | 更新时间 |

#### CartItemVO

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | `Long` | 购物车项 ID |
| `productId` | `Long` | 商品 ID |
| `skuId` | `Long` | SKU ID |
| `skuAttributes` | `String` | SKU 属性快照 |
| `quantity` | `Integer` | 加购数量 |
| `selected` | `Boolean` | 是否选中 |
| `productName` | `String` | 商品名称 |
| `mainImage` | `String` | 商品主图 |
| `skuMainImage` | `String` | SKU 主图 |
| `originalPrice` | `BigDecimal` | 单价 |
| `stock` | `Integer` | 库存 |
| `productStatus` | `String` | 商品状态 |
| `subtotal` | `BigDecimal` | 小计 = 单价 × 数量 |

#### CartCheckoutRequest（保留在 dto 包，order 模块使用）

| 字段 | 类型 | 说明 |
|---|---|---|
| `addressId` | `Long` | 收货地址 ID |
| `cartIds` | `List<Long>` | 待结算购物车项 ID 列表 |
| `remark` | `String` | 备注 |
| `userCouponId` | `Long` | 优惠券 ID |

### 1.6 CartMapper 自定义方法

| 方法 | 签名 | 用途 |
|---|---|---|
| `selectByUserAndProductIncludeDeleted` | `Cart selectByUserAndProductIncludeDeleted(Long userId, Long productId)` | 绕过 @TableLogic 查询（唯一约束冲突恢复用） |
| `restoreAndSetQuantity` | `int restoreAndSetQuantity(Long id, Integer quantity)` | 恢复逻辑删除记录并设置数量 |

---

## 2. Cart 目标结构（Pragmatic DDD + Modular Monolith）

### 2.1 目标包结构

```
com.seckill.mall.cart/
├── package-info.java                              # 模块根包
├── api/                                            # API 契约层（对外暴露）
│   ├── package-info.java
│   ├── CartApi.java                               # 购物车业务能力 API
│   ├── dto/
│   │   ├── package-info.java
│   │   └── CartItemDTO.java                       # 购物车项 DTO（替代 CartItemVO 跨模块传递）
│   ├── command/
│   │   ├── package-info.java
│   │   ├── AddToCartCommand.java                  # 加购命令
│   │   ├── UpdateCartQuantityCommand.java         # 修改数量命令
│   │   ├── RemoveCartItemCommand.java             # 删除购物车项命令
│   │   ├── UpdateSelectedCommand.java             # 更新选中状态命令
│   │   └── BatchUpdateSelectedCommand.java        # 批量更新选中状态命令
│   ├── query/
│   │   └── package-info.java                      # （预留，当前查询仅按 userId）
│   └── result/
│       └── package-info.java                      # （预留，当前无复杂 Result）
├── application/                                    # 应用层
│   ├── package-info.java
│   ├── CartApplicationService.java                # 实现 CartApi，委托 CartService
│   └── facade/
│       ├── package-info.java
│       └── CartApiConverter.java                  # VO/Entity ↔ DTO/Command 转换
├── domain/                                         # 领域层（当前贫血，预留）
│   └── package-info.java
├── infrastructure/                                 # 基础设施层
│   ├── package-info.java
│   ├── mapper/
│   │   ├── package-info.java
│   │   └── CartMapper.java                        # 从 mapper 包迁入
│   └── entity/
│       ├── package-info.java
│       └── Cart.java                              # 从 entity 包迁入
└── interfaces/                                     # 接口层
    ├── package-info.java
    ├── vo/
    │   ├── package-info.java
    │   └── CartItemVO.java                         # 从 vo 包迁入（前端展示用）
    └── web/
        ├── package-info.java
        └── CartController.java                     # 从 controller 包迁入
```

### 2.2 分层依赖规则（目标）

```
interfaces ──→ application ──→ api
                    │
                    └──→ service（旧层，Strangler 过渡期）
                              │
                              └──→ infrastructure.mapper ──→ infrastructure.entity

api ──✗──→ infrastructure     （API 不依赖基础设施）
application ──✗──→ mapper     （Application 不直接依赖 Mapper）
interfaces ──✗──→ mapper      （Interfaces 不直接依赖 Mapper）
```

### 2.3 与其他模块的关系（目标）

| 关系 | 模块 | 依赖方式 |
|---|---|---|
| Cart → Product | `ProductApi` + `SkuApi` | 加购校验、组装 DTO、维护 cart_count |
| Cart → Identity | `SecurityUtils`（Controller 层） | 获取当前用户 ID |
| Order → Cart | `CartApi`（替代 `CartService`） | 购物车结算创建订单 |

---

## 3. 文件迁移路径

### 3.1 移动文件（git mv）

| 源路径 | 目标路径 | 阶段 |
|---|---|---|
| `entity/Cart.java` | `cart/infrastructure/entity/Cart.java` | Phase C.3 |
| `mapper/CartMapper.java` | `cart/infrastructure/mapper/CartMapper.java` | Phase C.3 |
| `vo/CartItemVO.java` | `cart/interfaces/vo/CartItemVO.java` | Phase C.4-B |
| `controller/CartController.java` | `cart/interfaces/web/CartController.java` | Phase C.4-B |

### 3.2 新增文件

| 目标路径 | 阶段 |
|---|---|
| `cart/package-info.java` 及所有子包 `package-info.java` | Phase C.0 |
| `cart/api/CartApi.java` | Phase C.2 |
| `cart/api/dto/CartItemDTO.java` | Phase C.2 |
| `cart/api/command/{AddToCart,UpdateCartQuantity,RemoveCartItem,UpdateSelected,BatchUpdateSelected}Command.java` | Phase C.2 |
| `cart/application/CartApplicationService.java` | Phase C.4-A |
| `cart/application/facade/CartApiConverter.java` | Phase C.4-A |

### 3.3 保留不动的文件

| 文件 | 原因 |
|---|---|
| `service/CartService.java` | Strangler 过渡期保留，CartApplicationService 委托它 |
| `service/impl/CartServiceImpl.java` | 同上 |
| `dto/CartCheckoutRequest.java` | 由 order 模块使用，不属于 cart 模块对外契约 |

### 3.4 后续可删除的文件（Strangler 完成后）

| 文件 | 删除时机 |
|---|---|
| `service/CartService.java` + `service/impl/CartServiceImpl.java` | 业务逻辑完全迁入 cart.application 后 |

---

## 4. API 边界（CartApi）

### 4.1 设计原则

1. **方法用业务语言命名**，禁止 CRUD 命名
2. **入参用 Command/Query 对象**，禁止裸露多参数
3. **出参用 Result/DTO/Snapshot**，禁止暴露 Entity/Mapper/PO
4. **异常通过 `BusinessException(ErrorCode)` 抛出**，不泄露堆栈
5. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型

### 4.2 CartApi 方法清单

| 方法 | 签名 | 替代旧方法 |
|---|---|---|
| `getCartList` | `List<CartItemDTO> getCartList(Long userId)` | `CartService.getCartList` |
| `addToCart` | `void addToCart(AddToCartCommand command)` | `CartService.addToCart` |
| `updateQuantity` | `void updateQuantity(UpdateCartQuantityCommand command)` | `CartService.updateQuantity` |
| `removeFromCart` | `void removeFromCart(RemoveCartItemCommand command)` | `CartService.removeFromCart` |
| `clearCart` | `void clearCart(Long userId)` | `CartService.clearCart` |
| `updateSelected` | `void updateSelected(UpdateSelectedCommand command)` | `CartService.updateSelected` |
| `batchUpdateSelected` | `void batchUpdateSelected(BatchUpdateSelectedCommand command)` | `CartService.batchUpdateSelected` |
| `getCartCount` | `int getCartCount(Long userId)` | `CartService.getCartCount` |
| `getCartItemsByIds` | `List<CartItemDTO> getCartItemsByIds(List<Long> cartIds)` | `CartService.getCartsByIds`（返回 Entity → DTO） |
| `deleteCartItemsByIds` | `void deleteCartItemsByIds(List<Long> cartIds)` | `CartService.deleteCartsByIds` |

> **关键变化**：`getCartsByIds` 返回 `List<Cart>` Entity → `getCartItemsByIds` 返回 `List<CartItemDTO>`，消除 order 模块对 Cart Entity 的依赖。

### 4.3 DTO/Command 定义

详见 [CART-API-CONTRACT.md](CART-API-CONTRACT.md)。

---

## 5. 迁移步骤（Phase C）

### Phase C.0：创建包结构

- 创建 `cart/` 及所有子包
- 每个包创建 `package-info.java`
- **不修改任何现有代码**

### Phase C.2：创建 API 层

- 创建 `CartApi` 接口
- 创建 `CartItemDTO`
- 创建 5 个 Command 对象
- **不修改任何现有代码**

### Phase C.3：迁移持久化层

- `git mv entity/Cart.java cart/infrastructure/entity/Cart.java`
- `git mv mapper/CartMapper.java cart/infrastructure/mapper/CartMapper.java`
- 更新 package 声明
- 更新所有 import（CartServiceImpl、OrderServiceImpl、CartMapper 自定义 SQL 引用）
- **回归测试**：mvn test

### Phase C.4-A：创建 Application 层

- 创建 `CartApplicationService implements CartApi`，委托 `CartService`
- 创建 `CartApiConverter`：`CartItemVO` ↔ `CartItemDTO`、Command → 裸参数
- **不删除旧 CartService**（Strangler 过渡）

### Phase C.4-B：移动 Controller + VO

- `git mv vo/CartItemVO.java cart/interfaces/vo/CartItemVO.java`
- `git mv controller/CartController.java cart/interfaces/web/CartController.java`
- 更新 package 声明与所有 import

### Phase C.4-C：切换 Controller 调用 CartApi

- `CartController` 注入 `CartApi` 替代 `CartService`
- 调用 `CartApi` 方法，VO → DTO 转换由 Controller 或 Converter 完成
- **回归测试**：mvn test

### Phase C.5：迁移外部调用方

- `OrderServiceImpl`：`CartService` → `CartApi`
  - `cartService.getCartsByIds(cartIds)` → `cartApi.getCartItemsByIds(cartIds)`（返回 `List<CartItemDTO>`）
  - `cartService.deleteCartsByIds(cartIds)` → `cartApi.deleteCartItemsByIds(cartIds)`
  - `Cart` entity 引用 → `CartItemDTO`（适配 `getUserId/getProductId/getSkuId/getQuantity`）
- **回归测试**：mvn test

### Phase C.6：ArchUnit 规则

新增 3 条 cart 模块包边界规则：

| 规则 | 内容 |
|---|---|
| `cart_api_should_not_depend_on_infrastructure` | `cart.api` 不应依赖 `cart.infrastructure` |
| `cart_application_should_not_depend_on_mapper` | `cart.application` 不应直接依赖 `cart.infrastructure.mapper` |
| `cart_interfaces_should_not_depend_on_mapper` | `cart.interfaces` 不应直接依赖 `cart.infrastructure.mapper` |

**回归标准**：mvn test Failures ≤ 1, Errors ≤ 19, ArchUnit 全绿。

---

## 6. 风险点与缓解

### 6.1 OrderServiceImpl 对 Cart Entity 的强耦合

**风险**：`OrderServiceImpl.createOrderFromCart` 直接使用 `Cart` entity 的 `getUserId/getProductId/getSkuId/getQuantity` 方法。切换到 `CartApi.getCartItemsByIds` 后返回 `CartItemDTO`，需保证 DTO 包含所有被使用字段。

**缓解**：`CartItemDTO` 必须包含 `id/userId/productId/skuId/quantity/selected` 字段（至少覆盖 OrderServiceImpl 使用范围）。详见 [CART-API-CONTRACT.md](CART-API-CONTRACT.md)。

### 6.2 CartServiceImpl 内部对 CartMapper 自定义方法的依赖

**风险**：`CartMapper.selectByUserAndProductIncludeDeleted` 和 `restoreAndSetQuantity` 是绕过 @TableLogic 的特殊方法，迁移时需保证 @Select/@Update 注解 SQL 仍能正确执行。

**缓解**：仅移动文件位置，不修改 SQL 内容；迁移后立即跑 mvn test 验证加购场景。

### 6.3 CartItemVO 被前端接口契约锁定

**风险**：`CartItemVO` 是 `/api/v1/cart/list` 端点的返回体，前端依赖其字段。若切换为 `CartItemDTO` 可能破坏前端契约。

**缓解**：**Controller 仍返回 `CartItemVO`**（保留前端契约），内部调用 `CartApi.getCartList` 拿到 `List<CartItemDTO>` 后通过 `CartApiConverter.toVO` 转回 VO。`CartItemVO` 移极迁入 `cart/interfaces/vo/` 但保留所有字段。

### 6.4 CartCheckoutRequest 位置

**风险**：`CartCheckoutRequest` 在 `dto/` 包，被 `OrderController` 使用，不属于 cart 模块对外契约。

**缓解**：本次迁移**不动** `CartCheckoutRequest`，保留在 `dto/` 包（属于 order 模块的请求 DTO）。

### 6.5 Strangler 过渡期 CartService 保留

**风险**：`CartService` + `CartServiceImpl` 在过渡期保留，可能与 `CartApi` + `CartApplicationService` 并存导致语义重复。

**缓解**：`CartApplicationService` 仅做门面委托，不含业务逻辑；待所有外部调用方迁移完毕且稳定后，再考虑将业务逻辑从 `CartServiceImpl` 迁入 `CartApplicationService` 并删除旧 Service（不在本次迁移范围）。

### 6.6 ArchUnit freeze 模式

**风险**：新增 3 条 cart 模块规则后，freeze 模式首次运行会冻结当前违规。若 `cart.application` 当前仍依赖 `cart.infrastructure.mapper`（通过旧 Service 间接依赖），可能被冻结而非立即报错。

**缓解**：Phase C.4-A 保证 `CartApplicationService` 不直接 import `CartMapper`；Phase C.5 保证 `OrderServiceImpl` 不再直接 import `Cart` entity。新增规则后跑 ArchUnit 验证全绿。

---

## 7. 回归测试基线

| 指标 | 基线 | 目标 |
|---|---|---|
| Tests run | 129 | ≥ 129 |
| Failures | 1 | ≤ 1 |
| Errors | 19 | ≤ 19 |
| ArchUnit 规则 | 30 全绿 | 33 全绿（新增 3 条 cart 规则） |

---

## 8. 参考文档

- [PRODUCT-MIGRATION-PLAN.md](PRODUCT-MIGRATION-PLAN.md) - Product 模块迁移经验
- [ORDER-MIGRATION-PLAN.md](ORDER-MIGRATION-PLAN.md) - Order 模块迁移经验
- [CART-API-CONTRACT.md](CART-API-CONTRACT.md) - Cart 模块 API 契约