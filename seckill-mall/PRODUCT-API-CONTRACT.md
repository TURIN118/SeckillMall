# Product 模块 API 契约 (PRODUCT-API-CONTRACT)

> 生成时间：2026-08-17
> 阶段：Phase 3 - Product 模块迁移（Strangler Pattern 第一阶段）
> 依据：PRODUCT-MIGRATION-PLAN.md + ORDER-API-CONTRACT.md（格式参考）
> 说明：纯架构设计文档，未创建任何代码/接口。本文档定义 API 契约，实施时再创建接口。

---

## 1. 契约设计原则

1. **API 方法用业务语言命名**：如 `createProduct`、`getProductById`、`deductStock`，不用技术语言（如 `insert`、`updateById`）。
2. **入参用 Command/Query，出参用 Result/DTO/Snapshot**：禁止裸露 `Long productId, Integer quantity, ...` 多参数，封装为 Command 对象（简单只读查询可保留基本类型入参）。
3. **禁止暴露 Entity/Mapper/PO**：API 签名中不出现 `Product`、`ProductSku`、`ProductReview`、`ProductAttribute`、`ProductAttributeValue`、`ProductMapper` 等基础设施类型。
4. **禁止暴露内部 Service**：`ProductServiceImpl` 等实现类不对外暴露，仅通过 `ProductApi` 等 API 接口交互。
5. **异常用业务错误码（ErrorCode）**：所有异常通过 `BusinessException(ErrorCode)` 抛出，不泄露堆栈。
6. **向后兼容**：契约一旦发布，方法签名只增不改不删，DTO/Snapshot 字段只增不删不改变类型。
7. **Entity → Snapshot 映射**：跨模块传递时，`Product` Entity 映射为 `ProductSnapshot`，`ProductSku` Entity 映射为 `SkuSnapshot`，裁剪掉 `isDeleted` 等基础设施字段。

---

## 2. ProductApi（商品业务能力）

> 包路径：`com.seckill.mall.product.api.ProductApi`
> 职责：商品 CRUD + 跨模块只读快照查询 + 冗余计数维护
> 原 Service：`ProductService`（11 方法）
> 实现类：`ProductApplicationService`（`product.application`）

### 2.1 listProducts - 商品列表分页

| 项 | 定义 |
|---|---|
| **方法签名** | `PageResult<ProductSummaryDTO> listProducts(ProductListQuery query)` |
| **调用方** | ProductController, ai（ProductSearchTool）, stats |
| **业务语义** | 商品列表分页查询（支持按分类/状态/关键字筛选） |
| **原方法** | `ProductService.listProducts(ProductQueryRequest)` → `PageResult<ProductVO>` |

**入参 ProductListQuery**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| categoryId | Long | 否 | 分类 ID 筛选 |
| status | String | 否 | 商品状态筛选（ON_SALE/OFF_SHELF） |
| keyword | String | 否 | 商品名关键字模糊匹配 |
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10，上限 50） |

**出参 PageResult\<ProductSummaryDTO\>**：见 §7.4

**异常**：`PARAM_ERROR`

**依赖的外部 API**：无（仅查自身持久层）

---

### 2.2 getProductDetail - 商品详情

| 项 | 定义 |
|---|---|
| **方法签名** | `ProductDetailResult getProductDetail(Long id)` |
| **调用方** | ProductController, ai（ProductSearchTool） |
| **业务语义** | 查询商品详情（含 SKU 列表 + 属性列表 + 价格区间） |
| **原方法** | `ProductService.getProductDetail(Long)` → `ProductVO` |

**入参**：`Long id`（商品 ID）

**出参 ProductDetailResult**：见 §7.3

**异常**：`PRODUCT_NOT_FOUND`

**依赖的外部 API**：无（内部聚合 SkuApi + AttributeApi）

---

### 2.3 createProduct - 新增商品

| 项 | 定义 |
|---|---|
| **方法签名** | `ProductDetailResult createProduct(CreateProductCommand command)` |
| **调用方** | ProductController（管理员） |
| **业务语义** | 新增商品（保存商品基本信息 → 保存 SKU → 保存属性 → 刷新价格区间冗余字段） |
| **原方法** | `ProductService.createProduct(ProductCreateRequest)` → `ProductVO` |

**入参 CreateProductCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | String | 是 | 商品名（≤200 字符） |
| description | String | 否 | 商品描述 |
| originalPrice | BigDecimal | 是 | 原价（>0） |
| stock | Integer | 否 | 库存（无 SKU 时必填，≥0） |
| categoryId | Long | 是 | 分类 ID |
| images | String | 否 | 商品图片（JSON 数组） |
| mainImage | String | 否 | 主图 URL |
| detailHtml | String | 否 | 详情 HTML |
| status | String | 是 | 商品状态（ON_SALE/OFF_SHELF） |
| skus | List\<SkuSnapshot\> | 否 | SKU 列表（有规格商品必填） |
| attributes | List\<AttributeDTO\> | 否 | 属性及属性值列表 |

**出参**：`ProductDetailResult`（见 §7.3）

**异常**：`PARAM_ERROR`、`CATEGORY_NOT_FOUND`

**依赖的外部 API**：`category.api.CategoryApi`（可选，校验分类存在）

---

### 2.4 updateProduct - 编辑商品

| 项 | 定义 |
|---|---|
| **方法签名** | `ProductDetailResult updateProduct(UpdateProductCommand command)` |
| **调用方** | ProductController（管理员） |
| **业务语义** | 编辑商品（更新基本信息 → 重建 SKU → 重建属性 → 刷新冗余字段） |
| **原方法** | `ProductService.updateProduct(Long, ProductUpdateRequest)` → `ProductVO` |

**入参 UpdateProductCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| id | Long | 是 | 商品 ID |
| name | String | 否 | 商品名 |
| description | String | 否 | 商品描述 |
| originalPrice | BigDecimal | 否 | 原价 |
| stock | Integer | 否 | 库存 |
| categoryId | Long | 否 | 分类 ID |
| images | String | 否 | 商品图片 |
| mainImage | String | 否 | 主图 URL |
| detailHtml | String | 否 | 详情 HTML |
| status | String | 否 | 商品状态 |
| skus | List\<SkuSnapshot\> | 否 | SKU 列表（传入则重建） |
| attributes | List\<AttributeDTO\> | 否 | 属性列表（传入则重建） |

