# Product 模块迁移计划 (PRODUCT-MIGRATION-PLAN)

> 生成时间：2026-08-17
> 阶段：Phase 3 - Product 模块迁移（Strangler Pattern 第一阶段）
> 依据：ORDER-MIGRATION-PLAN.md（Order 模块迁移经验）+ Pragmatic DDD + Modular Monolith + Strangler Pattern
> 说明：纯架构设计文档，未创建任何代码/接口/包，未修改任何现有代码。

---

## 1. Product 当前结构

### 1.1 当前包结构

Product 相关代码散落在 5 个不同的顶层包中，共 **18 个核心文件**（不含 DTO/VO）：

| 文件 | 当前包 | 职责 |
|---|---|---|
| `ProductController` | `controller` | 商品 CRUD 与分页（/api/v1/products） |
| `ProductSkuController` | `controller` | 前台查询商品启用 SKU（/api/v1/products/{productId}/skus） |
| `AdminProductSkuController` | `controller` | 后台 SKU 笛卡尔积生成（/api/v1/admin/product/skus） |
| `ProductReviewController` | `controller` | 前台商品评论查询/发表（/api/v1/reviews） |
| `ProductService` | `service` | 商品 CRUD + 跨模块只读访问 + 冗余计数维护 |
| `ProductSkuService` | `service` | SKU 查询/保存/价格库存聚合 |
| `ProductReviewService` | `service` | 评论查询/发表/回复/状态管理 |
| `ProductAttributeService` | `service` | 商品属性及属性值维护 |
| `ProductServiceImpl` | `service.impl` | ProductService 实现 |
| `ProductSkuServiceImpl` | `service.impl` | ProductSkuService 实现 |
| `ProductReviewServiceImpl` | `service.impl` | ProductReviewService 实现 |
| `ProductAttributeServiceImpl` | `service.impl` | ProductAttributeService 实现 |
| `Product` | `entity` | 商品 PO（t_product） |
| `ProductSku` | `entity` | 商品 SKU PO（t_product_sku） |
| `ProductReview` | `entity` | 商品评论 PO（t_product_review） |
| `ProductAttribute` | `entity` | 商品属性 PO（t_product_attribute） |
| `ProductAttributeValue` | `entity` | 商品属性值 PO（t_product_attribute_value） |
| `ProductMapper` | `mapper` | 商品 Mapper |
| `ProductSkuMapper` | `mapper` | 商品 SKU Mapper |
| `ProductReviewMapper` | `mapper` | 商品评论 Mapper |
| `ProductAttributeMapper` | `mapper` | 商品属性 Mapper |
| `ProductAttributeValueMapper` | `mapper` | 商品属性值 Mapper |

> 另：`InventoryService` / `InventoryServiceImpl` 属于 product 模块的库存能力（依赖 Product/ProductSku entity + ProductMapper/ProductSkuMapper），本计划将其纳入 product 模块一并迁移。

### 1.2 Service 方法签名

#### ProductService（11 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `listProducts` | `PageResult<ProductVO> listProducts(ProductQueryRequest)` | 商品列表分页 | ✅ |
| `getProductDetail` | `ProductVO getProductDetail(Long id)` | 商品详情 | ✅ |
| `createProduct` | `ProductVO createProduct(ProductCreateRequest)` | 新增商品 | ✅ |
| `updateProduct` | `ProductVO updateProduct(Long id, ProductUpdateRequest)` | 编辑商品 | ✅ |
| `deleteProduct` | `void deleteProduct(Long id)` | 逻辑删除商品 | ✅ |
| `existsById` | `boolean existsById(Long id)` | 检查商品是否存在 | ✅ 跨模块只读 |
| `getProductById` | `Product getProductById(Long id)` | 根据 ID 查询商品实体 | ⚠️ 返回 Entity（跨模块只读访问） |
| `getProductsByIds` | `List<Product> getProductsByIds(List<Long> ids)` | 批量查询商品实体 | ⚠️ 返回 Entity |
| `countAll` | `long countAll()` | 商品总数 | ✅ 封装 Mapper，消除跨模块 Mapper 依赖 |
| `updateCartCount` | `void updateCartCount(Long productId, int delta)` | 递增/递减加购计数 | ✅ 被 cart 调用 |
| `updateFavoriteCount` | `void updateFavoriteCount(Long productId, int delta)` | 递增/递减收藏计数 | ✅ 被 favorite 调用 |

#### ProductSkuService（9 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `listByProductId` | `List<ProductSkuVO> listByProductId(Long productId)` | 查询商品所有 SKU（含禁用，后台用） | ✅ |
| `listEnabledByProductId` | `List<ProductSkuVO> listEnabledByProductId(Long productId)` | 查询启用 SKU（前台用） | ✅ |
| `saveSkus` | `void saveSkus(Long productId, List<ProductSkuDTO> skus)` | 批量保存 SKU | ✅ |
| `deleteByProductId` | `void deleteByProductId(Long productId)` | 逻辑删除商品所有 SKU | ✅ |
| `getByIdEnabled` | `ProductSku getByIdEnabled(Long skuId)` | 根据 SKU ID 获取启用 SKU 实体 | ⚠️ 返回 Entity |
| `calculateMinPrice` | `BigDecimal calculateMinPrice(Long productId)` | 计算价格区间最小值 | ✅ |
| `calculateMaxPrice` | `BigDecimal calculateMaxPrice(Long productId)` | 计算价格区间最大值 | ✅ |
| `calculateTotalStock` | `Integer calculateTotalStock(Long productId)` | 计算商品总库存 | ✅ |
| `refreshTotalStock` | `void refreshTotalStock(Long productId)` | 刷新 t_product.total_stock 冗余字段 | ✅ |

#### ProductReviewService（5 个方法）

| 方法 | 签名 | 性质 | 问题 |
|---|---|---|---|
| `listByProductId` | `PageResult<ProductReviewVO> listByProductId(Long productId, int pageNum, int pageSize)` | 查询商品评论分页 | ✅ |
| `create` | `ProductReviewVO create(Long userId, Long productId, Long skuId, String content, Integer rating, String images)` | 发表评论（含购买校验） | ⚠️ 依赖 t_normal_order_item 做购买校验（跨模块依赖 order） |
| `listAll` | `PageResult<ProductReviewVO> listAll(Integer status, int pageNum, int pageSize)` | 后台查询所有评论 | ✅ |
| `reply` | `void reply(Long id, String replyContent)` | 回复评论 | ✅ |
| `updateStatus` | `void updateStatus(Long id, Integer status)` | 隐藏/显示评论 | ✅ |

#### ProductAttributeService（3 个方法）

| 方法 | 签名 | 性质 |
|---|---|---|
| `listByProductId` | `List<ProductAttributeVO> listByProductId(Long productId)` | 查询商品所有属性及值 |
| `saveAttributes` | `void saveAttributes(Long productId, List<ProductAttributeDTO> attributes)` | 批量保存商品属性及值 |
| `deleteByProductId` | `void deleteByProductId(Long productId)` | 逻辑删除商品所有属性及值 |

### 1.3 Controller 端点

| Controller | 基路径 | 端点 | 方法 | 权限 |
|---|---|---|---|---|
| `ProductController` | `/api/v1/products` | `GET /` | 商品列表分页 | 公开 |
| | | `GET /{id}` | 商品详情 | 公开 |
| | | `POST /` | 新增商品 | ADMIN |
| | | `PUT /{id}` | 编辑商品 | ADMIN |
| | | `DELETE /{id}` | 逻辑删除商品 | ADMIN |
| `ProductSkuController` | `/api/v1/products/{productId}/skus` | `GET /` | 查询启用 SKU | 公开 |
| `AdminProductSkuController` | `/api/v1/admin/product/skus` | `POST /generate` | SKU 笛卡尔积生成 | ADMIN |
| `ProductReviewController` | `/api/v1/reviews` | `GET /product/{productId}` | 查商品评论分页 | 公开 |
| | | `POST /create` | 发表评论 | BUYER/SELLER/ADMIN |

### 1.4 Entity 字段

#### Product（t_product，19 字段）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键（ASSIGN_ID） |
| `name` | String | 商品名 |
| `description` | String | 商品描述 |
| `originalPrice` | BigDecimal | 原价 |
| `stock` | Integer | 库存（无 SKU 时用） |
| `minPrice` | BigDecimal | 价格区间最小值（冗余） |
| `maxPrice` | BigDecimal | 价格区间最大值（冗余） |
| `totalStock` | Integer | 总库存（冗余） |
| `salesCount` | Integer | 销量 |
| `cartCount` | Integer | 加购数量（冗余计数） |
| `favoriteCount` | Integer | 收藏数量（冗余计数） |
| `categoryId` | Long | 分类 ID |
| `images` | String | 商品图片 |
| `mainImage` | String | 主图 |
| `detailHtml` | String | 详情 HTML |
| `status` | ProductStatus | 状态枚举 |
| `isDeleted` | Integer | 逻辑删除 |
| `createTime` | LocalDateTime | 创建时间 |
| `updateTime` | LocalDateTime | 更新时间 |

#### ProductSku（t_product_sku，11 字段）

`id, productId, skuCode, price, stock, mainImage, attributes(JSON), status, isDeleted, createTime, updateTime`

#### ProductReview（t_product_review，15 字段）

`id, productId, skuId, skuAttributes, userId, orderId, content, rating, images, status, replyContent, replyTime, isDeleted, createTime, updateTime`

#### ProductAttribute（t_product_attribute，9 字段）

`id, productId, categoryAttributeId, name, type(AttributeType), sortOrder, isDeleted, createTime, updateTime`

#### ProductAttributeValue（t_product_attribute_value，9 字段）

`id, attributeId, productId, value, imageUrl, sortOrder, isDeleted, createTime, updateTime`

### 1.5 当前问题

#### 跨模块 Entity 引用（外部模块引用 Product Entity）

| 被引用的 Entity | 引用方 | 所属模块 | 问题 |
|---|---|---|---|
| `Product` | OrderServiceImpl, OrderQueryServiceImpl | order | order 直接引用 product Entity |
| `Product` | CartServiceImpl | cart | cart 直接引用 product Entity |
| `Product` | UserFavoriteServiceImpl | favorite | favorite 直接引用 product Entity |
| `Product` | SeckillGoodsServiceImpl, SeckillActivityServiceImpl, SeckillOrderConsumer | seckill | seckill 直接引用 product Entity |
| `Product` | CouponServiceImpl | coupon | coupon 直接引用 product Entity |
| `Product` | CategoryServiceImpl | category | category 直接引用 product Entity |
| `Product` | InventoryServiceImpl | product(inventory) | 自身模块（库存能力） |
| `ProductSku` | OrderServiceImpl | order | order 直接引用 product Entity |
| `ProductSku` | CartServiceImpl | cart | cart 直接引用 product Entity |
| `ProductSku` | InventoryServiceImpl | product(inventory) | 自身模块 |
| `ProductReview` | ProductReviewServiceImpl | product(review) | 自身模块 |
| `ProductAttribute` / `ProductAttributeValue` | ProductAttributeServiceImpl | product | 自身模块 |

**跨模块 Entity 引用：2 类 Entity（Product/ProductSku）被 6 个外部模块引用**

#### 跨模块 Mapper 引用（外部模块直接引用 Product Mapper）

| 被引用的 Mapper | 引用方 | 所属模块 | 问题 |
|---|---|---|---|
| `ProductMapper` | CategoryServiceImpl | category | ⚠️ category 直接操作 product Mapper |
| `ProductMapper` | SeckillOrderConsumer | seckill(mq) | ⚠️ seckill 消费者直接操作 product Mapper |
| `ProductMapper` / `ProductSkuMapper` | InventoryServiceImpl | product(inventory) | 自身模块 |

**跨模块 Mapper 引用：2 个外部模块（category, seckill）直接引用 Product Mapper**

#### Service 暴露 Entity 给外部

| Service 方法 | 返回类型 | 调用方 | 问题 |
|---|---|---|---|
| `ProductService.getProductById(Long)` | `Product`（Entity） | order, cart, coupon, favorite, seckill, stats, ai | ⚠️ API 返回 Entity |
| `ProductService.getProductsByIds(List<Long>)` | `List<Product>`（Entity） | 外部模块 | ⚠️ API 返回 Entity |
| `ProductSkuService.getByIdEnabled(Long)` | `ProductSku`（Entity） | order, cart, inventory | ⚠️ API 返回 Entity |

#### ProductReviewService 跨模块依赖 order