**出参**：`ProductDetailResult`

**异常**：`PRODUCT_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：`category.api.CategoryApi`（可选）

---

### 2.5 deleteProduct - 逻辑删除商品

| 项 | 定义 |
|---|---|
| **方法签名** | `void deleteProduct(Long id)` |
| **调用方** | ProductController（管理员） |
| **业务语义** | 逻辑删除商品（同时逻辑删除关联 SKU/属性/评论） |
| **原方法** | `ProductService.deleteProduct(Long)` |

**入参**：`Long id`（商品 ID）

**异常**：`PRODUCT_NOT_FOUND`

**依赖的外部 API**：无

---

### 2.6 existsById - 检查商品是否存在

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean existsById(Long id)` |
| **调用方** | order, cart, coupon, favorite, seckill（跨模块只读校验） |
| **业务语义** | 检查商品是否存在（未逻辑删除） |
| **原方法** | `ProductService.existsById(Long)` |

**入参**：`Long id`

**出参**：`boolean`（true=存在）

**异常**：无

**依赖的外部 API**：无

---

### 2.7 getProductById - 查询商品快照

| 项 | 定义 |
|---|---|
| **方法签名** | `ProductSnapshot getProductById(Long id)` |
| **调用方** | order, cart, coupon, favorite, seckill, stats, ai, category |
| **业务语义** | 根据 ID 查询商品快照（跨模块只读访问，**返回 Snapshot 而非 Entity**） |
| **原方法** | `ProductService.getProductById(Long)` → `Product`（**Entity 泄露，改为返回 ProductSnapshot**） |

**入参**：`Long id`

**出参 ProductSnapshot**：见 §7.1（不存在返回 null）

**异常**：无

**依赖的外部 API**：无

---

### 2.8 getProductsByIds - 批量查询商品快照

| 项 | 定义 |
|---|---|
| **方法签名** | `List<ProductSnapshot> getProductsByIds(List<Long> ids)` |
| **调用方** | order, cart, seckill（跨模块批量只读） |
| **业务语义** | 根据 ID 列表批量查询商品快照 |
| **原方法** | `ProductService.getProductsByIds(List<Long>)` → `List<Product>`（**Entity 泄露，改为返回 List\<ProductSnapshot\>**） |

**入参**：`List<Long> ids`

**出参**：`List<ProductSnapshot>`（见 §7.1）

**异常**：无

**依赖的外部 API**：无

---

### 2.9 countAll - 商品总数

| 项 | 定义 |
|---|---|
| **方法签名** | `long countAll()` |
| **调用方** | stats, seckill |
| **业务语义** | 查询商品总数（封装 Mapper.selectCount，消除跨模块 Mapper 依赖） |
| **原方法** | `ProductService.countAll()` |

**入参**：无

**出参**：`long`

**异常**：无

**依赖的外部 API**：无

---

### 2.10 updateCartCount - 更新加购计数

| 项 | 定义 |
|---|---|
| **方法签名** | `void updateCartCount(UpdateCartCountCommand command)` |
| **调用方** | cart（CartService 加购/移除时维护冗余计数） |
| **业务语义** | 递增/递减商品加购计数（`cart_count = cart_count + delta`，乐观更新避免并发覆盖） |
| **原方法** | `ProductService.updateCartCount(Long productId, int delta)` |

**入参 UpdateCartCountCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productId | Long | 是 | 商品 ID |
| delta | int | 是 | 变化量（+1 或 -1） |

**异常**：无（商品不存在时静默忽略）

**依赖的外部 API**：无

---

### 2.11 updateFavoriteCount - 更新收藏计数

| 项 | 定义 |
|---|---|
| **方法签名** | `void updateFavoriteCount(UpdateFavoriteCountCommand command)` |
| **调用方** | favorite（UserFavoriteService 收藏/取消收藏时维护冗余计数） |
| **业务语义** | 递增/递减商品收藏计数（`favorite_count = favorite_count + delta`） |
| **原方法** | `ProductService.updateFavoriteCount(Long productId, int delta)` |

**入参 UpdateFavoriteCountCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productId | Long | 是 | 商品 ID |
| delta | int | 是 | 变化量（+1 或 -1） |

**异常**：无

**依赖的外部 API**：无

---

## 3. SkuApi（SKU 能力）

> 包路径：`com.seckill.mall.product.api.SkuApi`
> 职责：SKU 查询/保存/价格库存聚合
> 原 Service：`ProductSkuService`（9 方法）
> 实现类：`SkuApplicationService`（`product.application`）

### 3.1 listByProductId - 查询商品所有 SKU

| 项 | 定义 |
|---|---|
| **方法签名** | `List<SkuSnapshot> listByProductId(Long productId)` |
| **调用方** | ProductController（后台商品详情）, order |
| **业务语义** | 查询商品所有 SKU（含禁用，后台用） |
| **原方法** | `ProductSkuService.listByProductId(Long)` → `List<ProductSkuVO>` |

**入参**：`Long productId`

**出参**：`List<SkuSnapshot>`（见 §7.2）

**异常**：无

**依赖的外部 API**：无

---

### 3.2 listEnabledByProductId - 查询启用 SKU

| 项 | 定义 |
|---|---|
| **方法签名** | `List<SkuSnapshot> listEnabledByProductId(Long productId)` |
| **调用方** | ProductSkuController（前台商品详情页）, order, cart |
| **业务语义** | 查询商品所有启用 SKU（status=1，前台用） |
| **原方法** | `ProductSkuService.listEnabledByProductId(Long)` → `List<ProductSkuVO>` |

**入参**：`Long productId`

**出参**：`List<SkuSnapshot>`

**异常**：无

**依赖的外部 API**：无

---

### 3.3 saveSkus - 批量保存 SKU

| 项 | 定义 |
|---|---|
| **方法签名** | `void saveSkus(SaveSkusCommand command)` |
| **调用方** | ProductApplicationService（内部，商品创建/更新时调用） |
| **业务语义** | 批量保存商品 SKU（先逻辑删除旧 SKU，再插入新 SKU） |
| **原方法** | `ProductSkuService.saveSkus(Long, List<ProductSkuDTO>)` |