| 依赖 | 说明 |
|---|---|
| `ProductReviewService.create()` | 校验用户是否购买过该 SKU，查询 t_normal_order_item（依赖 order 模块） |

---

## 2. Product 目标结构

### 2.1 目标包结构

```
com.seckill.mall.product
├── api/                          # Public API（接口 + DTO + Command + Query + Result）
│   ├── ProductApi.java           # 商品业务能力（CRUD + 只读查询 + 冗余计数维护）
│   ├── SkuApi.java               # SKU 能力（查询/保存/价格库存聚合）
│   ├── ReviewApi.java            # 评论能力（查询/发表/回复/状态管理）
│   ├── AttributeApi.java         # 属性能力（查询/保存/删除）
│   ├── InventoryApi.java         # 库存能力（扣减/回补/查询）—— 从 InventoryService 提升
│   ├── dto/                      # 对外数据契约（Snapshot）
│   │   ├── ProductSnapshot.java      # 商品快照（替代 Product Entity 跨模块传递）
│   │   ├── SkuSnapshot.java          # SKU 快照（替代 ProductSku Entity）
│   │   ├── ReviewDTO.java            # 评论 DTO
│   │   ├── AttributeDTO.java         # 属性 DTO
│   │   └── ProductSummaryDTO.java    # 商品摘要（列表/统计用）
│   ├── command/                  # 入参 Command
│   │   ├── CreateProductCommand.java
│   │   ├── UpdateProductCommand.java
│   │   ├── SaveSkusCommand.java
│   │   ├── SaveAttributesCommand.java
│   │   ├── CreateReviewCommand.java
│   │   ├── ReplyReviewCommand.java
│   │   ├── UpdateReviewStatusCommand.java
│   │   ├── DeductStockCommand.java
│   │   ├── RestoreStockCommand.java
│   │   ├── UpdateCartCountCommand.java
│   │   └── UpdateFavoriteCountCommand.java
│   ├── query/                    # 查询入参 Query
│   │   ├── ProductListQuery.java
│   │   └── ReviewListQuery.java
│   └── result/                   # 出参 Result
│       ├── ProductDetailResult.java
│       └── StockCheckResult.java
├── application/                  # 应用服务（用例编排，实现 api 接口）
│   ├── ProductApplicationService.java    # 实现 ProductApi
│   ├── SkuApplicationService.java        # 实现 SkuApi
│   ├── ReviewApplicationService.java     # 实现 ReviewApi
│   ├── AttributeApplicationService.java  # 实现 AttributeApi
│   └── InventoryApplicationService.java  # 实现 InventoryApi
├── domain/                       # 领域模型
│   ├── ProductStatus.java            # 商品状态枚举
│   ├── AttributeType.java            # 属性类型枚举
│   └── model/                        # 领域模型（可选，Pragmatic DDD 初期可空）
├── infrastructure/               # 持久化
│   ├── mapper/
│   │   ├── ProductMapper.java
│   │   ├── ProductSkuMapper.java
│   │   ├── ProductReviewMapper.java
│   │   ├── ProductAttributeMapper.java
│   │   └── ProductAttributeValueMapper.java
│   ├── entity/                   # PO（模块内部可见）
│   │   ├── Product.java
│   │   ├── ProductSku.java
│   │   ├── ProductReview.java
│   │   ├── ProductAttribute.java
│   │   └── ProductAttributeValue.java
│   └── repository/               # Repository（可选，封装 Mapper）
├── interfaces/                   # Controller
│   ├── ProductController.java
│   ├── ProductSkuController.java
│   ├── AdminProductSkuController.java
│   ├── ProductReviewController.java
│   └── vo/                       # 面向前端的 VO
│       ├── ProductVO.java
│       ├── ProductSkuVO.java
│       ├── ProductReviewVO.java
│       └── ProductAttributeVO.java
└── mq/                           # MQ（product 当前无 MQ，预留）
```

### 2.2 目标依赖规则

| 层 | 允许依赖 | 禁止依赖 |
|---|---|---|
| `product.api` | shared.kernel（仅常量/枚举）、JDK | 任何模块的 infrastructure/mapper/domain/entity |
| `product.application` | product.api、product.domain、product.infrastructure（自身）、其他模块的 **api**、shared.kernel | 其他模块的 infrastructure/mapper/domain/entity |
| `product.infrastructure` | product.domain（自身）、MyBatis-Plus、shared.kernel | 任何业务模块 |
| `product.interfaces` | product.application、product.api.dto/command/query、product.interfaces.vo、shared.kernel（CurrentUserContext） | product.infrastructure（Mapper）、其他模块的 Service |
| `product.mq` | product.application、product.api、shared.kernel（MessageBusPort） | 其他模块的 Service/Mapper |

### 2.3 API 边界设计

#### 对外暴露的 API

| API 接口 | 方法数 | 暴露给 | 说明 |
|---|---|---|---|
| `ProductApi` | 11 | ProductController, order, cart, coupon, favorite, seckill, stats, ai, category | 商品 CRUD + 只读快照查询 + 冗余计数维护 |
| `SkuApi` | 9 | ProductSkuController, order, cart, inventory | SKU 查询/保存/价格库存聚合 |
| `ReviewApi` | 5 | ProductReviewController, AdminReviewController | 评论查询/发表/回复/状态管理 |
| `AttributeApi` | 3 | （内部为主，商品创建/更新时调用） | 属性查询/保存/删除 |
| `InventoryApi` | 4 | order, seckill | 库存扣减/回补/查询/校验 |

#### 对内隐藏的实现

| 隐藏项 | 类型 | 说明 |
|---|---|---|
| `Product` / `ProductSku` / `ProductReview` / `ProductAttribute` / `ProductAttributeValue` | Entity/PO | 仅 product.infrastructure 内部可见 |
| 5 个 Mapper | Mapper | 仅 product.infrastructure 内部可见 |
| 5 个 ApplicationService | ApplicationService | 实现细节，不对外暴露 |
| `ProductServiceImpl` / `ProductSkuServiceImpl` / `ProductReviewServiceImpl` / `ProductAttributeServiceImpl` / `InventoryServiceImpl` | （合并删除） | 逻辑并入 ApplicationService |

#### Entity → Snapshot 映射（消除跨模块 Entity 引用）