**入参 SaveSkusCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productId | Long | 是 | 商品 ID |
| skus | List\<SkuSnapshot\> | 是 | SKU 列表（非空） |

**异常**：`PARAM_ERROR`

**依赖的外部 API**：无

---

### 3.4 deleteByProductId - 逻辑删除商品所有 SKU

| 项 | 定义 |
|---|---|
| **方法签名** | `void deleteByProductId(Long productId)` |
| **调用方** | ProductApplicationService（内部，删除商品时调用） |
| **业务语义** | 逻辑删除商品的所有 SKU |
| **原方法** | `ProductSkuService.deleteByProductId(Long)` |

**入参**：`Long productId`

**异常**：无

**依赖的外部 API**：无

---

### 3.5 getSkuById - 查询启用 SKU 快照

| 项 | 定义 |
|---|---|
| **方法签名** | `SkuSnapshot getSkuById(Long skuId)` |
| **调用方** | order, cart, inventory |
| **业务语义** | 根据 SKU ID 获取启用 SKU 快照（校验启用状态，**返回 Snapshot 而非 Entity**） |
| **原方法** | `ProductSkuService.getByIdEnabled(Long)` → `ProductSku`（**Entity 泄露，改为返回 SkuSnapshot**） |

**入参**：`Long skuId`

**出参 SkuSnapshot**：见 §7.2（不存在或已禁用返回 null）

**异常**：无

**依赖的外部 API**：无

---

### 3.6 calculateMinPrice - 计算价格区间最小值

| 项 | 定义 |
|---|---|
| **方法签名** | `BigDecimal calculateMinPrice(Long productId)` |
| **调用方** | ProductApplicationService（内部，刷新冗余字段） |
| **业务语义** | 计算商品价格区间最小值（取最低启用 SKU 价格，无启用 SKU 返回 ZERO） |
| **原方法** | `ProductSkuService.calculateMinPrice(Long)` |

**入参**：`Long productId`

**出参**：`BigDecimal`（无启用 SKU 返回 `BigDecimal.ZERO`）

**异常**：无

**依赖的外部 API**：无

---

### 3.7 calculateMaxPrice - 计算价格区间最大值

| 项 | 定义 |
|---|---|
| **方法签名** | `BigDecimal calculateMaxPrice(Long productId)` |
| **调用方** | ProductApplicationService（内部） |
| **业务语义** | 计算商品价格区间最大值 |
| **原方法** | `ProductSkuService.calculateMaxPrice(Long)` |

**入参**：`Long productId`

**出参**：`BigDecimal`

**异常**：无

**依赖的外部 API**：无

---

### 3.8 calculateTotalStock - 计算商品总库存

| 项 | 定义 |
|---|---|
| **方法签名** | `Integer calculateTotalStock(Long productId)` |
| **调用方** | ProductApplicationService（内部）, inventory |
| **业务语义** | 计算商品总库存（所有启用 SKU 库存之和） |
| **原方法** | `ProductSkuService.calculateTotalStock(Long)` |

**入参**：`Long productId`

**出参**：`Integer`

**异常**：无

**依赖的外部 API**：无

---

### 3.9 refreshTotalStock - 刷新总库存冗余字段

| 项 | 定义 |
|---|---|
| **方法签名** | `void refreshTotalStock(Long productId)` |
| **调用方** | InventoryApplicationService（内部，库存扣减/回补后调用） |
| **业务语义** | 同步刷新 t_product.total_stock 冗余字段，保持列表页展示一致性 |
| **原方法** | `ProductSkuService.refreshTotalStock(Long)` |

**入参**：`Long productId`

**异常**：无

**依赖的外部 API**：无

---

## 4. ReviewApi（评论能力）

> 包路径：`com.seckill.mall.product.api.ReviewApi`
> 职责：评论查询/发表/回复/状态管理
> 原 Service：`ProductReviewService`（5 方法）
> 实现类：`ReviewApplicationService`（`product.application`）

### 4.1 listByProductId - 查询商品评论分页

| 项 | 定义 |
|---|---|
| **方法签名** | `PageResult<ReviewDTO> listByProductId(ReviewListQuery query)` |
| **调用方** | ProductReviewController（公开接口） |
| **业务语义** | 查询商品评论分页（只查 status=1 的显示评论） |
| **原方法** | `ProductReviewService.listByProductId(Long, int, int)` → `PageResult<ProductReviewVO>` |

**入参 ReviewListQuery**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productId | Long | 是 | 商品 ID |
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |

**出参 PageResult\<ReviewDTO\>**：见 §7.5

**异常**：无

**依赖的外部 API**：无

---

### 4.2 createReview - 发表评论

| 项 | 定义 |
|---|---|
| **方法签名** | `ReviewDTO createReview(CreateReviewCommand command)` |
| **调用方** | ProductReviewController（需登录） |
| **业务语义** | 发表评论（校验 SKU 归属 → 校验用户是否购买过该 SKU → 保存评论） |
| **原方法** | `ProductReviewService.create(Long, Long, Long, String, Integer, String)` |

**入参 CreateReviewCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| userId | Long | 是 | 用户 ID（由 CurrentUserContext 注入） |
| productId | Long | 是 | 商品 ID |
| skuId | Long | 否 | SKU ID（null 或 0 表示无规格评论） |
| content | String | 是 | 评论内容（≤1000 字符，已 HTML 转义） |
| rating | Integer | 是 | 评分（1-5） |
| images | String | 否 | 评论图片 URL 数组（JSON 字符串） |

**出参 ReviewDTO**：见 §7.5

**异常**：`PRODUCT_NOT_FOUND`、`SKU_NOT_BELONG_TO_PRODUCT`、`REVIEW_PURCHASE_REQUIRED`（未购买不可评论）、`PARAM_ERROR`

**依赖的外部 API**：`order.api.OrderQueryApi.hasUserPurchased(userId, productId, skuId)`（购买校验）

---

### 4.3 listAllReviews - 后台查询所有评论

| 项 | 定义 |
|---|---|
| **方法签名** | `PageResult<ReviewDTO> listAllReviews(ReviewListQuery query)` |
| **调用方** | AdminReviewController（后台） |
| **业务语义** | 后台查询所有评论（可按 status 筛选） |
| **原方法** | `ProductReviewService.listAll(Integer, int, int)` |