| 原 Entity 返回 | 新 Snapshot/DTO | 字段裁剪 |
|---|---|---|
| `Product` | `ProductSnapshot` | id, name, mainImage, originalPrice, minPrice, maxPrice, totalStock, status, categoryId（按需裁剪，不暴露 isDeleted/createTime） |
| `List<Product>` | `List<ProductSnapshot>` | 同上 |
| `ProductSku` | `SkuSnapshot` | id, productId, skuCode, price, stock, mainImage, attributes, status |

---

## 3. 文件迁移路径

### 3.1 迁移映射表

| 当前路径 | 目标路径 | 迁移类型 | 说明 |
|---|---|---|---|
| `controller/ProductController.java` | `product/interfaces/ProductController.java` | 改造 | 改为依赖 ProductApplicationService |
| `controller/ProductSkuController.java` | `product/interfaces/ProductSkuController.java` | 改造 | 改为依赖 SkuApplicationService |
| `controller/AdminProductSkuController.java` | `product/interfaces/AdminProductSkuController.java` | 移动 | 笛卡尔积生成逻辑无 Service 依赖，仅移动 |
| `controller/ProductReviewController.java` | `product/interfaces/ProductReviewController.java` | 改造 | 改为依赖 ReviewApplicationService；SecurityUtils → CurrentUserContext |
| `service/ProductService.java` | `product/api/ProductApi.java` | 改造 | 接口转为 API，返回 Entity 的方法改为返回 Snapshot |
| `service/ProductSkuService.java` | `product/api/SkuApi.java` | 改造 | 接口转为 API，getByIdEnabled 返回 SkuSnapshot |
| `service/ProductReviewService.java` | `product/api/ReviewApi.java` | 改造 | 接口转为 API |
| `service/ProductAttributeService.java` | `product/api/AttributeApi.java` | 改造 | 接口转为 API |
| `service/InventoryService.java` | `product/api/InventoryApi.java` | 改造 | 库存能力归入 product 模块 |
| `service/impl/ProductServiceImpl.java` | `product/application/ProductApplicationService.java` | 改造 | 改为依赖外部 api，实现 ProductApi |
| `service/impl/ProductSkuServiceImpl.java` | `product/application/SkuApplicationService.java` | 改造 | 实现 SkuApi |
| `service/impl/ProductReviewServiceImpl.java` | `product/application/ReviewApplicationService.java` | 改造 | 购买校验改为调 order.api.OrderQueryApi.hasUserPurchased() |
| `service/impl/ProductAttributeServiceImpl.java` | `product/application/AttributeApplicationService.java` | 改造 | 实现 AttributeApi |
| `service/impl/InventoryServiceImpl.java` | `product/application/InventoryApplicationService.java` | 改造 | 实现 InventoryApi |
| `entity/Product.java` | `product/infrastructure/entity/Product.java` | 移动 | PO 归入 infrastructure |
| `entity/ProductSku.java` | `product/infrastructure/entity/ProductSku.java` | 移动 | 同上 |
| `entity/ProductReview.java` | `product/infrastructure/entity/ProductReview.java` | 移动 | 同上 |
| `entity/ProductAttribute.java` | `product/infrastructure/entity/ProductAttribute.java` | 移动 | 同上 |
| `entity/ProductAttributeValue.java` | `product/infrastructure/entity/ProductAttributeValue.java` | 移动 | 同上 |
| `entity/enums/ProductStatus.java` | `product/domain/ProductStatus.java` | 移动 | 枚举提升到 domain |
| `entity/enums/AttributeType.java` | `product/domain/AttributeType.java` | 移动 | 枚举提升到 domain |
| `mapper/ProductMapper.java` | `product/infrastructure/mapper/ProductMapper.java` | 移动 | Mapper 归入 infrastructure |
| `mapper/ProductSkuMapper.java` | `product/infrastructure/mapper/ProductSkuMapper.java` | 移动 | 同上 |
| `mapper/ProductReviewMapper.java` | `product/infrastructure/mapper/ProductReviewMapper.java` | 移动 | 同上 |
| `mapper/ProductAttributeMapper.java` | `product/infrastructure/mapper/ProductAttributeMapper.java` | 移动 | 同上 |
| `mapper/ProductAttributeValueMapper.java` | `product/infrastructure/mapper/ProductAttributeValueMapper.java` | 移动 | 同上 |
| `dto/ProductCreateRequest.java` | `product/api/command/CreateProductCommand.java` | 改造 | Request → Command |
| `dto/ProductUpdateRequest.java` | `product/api/command/UpdateProductCommand.java` | 改造 | Request → Command |
| `dto/ProductQueryRequest.java` | `product/api/query/ProductListQuery.java` | 改造 | Request → Query |
| `dto/ProductSkuDTO.java` | `product/api/dto/SkuSnapshot.java`（或保留为内部 DTO） | 改造 | DTO 归 api |
| `dto/ProductAttributeDTO.java` | `product/api/dto/AttributeDTO.java` | 改造 | DTO 归 api |
| `vo/ProductVO.java` | `product/interfaces/vo/ProductVO.java` | 移动 | 前端 VO 归 interfaces |
| `vo/ProductSkuVO.java` | `product/interfaces/vo/ProductSkuVO.java` | 移动 | 同上 |
| `vo/ProductReviewVO.java` | `product/interfaces/vo/ProductReviewVO.java` | 移动 | 同上 |
| `vo/ProductAttributeVO.java` | `product/interfaces/vo/ProductAttributeVO.java` | 移动 | 同上 |

### 3.2 新增文件清单

| 新增文件 | 包路径 | 说明 |
|---|---|---|
| `ProductApi.java` | `product.api` | 商品业务能力 API 接口 |
| `SkuApi.java` | `product.api` | SKU 能力 API 接口 |
| `ReviewApi.java` | `product.api` | 评论能力 API 接口 |
| `AttributeApi.java` | `product.api` | 属性能力 API 接口 |
| `InventoryApi.java` | `product.api` | 库存能力 API 接口 |
| `ProductSnapshot.java` | `product.api.dto` | 商品快照（替代 Product Entity 跨模块传递） |
| `SkuSnapshot.java` | `product.api.dto` | SKU 快照（替代 ProductSku Entity） |
| `ReviewDTO.java` | `product.api.dto` | 评论 DTO |
| `AttributeDTO.java` | `product.api.dto` | 属性 DTO |
| `ProductSummaryDTO.java` | `product.api.dto` | 商品摘要（列表/统计用） |
| `CreateProductCommand.java` | `product.api.command` | 新增商品入参 |
| `UpdateProductCommand.java` | `product.api.command` | 编辑商品入参 |
| `SaveSkusCommand.java` | `product.api.command` | 保存 SKU 入参 |
| `SaveAttributesCommand.java` | `product.api.command` | 保存属性入参 |
| `CreateReviewCommand.java` | `product.api.command` | 发表评论入参 |
| `ReplyReviewCommand.java` | `product.api.command` | 回复评论入参 |
| `UpdateReviewStatusCommand.java` | `product.api.command` | 更新评论状态入参 |
| `DeductStockCommand.java` | `product.api.command` | 扣减库存入参 |
| `RestoreStockCommand.java` | `product.api.command` | 回补库存入参 |
| `UpdateCartCountCommand.java` | `product.api.command` | 更新加购计数入参 |
| `UpdateFavoriteCountCommand.java` | `product.api.command` | 更新收藏计数入参 |
| `ProductListQuery.java` | `product.api.query` | 商品列表查询入参 |
| `ReviewListQuery.java` | `product.api.query` | 评论列表查询入参 |
| `ProductDetailResult.java` | `product.api.result` | 商品详情出参 |
| `StockCheckResult.java` | `product.api.result` | 库存校验出参 |
| `ProductApplicationService.java` | `product.application` | 实现 ProductApi |
| `SkuApplicationService.java` | `product.application` | 实现 SkuApi |
| `ReviewApplicationService.java` | `product.application` | 实现 ReviewApi |
| `AttributeApplicationService.java` | `product.application` | 实现 AttributeApi |
| `InventoryApplicationService.java` | `product.application` | 实现 InventoryApi |

**新增文件：30 个**

### 3.3 改造文件清单

| 改造文件 | 改造内容 |
|---|---|
| `ProductController` | 注入改为 `ProductApplicationService` |
| `ProductSkuController` | 注入改为 `SkuApplicationService` |
| `ProductReviewController` | 注入改为 `ReviewApplicationService`；`SecurityUtils` → `CurrentUserContext` |
| `ProductServiceImpl` → `ProductApplicationService` | 字段注入改为外部模块 api；Entity 引用改为 DTO/Snapshot |
| `ProductSkuServiceImpl` → `SkuApplicationService` | 同上 |
| `ProductReviewServiceImpl` → `ReviewApplicationService` | 购买校验改为调 `order.api.OrderQueryApi.hasUserPurchased()`；`ProductMapper`/`ProductSkuMapper` 改为内部 Mapper |
| `ProductAttributeServiceImpl` → `AttributeApplicationService` | Mapper 引用改为内部 Mapper |
| `InventoryServiceImpl` → `InventoryApplicationService` | Mapper 引用改为内部 Mapper |
| `ProductService` 接口 | 转为 `ProductApi`，`getProductById()` 返回 `ProductSnapshot` 而非 `Product`；`getProductsByIds()` 返回 `List<ProductSnapshot>` |
| `ProductSkuService` 接口 | 转为 `SkuApi`，`getByIdEnabled()` 返回 `SkuSnapshot` 而非 `ProductSku` |
| 外部调用方 `OrderServiceImpl`（order） | `ProductService` → `product.api.ProductApi`；`ProductSkuService` → `product.api.SkuApi`；`Product`/`ProductSku` entity → Snapshot |
| 外部调用方 `OrderQueryServiceImpl`（order） | `ProductService` → `product.api.ProductApi`；`Product` entity → `ProductSnapshot` |
| 外部调用方 `OrderLifecycleServiceImpl`（order） | `ProductSkuService` → `product.api.SkuApi` |
| 外部调用方 `CartServiceImpl`（cart） | `ProductService`/`ProductSkuService` → `product.api.ProductApi`/`SkuApi`；Entity → Snapshot |
| 外部调用方 `CouponServiceImpl`（coupon） | `ProductService` → `product.api.ProductApi`；`Product` entity → `ProductSnapshot` |
| 外部调用方 `UserFavoriteServiceImpl`（favorite） | `ProductService` → `product.api.ProductApi`；`Product` entity → `ProductSnapshot` |
| 外部调用方 `SeckillOrderServiceImpl` / `SeckillGoodsServiceImpl` / `SeckillActivityServiceImpl`（seckill） | `ProductService` → `product.api.ProductApi`；`Product` entity → `ProductSnapshot` |
| 外部调用方 `SeckillOrderConsumer`（seckill mq） | `ProductMapper` → `product.api.ProductApi`（消除直接 Mapper 依赖）；`Product` entity → `ProductSnapshot` |
| 外部调用方 `StatsServiceImpl`（stats） | `ProductService` → `product.api.ProductApi` |
| 外部调用方 `CategoryServiceImpl`（category） | `ProductMapper` → `product.api.ProductApi`（消除直接 Mapper 依赖）；`Product` entity → `ProductSnapshot` |
| 外部调用方 `ProductSearchTool`（ai） | `ProductService` → `product.api.ProductApi` |
| 外部调用方 `AdminReviewController`（admin review） | `ProductReviewService` → `product.api.ReviewApi` |

**改造文件：20+ 个（含外部调用方）**

---

## 4. 外部模块依赖

### 4.1 product 依赖的外部模块 API

| 外部模块 | 当前依赖方式 | 目标依赖方式 | 需对方提供的 API |
|---|---|---|---|
| **order** | `ProductReviewServiceImpl` 查询 t_normal_order_item 做购买校验 | `order.api.OrderQueryApi.hasUserPurchased()` | `hasUserPurchased(UserId, ProductId, SkuId)` → boolean |
| **identity** | `SecurityUtils`（ProductReviewController） | `shared.kernel.CurrentUserContext` | `getCurrentUser()` → `CurrentUser` |
| **category** | （商品创建/更新时关联分类） | `category.api.CategoryApi`（可选） | `getCategoryById()` → `CategoryDTO` |
| **shared.kernel** | `MessageBusPort`（如有 MQ） | `shared.kernel.MessageBusPort` | `publish()` |

### 4.2 依赖 product 的外部模块