**入参 ReviewListQuery**（后台版，productId 可空）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| status | Integer | 否 | 状态筛选（null=不筛选，1=显示，0=隐藏） |
| pageNum | Integer | 否 | 页码（默认 1） |
| pageSize | Integer | 否 | 每页大小（默认 10） |

**出参**：`PageResult<ReviewDTO>`

**异常**：无

**依赖的外部 API**：无

---

### 4.4 replyReview - 回复评论

| 项 | 定义 |
|---|---|
| **方法签名** | `void replyReview(ReplyReviewCommand command)` |
| **调用方** | AdminReviewController（后台） |
| **业务语义** | 商家/管理员回复评论 |
| **原方法** | `ProductReviewService.reply(Long, String)` |

**入参 ReplyReviewCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reviewId | Long | 是 | 评论 ID |
| replyContent | String | 是 | 回复内容 |

**异常**：`REVIEW_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：无

---

### 4.5 updateReviewStatus - 隐藏/显示评论

| 项 | 定义 |
|---|---|
| **方法签名** | `void updateReviewStatus(UpdateReviewStatusCommand command)` |
| **调用方** | AdminReviewController（后台） |
| **业务语义** | 隐藏/显示评论（status: 1=显示, 0=隐藏） |
| **原方法** | `ProductReviewService.updateStatus(Long, Integer)` |

**入参 UpdateReviewStatusCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| reviewId | Long | 是 | 评论 ID |
| status | Integer | 是 | 状态（1=显示, 0=隐藏） |

**异常**：`REVIEW_NOT_FOUND`、`PARAM_ERROR`

**依赖的外部 API**：无

---

## 5. AttributeApi（属性能力）

> 包路径：`com.seckill.mall.product.api.AttributeApi`
> 职责：商品属性及属性值维护
> 原 Service：`ProductAttributeService`（3 方法）
> 实现类：`AttributeApplicationService`（`product.application`）

### 5.1 listAttributesByProductId - 查询商品属性

| 项 | 定义 |
|---|---|
| **方法签名** | `List<AttributeDTO> listAttributesByProductId(Long productId)` |
| **调用方** | ProductApplicationService（内部，商品详情聚合） |
| **业务语义** | 查询商品的所有属性及其值（按 sortOrder 升序） |
| **原方法** | `ProductAttributeService.listByProductId(Long)` → `List<ProductAttributeVO>` |

**入参**：`Long productId`

**出参**：`List<AttributeDTO>`（见 §7.6）

**异常**：无

**依赖的外部 API**：无

---

### 5.2 saveAttributes - 批量保存属性

| 项 | 定义 |
|---|---|
| **方法签名** | `void saveAttributes(SaveAttributesCommand command)` |
| **调用方** | ProductApplicationService（内部，商品创建/更新时调用） |
| **业务语义** | 批量保存商品的属性及其值（先逻辑删除旧属性，再插入新属性） |
| **原方法** | `ProductAttributeService.saveAttributes(Long, List<ProductAttributeDTO>)` |

**入参 SaveAttributesCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productId | Long | 是 | 商品 ID |
| attributes | List\<AttributeDTO\> | 是 | 属性及属性值列表 |

**异常**：`PARAM_ERROR`

**依赖的外部 API**：无

---

### 5.3 deleteAttributesByProductId - 逻辑删除属性

| 项 | 定义 |
|---|---|
| **方法签名** | `void deleteAttributesByProductId(Long productId)` |
| **调用方** | ProductApplicationService（内部，删除商品时调用） |
| **业务语义** | 逻辑删除商品的所有属性及其值 |
| **原方法** | `ProductAttributeService.deleteByProductId(Long)` |

**入参**：`Long productId`

**异常**：无

**依赖的外部 API**：无

---

## 6. InventoryApi（库存能力）

> 包路径：`com.seckill.mall.product.api.InventoryApi`
> 职责：库存扣减/回补（无规格商品 + SKU）
> 原 Service：`InventoryService`（4 方法）
> 实现类：`InventoryApplicationService`（`product.application`）
> 说明：Phase 8 起统一负责 t_product.stock 与 t_product_sku.stock 的库存操作，建立库存操作统一入口。

### 6.1 deductProductStock - 扣减商品库存

| 项 | 定义 |
|---|---|
| **方法签名** | `int deductProductStock(DeductStockCommand command)` |
| **调用方** | order（OrderApplicationService 创建无规格商品订单时） |
| **业务语义** | 乐观锁扣减商品库存与销量（`WHERE stock >= quantity AND status=ON_SALE`） |
| **原方法** | `InventoryService.deductProductStock(Long, Integer)` |

**入参 DeductStockCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productId | Long | 是 | 商品 ID（无规格商品） |
| quantity | Integer | 是 | 扣减数量（≥1） |

**出参**：`int`（受影响行数：1=成功，0=库存不足或商品不在售）

**异常**：无（通过返回值判断成功/失败）

**依赖的外部 API**：无

---

### 6.2 rollbackProductStock - 回补商品库存

| 项 | 定义 |
|---|---|
| **方法签名** | `void rollbackProductStock(RestoreStockCommand command)` |
| **调用方** | order（取消/超时取消无规格商品订单时） |
| **业务语义** | 回补商品库存与销量（乐观锁，仅对在售商品生效；回补失败仅记录日志，不抛异常） |
| **原方法** | `InventoryService.rollbackProductStock(Long, Integer)` |

**入参 RestoreStockCommand**：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| productId | Long | 是 | 商品 ID |
| quantity | Integer | 是 | 回补数量（≥1） |

**异常**：无（失败静默处理）

**依赖的外部 API**：无

---

### 6.3 deductSkuStock - 扣减 SKU 库存

| 项 | 定义 |
|---|---|
| **方法签名** | `boolean deductSkuStock(DeductStockCommand command)` |
| **调用方** | order（OrderApplicationService 创建有规格商品订单时）, seckill |
| **业务语义** | 扣减 SKU 库存（乐观锁，`stock = stock - quantity WHERE id = ? AND stock >= ?`） |
| **原方法** | `InventoryService.deductSkuStock(Long, Integer)` |

**入参 DeductStockCommand**（SKU 版本）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| skuId | Long | 是 | SKU ID（有规格商品） |
| quantity | Integer | 是 | 扣减数量（≥1） |

**出参**：`boolean`（true=扣减成功，false=库存不足或 SKU 不存在）

**异常**：无

**依赖的外部 API**：无

---

### 6.4 rollbackSkuStock - 回补 SKU 库存

| 项 | 定义 |
|---|---|
| **方法签名** | `void rollbackSkuStock(RestoreStockCommand command)` |
| **调用方** | order（取消/超时取消有规格商品订单时）, seckill |
| **业务语义** | 回补 SKU 库存（`stock = stock + quantity`） |
| **原方法** | `InventoryService.rollbackSkuStock(Long, Integer)` |

**入参 RestoreStockCommand**（SKU 版本）：

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| skuId | Long | 是 | SKU ID |
| quantity | Integer | 是 | 回补数量（≥1） |

**异常**：无

**依赖的外部 API**：无

---

## 7. 数据契约（DTO/Command/Query/Result/Snapshot）

### 7.1 ProductSnapshot（商品快照，替代 Product Entity 跨模块传递）

> 包路径：`com.seckill.mall.product.api.dto.ProductSnapshot`
> 用途：跨模块只读传递，避免暴露 `Product` Entity

| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 商品 ID | Product.id |
| name | String | 商品名 | Product.name |
| description | String | 商品描述 | Product.description |
| originalPrice | BigDecimal | 原价 | Product.originalPrice |
| stock | Integer | 库存（无 SKU 时用） | Product.stock |
| minPrice | BigDecimal | 价格区间最小值（冗余） | Product.minPrice |
| maxPrice | BigDecimal | 价格区间最大值（冗余） | Product.maxPrice |
| totalStock | Integer | 总库存（冗余） | Product.totalStock |
| salesCount | Integer | 销量 | Product.salesCount |
| cartCount | Integer | 加购数量（冗余） | Product.cartCount |
| favoriteCount | Integer | 收藏数量（冗余） | Product.favoriteCount |
| categoryId | Long | 分类 ID | Product.categoryId |
| images | String | 商品图片（JSON） | Product.images |
| mainImage | String | 主图 URL | Product.mainImage |
| detailHtml | String | 详情 HTML | Product.detailHtml |
| status | String | 商品状态码 | Product.status.getCode() |
| createTime | LocalDateTime | 创建时间 | Product.createTime |
| updateTime | LocalDateTime | 更新时间 | Product.updateTime |

> **裁剪字段**：`isDeleted`（基础设施字段，不对外暴露）

### 7.2 SkuSnapshot（SKU 快照，替代 ProductSku Entity 跨模块传递）

> 包路径：`com.seckill.mall.product.api.dto.SkuSnapshot`

| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | SKU ID | ProductSku.id |
| productId | Long | 商品 ID | ProductSku.productId |
| skuCode | String | SKU 编码 | ProductSku.skuCode |
| price | BigDecimal | SKU 价格 | ProductSku.price |
| stock | Integer | SKU 库存 | ProductSku.stock |
| mainImage | String | SKU 主图 URL | ProductSku.mainImage |
| attributes | String | SKU 属性键值对 JSON | ProductSku.attributes |
| status | Integer | 状态（1=启用, 0=禁用） | ProductSku.status |

> **裁剪字段**：`isDeleted`、`createTime`、`updateTime`

### 7.3 ProductDetailResult（商品详情出参）

> 包路径：`com.seckill.mall.product.api.result.ProductDetailResult`

| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 商品 ID | Product.id |
| name | String | 商品名 | Product.name |
| description | String | 商品描述 | Product.description |
| originalPrice | BigDecimal | 原价 | Product.originalPrice |
| stock | Integer | 库存 | Product.stock |
| minPrice | BigDecimal | 价格区间最小值 | Product.minPrice |
| maxPrice | BigDecimal | 价格区间最大值 | Product.maxPrice |
| totalStock | Integer | 总库存 | Product.totalStock |
| salesCount | Integer | 销量 | Product.salesCount |
| cartCount | Integer | 加购数量 | Product.cartCount |
| favoriteCount | Integer | 收藏数量 | Product.favoriteCount |
| categoryId | Long | 分类 ID | Product.categoryId |
| images | String | 商品图片 | Product.images |
| mainImage | String | 主图 URL | Product.mainImage |
| detailHtml | String | 详情 HTML | Product.detailHtml |
| status | String | 商品状态码 | Product.status.getCode() |
| skus | List\<SkuSnapshot\> | SKU 列表 | ProductSku → SkuSnapshot |
| attributes | List\<AttributeDTO\> | 属性及属性值列表 | ProductAttribute → AttributeDTO |
| createTime | LocalDateTime | 创建时间 | Product.createTime |
| updateTime | LocalDateTime | 更新时间 | Product.updateTime |

### 7.4 ProductSummaryDTO（商品摘要，列表/统计用）

> 包路径：`com.seckill.mall.product.api.dto.ProductSummaryDTO`

| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 商品 ID | Product.id |
| name | String | 商品名 | Product.name |
| mainImage | String | 主图 URL | Product.mainImage |
| originalPrice | BigDecimal | 原价 | Product.originalPrice |
| minPrice | BigDecimal | 价格区间最小值 | Product.minPrice |
| maxPrice | BigDecimal | 价格区间最大值 | Product.maxPrice |
| totalStock | Integer | 总库存 | Product.totalStock |
| salesCount | Integer | 销量 | Product.salesCount |
| status | String | 商品状态码 | Product.status.getCode() |
| categoryId | Long | 分类 ID | Product.categoryId |
| createTime | LocalDateTime | 创建时间 | Product.createTime |

### 7.5 ReviewDTO（评论 DTO）

> 包路径：`com.seckill.mall.product.api.dto.ReviewDTO`

| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 评论 ID | ProductReview.id |
| productId | Long | 商品 ID | ProductReview.productId |
| skuId | Long | SKU ID | ProductReview.skuId |
| skuAttributes | String | SKU 属性快照 | ProductReview.skuAttributes |
| userId | Long | 用户 ID | ProductReview.userId |
| orderId | Long | 订单 ID | ProductReview.orderId |
| content | String | 评论内容 | ProductReview.content |
| rating | Integer | 评分（1-5） | ProductReview.rating |
| images | String | 评论图片（JSON） | ProductReview.images |
| status | Integer | 状态（1=显示, 0=隐藏） | ProductReview.status |
| replyContent | String | 回复内容 | ProductReview.replyContent |
| replyTime | LocalDateTime | 回复时间 | ProductReview.replyTime |
| createTime | LocalDateTime | 创建时间 | ProductReview.createTime |

### 7.6 AttributeDTO（属性 DTO）

> 包路径：`com.seckill.mall.product.api.dto.AttributeDTO`

| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 属性 ID | ProductAttribute.id |
| productId | Long | 商品 ID | ProductAttribute.productId |
| categoryAttributeId | Long | 分类属性模板 ID | ProductAttribute.categoryAttributeId |
| name | String | 属性名（如「颜色」） | ProductAttribute.name |
| type | String | 属性类型（IMAGE/TEXT） | ProductAttribute.type.getCode() |
| sortOrder | Integer | 排序序号 | ProductAttribute.sortOrder |
| values | List\<AttributeValueDTO\> | 属性值列表 | ProductAttributeValue → AttributeValueDTO |

#### AttributeValueDTO（属性值 DTO）

| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| id | Long | 属性值 ID | ProductAttributeValue.id |
| attributeId | Long | 属性 ID | ProductAttributeValue.attributeId |
| productId | Long | 商品 ID | ProductAttributeValue.productId |
| value | String | 属性值（如「曜石黑」） | ProductAttributeValue.value |
| imageUrl | String | 图片型属性值的色块 URL | ProductAttributeValue.imageUrl |
| sortOrder | Integer | 排序序号 | ProductAttributeValue.sortOrder |

### 7.7 Command 定义汇总

#### CreateProductCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| name | String | 是 | 商品名 | ProductCreateRequest.name |
| description | String | 否 | 商品描述 | ProductCreateRequest.description |
| originalPrice | BigDecimal | 是 | 原价 | ProductCreateRequest.originalPrice |
| stock | Integer | 否 | 库存 | ProductCreateRequest.stock |
| categoryId | Long | 是 | 分类 ID | ProductCreateRequest.categoryId |
| images | String | 否 | 商品图片 | ProductCreateRequest.images |
| mainImage | String | 否 | 主图 URL | ProductCreateRequest.mainImage |
| detailHtml | String | 否 | 详情 HTML | ProductCreateRequest.detailHtml |
| status | String | 是 | 商品状态 | ProductCreateRequest.status |
| skus | List\<SkuSnapshot\> | 否 | SKU 列表 | ProductCreateRequest.skus |
| attributes | List\<AttributeDTO\> | 否 | 属性列表 | ProductCreateRequest.attributes |

#### UpdateProductCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| id | Long | 是 | 商品 ID | 路径参数 |
| name | String | 否 | 商品名 | ProductUpdateRequest.name |
| description | String | 否 | 商品描述 | ProductUpdateRequest.description |
| originalPrice | BigDecimal | 否 | 原价 | ProductUpdateRequest.originalPrice |
| stock | Integer | 否 | 库存 | ProductUpdateRequest.stock |
| categoryId | Long | 否 | 分类 ID | ProductUpdateRequest.categoryId |
| images | String | 否 | 商品图片 | ProductUpdateRequest.images |
| mainImage | String | 否 | 主图 URL | ProductUpdateRequest.mainImage |
| detailHtml | String | 否 | 详情 HTML | ProductUpdateRequest.detailHtml |
| status | String | 否 | 商品状态 | ProductUpdateRequest.status |
| skus | List\<SkuSnapshot\> | 否 | SKU 列表 | ProductUpdateRequest.skus |
| attributes | List\<AttributeDTO\> | 否 | 属性列表 | ProductUpdateRequest.attributes |

#### SaveSkusCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| productId | Long | 是 | 商品 ID | 内部传入 |
| skus | List\<SkuSnapshot\> | 是 | SKU 列表 | ProductSkuDTO 列表 |

#### SaveAttributesCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| productId | Long | 是 | 商品 ID | 内部传入 |
| attributes | List\<AttributeDTO\> | 是 | 属性列表 | ProductAttributeDTO 列表 |

#### CreateReviewCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| userId | Long | 是 | 用户 ID | CurrentUserContext |
| productId | Long | 是 | 商品 ID | ReviewCreateRequest.productId |
| skuId | Long | 否 | SKU ID | ReviewCreateRequest.skuId |
| content | String | 是 | 评论内容（已 HTML 转义） | ReviewCreateRequest.content |
| rating | Integer | 是 | 评分 | ReviewCreateRequest.rating |
| images | String | 否 | 评论图片 | ReviewCreateRequest.images |

#### ReplyReviewCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| reviewId | Long | 是 | 评论 ID | 路径参数 |
| replyContent | String | 是 | 回复内容 | 请求体 |

#### UpdateReviewStatusCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| reviewId | Long | 是 | 评论 ID | 路径参数 |
| status | Integer | 是 | 状态 | 请求体 |

#### DeductStockCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| productId | Long | 否 | 商品 ID（无规格商品） | 内部传入 |
| skuId | Long | 否 | SKU ID（有规格商品） | 内部传入 |
| quantity | Integer | 是 | 扣减数量 | 内部传入 |

> **约束**：`productId` 与 `skuId` 二选一（无规格商品传 productId，有规格商品传 skuId）。

#### RestoreStockCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| productId | Long | 否 | 商品 ID（无规格商品） | 内部传入 |
| skuId | Long | 否 | SKU ID（有规格商品） | 内部传入 |
| quantity | Integer | 是 | 回补数量 | 内部传入 |

#### UpdateCartCountCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| productId | Long | 是 | 商品 ID | 内部传入 |
| delta | int | 是 | 变化量（+1/-1） | 内部传入 |

#### UpdateFavoriteCountCommand
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| productId | Long | 是 | 商品 ID | 内部传入 |
| delta | int | 是 | 变化量（+1/-1） | 内部传入 |

### 7.8 Query 定义汇总

#### ProductListQuery
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| categoryId | Long | 否 | 分类 ID | ProductQueryRequest.categoryId |
| status | String | 否 | 商品状态 | ProductQueryRequest.status |
| keyword | String | 否 | 关键字 | ProductQueryRequest.keyword |
| pageNum | Integer | 否 | 页码（默认 1） | ProductQueryRequest.pageNum |
| pageSize | Integer | 否 | 每页大小（默认 10） | ProductQueryRequest.pageSize |

#### ReviewListQuery
| 字段 | 类型 | 必填 | 说明 | 来源 |
|---|---|---|---|---|
| productId | Long | 否 | 商品 ID（前台必填） | 路径参数 |
| status | Integer | 否 | 状态筛选（后台用） | 请求参数 |
| pageNum | Integer | 否 | 页码（默认 1） | 请求参数 |
| pageSize | Integer | 否 | 每页大小（默认 10） | 请求参数 |

### 7.9 Result 定义汇总

#### ProductDetailResult
见 §7.3

#### StockCheckResult（库存校验出参，预留）
| 字段 | 类型 | 说明 | 来源映射 |
|---|---|---|---|
| available | boolean | 库存是否充足 | 扣减结果 |
| currentStock | Integer | 当前库存 | Product.stock / ProductSku.stock |

---

## 8. 异常定义

| 错误码 | 含义 | 触发场景 | 原 ErrorCode |
|---|---|---|---|
| `PRODUCT_NOT_FOUND` | 商品不存在 | 查询/编辑/删除时商品 ID 无效 | ErrorCode.PRODUCT_NOT_FOUND |
| `SKU_NOT_FOUND` | SKU 不存在 | 查询 SKU 时 ID 无效 | ErrorCode.SKU_NOT_FOUND |
| `SKU_NOT_BELONG_TO_PRODUCT` | SKU 不属于该商品 | 发表评论时 skuId 与 productId 不匹配 | ErrorCode.SKU_NOT_BELONG_TO_PRODUCT |
| `REVIEW_NOT_FOUND` | 评论不存在 | 回复/更新状态时评论 ID 无效 | ErrorCode.REVIEW_NOT_FOUND |
| `REVIEW_PURCHASE_REQUIRED` | 未购买不可评论 | 发表评论时校验用户未购买该商品 | ErrorCode.REVIEW_PURCHASE_REQUIRED |
| `STOCK_NOT_ENOUGH` | 库存不足 | 扣减库存时库存 < 需求量 | ErrorCode.STOCK_NOT_ENOUGH |
| `PRODUCT_OFF_SHELF` | 商品已下架 | 操作时商品状态非在售 | ErrorCode.PRODUCT_OFF_SHELF |
| `CATEGORY_NOT_FOUND` | 分类不存在 | 创建/编辑商品时分类 ID 无效 | ErrorCode.CATEGORY_NOT_FOUND |
| `PARAM_ERROR` | 参数校验失败 | 入参校验不通过 | ErrorCode.PARAM_ERROR |

---

## 9. API 不变性（契约约束）

1. **方法签名向后兼容**：API 方法一旦发布，签名只增不改不删。新增方法不影响已有调用方。
2. **DTO/Snapshot 字段只增不删**：已发布 DTO 的字段不可删除，字段类型不可改变（Long 不可变 Integer，BigDecimal 不可变 double）。
3. **Command 字段可新增可选字段**：新增字段必须可选（nullable），不能删除已有字段。
4. **错误码只增不删**：已定义错误码不可删除或改变含义，可新增错误码。
5. **返回类型不可窄化**：如 `ProductSnapshot` 不可改为字段更少的子类型，只可扩展。
6. **枚举值只增不删**：`ProductStatus` 枚举不可删除已有值（ON_SALE/OFF_SHELF），可新增；`AttributeType` 枚举不可删除已有值（IMAGE/TEXT），可新增。
7. **Entity → Snapshot 映射稳定**：`Product` → `ProductSnapshot`、`ProductSku` → `SkuSnapshot` 的字段映射关系一旦确定不可变更（Snapshot 字段来源不可换为其他 Entity）。

---

## 10. 与外部模块 API 的交互契约

### 10.1 product → order.api（product 依赖 order）

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `order.api.OrderQueryApi.hasUserPurchased(userId, productId, skuId)` | 校验用户是否购买 | 发表评论前购买校验 | ReviewApi.createReview |

> **注意**：product → order 是单向依赖（仅评论购买校验）。order 不因评论校验反查 product（打破循环）。order 模块已完成迁移，`OrderQueryApi.hasUserPurchased()` 已就绪。

### 10.2 product → shared.kernel

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `shared.kernel.CurrentUserContext.getCurrentUser()` | 获取当前用户 | Controller 注入 userId | ProductReviewController.createReview |

### 10.3 product → category.api（可选）

| 调用 | 方法 | 用途 | 调用位置 |
|---|---|---|---|
| `category.api.CategoryApi.getCategoryById(categoryId)` | 校验分类存在 | 创建/编辑商品时 | ProductApi.createProduct, updateProduct |

### 10.4 外部 → product.api（外部调用 product）

| 调用方 | 调用的 product API | 用途 | 原调用 |
|---|---|---|---|
| **order**（OrderApplicationService） | `ProductApi.getProductById()` / `getProductsByIds()` / `existsById()` | 创建订单时校验商品状态/获取价格快照 | `ProductService.getProductById()` → `Product` |
| **order**（OrderApplicationService） | `SkuApi.getSkuById()` / `listEnabledByProductId()` | 创建订单时获取 SKU 价格/属性 | `ProductSkuService.getByIdEnabled()` → `ProductSku` |
| **order**（OrderApplicationService） | `InventoryApi.deductProductStock()` / `deductSkuStock()` | 创建订单时扣库存 | `InventoryService.deductProductStock()` / `deductSkuStock()` |
| **order**（OrderApplicationService） | `InventoryApi.rollbackProductStock()` / `rollbackSkuStock()` | 取消/超时取消时回补库存 | `InventoryService.rollbackProductStock()` / `rollbackSkuStock()` |
| **cart**（CartApplicationService） | `ProductApi.getProductById()` / `existsById()` | 加购时校验商品 | `ProductService.getProductById()` |
| **cart**（CartApplicationService） | `SkuApi.getSkuById()` | 加购时校验 SKU | `ProductSkuService.getByIdEnabled()` |
| **cart**（CartApplicationService） | `ProductApi.updateCartCount()` | 加购/移除时维护冗余计数 | `ProductService.updateCartCount()` |
| **coupon**（CouponApplicationService） | `ProductApi.getProductById()` | 优惠券适用商品校验 | `ProductService.getProductById()` |
| **favorite**（UserFavoriteApplicationService） | `ProductApi.getProductById()` / `existsById()` | 收藏时校验商品 | `ProductService.getProductById()` |
| **favorite**（UserFavoriteApplicationService） | `ProductApi.updateFavoriteCount()` | 收藏/取消时维护冗余计数 | `ProductService.updateFavoriteCount()` |
| **seckill**（SeckillOrderApplicationService） | `ProductApi.getProductById()` / `countAll()` | 秒杀活动关联商品校验 | `ProductService.getProductById()` |
| **seckill**（SeckillGoodsApplicationService） | `ProductApi.getProductById()` | 秒杀商品管理 | `ProductService.getProductById()` |
| **seckill**（SeckillActivityApplicationService） | `ProductApi.getProductById()` | 秒杀活动创建时校验商品 | `ProductService.getProductById()` |
| **seckill**（SeckillOrderConsumer） | `ProductApi.getProductById()` | 秒杀订单消费时获取商品快照 | `ProductMapper.selectById()`（**消除直接 Mapper 依赖**） |
| **seckill**（SeckillOrderApplicationService） | `InventoryApi.deductSkuStock()` / `rollbackSkuStock()` | 秒杀订单扣减/回补 SKU 库存 | `InventoryService.deductSkuStock()` / `rollbackSkuStock()` |
| **stats**（StatsApplicationService） | `ProductApi.countAll()` / `listProducts()` | 统计概览商品数/列表 | `ProductService.countAll()` / `listProducts()` |
| **ai**（ProductSearchTool） | `ProductApi.listProducts()` / `getProductDetail()` | AI 商品搜索 | `ProductService.listProducts()` / `getProductDetail()` |
| **category**（CategoryApplicationService） | `ProductApi.countAll()`（或等价查询） | 分类下商品数量统计 | `ProductMapper.selectCount()`（**消除直接 Mapper 依赖**） |
| **review/admin**（AdminReviewController） | `ReviewApi.listAllReviews()` / `replyReview()` / `updateReviewStatus()` | 后台评论管理 | `ProductReviewService.listAll()` / `reply()` / `updateStatus()` |

---

## 11. 契约汇总

| 类别 | 数量 | 清单 |
|---|---|---|
| **API 接口** | 5 | ProductApi, SkuApi, ReviewApi, AttributeApi, InventoryApi |
| **API 方法** | 32 | ProductApi(11) + SkuApi(9) + ReviewApi(5) + AttributeApi(3) + InventoryApi(4) |
| **Command** | 11 | CreateProductCommand, UpdateProductCommand, SaveSkusCommand, SaveAttributesCommand, CreateReviewCommand, ReplyReviewCommand, UpdateReviewStatusCommand, DeductStockCommand, RestoreStockCommand, UpdateCartCountCommand, UpdateFavoriteCountCommand |
| **Query** | 2 | ProductListQuery, ReviewListQuery |
| **Result** | 2 | ProductDetailResult, StockCheckResult |
| **DTO/Snapshot** | 6 | ProductSnapshot, SkuSnapshot, ProductSummaryDTO, ReviewDTO, AttributeDTO, AttributeValueDTO |
| **异常错误码** | 9 | PRODUCT_NOT_FOUND, SKU_NOT_FOUND, SKU_NOT_BELONG_TO_PRODUCT, REVIEW_NOT_FOUND, REVIEW_PURCHASE_REQUIRED, STOCK_NOT_ENOUGH, PRODUCT_OFF_SHELF, CATEGORY_NOT_FOUND, PARAM_ERROR |
| **product 依赖的外部 API** | 2 | order.api.OrderQueryApi, shared.kernel.CurrentUserContext（+ 可选 category.api.CategoryApi） |
| **依赖 product 的外部模块** | 9 | order, cart, coupon, favorite, seckill, stats, ai, category, review/admin |

---

## 12. 与 Order 模块 API 契约的差异对比

| 维度 | Order API Contract | Product API Contract | 说明 |
|---|---|---|---|
| API 接口数 | 3 | 5 | Product 多 SkuApi/AttributeApi/InventoryApi |
| API 方法数 | 14 | 32 | Product 方法更多（SKU/属性/库存/评论能力丰富） |
| Command 数 | 7 | 11 | Product 多库存/计数维护 Command |
| DTO/Snapshot 数 | 9 | 6 | Order DTO 更多（订单明细/列表/快照等） |
| 异常错误码数 | 16 | 9 | Product 异常更少（业务场景更聚焦） |
| 对外依赖的 API 数 | 7 | 2 | Product 对外依赖更少（仅 order 购买校验 + CurrentUserContext） |
| 被依赖的外部模块数 | 5 | 9 | Product 被依赖更多（商品是核心领域） |
| Entity 泄露方法数 | 1（getWalletPaidOrdersByUser） | 3（getProductById/getProductsByIds/getByIdEnabled） | Product 需消除更多 Entity 泄露 |
| 跨模块 Mapper 依赖 | 0 | 2（category/seckill 直接引用 ProductMapper） | Product 需消除违规 Mapper 引用 |

**结论**：Product 模块 API 契约方法数更多（32 vs 14），但对外依赖更少（2 vs 7），迁移前置条件更轻。主要工作量在 P.4-C 阶段：9 个外部模块的调用方需切换到 product.api，并消除 3 处 Entity 泄露 + 2 处违规 Mapper 引用。

---

> **本文件为 Phase 3 Product 模块迁移前置设计文档，未创建任何代码/接口/包，未修改任何现有代码。**
> **本文件与 PRODUCT-MIGRATION-PLAN.md 共同构成 Product 模块迁移的完整设计依据。**
> **下一步**：基于本契约，进入实施阶段——按迁移步骤（P.0 → P.6）创建 API 接口与 DTO，迁移代码。