| 外部模块 | 当前调用 | 目标调用 | 需 product 提供的 API |
|---|---|---|---|
| **order**（OrderServiceImpl, OrderQueryServiceImpl, OrderLifecycleServiceImpl） | `ProductService` + `ProductSkuService` + `Product`/`ProductSku` entity | `product.api.ProductApi` + `product.api.SkuApi` | `getProductById()` → `ProductSnapshot`；`getSkuById()` → `SkuSnapshot`；`existsById()` |
| **cart**（CartServiceImpl） | `ProductService` + `ProductSkuService` + `Product`/`ProductSku` entity | `product.api.ProductApi` + `product.api.SkuApi` | 同上 + `updateCartCount()` |
| **coupon**（CouponServiceImpl） | `ProductService` + `Product` entity | `product.api.ProductApi` | `getProductById()` → `ProductSnapshot` |
| **favorite**（UserFavoriteServiceImpl） | `ProductService` + `Product` entity | `product.api.ProductApi` | `getProductById()` → `ProductSnapshot`；`updateFavoriteCount()` |
| **seckill**（SeckillOrderServiceImpl, SeckillGoodsServiceImpl, SeckillActivityServiceImpl, SeckillOrderConsumer） | `ProductService` + `Product` entity + `ProductMapper` | `product.api.ProductApi` | `getProductById()` → `ProductSnapshot`；`countAll()`；消除直接 Mapper 依赖 |
| **stats**（StatsServiceImpl） | `ProductService` | `product.api.ProductApi` | `countAll()`；`listProducts()` |
| **ai**（ProductSearchTool） | `ProductService` | `product.api.ProductApi` | `listProducts()`；`getProductDetail()` |
| **category**（CategoryServiceImpl） | `ProductMapper` + `Product` entity | `product.api.ProductApi` | 消除直接 Mapper 依赖，改为调 api |
| **review/admin**（AdminReviewController） | `ProductReviewService` | `product.api.ReviewApi` | `listAll()`；`reply()`；`updateStatus()` |
| **order**（购买校验反向依赖） | — | product 调 `order.api.OrderQueryApi.hasUserPurchased()` | product → order（见 4.1） |

**依赖 product 的外部模块：9 个（order, cart, coupon, favorite, seckill, stats, ai, category, review/admin）**

### 4.3 同步需定义的 API 契约（最小契约集）

product 迁移时，需同步定义以下外部模块的最小 API 契约（product 迁移的前置条件）：

| 外部模块 | 需定义的 API 方法 | 返回类型 | 紧迫性 |
|---|---|---|---|
| `order.api.OrderQueryApi` | `hasUserPurchased(UserId, ProductId, SkuId)` | `boolean` | P0（评论发表购买校验必需） |
| `shared.kernel.CurrentUserContext` | `getCurrentUser()` | `CurrentUser` | P0（替代 SecurityUtils） |

> 说明：product 对外依赖较少（主要是 order 的购买校验），相比 order 迁移前置条件更轻量。order 模块已完成迁移，`OrderQueryApi.hasUserPurchased()` 应已就绪。

---

## 5. 迁移步骤

> 遵循 Strangler Pattern（绞杀者模式）：逐步迁移，每步可独立编译/测试/回滚，旧 Service 在外部调用方全部切换前保留作为适配层。
> 阶段编号沿用 Order 模块迁移经验（P.0 → P.6），便于团队对齐。

### Phase P.0：创建 product 模块包结构

**操作**：创建以下空包（含 `package-info.java`）：
```
com.seckill.mall.product
com.seckill.mall.product.api
com.seckill.mall.product.api.dto
com.seckill.mall.product.api.command
com.seckill.mall.product.api.query
com.seckill.mall.product.api.result
com.seckill.mall.product.application
com.seckill.mall.product.domain
com.seckill.mall.product.infrastructure
com.seckill.mall.product.infrastructure.mapper
com.seckill.mall.product.infrastructure.entity
com.seckill.mall.product.interfaces
com.seckill.mall.product.interfaces.vo
com.seckill.mall.product.mq
```

**影响文件**：新增约 14 个 `package-info.java`
**验证方式**：`mvn compile` 通过（空包不影响编译）
**可回滚性**：删除新增包即可
**提交粒度**：1 次提交

### Phase P.2：定义 product.api（接口 + DTO + Command + Query + Result）

**操作**：创建 API 接口和数据契约：
- `ProductApi`、`SkuApi`、`ReviewApi`、`AttributeApi`、`InventoryApi` 接口
- 11 个 Command、2 个 Query、2 个 Result、5 个 DTO/Snapshot
- 暂无实现，仅定义契约
- `ProductApi.getProductById()` 返回 `ProductSnapshot`（非 Entity）
- `SkuApi.getByIdEnabled()` 返回 `SkuSnapshot`（非 Entity）

**影响文件**：新增 30 个文件（见 3.2 新增文件清单）
**验证方式**：`mvn compile` 通过（接口和 DTO 无依赖循环）
**可回滚性**：删除新增文件即可
**提交粒度**：1 次提交

### Phase P.3：迁移 infrastructure（Mapper + Entity + 枚举）

**操作**：
1. 移动 5 个 Entity → `product.infrastructure.entity`
2. 移动 5 个 Mapper → `product.infrastructure.mapper`
3. 移动 `ProductStatus`、`AttributeType` → `product.domain`
4. 更新所有引用这些类的文件的 import（MyBatis XML namespace 需同步更新）
5. 检查 `resources/mapper/*.xml` 中 Product 相关 Mapper 的 namespace

**影响文件**：12 个移动 + 约 20 个 import 更新（含外部模块）
**验证方式**：`mvn compile` 通过 + `mvn test` 通过（Mapper 测试）
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交
**风险**：⚠️ 高 — 外部模块（order/cart/coupon/favorite/seckill/category）import Product Entity，需同步更新

### Phase P.4-A：创建 Application 层门面（保留原 Service）

**操作**：
1. 创建 `ProductApplicationService`（复制 `ProductServiceImpl` 逻辑），实现 `ProductApi`
2. 创建 `SkuApplicationService`（复制 `ProductSkuServiceImpl` 逻辑），实现 `SkuApi`
3. 创建 `ReviewApplicationService`（复制 `ProductReviewServiceImpl` 逻辑），实现 `ReviewApi`；购买校验改为调 `order.api.OrderQueryApi.hasUserPurchased()`
4. 创建 `AttributeApplicationService`（复制 `ProductAttributeServiceImpl` 逻辑），实现 `AttributeApi`
5. 创建 `InventoryApplicationService`（复制 `InventoryServiceImpl` 逻辑），实现 `InventoryApi`
6. 字段注入改为内部 Mapper（已迁移到 infrastructure）+ 外部模块 api
7. **保留原 Service 接口和 impl**（作为适配层，暂不删除）

**影响文件**：5 个新增 ApplicationService
**验证方式**：`mvn compile` 通过 + `mvn test` 通过
**可回滚性**：删除新增 ApplicationService 即可（原 Service 仍在）
**提交粒度**：1 次提交
**说明**：此步为"双轨期"开始，新旧并存，外部调用方仍用旧 Service

### Phase P.4-B：移动 Controller 到 interfaces

**操作**：
1. 移动 4 个 Controller → `product.interfaces`
2. `ProductController` 注入改为 `ProductApplicationService`
3. `ProductSkuController` 注入改为 `SkuApplicationService`
4. `ProductReviewController` 注入改为 `ReviewApplicationService`；`SecurityUtils` → `shared.kernel.CurrentUserContext`
5. `AdminProductSkuController` 仅移动（无 Service 依赖）
6. 移动前端 VO → `product.interfaces.vo`

**影响文件**：4 个 Controller 改造 + 4 个 VO 移动
**验证方式**：`mvn compile` 通过 + Controller API 接口测试
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交
**说明**：Controller 切换到 ApplicationService 后，原 Service 仅被外部模块引用

### Phase P.4-C：切换外部调用方到 product.api

**操作**：
1. `OrderServiceImpl` / `OrderQueryServiceImpl` / `OrderLifecycleServiceImpl`（order）：`ProductService`/`ProductSkuService` → `product.api.ProductApi`/`SkuApi`；Entity → Snapshot
2. `CartServiceImpl`（cart）：同上
3. `CouponServiceImpl`（coupon）：`ProductService` → `product.api.ProductApi`
4. `UserFavoriteServiceImpl`（favorite）：`ProductService` → `product.api.ProductApi`
5. `SeckillOrderServiceImpl` / `SeckillGoodsServiceImpl` / `SeckillActivityServiceImpl`（seckill）：`ProductService` → `product.api.ProductApi`
6. `SeckillOrderConsumer`（seckill mq）：`ProductMapper` → `product.api.ProductApi`（消除直接 Mapper 依赖）
7. `StatsServiceImpl`（stats）：`ProductService` → `product.api.ProductApi`
8. `CategoryServiceImpl`（category）：`ProductMapper` → `product.api.ProductApi`（消除直接 Mapper 依赖）
9. `ProductSearchTool`（ai）：`ProductService` → `product.api.ProductApi`
10. `AdminReviewController`（admin review）：`ProductReviewService` → `product.api.ReviewApi`

**影响文件**：10+ 个外部文件改造
**验证方式**：`mvn compile` 通过 + 全量测试
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交（或按模块拆为多个子提交）
**说明**：此步完成后，原 Product Service 接口和 impl 无任何引用

### Phase P.5：删除旧 Service 接口和 impl（清理绞杀残留）

**操作**：
1. 删除 `ProductService` / `ProductServiceImpl`
2. 删除 `ProductSkuService` / `ProductSkuServiceImpl`
3. 删除 `ProductReviewService` / `ProductReviewServiceImpl`
4. 删除 `ProductAttributeService` / `ProductAttributeServiceImpl`
5. 删除 `InventoryService` / `InventoryServiceImpl`（已由 `InventoryApplicationService` 替代）
6. 删除旧 `ProductCreateRequest` / `ProductUpdateRequest` / `ProductQueryRequest`（已由 Command/Query 替代）

**影响文件**：11 个删除
**验证方式**：`mvn compile` 通过 + 全量测试全绿
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交
**说明**：此步为"双轨期"结束，绞杀完成

### Phase P.6：ArchUnit 边界规则验证

**操作**：
1. 更新 `ArchitectureRulesTest`，新增 product 模块的包边界规则：
   - `product.api` 只能依赖 shared.kernel + JDK
   - `product.application` 只能依赖 product.api + product.domain + product.infrastructure + 其他模块 api + shared.kernel
   - `product.infrastructure` 只能依赖 product.domain + MyBatis-Plus + shared.kernel
   - `product.interfaces` 只能依赖 product.application + product.api + product.interfaces.vo + shared.kernel
   - 禁止任何模块直接依赖 product.infrastructure.mapper / product.infrastructure.entity
2. 运行 `mvn test -Dtest=ArchitectureRulesTest`
3. 确认无违规依赖

**影响文件**：1 个测试文件更新
**验证方式**：ArchUnit 测试全绿
**可回滚性**：`git revert` 此步提交
**提交粒度**：1 次提交

---

## 6. 风险点与回滚策略

### 6.1 风险点

| 风险 | 等级 | 影响 | 缓解措施 |
|---|---|---|---|
| **import 大量修改导致编译错误** | 高 | 12 个文件移动 + 20+ 个改造，import 路径全变 | 分步迁移（P.3 → P.4），每步编译验证；IDE 批量更新 import |
| **跨模块 Entity 引用需同步改造** | 高 | 6 个外部模块引用 Product/ProductSku Entity，需改为 Snapshot | P.4-C 中同步将外部模块的 Entity 引用改为 Snapshot；先确认 ProductSnapshot 字段覆盖所有外部使用场景 |
| **跨模块 Mapper 直接引用** | 高 | category 和 seckill 直接引用 ProductMapper，违反模块边界 | P.4-C 中改为调 product.api，消除直接 Mapper 依赖；需确认 ProductApi 提供等价查询方法 |
| **MyBatis XML namespace 需同步更新** | 中 | 5 个 Mapper 移动后，XML 的 namespace 需更新 | P.3 中同步更新 XML；检查 `resources/mapper/Product*.xml` |
| **ProductReviewService 购买校验跨模块依赖 order** | 中 | 评论发表时查询 t_normal_order_item，迁移后改为调 order.api | P.4-A 中改为调 `order.api.OrderQueryApi.hasUserPurchased()`；确认 order 模块已提供该 API |
| **InventoryService 归属变更** | 中 | InventoryService 从顶层 service 移入 product.application，外部调用方（order/seckill）需更新 import | P.4-C 中同步更新 order/seckill 对 InventoryService 的引用为 product.api.InventoryApi |
| **冗余计数维护方法（updateCartCount/updateFavoriteCount）被外部调用** | 低 | cart/favorite 调用 ProductService 维护冗余计数，迁移后改为 ProductApi | P.4-C 中同步更新 cart/favorite 调用 |
| **测试可能失败** | 中 | 现有测试可能直接依赖 ProductService 接口或 Product entity | P.4 后运行全量测试，修复测试 import |
| **ArchUnit 规则可能报新违规** | 低 | 新包结构可能触发已有 ArchUnit 规则 | P.6 更新规则，逐步修复 |
| **Spring Bean 注入冲突** | 低 | P.4-A 双轨期新旧 Service 并存，bean 名称可能冲突 | 使用 `@Service("productApplicationService")` 显式命名；或用 `@Primary` 标注新 ApplicationService |

### 6.2 回滚策略

#### 每 Phase 的回滚方式

| Phase | 回滚方式 | 说明 |
|---|---|---|
| P.0 | 删除新增空包 | 无代码影响 |
| P.2 | 删除新增 API/DTO 文件 | 无代码影响（未引用） |
| P.3 | `git revert` | Mapper/Entity 移动涉及 import，需整体回滚 |
| P.4-A | 删除新增 ApplicationService | 原 Service 仍在，无影响（双轨期安全） |
| P.4-B | `git revert` | Controller 改造，回滚后 Controller 仍用旧 Service |
| P.4-C | `git revert` | 外部调用方改造，回滚后外部仍用旧 Service |
| P.5 | `git revert` | 删除旧 Service，回滚后恢复旧 Service |
| P.6 | `git revert` | ArchUnit 规则更新 |

#### 紧急回滚

如果迁移导致严重问题（编译失败/测试全红/运行时错误）：

```bash
# 回滚到迁移前最后一个绿色提交
git log --oneline -20          # 找到迁移前的 commit
git revert <commit>..HEAD      # 回滚所有迁移提交
# 或
git reset --hard <迁移前commit>  # 硬回滚（丢弃所有迁移变更）
```

**前提**：每个 Phase 单独提交，提交信息标注 `Phase 3 Product P.X`。

#### 部分回滚（双轨期保障）

| 失败的 Phase | 回滚范围 | 保留的成果 |
|---|---|---|
| P.4-A 失败 | 仅删除新增 ApplicationService | P.0-P.3（包结构 + API + infrastructure）保留 |
| P.4-B 失败 | 仅 revert P.4-B | P.0-P.4-A 保留（ApplicationService 已创建，Controller 暂未切换，原 Service 仍在） |
| P.4-C 失败 | 仅 revert P.4-C | P.0-P.4-B 保留（Controller 已切换，外部调用方暂未更新，原 Service 作为适配层仍在） |
| P.5 失败 | 仅 revert P.5 | P.0-P.4-C 保留（旧 Service 恢复，与新 ApplicationService 并存，无引用冲突） |

**关键**：P.4-A 创建新 ApplicationService 时**保留原 Service**，P.4-B/P.4-C 逐步切换调用方，P.5 确认无误后再删除原 Service。这样任一阶段失败均可安全回滚到上一阶段（原 Service 仍在）。

---

## 7. 预计提交粒度

| Phase | 提交数 | 提交信息模板 | 说明 |
|---|---|---|---|
| P.0 | 1 | `refactor(product): P.0 create product module package structure` | 空包 |
| P.2 | 1 | `refactor(product): P.2 define product api contracts (5 Api + 30 dto/command/query/result)` | API 契约 |
| P.3 | 1 | `refactor(product): P.3 move infrastructure (5 entity + 5 mapper + 2 enum)` | 持久化层迁移 |
| P.4-A | 1 | `refactor(product): P.4-A create application services (5 ApplicationService, keep legacy Service)` | Application 门面（双轨期开始） |
| P.4-B | 1 | `refactor(product): P.4-B move controllers to interfaces, switch to ApplicationService` | Controller 迁移 |
| P.4-C | 1（或按模块拆 3-5 个） | `refactor(product): P.4-C switch external callers to product.api` | 外部调用方切换 |
| P.5 | 1 | `refactor(product): P.5 remove legacy Service interfaces and impls` | 清理绞杀残留 |
| P.6 | 1 | `test(product): P.6 add ArchUnit boundary rules for product module` | 边界规则 |

**总计：8 次提交（最小粒度），最多 10-12 次（按模块拆分 P.4-C）**

---

## 8. 与 Order 模块迁移的差异对比

| 维度 | Order 模块 | Product 模块 | 说明 |
|---|---|---|---|
| 核心文件数 | 28 | 18（+Inventory 2） | Product 更精简 |
| Service 数 | 4（含 Lifecycle） | 4（+Inventory 1） | Product 多一个库存能力 |
| Controller 数 | 2 | 4 | Product Controller 更多（SKU/Review/AdminSku） |
| Entity 数 | 2 | 5 | Product Entity 更多（含 Attribute/AttributeValue） |
| 对外依赖的模块数 | 7（product/identity/cart/coupon/payment/seckill/upload） | 2（order/identity） | Product 对外依赖更少，迁移前置条件更轻 |
| 被依赖的外部模块数 | 5（payment/review/ai/stats/system） | 9（order/cart/coupon/favorite/seckill/stats/ai/category/review-admin） | Product 被依赖更多，P.4-C 改造范围更大 |
| 跨模块 Entity 引用 | 5 类 Entity / 4 模块 | 2 类 Entity / 6 模块 | Product 的 Product/ProductSku 被广泛引用 |
| 跨模块 Mapper 引用 | 0 | 2（category/seckill） | Product 存在违规 Mapper 引用，需重点消除 |
| MQ 组件 | 2（Consumer + Message） | 0 | Product 当前无 MQ |
| 迁移步骤数 | 8 | 8（P.0-P.6，P.4 拆 A/B/C） | 步骤对齐 |

**结论**：Product 模块对外依赖更少（迁移前置条件更轻），但被依赖更多（P.4-C 外部调用方改造范围更大）。最大的风险点是 6 个外部模块引用 Product/ProductSku Entity，以及 2 个外部模块直接引用 ProductMapper，需在 P.4-C 中重点消除。

---

> **本文件为 Phase 3 Product 模块迁移前置设计文档，未创建任何代码/接口/包，未修改任何现有代码。**
> **下一步**：基于本迁移计划，生成 `PRODUCT-API-CONTRACT.md`（API 契约详细定义），然后按 Phase P.0 → P.6 逐步执行迁移。