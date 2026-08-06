# SKU 多规格商品系统技术设计方案（TDD）

> 文档版本：v1.0
> 编写日期：2026-08-06
> 编写人：WNJ（nj651217@163.com）
> 适用项目：seckill-mall（Spring Boot + MyBatis-Plus + MySQL 8.0 / Vue 3 + TypeScript + Element Plus + Pinia）
> 设计稿基准：`docs/product-detail-taobao-design.html`

---

## 1. 概述

### 1.1 项目背景

当前 `seckill-mall` 商品系统采用「单一价格 + 单一库存」的扁平模型：

- 数据库 `t_product` 表只保存一个 `original_price` 与一个 `stock`，无任何规格（SKU）相关字段。
- 购物车 `t_cart` 仅以 `product_id` 关联商品，无法区分用户选中的具体规格。
- 普通订单明细 `t_normal_order_item` 虽然冗余了商品名称/主图/单价快照，但同样没有规格信息。
- 商品评论 `t_product_review` 只关联 `product_id`，无法体现「用户对哪个规格的评论」。
- 前端 `frontend/src/views/admin/ProductEdit.vue` 后台商品编辑页只有名称、分类、原价、库存、主图、多图、简介、富文本详情，没有规格管理功能。
- 前端 `frontend/src/views/front/ProductDetail.vue` 商品详情页虽然已经按天猫风格优化，但 SKU 选择器（颜色/版本/表带）只是静态 UI，未对接后端数据。

而设计稿 `docs/product-detail-taobao-design.html` 明确要求商品详情页支持多维度规格选择：

- **颜色**（图片类型 SKU）：曜石黑、银月白、赤霞红、冰川蓝、森野绿、深空灰 —— 每个颜色用色块圆点展示（`.sku-color`）。
- **版本**（文字类型 SKU）：标准版、旗舰版、尊享版、运动版（禁用） —— 文字按钮，有 `active` / `disabled` 状态（`.sku-text`）。
- **表带**（文字类型 SKU）：氟橡胶表带、米兰尼斯表带、真皮表带 —— 文字按钮。

用户选择不同 SKU 组合后，价格、库存、主图可能随之变化。当前系统无法满足该需求。

### 1.2 设计目标

为 `seckill-mall` 引入完整的 SKU 多规格商品系统，实现：

1. **多维度规格定义**：支持颜色（图片型）、版本（文字型）、表带（文字型）等多种属性，且属性类型可扩展（IMAGE / TEXT）。
2. **SKU 组合独立计价**：每个属性值组合（即一个 SKU）拥有独立的价格、库存、主图、编码与上下架状态。
3. **全链路 SKU 信息传递**：商品详情 → 加购 / 立即购买 → 购物车 → 订单 → 评论，整条链路都能携带并展示 SKU 信息。
4. **后台可视化管理**：在 `ProductEdit.vue` 中提供「商品规格」配置区与「SKU 组合」表格，自动笛卡尔积生成所有组合，逐行设置价格 / 库存 / 图片。
5. **前台动态展示**：`ProductDetail.vue` SKU 选择器对接后端数据，价格 / 库存 / 主图随选择实时变化，禁用无库存组合。
6. **向后兼容**：对没有 SKU 的旧商品（无属性、无 SKU 记录）继续以 `t_product.original_price` / `t_product.stock` 兜底，保证平滑过渡。

### 1.3 改造范围

本次改造涉及数据库、后端、前端后台、前端前台四个层面的全链路改造：

| 层面 | 主要工作 |
| --- | --- |
| 数据库 | 新增 3 张 SKU 相关表，修改 3 张现有表，编写迁移 SQL |
| 后端 | 新增 3 个实体 / 3 个 Mapper / 2 个 Service / 1 个 Controller，修改 3 个实体 / 多个 DTO/VO / 现有 Service 与 Controller |
| 前端后台 | 改造 `ProductEdit.vue` 增加规格与 SKU 管理，新增 `api/sku.ts`，扩展 `types/index.ts` |
| 前端前台 | 改造 `ProductDetail.vue` 对接 SKU 数据，改造购物车 / 订单 / 评论展示 SKU 信息 |

---

## 2. 需求分析

### 2.0 分类规格模板需求

#### 2.0.1 多品类规格维度差异

`seckill-mall` 是多品类商城，不同品类的商品规格维度差异显著，若每个商品都从零定义属性，将带来大量重复配置工作且易出错。典型品类的规格维度如下：

| 品类 | 规格维度 1 | 规格维度 2 | 规格维度 3 | 规格维度 4 |
| --- | --- | --- | --- | --- |
| 智能手表 | 颜色（IMAGE） | 版本（TEXT） | 表带（TEXT） | — |
| 手机 | 颜色（IMAGE） | 内存（TEXT） | 存储（TEXT） | — |
| 服装 | 颜色（IMAGE） | 尺码（TEXT） | — | — |
| 电脑 | 颜色（IMAGE） | CPU（TEXT） | 内存（TEXT） | 硬盘（TEXT） |

#### 2.0.2 模板驱动配置

- **分类级模板定义**：在商品分类（`t_category`）级别定义规格维度模板，每个分类可配置若干规格属性（如「颜色」「版本」「表带」），每个属性包含属性名、属性类型（IMAGE/TEXT）、录入方式（SELECT/INPUT）、是否必选、排序权重，以及一组预设可选值（如「颜色」预设「曜石黑 / 银月白 / 赤霞红 / 冰川蓝」）。
- **添加商品自动带出**：商家在 `ProductEdit.vue` 选择商品分类后，前端自动调用分类规格模板接口拉取该分类的规格属性模板，自动填充到「商品规格」区域，避免重复录入。
- **模板覆盖与扩展**：商家可在分类模板基础上对具体商品做覆盖或扩展：
  - **删除**：移除该商品不需要的模板属性（如某款手表不带「表带」选项）。
  - **新增**：添加分类模板未覆盖的自定义属性（`category_attribute_id = NULL`，如某款手表特有「限量编号」属性）。
  - **修改属性值**：在模板预设值基础上增删属性值，或对 `input_type = INPUT` 的属性自由输入值。
- **向后兼容**：分类模板为「可选增强」，分类未配置模板时商家仍可像 v1.0 方案那样从零自定义属性，保证平滑过渡。

#### 2.0.3 模板字段约束

分类规格模板包含以下字段：

| 字段 | 说明 |
| --- | --- |
| `name` | 属性名（如「颜色」「版本」），最长 50 字符 |
| `type` | 属性类型：`IMAGE`（图片型，需色块 URL）/ `TEXT`（文字型） |
| `input_type` | 录入方式：`SELECT`（从预设值选）/ `INPUT`（自由输入） |
| `is_required` | 是否必选：0-否 / 1-是（用于商品编辑时校验该属性必须填写） |
| `sort_order` | 排序权重，控制添加商品时属性展示顺序 |
| 预设值列表 | 当 `input_type = SELECT` 时，提供一组预设可选值，每个值可附带 `image_url`（图片型）和 `sort_order` |

### 2.1 SKU 维度需求

- **属性（Attribute）**：一个商品可定义多个属性，每个属性有一个名称（如「颜色」「版本」「表带」）和一个类型（`IMAGE` 图片型 / `TEXT` 文字型）。
- **属性值（AttributeValue）**：每个属性下有若干属性值。图片型属性值可附带一个 `image_url`（色块 URL 或图片 URL），文字型属性值只有 `value` 文本。
- **自定义属性**：属性名与属性值均由后台管理员自由定义，不预设固定枚举，以支持不同品类商品（如服装的「尺码」、手机的「存储容量」）。
- **排序**：属性和属性值都支持 `sort_order` 排序权重，控制前台展示顺序。
- **设计稿样例**（智能手表 Pro X）：
  - 颜色（IMAGE）：深空灰、曜石黑、银月白、赤霞红、冰川蓝、森野绿
  - 版本（TEXT）：标准版、旗舰版、尊享版、运动版
  - 表带（TEXT）：氟橡胶表带、米兰尼斯表带、真皮表带

### 2.2 SKU 组合需求

- **笛卡尔积生成**：当管理员定义完所有属性及其值后，系统自动生成所有属性值的笛卡尔积，每个组合即一个 SKU。
- **独立字段**：每个 SKU 拥有独立的：
  - `sku_code`：SKU 编码（可自动生成或手填，用于对接 ERP / 仓储）
  - `price`：SKU 价格（覆盖 `t_product.original_price`）
  - `stock`：SKU 库存（覆盖 `t_product.stock`）
  - `main_image`：SKU 主图（覆盖 `t_product.main_image`，可空表示沿用商品主图）
  - `attributes`：JSON 字符串，记录该 SKU 的属性键值对，如 `{"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}`
  - `status`：1 启用 / 0 禁用（用于「运动版」这种暂时不售的规格）
- **库存聚合**：商品的 `t_product.stock` 在有 SKU 时等于所有启用 SKU 库存之和（由 Service 层维护，或查询时动态聚合）。
- **价格区间**：商品的展示价格在有 SKU 时为「最低价 - 最高价」区间，由前端根据 SKU 列表计算。

### 2.3 前台展示需求

- **SKU 选择器**：`ProductDetail.vue` 在商品主图下方渲染 SKU 选择器，每个属性一组，按 `sort_order` 排序。
  - 图片型属性：渲染色块圆点（`.sku-color`），选中态加边框 + 右下角圆点（参考设计稿 864-923 行）。
  - 文字型属性：渲染文字按钮（`.sku-text`），选中态高亮，禁用态置灰 + 删除线（参考设计稿 925-968 行）。
- **价格 / 库存联动**：用户每选择一个属性值，前端根据已选属性值组合查找匹配的 SKU：
  - 完整匹配到唯一 SKU：显示该 SKU 的价格、库存、主图；库存为 0 时按钮置灰提示「无货」。
  - 部分匹配（多个候选 SKU）：显示价格区间「¥最低 - ¥最高」与库存合计。
  - 无匹配（某组合不存在或被禁用）：将该属性值按钮置灰禁用。
- **加入购物车 / 立即购买**：必须选中所有属性后才能操作，提交时携带 `skuId`。
- **数量选择器**：上限为当前选中 SKU 的 `stock`。

### 2.4 后台管理需求

- **`ProductEdit.vue` 改造**：在「库存」字段下方新增「商品规格」区域：
  - 动态添加属性行：每行包含属性名输入框、类型下拉（IMAGE / TEXT）、属性值标签组（el-tag 可增删）、图片型属性值的色块上传。
  - 「生成 SKU 组合」按钮：点击后根据当前所有属性值笛卡尔积生成 SKU 表格。
  - SKU 表格：每行一个 SKU 组合，列包含「组合名称（只读，由属性值拼接）」「价格」「库存」「主图（单图上传）」「编码」「状态（启用/禁用）」。
  - 支持手动删除某行 SKU（即该组合不售）。
- **校验**：
  - 至少一个属性且每个属性至少一个属性值才能生成 SKU。
  - 每个 SKU 的价格必须 > 0。
  - 库存 ≥ 0。
  - SKU 编码唯一（同商品内）。
- **向后兼容**：若管理员不添加任何属性，则商品保持「单一价格 + 单一库存」模式，提交时不创建 SKU 记录。

### 2.5 订单 / 购物车需求

- **购物车**：`t_cart` 新增 `sku_id` 字段，加购时记录具体 SKU。同一商品不同 SKU 视为不同购物车项（修改唯一约束 `uk_user_product` 为 `uk_user_product_sku`）。
- **购物车展示**：`CartItemVO` 新增 `skuId` / `skuAttributes`（如「曜石黑 / 旗舰版 / 氟橡胶表带」），价格 / 主图 / 库存取自 SKU。
- **立即购买**：`BuyNowRequest` 新增 `skuId`，下单时校验 SKU 状态与库存，扣减 SKU 库存。
- **购物车结算**：从购物车项读取 `sku_id`，下单时为每个购物车项生成对应的订单明细并扣减对应 SKU 库存。
- **订单明细**：`t_normal_order_item` 新增 `sku_id` 与 `sku_attributes`（JSON 字符串快照），下单时写入，避免后续 SKU 修改影响历史订单展示。
- **库存回补**：取消订单 / 超时取消时，按 `sku_id` 回补对应 SKU 库存。

### 2.6 评论需求

- **评论关联 SKU**：`t_product_review` 新增 `sku_id` 与 `sku_attributes`（JSON 字符串快照），发表评论时携带用户购买的 SKU。
- **评论展示**：`ProductReviewVO` 新增 `skuAttributes` 字段，前台评论列表在评论内容上方展示「规格：曜石黑 / 旗舰版 / 氟橡胶表带」（参考设计稿 2652 行 `.review-item__sku`）。
- **评论入口**：用户在「我的订单」对某订单项评论时，自动带入该订单项的 `sku_id` 与 `sku_attributes`。

---

## 3. 当前系统分析

### 3.1 数据库现状

#### 3.1.1 分类规格模板现状

当前 `t_category` 表（`seckill-mall/src/main/resources/sql/schema.sql`）只保存分类的基础信息（分类名、父级、排序、状态等），**无任何规格模板字段**。这意味着：

- 不同品类的规格维度差异（手表：颜色/版本/表带；手机：颜色/内存/存储；服装：颜色/尺码；电脑：颜色/CPU/内存/硬盘）目前无法在分类级别固化。
- 商家添加商品时需对每个商品从零定义属性，重复配置工作量大且易错。
- 本次改造需新增 `t_category_attribute`（分类属性模板表）与 `t_category_attribute_value`（分类属性预设值表）两张表，并在 `t_product_attribute` 上新增 `category_attribute_id` 字段关联模板（允许 NULL 表示商品自定义属性）。

#### 3.1.2 现有表 SKU 缺口

| 表名 | 文件 | 现状 | SKU 缺口 |
| --- | --- | --- | --- |
| `t_product` | `seckill-mall/src/main/resources/sql/schema.sql` (52-71 行) | 单一 `original_price DECIMAL(10,2)` / 单一 `stock INT` | 无 SKU 字段，无法表达多规格 |
| `t_cart` | `seckill-mall/src/main/resources/sql/migration_v3.sql` (17-29 行) | 仅 `product_id` 关联，唯一约束 `uk_user_product(user_id, product_id)` | 无 `sku_id`，无法区分规格；唯一约束需扩展 |
| `t_normal_order_item` | `seckill-mall/src/main/resources/sql/normal_order_tables.sql` (56-71 行) | 冗余 `product_name` / `product_image` / `unit_price` 快照 | 无 SKU 快照字段 |
| `t_product_review` | `seckill-mall/src/main/resources/sql/migration_v2.sql` (33-51 行) | 仅 `product_id` 关联 | 无 SKU 关联 |
| `t_seckill_goods` / `t_seckill_order` | `schema.sql` (76-129 行) | 秒杀活动绑定 `product_id` | 秒杀场景暂不引入 SKU（保持简单），如需可后续扩展 |

### 3.2 后端现状

**实体层**（`seckill-mall/src/main/java/com/seckill/mall/entity/`）：

- `Product.java`：字段 `id, name, description, originalPrice(BigDecimal), stock(Integer), salesCount, cartCount, favoriteCount, categoryId, images(String, JSON), mainImage, detailHtml, status(ProductStatus), isDeleted, createTime, updateTime`。无任何 SKU 字段。
- `Cart.java`：字段 `id, userId, productId, quantity, selected, isDeleted, createTime, updateTime`。无 `skuId`。
- `NormalOrderItem.java`：字段 `id, orderId, productId, productName, productImage, unitPrice, quantity, subtotal, isDeleted, createTime, updateTime`。无 SKU 快照。
- `ProductReview.java`：字段 `id, productId, userId, orderId, content, rating, images, status, replyContent, replyTime, isDeleted, createTime, updateTime`。无 SKU 关联。

**DTO 层**（`seckill-mall/src/main/java/com/seckill/mall/dto/`）：

- `ProductCreateRequest.java`：`productName, categoryId, description, detailHtml, originalPrice, images(List<String>), stock, status`。无属性 / SKU 字段。
- `ProductUpdateRequest.java`：同上，无 SKU 字段。
- `BuyNowRequest.java`：`productId, quantity, addressId, remark`。无 `skuId`。
- `CartCheckoutRequest.java`：购物车结算请求，无 SKU 字段（结算时从购物车项读取）。

**VO 层**（`seckill-mall/src/main/java/com/seckill/mall/vo/`）：

- `ProductVO.java`：`id, productName, categoryId, categoryName, description, detailHtml, originalPrice, images(List<String>), stock, salesCount, status, createTime`。无属性 / SKU 列表字段。
- `CartItemVO.java`：`id, productId, quantity, selected, productName, mainImage, originalPrice, stock, productStatus, subtotal`。无 SKU 信息。
- `ProductReviewVO.java`：评论视图，无 `skuAttributes`。
- `NormalOrderDetailVO.java`：`order(NormalOrder) + items(List<NormalOrderItem>)`，依赖实体直接暴露。

**Service 层**：

- `ProductService.java`：`listProducts / getProductDetail / createProduct / updateProduct / deleteProduct`。无 SKU 加载 / 保存逻辑。
- `CartService.java`：`addToCart(userId, productId, quantity)` 签名无 `skuId`。
- `OrderService.java`：`createNormalOrder(userId, productId, quantity, addressId, remark)` 签名无 `skuId`。
- `ProductReviewService.java`：`create` 签名无 `skuId`。

**Controller 层**：

- `ProductController.java`（`/api/v1/products`）：`list / detail / create / update / delete`，详情接口返回 `ProductVO` 无 SKU。
- `CartController.java`（`/api/v1/cart`）：`AddToCartRequest` 内嵌类只有 `productId / quantity`，无 `skuId`。
- `ProductReviewController.java`（`/api/v1/reviews`）：`ReviewCreateRequest` 内嵌类只有 `productId / content / rating / images`，无 `skuId`。
- 无独立的 `ProductSkuController`。

**Mapper 层**：无 `ProductAttributeMapper` / `ProductAttributeValueMapper` / `ProductSkuMapper`。

### 3.3 前端现状

**前端后台**：

- `frontend/src/views/admin/ProductEdit.vue`（458 行）：表单只有「商品名称 / 分类 / 原价 / 库存 / 状态 / 主图 / 图片 / 商品简介 / 商品详情(富文本)」。无规格管理区域。提交 `payload` 仅含 `productName, categoryId, originalPrice, stock, description, detailHtml, images, status`。
- `frontend/src/views/admin/ProductManage.vue`：商品列表，无 SKU 列。

**前端前台**：

- `frontend/src/views/front/ProductDetail.vue`（2229 行）：已有「规格参数」展示区（262 / 283 行），但只是商品参数表格，不是 SKU 选择器。设计稿要求的 `.sku` / `.sku-color` / `.sku-text` 选择器未对接后端。
- `frontend/src/api/cart.ts`：`addCart(data: CartAddRequest)`，`CartAddRequest` 只有 `productId / quantity`。
- `frontend/src/api/product.ts`：`getProductDetail` 返回 `ProductVO`，无 SKU 数据。
- `frontend/src/api/review.ts`：`createReview` 不携带 `skuId`。
- `frontend/src/types/index.ts`（712 行）：`ProductVO` / `CartItemVO` / `CartAddRequest` / `ProductCreateRequest` / `ProductReviewVO` / `ReviewCreateRequest` 均无 SKU 字段。

### 3.4 影响范围分析

#### 3.4.1 后端受影响文件清单

| 类型 | 文件路径 | 操作 |
| --- | --- | --- |
| 新增实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/CategoryAttribute.java` | 新建（分类规格模板） |
| 新增实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/CategoryAttributeValue.java` | 新建（分类属性预设值） |
| 新增实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/ProductAttribute.java` | 新建 |
| 新增实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/ProductAttributeValue.java` | 新建 |
| 新增实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/ProductSku.java` | 新建 |
| 新增枚举 | `seckill-mall/src/main/java/com/seckill/mall/entity/enums/AttributeType.java` | 新建 |
| 新增枚举 | `seckill-mall/src/main/java/com/seckill/mall/entity/enums/AttributeInputType.java` | 新建（SELECT/INPUT） |
| 修改实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/Cart.java` | 加 `skuId` |
| 修改实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/NormalOrderItem.java` | 加 `skuId` / `skuAttributes` |
| 修改实体 | `seckill-mall/src/main/java/com/seckill/mall/entity/ProductReview.java` | 加 `skuId` / `skuAttributes` |
| 新增 Mapper | `seckill-mall/src/main/java/com/seckill/mall/mapper/CategoryAttributeMapper.java` | 新建（分类规格模板） |
| 新增 Mapper | `seckill-mall/src/main/java/com/seckill/mall/mapper/CategoryAttributeValueMapper.java` | 新建（分类属性预设值） |
| 新增 Mapper | `seckill-mall/src/main/java/com/seckill/mall/mapper/ProductAttributeMapper.java` | 新建 |
| 新增 Mapper | `seckill-mall/src/main/java/com/seckill/mall/mapper/ProductAttributeValueMapper.java` | 新建 |
| 新增 Mapper | `seckill-mall/src/main/java/com/seckill/mall/mapper/ProductSkuMapper.java` | 新建 |
| 新增 Service | `seckill-mall/src/main/java/com/seckill/mall/service/CategoryAttributeService.java` | 新建（分类规格模板管理） |
| 新增 Service | `seckill-mall/src/main/java/com/seckill/mall/service/CategoryAttributeValueService.java` | 新建 |
| 新增 Service | `seckill-mall/src/main/java/com/seckill/mall/service/ProductSkuService.java` | 新建 |
| 新增 Service | `seckill-mall/src/main/java/com/seckill/mall/service/ProductAttributeService.java` | 新建 |
| 新增 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/CategoryAttributeServiceImpl.java` | 新建 |
| 新增 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/CategoryAttributeValueServiceImpl.java` | 新建 |
| 新增 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductSkuServiceImpl.java` | 新建 |
| 新增 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductAttributeServiceImpl.java` | 新建 |
| 修改 Service | `seckill-mall/src/main/java/com/seckill/mall/service/ProductService.java` | 详情加载 SKU |
| 修改 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductServiceImpl.java` | 详情 / 创建 / 更新加 SKU |
| 修改 Service | `seckill-mall/src/main/java/com/seckill/mall/service/CartService.java` | `addToCart` 加 `skuId` |
| 修改 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/CartServiceImpl.java` | SKU 校验 / 唯一约束 / 展示 |
| 修改 Service | `seckill-mall/src/main/java/com/seckill/mall/service/OrderService.java` | `createNormalOrder` 加 `skuId` |
| 修改 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/OrderServiceImpl.java` | SKU 库存扣减 / 快照写入 / 回补 |
| 修改 Service | `seckill-mall/src/main/java/com/seckill/mall/service/ProductReviewService.java` | `create` 加 `skuId` |
| 修改 ServiceImpl | `seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductReviewServiceImpl.java` | SKU 快照写入 |
| 新增 Controller | `seckill-mall/src/main/java/com/seckill/mall/controller/CategoryAttributeController.java` | 新建（分类规格模板后台管理） |
| 新增 Controller | `seckill-mall/src/main/java/com/seckill/mall/controller/ProductSkuController.java` | 新建 |
| 修改 Controller | `seckill-mall/src/main/java/com/seckill/mall/controller/ProductController.java` | 详情返回扩展 VO |
| 修改 Controller | `seckill-mall/src/main/java/com/seckill/mall/controller/CartController.java` | `AddToCartRequest` 加 `skuId` |
| 修改 Controller | `seckill-mall/src/main/java/com/seckill/mall/controller/ProductReviewController.java` | `ReviewCreateRequest` 加 `skuId` |
| 修改 DTO | `seckill-mall/src/main/java/com/seckill/mall/dto/ProductCreateRequest.java` | 加 `attributes` / `skus` |
| 修改 DTO | `seckill-mall/src/main/java/com/seckill/mall/dto/ProductUpdateRequest.java` | 加 `attributes` / `skus` |
| 修改 DTO | `seckill-mall/src/main/java/com/seckill/mall/dto/BuyNowRequest.java` | 加 `skuId` |
| 新增 DTO | `seckill-mall/src/main/java/com/seckill/mall/dto/CategoryAttributeDTO.java` | 新建（分类规格模板创建/更新） |
| 新增 DTO | `seckill-mall/src/main/java/com/seckill/mall/dto/ProductAttributeDTO.java` | 新建 |
| 新增 DTO | `seckill-mall/src/main/java/com/seckill/mall/dto/ProductSkuDTO.java` | 新建 |
| 修改 VO | `seckill-mall/src/main/java/com/seckill/mall/vo/ProductVO.java` | 加 `attributes` / `skus` / `priceRange` |
| 修改 VO | `seckill-mall/src/main/java/com/seckill/mall/vo/CartItemVO.java` | 加 `skuId` / `skuAttributes` |
| 修改 VO | `seckill-mall/src/main/java/com/seckill/mall/vo/ProductReviewVO.java` | 加 `skuId` / `skuAttributes` |
| 新增 VO | `seckill-mall/src/main/java/com/seckill/mall/vo/CategoryAttributeVO.java` | 新建（分类规格模板视图） |
| 新增 VO | `seckill-mall/src/main/java/com/seckill/mall/vo/ProductSkuVO.java` | 新建 |
| 新增 VO | `seckill-mall/src/main/java/com/seckill/mall/vo/ProductAttributeVO.java` | 新建 |
| 新增 SQL | `seckill-mall/src/main/resources/sql/migration_v7_sku.sql` | 新建迁移脚本 |

#### 3.4.2 前端受影响文件清单

| 类型 | 文件路径 | 操作 |
| --- | --- | --- |
| 新增页面 | `frontend/src/views/admin/CategoryAttributeManage.vue` | 新建（分类规格模板管理） |
| 修改路由 | `frontend/src/router/index.ts`（如存在） | 新增分类规格模板管理路由 |
| 修改页面 | `frontend/src/views/admin/ProductEdit.vue` | 新增规格管理区 + SKU 表格 + 选择分类自动带出模板 |
| 修改页面 | `frontend/src/views/front/ProductDetail.vue` | SKU 选择器对接后端 + 价格 / 库存联动 + 加购 / 购买带 skuId |
| 修改页面 | `frontend/src/views/front/Cart.vue`（如存在） | 展示 SKU 信息 |
| 修改页面 | `frontend/src/views/front/OrderDetail.vue`（如存在） | 订单项展示 SKU 信息 |
| 修改页面 | `frontend/src/views/front/ProductReview.vue` 或评论组件 | 展示 SKU 信息 |
| 新增 API | `frontend/src/api/categoryAttribute.ts` | 新建（分类规格模板管理 API） |
| 新增 API | `frontend/src/api/sku.ts` | 新建 |
| 修改 API | `frontend/src/api/product.ts` | 详情类型扩展 |
| 修改 API | `frontend/src/api/cart.ts` | `CartAddRequest` 加 `skuId` |
| 修改 API | `frontend/src/api/review.ts` | `createReview` 加 `skuId` |
| 修改类型 | `frontend/src/types/index.ts` | 新增 SKU 相关接口，扩展现有接口 |

---

## 4. 数据库设计方案

### 4.1 新增表设计

#### 4.1.0 `t_category_attribute` 分类属性模板表

存储分类级别的规格维度模板定义，如「智能手表」分类下定义「颜色 / 版本 / 表带」三个属性模板。商家添加商品时选择分类后自动带出该分类的属性模板，避免每个商品重复配置。

```sql
CREATE TABLE `t_category_attribute` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID',
    `category_id` BIGINT       NOT NULL              COMMENT '所属分类ID',
    `name`        VARCHAR(50)  NOT NULL              COMMENT '属性名称（如：颜色、版本、表带）',
    `type`        ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：TEXT-文字/IMAGE-图片(色块)',
    `input_type`  ENUM('SELECT','INPUT') NOT NULL DEFAULT 'SELECT' COMMENT '录入方式：SELECT-从预设值选/INPUT-自由输入',
    `is_required` TINYINT      NOT NULL DEFAULT 0    COMMENT '是否必选：0-否/1-是',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性模板表';
```

字段说明：
- `category_id`：关联 `t_category.id`，一个分类可定义多个属性模板。
- `name`：属性名，如「颜色」「版本」「表带」。
- `type`：`IMAGE` 表示图片型（如颜色，需色块 URL），`TEXT` 表示文字型（如版本、表带）。
- `input_type`：`SELECT` 表示商家添加商品时从预设值列表中选择；`INPUT` 表示商家自由输入属性值（如「限量编号」）。
- `is_required`：是否必选，用于商品编辑时校验该属性必须填写。
- `sort_order`：控制添加商品时属性展示顺序。

#### 4.1.0.1 `t_category_attribute_value` 分类属性预设值表

存储分类属性模板的预设可选值，如「颜色」属性模板下预设「曜石黑 / 银月白 / 赤霞红 / 冰川蓝 / 森野绿 / 深空灰」。

```sql
CREATE TABLE `t_category_attribute_value` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID',
    `attribute_id` BIGINT      NOT NULL              COMMENT '所属分类属性ID',
    `value`       VARCHAR(100) NOT NULL              COMMENT '预设值（如：曜石黑、标准版）',
    `image_url`   VARCHAR(255) DEFAULT NULL          COMMENT '图片类型时的色块/图片URL',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_attribute` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性预设值表';
```

字段说明：
- `attribute_id`：关联 `t_category_attribute.id`。
- `value`：预设值文本，如「曜石黑」「标准版」。
- `image_url`：仅当所属属性 `type=IMAGE` 时使用，存储色块图片 URL（如 `#1f2937` 纯色或上传的色块图片）。
- `sort_order`：控制预设值在添加商品时的展示顺序。

#### 4.1.1 `t_product_attribute` 商品属性表

存储商品的规格维度定义，如「颜色」「版本」「表带」。在 v1.0 基础上新增 `category_attribute_id` 字段，关联分类属性模板（NULL 表示商品自定义属性，非 NULL 表示该属性来自分类模板）。

```sql
CREATE TABLE `t_product_attribute` (
    `id`                    BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `product_id`            BIGINT       NOT NULL             COMMENT '所属商品ID',
    `category_attribute_id` BIGINT       DEFAULT NULL         COMMENT '关联分类属性模板ID（NULL=商品自定义属性）',
    `name`                  VARCHAR(50)  NOT NULL             COMMENT '属性名（如：颜色/版本/表带）',
    `type`                  ENUM('IMAGE','TEXT') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：IMAGE-图片型/TEXT-文字型',
    `sort_order`            INT          NOT NULL DEFAULT 0   COMMENT '排序权重（值越小越靠前）',
    `is_deleted`            TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_category_attribute_id` (`category_attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性表（SKU维度定义）';
```

字段说明：
- `product_id`：关联 `t_product.id`，一个商品可有多个属性。
- `category_attribute_id`：关联 `t_category_attribute.id`。非 NULL 表示该属性来源于分类模板（商家在添加商品时由模板自动带出，可在此基础上修改属性值）；NULL 表示商品自定义属性（商家手动添加的分类模板未覆盖的属性）。该字段用于前端区分「模板属性」与「自定义属性」，以及后续模板变更时的同步策略。
- `name`：属性名，如「颜色」「版本」「表带」。
- `type`：`IMAGE` 表示图片型（如颜色，需色块 URL），`TEXT` 表示文字型（如版本、表带）。
- `sort_order`：控制前台展示顺序。

**对 v1.0 已执行迁移的兼容**：若 `migration_v7_sku.sql` 已在测试环境执行过（无 `category_attribute_id` 字段），通过追加 ALTER 语句补字段：

```sql
ALTER TABLE `t_product_attribute` ADD COLUMN `category_attribute_id` BIGINT DEFAULT NULL COMMENT '关联分类属性模板ID（NULL=商品自定义属性）' AFTER `product_id`;
ALTER TABLE `t_product_attribute` ADD KEY `idx_category_attribute_id` (`category_attribute_id`);
```

#### 4.1.2 `t_product_attribute_value` 属性值表

存储每个属性下的可选值，如「颜色」下的「曜石黑」「银月白」等。

```sql
CREATE TABLE `t_product_attribute_value` (
    `id`           BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `attribute_id` BIGINT       NOT NULL             COMMENT '所属属性ID',
    `product_id`   BIGINT       NOT NULL             COMMENT '所属商品ID（冗余，加速查询）',
    `value`        VARCHAR(100) NOT NULL             COMMENT '属性值（如：曜石黑/旗舰版/氟橡胶表带）',
    `image_url`    VARCHAR(255) DEFAULT NULL         COMMENT '图片型属性值的色块URL或图片URL（文字型为NULL）',
    `sort_order`   INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_attribute_id` (`attribute_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性值表（SKU维度可选值）';
```

字段说明：
- `attribute_id`：关联 `t_product_attribute.id`。
- `product_id`：冗余字段，避免查询属性值时再 JOIN 属性表取 `product_id`，加速「按商品查所有属性值」场景。
- `value`：属性值文本。
- `image_url`：仅当所属属性 `type=IMAGE` 时使用，存储色块图片 URL（如 `#1f2937` 纯色或上传的色块图片）。

#### 4.1.3 `t_product_sku` SKU 表

存储每个属性值组合的独立价格 / 库存 / 图片 / 编码 / 状态。

```sql
CREATE TABLE `t_product_sku` (
    `id`          BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `product_id`  BIGINT        NOT NULL            COMMENT '所属商品ID',
    `sku_code`    VARCHAR(64)   DEFAULT NULL        COMMENT 'SKU编码（同商品内唯一，可空表示自动生成）',
    `price`       DECIMAL(10,2) NOT NULL            COMMENT 'SKU价格（元）',
    `stock`       INT           NOT NULL DEFAULT 0  COMMENT 'SKU库存',
    `main_image`  VARCHAR(255)  DEFAULT NULL        COMMENT 'SKU主图URL（NULL表示沿用商品主图）',
    `attributes`  JSON          NOT NULL            COMMENT 'SKU属性键值对JSON，如{"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}',
    `status`      TINYINT       NOT NULL DEFAULT 1  COMMENT '状态：1-启用/0-禁用',
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除：0-正常/1-已删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku_code` (`product_id`, `sku_code`) COMMENT '同商品内SKU编码唯一',
    KEY `idx_product_id` (`product_id`),
    KEY `idx_product_status` (`product_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品SKU表（属性值组合）';
```

字段说明：
- `sku_code`：可空，由后台手填或自动生成（如 `颜色:曜石黑-版本:旗舰版-表带:氟橡胶`），同商品内唯一。
- `price` / `stock`：覆盖 `t_product.original_price` / `t_product.stock`。
- `main_image`：可空，空则前台展示时回退到 `t_product.main_image`。
- `attributes`：JSON 字符串，记录该 SKU 的属性键值对，便于订单 / 评论快照与前端展示。MySQL 8.0 原生 JSON 类型支持 JSON_EXTRACT 等查询。
- `status`：0 禁用表示该组合暂不售卖（如设计稿中「运动版」按钮置灰）。

### 4.2 现有表修改

#### 4.2.1 `t_cart` 新增 `sku_id` 并修改唯一约束

```sql
-- 新增 sku_id 字段
ALTER TABLE `t_cart` ADD COLUMN `sku_id` BIGINT DEFAULT NULL COMMENT 'SKU ID（NULL表示无规格商品）' AFTER `product_id`;

-- 删除原唯一约束，改为 (user_id, product_id, sku_id) 三列唯一
ALTER TABLE `t_cart` DROP INDEX `uk_user_product`;
ALTER TABLE `t_cart` ADD UNIQUE KEY `uk_user_product_sku` (`user_id`, `product_id`, `sku_id`) COMMENT '用户+商品+SKU唯一约束';

-- 新增索引加速按 SKU 查询
ALTER TABLE `t_cart` ADD KEY `idx_sku_id` (`sku_id`);
```

说明：`sku_id` 允许 NULL，用于无规格的旧商品（向后兼容）。唯一约束改为三列，使同一商品不同 SKU 视为不同购物车项。

#### 4.2.2 `t_normal_order_item` 新增 `sku_id` / `sku_attributes`

```sql
ALTER TABLE `t_normal_order_item` ADD COLUMN `sku_id`         BIGINT       DEFAULT NULL COMMENT 'SKU ID（下单时快照，NULL表示无规格）' AFTER `product_id`;
ALTER TABLE `t_normal_order_item` ADD COLUMN `sku_attributes` VARCHAR(500) DEFAULT NULL COMMENT 'SKU属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带）' AFTER `sku_id`;
ALTER TABLE `t_normal_order_item` ADD KEY `idx_sku_id` (`sku_id`);
```

说明：`sku_attributes` 用 VARCHAR(500) 存储可读字符串快照（如「曜石黑 / 旗舰版 / 氟橡胶表带」），而非 JSON，因为订单展示只需可读文本，且历史订单不受后续 SKU 修改影响。

#### 4.2.3 `t_product_review` 新增 `sku_id` / `sku_attributes`

```sql
ALTER TABLE `t_product_review` ADD COLUMN `sku_id`         BIGINT       DEFAULT NULL COMMENT 'SKU ID（NULL表示无规格评论）' AFTER `product_id`;
ALTER TABLE `t_product_review` ADD COLUMN `sku_attributes` VARCHAR(500) DEFAULT NULL COMMENT 'SKU属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带）' AFTER `sku_id`;
ALTER TABLE `t_product_review` ADD KEY `idx_sku_id` (`sku_id`);
```

#### 4.2.4 `t_product` 字段语义调整（不改结构）

`t_product.original_price` 与 `t_product.stock` **保持原字段不变**，但语义调整为：

- 当商品**无 SKU**（`t_product_sku` 中无该商品的启用记录）时：`original_price` / `stock` 仍为商品的价格 / 库存（向后兼容）。
- 当商品**有 SKU** 时：`original_price` / `stock` 作为**兜底默认值**（用于 SKU 未全部覆盖时的回退），实际展示价格取 SKU 价格区间，实际库存取所有启用 SKU 库存之和。

这样无需 ALTER 表结构，仅靠 Service 层逻辑即可兼容。新增商品时若定义了 SKU，`original_price` 可设为最低 SKU 价格、`stock` 设为 SKU 库存之和，保持列表页展示一致性。

### 4.3 索引设计汇总

| 表 | 索引名 | 字段 | 用途 |
| --- | --- | --- | --- |
| `t_category_attribute` | `idx_category` | `category_id` | 按分类查所有属性模板 |
| `t_category_attribute_value` | `idx_attribute` | `attribute_id` | 按属性模板查所有预设值 |
| `t_product_attribute` | `idx_product_id` | `product_id` | 按商品查所有属性 |
| `t_product_attribute` | `idx_category_attribute_id` | `category_attribute_id` | 按模板 ID 查关联的商品属性（模板变更同步用） |
| `t_product_attribute_value` | `idx_attribute_id` | `attribute_id` | 按属性查所有值 |
| `t_product_attribute_value` | `idx_product_id` | `product_id` | 按商品查所有属性值（冗余字段加速） |
| `t_product_sku` | `uk_product_sku_code` | `(product_id, sku_code)` | 同商品内 SKU 编码唯一 |
| `t_product_sku` | `idx_product_id` | `product_id` | 按商品查所有 SKU |
| `t_product_sku` | `idx_product_status` | `(product_id, status)` | 按商品查启用 SKU |
| `t_cart` | `uk_user_product_sku` | `(user_id, product_id, sku_id)` | 同用户同商品同 SKU 唯一 |
| `t_cart` | `idx_sku_id` | `sku_id` | 按 SKU 查购物车项 |
| `t_normal_order_item` | `idx_sku_id` | `sku_id` | 按 SKU 查订单明细 |
| `t_product_review` | `idx_sku_id` | `sku_id` | 按 SKU 查评论 |

### 4.4 迁移 SQL 脚本

新建文件：`seckill-mall/src/main/resources/sql/migration_v7_sku.sql`

```sql
-- ============================================================
-- SeckillMall 数据库迁移脚本 V7：SKU 多规格商品系统
--
-- 新增表：t_category_attribute / t_category_attribute_value
--        t_product_attribute / t_product_attribute_value / t_product_sku
-- 变更表：t_cart / t_normal_order_item / t_product_review 新增 sku_id 等字段
--
-- ⚠️  请手动执行此脚本，不会自动执行
-- 执行前请确认数据库连接和权限
-- ============================================================
-- MySQL 8.0 + InnoDB + utf8mb4_general_ci
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID，BIGINT）
-- 执行顺序：t_category_attribute → t_category_attribute_value
--           → t_product_attribute → t_product_attribute_value → t_product_sku
--           → ALTER t_cart → ALTER t_normal_order_item → ALTER t_product_review
-- ============================================================

-- ------------------------------------------------------------
-- 0. 分类属性模板表
-- ------------------------------------------------------------
CREATE TABLE `t_category_attribute` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID',
    `category_id` BIGINT       NOT NULL              COMMENT '所属分类ID',
    `name`        VARCHAR(50)  NOT NULL              COMMENT '属性名称（如：颜色、版本、表带）',
    `type`        ENUM('TEXT','IMAGE') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：TEXT-文字/IMAGE-图片(色块)',
    `input_type`  ENUM('SELECT','INPUT') NOT NULL DEFAULT 'SELECT' COMMENT '录入方式：SELECT-从预设值选/INPUT-自由输入',
    `is_required` TINYINT      NOT NULL DEFAULT 0    COMMENT '是否必选：0-否/1-是',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性模板表';

-- ------------------------------------------------------------
-- 0.1 分类属性预设值表
-- ------------------------------------------------------------
CREATE TABLE `t_category_attribute_value` (
    `id`          BIGINT       NOT NULL              COMMENT '主键ID',
    `attribute_id` BIGINT      NOT NULL              COMMENT '所属分类属性ID',
    `value`       VARCHAR(100) NOT NULL              COMMENT '预设值（如：曜石黑、标准版）',
    `image_url`   VARCHAR(255) DEFAULT NULL          COMMENT '图片类型时的色块/图片URL',
    `sort_order`  INT          NOT NULL DEFAULT 0    COMMENT '排序权重',
    `is_deleted`  TINYINT      NOT NULL DEFAULT 0    COMMENT '逻辑删除',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_attribute` (`attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分类属性预设值表';

-- ------------------------------------------------------------
-- 1. 商品属性表（含 category_attribute_id 关联分类模板）
-- ------------------------------------------------------------
CREATE TABLE `t_product_attribute` (
    `id`                    BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `product_id`            BIGINT       NOT NULL             COMMENT '所属商品ID',
    `category_attribute_id` BIGINT       DEFAULT NULL         COMMENT '关联分类属性模板ID（NULL=商品自定义属性）',
    `name`                  VARCHAR(50)  NOT NULL             COMMENT '属性名（如：颜色/版本/表带）',
    `type`                  ENUM('IMAGE','TEXT') NOT NULL DEFAULT 'TEXT' COMMENT '属性类型：IMAGE-图片型/TEXT-文字型',
    `sort_order`            INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `is_deleted`            TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_category_attribute_id` (`category_attribute_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性表（SKU维度定义）';

-- ------------------------------------------------------------
-- 2. 商品属性值表
-- ------------------------------------------------------------
CREATE TABLE `t_product_attribute_value` (
    `id`           BIGINT       NOT NULL             COMMENT '主键ID（雪花算法）',
    `attribute_id` BIGINT       NOT NULL             COMMENT '所属属性ID',
    `product_id`   BIGINT       NOT NULL             COMMENT '所属商品ID（冗余）',
    `value`        VARCHAR(100) NOT NULL             COMMENT '属性值',
    `image_url`    VARCHAR(255) DEFAULT NULL         COMMENT '图片型属性值的色块URL',
    `sort_order`   INT          NOT NULL DEFAULT 0   COMMENT '排序权重',
    `is_deleted`   TINYINT      NOT NULL DEFAULT 0   COMMENT '逻辑删除',
    `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_attribute_id` (`attribute_id`),
    KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品属性值表';

-- ------------------------------------------------------------
-- 3. 商品SKU表
-- ------------------------------------------------------------
CREATE TABLE `t_product_sku` (
    `id`          BIGINT        NOT NULL            COMMENT '主键ID（雪花算法）',
    `product_id`  BIGINT        NOT NULL            COMMENT '所属商品ID',
    `sku_code`    VARCHAR(64)   DEFAULT NULL        COMMENT 'SKU编码（同商品内唯一）',
    `price`       DECIMAL(10,2) NOT NULL            COMMENT 'SKU价格（元）',
    `stock`       INT           NOT NULL DEFAULT 0  COMMENT 'SKU库存',
    `main_image`  VARCHAR(255)  DEFAULT NULL        COMMENT 'SKU主图URL（NULL沿用商品主图）',
    `attributes`  JSON          NOT NULL            COMMENT 'SKU属性键值对JSON',
    `status`      TINYINT       NOT NULL DEFAULT 1  COMMENT '状态：1-启用/0-禁用',
    `is_deleted`  TINYINT       NOT NULL DEFAULT 0  COMMENT '逻辑删除',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
    `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_sku_code` (`product_id`, `sku_code`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_product_status` (`product_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品SKU表';

-- ------------------------------------------------------------
-- 4. t_cart 新增 sku_id 并修改唯一约束
-- ------------------------------------------------------------
ALTER TABLE `t_cart` ADD COLUMN `sku_id` BIGINT DEFAULT NULL COMMENT 'SKU ID（NULL表示无规格商品）' AFTER `product_id`;
ALTER TABLE `t_cart` DROP INDEX `uk_user_product`;
ALTER TABLE `t_cart` ADD UNIQUE KEY `uk_user_product_sku` (`user_id`, `product_id`, `sku_id`) COMMENT '用户+商品+SKU唯一约束';
ALTER TABLE `t_cart` ADD KEY `idx_sku_id` (`sku_id`);

-- ------------------------------------------------------------
-- 5. t_normal_order_item 新增 sku_id / sku_attributes
-- ------------------------------------------------------------
ALTER TABLE `t_normal_order_item` ADD COLUMN `sku_id`         BIGINT       DEFAULT NULL COMMENT 'SKU ID（下单时快照）' AFTER `product_id`;
ALTER TABLE `t_normal_order_item` ADD COLUMN `sku_attributes` VARCHAR(500) DEFAULT NULL COMMENT 'SKU属性快照' AFTER `sku_id`;
ALTER TABLE `t_normal_order_item` ADD KEY `idx_sku_id` (`sku_id`);

-- ------------------------------------------------------------
-- 6. t_product_review 新增 sku_id / sku_attributes
-- ------------------------------------------------------------
ALTER TABLE `t_product_review` ADD COLUMN `sku_id`         BIGINT       DEFAULT NULL COMMENT 'SKU ID' AFTER `product_id`;
ALTER TABLE `t_product_review` ADD COLUMN `sku_attributes` VARCHAR(500) DEFAULT NULL COMMENT 'SKU属性快照' AFTER `sku_id`;
ALTER TABLE `t_product_review` ADD KEY `idx_sku_id` (`sku_id`);
```

执行说明：脚本不自动执行，由运维手动在测试 / 生产环境执行。执行前请备份 `t_cart` / `t_normal_order_item` / `t_product_review` 三张表。

---

## 5. 后端改动方案

### 5.0 分类规格模板（CategoryAttribute）

本节为分类规格模板机制新增的后端代码，独立于商品 SKU 主流程，但被 `ProductEdit.vue` 选择分类时调用以自动带出模板属性。

#### 5.0.1 新增实体 `CategoryAttribute.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/CategoryAttribute.java`

```java
package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.AttributeType;
import com.seckill.mall.entity.enums.AttributeInputType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类属性模板实体
 * 对应表 t_category_attribute，在分类级别定义规格维度模板。
 */
@Data
@TableName("t_category_attribute")
public class CategoryAttribute {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long categoryId;

    /** 属性名，如「颜色」「版本」「表带」 */
    private String name;

    /** 属性类型：IMAGE-图片型 / TEXT-文字型 */
    @TableField("type")
    private AttributeType type;

    /** 录入方式：SELECT-从预设值选 / INPUT-自由输入 */
    @TableField("input_type")
    private AttributeInputType inputType;

    /** 是否必选：0-否 / 1-是 */
    private Integer isRequired;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 5.0.2 新增实体 `CategoryAttributeValue.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/CategoryAttributeValue.java`

```java
package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类属性预设值实体
 * 对应表 t_category_attribute_value，存储分类属性模板的预设可选值。
 */
@Data
@TableName("t_category_attribute_value")
public class CategoryAttributeValue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long attributeId;

    /** 预设值，如「曜石黑」「标准版」 */
    private String value;

    /** 图片型属性值的色块 URL，文字型为 null */
    private String imageUrl;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 5.0.3 新增枚举 `AttributeInputType.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/enums/AttributeInputType.java`

```java
package com.seckill.mall.entity.enums;

/**
 * 属性录入方式枚举
 * SELECT - 从预设值列表选择
 * INPUT  - 自由输入
 */
public enum AttributeInputType {
    SELECT,
    INPUT
}
```

#### 5.0.4 新增 Mapper

**`CategoryAttributeMapper.java`** 文件：`seckill-mall/src/main/java/com/seckill/mall/mapper/CategoryAttributeMapper.java`

```java
package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.CategoryAttribute;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryAttributeMapper extends BaseMapper<CategoryAttribute> {
}
```

**`CategoryAttributeValueMapper.java`** 文件：`seckill-mall/src/main/java/com/seckill/mall/mapper/CategoryAttributeValueMapper.java`

```java
package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.CategoryAttributeValue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryAttributeValueMapper extends BaseMapper<CategoryAttributeValue> {
}
```

#### 5.0.5 新增 DTO `CategoryAttributeDTO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/dto/CategoryAttributeDTO.java`

```java
package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 分类属性模板 DTO（创建 / 更新分类规格模板时携带）
 */
@Data
public class CategoryAttributeDTO {

    private Long id;

    private Long categoryId;

    @NotBlank(message = "属性名不能为空")
    @Size(max = 50, message = "属性名最大 50 字符")
    private String name;

    /** 属性类型：IMAGE / TEXT */
    private String type;

    /** 录入方式：SELECT / INPUT */
    private String inputType;

    /** 是否必选：0-否 / 1-是 */
    private Integer isRequired;

    private Integer sortOrder;

    /** 预设值列表（inputType=SELECT 时使用） */
    private List<CategoryAttributeValueDTO> values;

    @Data
    public static class CategoryAttributeValueDTO {
        private Long id;
        @NotBlank(message = "预设值不能为空")
        @Size(max = 100, message = "预设值最大 100 字符")
        private String value;
        /** 图片型属性值的色块 URL */
        private String imageUrl;
        private Integer sortOrder;
    }
}
```

#### 5.0.6 新增 VO `CategoryAttributeVO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/vo/CategoryAttributeVO.java`

```java
package com.seckill.mall.vo;

import lombok.Data;

import java.util.List;

/**
 * 分类属性模板视图对象（GET /api/v1/admin/category/{categoryId}/attributes 返回）
 */
@Data
public class CategoryAttributeVO {
    private Long id;
    private Long categoryId;
    private String name;
    /** IMAGE / TEXT */
    private String type;
    /** SELECT / INPUT */
    private String inputType;
    private Integer isRequired;
    private Integer sortOrder;
    private List<CategoryAttributeValueVO> values;

    @Data
    public static class CategoryAttributeValueVO {
        private Long id;
        private String value;
        private String imageUrl;
        private Integer sortOrder;
    }
}
```

#### 5.0.7 新增 Service `CategoryAttributeService.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/CategoryAttributeService.java`

```java
package com.seckill.mall.service;

import com.seckill.mall.dto.CategoryAttributeDTO;
import com.seckill.mall.vo.CategoryAttributeVO;

import java.util.List;

/**
 * 分类规格模板服务接口
 */
public interface CategoryAttributeService {

    /**
     * 获取分类的规格模板（含预设值），按 sortOrder 升序
     * 供 ProductEdit.vue 选择分类后自动带出模板属性
     */
    List<CategoryAttributeVO> getAttributesByCategory(Long categoryId);

    /**
     * 创建分类规格属性（含预设值）
     */
    CategoryAttributeVO createAttribute(CategoryAttributeDTO dto);

    /**
     * 更新分类规格属性（含预设值，先删后插）
     */
    CategoryAttributeVO updateAttribute(Long id, CategoryAttributeDTO dto);

    /**
     * 删除分类规格属性（逻辑删除，级联删除其预设值）
     */
    void deleteAttribute(Long id);

    /**
     * 为已有分类属性添加一个预设值
     */
    CategoryAttributeVO addAttributeValue(Long attributeId,
                                          CategoryAttributeDTO.CategoryAttributeValueDTO value);
}
```

**`CategoryAttributeValueService.java`** 文：`seckill-mall/src/main/java/com/seckill/mall/service/CategoryAttributeValueService.java`

```java
package com.seckill.mall.service;

import com.seckill.mall.entity.CategoryAttributeValue;

import java.util.List;

/**
 * 分类属性预设值服务接口
 */
public interface CategoryAttributeValueService {

    /** 按属性 ID 查所有预设值（按 sortOrder 升序） */
    List<CategoryAttributeValue> listByAttributeId(Long attributeId);

    /** 批量保存属性的预设值（先逻辑删除旧值再插入新值） */
    void saveValues(Long attributeId, List<CategoryAttributeValue> values);

    /** 逻辑删除属性的所有预设值 */
    void deleteByAttributeId(Long attributeId);
}
```

**实现要点**（`CategoryAttributeServiceImpl` / `CategoryAttributeValueServiceImpl`）：
- `getAttributesByCategory`：先查 `t_category_attribute`（按 `sort_order` 升序），再批量查 `t_category_attribute_value`（按 `sort_order` 升序），内存中按 `attributeId` 分组组装成 `CategoryAttributeVO` 列表。
- `createAttribute`：插入 `t_category_attribute`，再遍历 `values` 插入 `t_category_attribute_value`。
- `updateAttribute`：事务内更新 `t_category_attribute`，调用 `categoryAttributeValueService.saveValues` 先删后插预设值。
- `deleteAttribute`：事务内逻辑删除 `t_category_attribute` 与其所有 `t_category_attribute_value`。
- `addAttributeValue`：单条插入 `t_category_attribute_value`，返回更新后的属性 VO。

#### 5.0.8 新增 Controller `CategoryAttributeController.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/controller/CategoryAttributeController.java`

后台管理接口，前缀 `/api/v1/admin/category`，需管理员权限。

```java
package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.dto.CategoryAttributeDTO;
import com.seckill.mall.service.CategoryAttributeService;
import com.seckill.mall.vo.CategoryAttributeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类规格模板控制器（后台管理）
 */
@Tag(name = "分类规格模板", description = "分类级别定义规格维度模板")
@RestController
@RequestMapping("/api/v1/admin/category")
@RequiredArgsConstructor
public class CategoryAttributeController {

    private final CategoryAttributeService categoryAttributeService;

    @Operation(summary = "获取分类的规格模板（含预设值）")
    @GetMapping("/{categoryId}/attributes")
    public Result<List<CategoryAttributeVO>> listByCategory(@PathVariable Long categoryId) {
        return Result.success(categoryAttributeService.getAttributesByCategory(categoryId));
    }

    @Operation(summary = "创建分类规格属性")
    @PostMapping("/attributes")
    public Result<CategoryAttributeVO> create(@RequestBody CategoryAttributeDTO dto) {
        return Result.success(categoryAttributeService.createAttribute(dto));
    }

    @Operation(summary = "更新分类规格属性")
    @PutMapping("/attributes/{id}")
    public Result<CategoryAttributeVO> update(@PathVariable Long id, @RequestBody CategoryAttributeDTO dto) {
        return Result.success(categoryAttributeService.updateAttribute(id, dto));
    }

    @Operation(summary = "删除分类规格属性")
    @DeleteMapping("/attributes/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryAttributeService.deleteAttribute(id);
        return Result.success();
    }

    @Operation(summary = "为分类规格属性添加预设值")
    @PostMapping("/attributes/{id}/values")
    public Result<CategoryAttributeVO> addValue(@PathVariable Long id,
                                                @RequestBody CategoryAttributeDTO.CategoryAttributeValueDTO value) {
        return Result.success(categoryAttributeService.addAttributeValue(id, value));
    }
}
```

#### 5.0.9 修改 `ProductAttribute.java` 加 `categoryAttributeId`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/ProductAttribute.java`

在 `productId` 字段后新增：

```java
/** 关联分类属性模板 ID，null 表示商品自定义属性 */
private Long categoryAttributeId;
```

#### 5.0.10 修改 `ProductAttributeDTO.java` 加 `categoryAttributeId`

文件：`seckill-mall/src/main/java/com/seckill/mall/dto/ProductAttributeDTO.java`

新增字段：

```java
/** 关联分类属性模板 ID，null 表示商品自定义属性 */
private Long categoryAttributeId;
```

#### 5.0.11 修改 `ProductAttributeVO.java` 加 `categoryAttributeId`

文件：`seckill-mall/src/main/java/com/seckill/mall/vo/ProductAttributeVO.java`

新增字段：

```java
/** 关联分类属性模板 ID，null 表示商品自定义属性（前端据此区分模板属性 / 自定义属性） */
private Long categoryAttributeId;
```

#### 5.0.12 修改 `ProductController.java` 详情接口

商品详情接口返回 SKU 信息时，同时返回分类规格模板（供前端判断哪些是模板属性、哪些是自定义）。`ProductServiceImpl.getProductDetail` 在组装 `ProductVO` 时，若商品有 `categoryId`，调用 `categoryAttributeService.getAttributesByCategory(categoryId)` 填充 `ProductVO.categoryAttributes` 字段（新增）。

`ProductVO.java` 新增字段：

```java
/** 分类规格模板（供前端区分模板属性 / 自定义属性） */
private List<CategoryAttributeVO> categoryAttributes;
```

#### 5.0.13 修改 `ProductService.java` 创建 / 编辑商品校验

`ProductServiceImpl.createProduct` / `updateProduct` 在保存商品属性时，对每个 `ProductAttributeDTO`：
- 若 `categoryAttributeId != null`，校验该 ID 确实属于商品的 `categoryId`（即 `t_category_attribute` 中存在 `id = categoryAttributeId AND category_id = 商品 categoryId` 的记录），否则抛 `BusinessException("属性模板不属于该商品分类")`。
- 若 `categoryAttributeId == null`，视为商品自定义属性，直接放行。
- 保存 `t_product_attribute` 时写入 `category_attribute_id` 字段。

### 5.1 新增实体

#### 5.1.1 `ProductAttribute.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/ProductAttribute.java`

```java
package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.seckill.mall.entity.enums.AttributeType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品属性实体（SKU 维度定义）
 * 对应表 t_product_attribute，如「颜色」「版本」「表带」。
 */
@Data
@TableName("t_product_attribute")
public class ProductAttribute {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    /** 属性名，如「颜色」「版本」「表带」 */
    private String name;

    /** 属性类型：IMAGE-图片型 / TEXT-文字型 */
    @TableField("type")
    private AttributeType type;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 5.1.2 `ProductAttributeValue.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/ProductAttributeValue.java`

```java
package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品属性值实体
 * 对应表 t_product_attribute_value，如颜色属性下的「曜石黑」「银月白」。
 */
@Data
@TableName("t_product_attribute_value")
public class ProductAttributeValue {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long attributeId;

    /** 冗余字段，加速按商品查所有属性值 */
    private Long productId;

    /** 属性值，如「曜石黑」「旗舰版」 */
    private String value;

    /** 图片型属性值的色块 URL，文字型为 null */
    private String imageUrl;

    private Integer sortOrder;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 5.1.3 `ProductSku.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/ProductSku.java`

```java
package com.seckill.mall.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SKU 实体（属性值组合）
 * 对应表 t_product_sku，每个 SKU 拥有独立价格 / 库存 / 主图 / 编码 / 状态。
 */
@Data
@TableName("t_product_sku")
public class ProductSku {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long productId;

    /** SKU 编码，同商品内唯一，可空 */
    private String skuCode;

    /** SKU 价格 */
    private BigDecimal price;

    /** SKU 库存 */
    private Integer stock;

    /** SKU 主图 URL，null 表示沿用商品主图 */
    private String mainImage;

    /**
     * SKU 属性键值对 JSON 字符串
     * 如 {"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}
     * MySQL JSON 类型在 MyBatis-Plus 中按 String 读写
     */
    private String attributes;

    /** 状态：1-启用 / 0-禁用 */
    private Integer status;

    @TableLogic
    private Integer isDeleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

#### 5.1.4 `AttributeType.java` 枚举

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/enums/AttributeType.java`

```java
package com.seckill.mall.entity.enums;

/**
 * 属性类型枚举
 * IMAGE - 图片型（如颜色，需色块 URL）
 * TEXT  - 文字型（如版本、表带）
 */
public enum AttributeType {
    IMAGE,
    TEXT
}
```

### 5.2 修改实体

#### 5.2.1 `Cart.java` 加 `skuId`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/Cart.java`

在 `productId` 字段后新增：

```java
/** SKU ID，null 表示无规格商品 */
private Long skuId;
```

#### 5.2.2 `NormalOrderItem.java` 加 `skuId` / `skuAttributes`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/NormalOrderItem.java`

在 `productId` 字段后新增：

```java
/** SKU ID（下单时快照，null 表示无规格） */
private Long skuId;

/** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
private String skuAttributes;
```

#### 5.2.3 `ProductReview.java` 加 `skuId` / `skuAttributes`

文件：`seckill-mall/src/main/java/com/seckill/mall/entity/ProductReview.java`

在 `productId` 字段后新增：

```java
/** SKU ID，null 表示无规格评论 */
private Long skuId;

/** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
private String skuAttributes;
```

### 5.3 新增 DTO / VO

#### 5.3.1 `ProductAttributeDTO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/dto/ProductAttributeDTO.java`

```java
package com.seckill.mall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 商品属性 DTO（创建 / 更新商品时携带）
 */
@Data
public class ProductAttributeDTO {

    /** 属性 ID（更新时携带，新增时为 null） */
    private Long id;

    @NotBlank(message = "属性名不能为空")
    @Size(max = 50, message = "属性名最大 50 字符")
    private String name;

    /** 属性类型：IMAGE / TEXT */
    private String type;

    private Integer sortOrder;

    /** 属性值列表 */
    private List<AttributeValueDTO> values;

    @Data
    public static class AttributeValueDTO {
        private Long id;
        @NotBlank(message = "属性值不能为空")
        @Size(max = 100, message = "属性值最大 100 字符")
        private String value;
        /** 图片型属性值的色块 URL */
        private String imageUrl;
        private Integer sortOrder;
    }
}
```

#### 5.3.2 `ProductSkuDTO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/dto/ProductSkuDTO.java`

```java
package com.seckill.mall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU DTO（创建 / 更新商品时携带）
 */
@Data
public class ProductSkuDTO {

    private Long id;

    private String skuCode;

    @NotNull(message = "SKU 价格不能为空")
    @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
    private BigDecimal price;

    @NotNull(message = "SKU 库存不能为空")
    @Min(value = 0, message = "SKU 库存不能小于 0")
    private Integer stock;

    /** SKU 主图 URL，可空表示沿用商品主图 */
    private String mainImage;

    /**
     * SKU 属性键值对 JSON 字符串
     * 如 {"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}
     */
    @NotNull(message = "SKU 属性不能为空")
    private String attributes;

    /** 状态：1-启用 / 0-禁用，默认 1 */
    private Integer status = 1;
}
```

#### 5.3.3 `ProductAttributeVO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/vo/ProductAttributeVO.java`

```java
package com.seckill.mall.vo;

import lombok.Data;

import java.util.List;

/**
 * 商品属性视图对象（商品详情接口返回）
 */
@Data
public class ProductAttributeVO {
    private Long id;
    private String name;
    /** IMAGE / TEXT */
    private String type;
    private Integer sortOrder;
    private List<AttributeValueVO> values;

    @Data
    public static class AttributeValueVO {
        private Long id;
        private String value;
        private String imageUrl;
        private Integer sortOrder;
    }
}
```

#### 5.3.4 `ProductSkuVO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/vo/ProductSkuVO.java`

```java
package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU 视图对象（商品详情接口返回）
 */
@Data
public class ProductSkuVO {
    private Long id;
    private String skuCode;
    private BigDecimal price;
    private Integer stock;
    private String mainImage;
    /** SKU 属性键值对 JSON 字符串 */
    private String attributes;
    /** 状态：1-启用 / 0-禁用 */
    private Integer status;
}
```

#### 5.3.5 扩展 `ProductVO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/vo/ProductVO.java`

新增字段：

```java
/** 商品属性列表（无规格时为空列表） */
private List<ProductAttributeVO> attributes;

/** 商品 SKU 列表（无规格时为空列表） */
private List<ProductSkuVO> skus;

/** 是否有 SKU：true 表示多规格商品 */
private Boolean hasSku;

/** 价格区间最小值（有 SKU 时取最低 SKU 价格，无 SKU 时取 originalPrice） */
private BigDecimal minPrice;

/** 价格区间最大值（有 SKU 时取最高 SKU 价格，无 SKU 时取 originalPrice） */
private BigDecimal maxPrice;

/** 总库存（有 SKU 时取所有启用 SKU 库存之和，无 SKU 时取 stock） */
private Integer totalStock;
```

#### 5.3.6 扩展 `CartItemVO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/vo/CartItemVO.java`

新增字段：

```java
/** SKU ID，null 表示无规格 */
private Long skuId;

/** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
private String skuAttributes;

/** SKU 主图 URL（优先于 mainImage 展示） */
private String skuMainImage;
```

#### 5.3.7 扩展 `ProductReviewVO.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/vo/ProductReviewVO.java`

新增字段：

```java
/** SKU ID，null 表示无规格评论 */
private Long skuId;

/** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
private String skuAttributes;
```

### 5.4 新增 Mapper

#### 5.4.1 `ProductAttributeMapper.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/mapper/ProductAttributeMapper.java`

```java
package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.ProductAttribute;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductAttributeMapper extends BaseMapper<ProductAttribute> {
}
```

#### 5.4.2 `ProductAttributeValueMapper.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/mapper/ProductAttributeValueMapper.java`

```java
package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.ProductAttributeValue;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductAttributeValueMapper extends BaseMapper<ProductAttributeValue> {
}
```

#### 5.4.3 `ProductSkuMapper.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/mapper/ProductSkuMapper.java`

```java
package com.seckill.mall.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.seckill.mall.entity.ProductSku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    /**
     * 查询商品所有启用 SKU（按 id 升序）
     */
    List<ProductSku> selectEnabledByProductId(@Param("productId") Long productId);

    /**
     * 按 attributes JSON 精确匹配 SKU（用于加购 / 购买时根据用户选择定位 SKU）
     * 示例 attributesJson: '{"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}'
     */
    ProductSku selectByAttributes(@Param("productId") Long productId,
                                  @Param("attributesJson") String attributesJson);
}
```

对应 `ProductSkuMapper.xml`（新增 `seckill-mall/src/main/resources/mapper/ProductSkuMapper.xml`）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.seckill.mall.mapper.ProductSkuMapper">

    <select id="selectEnabledByProductId" resultType="com.seckill.mall.entity.ProductSku">
        SELECT * FROM t_product_sku
        WHERE product_id = #{productId}
          AND status = 1
          AND is_deleted = 0
        ORDER BY id ASC
    </select>

    <select id="selectByAttributes" resultType="com.seckill.mall.entity.ProductSku">
        SELECT * FROM t_product_sku
        WHERE product_id = #{productId}
          AND attributes = CAST(#{attributesJson} AS JSON)
          AND is_deleted = 0
        LIMIT 1
    </select>

</mapper>
```

### 5.5 新增 Service

#### 5.5.1 `ProductAttributeService.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/ProductAttributeService.java`

```java
package com.seckill.mall.service;

import com.seckill.mall.dto.ProductAttributeDTO;
import com.seckill.mall.vo.ProductAttributeVO;

import java.util.List;

/**
 * 商品属性服务接口
 */
public interface ProductAttributeService {

    /**
     * 查询商品的所有属性及其值（按 sortOrder 排序）
     */
    List<ProductAttributeVO> listByProductId(Long productId);

    /**
     * 批量保存商品的属性及其值（创建 / 更新商品时调用）
     * 采用「先逻辑删除旧属性，再插入新属性」策略，简化实现
     */
    void saveAttributes(Long productId, List<ProductAttributeDTO> attributes);

    /**
     * 逻辑删除商品的所有属性及其值
     */
    void deleteByProductId(Long productId);
}
```

#### 5.5.2 `ProductSkuService.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/ProductSkuService.java`

```java
package com.seckill.mall.service;

import com.seckill.mall.dto.ProductSkuDTO;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.vo.ProductSkuVO;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 SKU 服务接口
 */
public interface ProductSkuService {

    /**
     * 查询商品所有 SKU（含禁用，后台用）
     */
    List<ProductSkuVO> listByProductId(Long productId);

    /**
     * 查询商品所有启用 SKU（前台用）
     */
    List<ProductSkuVO> listEnabledByProductId(Long productId);

    /**
     * 批量保存商品 SKU（创建 / 更新商品时调用）
     * 采用「先逻辑删除旧 SKU，再插入新 SKU」策略
     */
    void saveSkus(Long productId, List<ProductSkuDTO> skus);

    /**
     * 逻辑删除商品的所有 SKU
     */
    void deleteByProductId(Long productId);

    /**
     * 根据 SKU ID 获取 SKU 实体（校验启用状态）
     */
    ProductSku getByIdEnabled(Long skuId);

    /**
     * 扣减 SKU 库存（乐观锁，返回是否成功）
     */
    boolean deductStock(Long skuId, Integer quantity);

    /**
     * 回补 SKU 库存（取消订单 / 超时）
     */
    void restoreStock(Long skuId, Integer quantity);

    /**
     * 计算商品价格区间最小值
     */
    BigDecimal calculateMinPrice(Long productId);

    /**
     * 计算商品价格区间最大值
     */
    BigDecimal calculateMaxPrice(Long productId);

    /**
     * 计算商品总库存（所有启用 SKU 库存之和）
     */
    Integer calculateTotalStock(Long productId);
}
```

#### 5.5.3 `ProductAttributeServiceImpl.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductAttributeServiceImpl.java`

关键方法实现要点：

- `listByProductId`：先查 `t_product_attribute`（按 `sort_order` 升序），再批量查 `t_product_attribute_value`（按 `sort_order` 升序），内存中按 `attributeId` 分组组装成 `ProductAttributeVO` 列表。
- `saveAttributes`：事务内先调用 `deleteByProductId` 逻辑删除旧属性与属性值，再遍历 `attributes` 列表插入新属性，对每个属性插入其 `values` 列表（设置 `productId` 冗余字段）。
- `deleteByProductId`：使用 MyBatis-Plus `LambdaUpdateWrapper` 逻辑删除 `t_product_attribute` 与 `t_product_attribute_value` 中 `product_id` 匹配的记录。

```java
package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.dto.ProductAttributeDTO;
import com.seckill.mall.entity.ProductAttribute;
import com.seckill.mall.entity.ProductAttributeValue;
import com.seckill.mall.entity.enums.AttributeType;
import com.seckill.mall.mapper.ProductAttributeMapper;
import com.seckill.mall.mapper.ProductAttributeValueMapper;
import com.seckill.mall.service.ProductAttributeService;
import com.seckill.mall.vo.ProductAttributeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductAttributeServiceImpl implements ProductAttributeService {

    private final ProductAttributeMapper attributeMapper;
    private final ProductAttributeValueMapper attributeValueMapper;

    @Override
    public List<ProductAttributeVO> listByProductId(Long productId) {
        // 1. 查所有属性，按 sortOrder 升序
        List<ProductAttribute> attrs = attributeMapper.selectList(
                new LambdaUpdateWrapper<ProductAttribute>()
                        .eq(ProductAttribute::getProductId, productId)
                        .orderByAsc(ProductAttribute::getSortOrder));
        if (attrs.isEmpty()) return new ArrayList<>();

        // 2. 查所有属性值，按 sortOrder 升序
        List<ProductAttributeValue> vals = attributeValueMapper.selectList(
                new LambdaUpdateWrapper<ProductAttributeValue>()
                        .eq(ProductAttributeValue::getProductId, productId)
                        .orderByAsc(ProductAttributeValue::getSortOrder));

        // 3. 按 attributeId 分组
        Map<Long, List<ProductAttributeValue>> valMap = vals.stream()
                .collect(Collectors.groupingBy(ProductAttributeValue::getAttributeId));

        // 4. 组装 VO
        return attrs.stream().map(a -> {
            ProductAttributeVO vo = new ProductAttributeVO();
            vo.setId(a.getId());
            vo.setName(a.getName());
            vo.setType(a.getType().name());
            vo.setSortOrder(a.getSortOrder());
            vo.setValues(valMap.getOrDefault(a.getId(), new ArrayList<>()).stream().map(v -> {
                ProductAttributeVO.AttributeValueVO vvo = new ProductAttributeVO.AttributeValueVO();
                vvo.setId(v.getId());
                vvo.setValue(v.getValue());
                vvo.setImageUrl(v.getImageUrl());
                vvo.setSortOrder(v.getSortOrder());
                return vvo;
            }).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAttributes(Long productId, List<ProductAttributeDTO> attributes) {
        deleteByProductId(productId);
        if (attributes == null || attributes.isEmpty()) return;
        int attrSort = 0;
        for (ProductAttributeDTO dto : attributes) {
            ProductAttribute attr = new ProductAttribute();
            attr.setProductId(productId);
            attr.setName(dto.getName());
            attr.setType(AttributeType.valueOf(dto.getType()));
            attr.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : attrSort++);
            attributeMapper.insert(attr);

            if (dto.getValues() != null) {
                int valSort = 0;
                for (ProductAttributeDTO.AttributeValueDTO vdto : dto.getValues()) {
                    ProductAttributeValue val = new ProductAttributeValue();
                    val.setAttributeId(attr.getId());
                    val.setProductId(productId);
                    val.setValue(vdto.getValue());
                    val.setImageUrl(vdto.getImageUrl());
                    val.setSortOrder(vdto.getSortOrder() != null ? vdto.getSortOrder() : valSort++);
                    attributeValueMapper.insert(val);
                }
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProductId(Long productId) {
        // 逻辑删除属性值
        attributeValueMapper.delete(new LambdaUpdateWrapper<ProductAttributeValue>()
                .eq(ProductAttributeValue::getProductId, productId));
        // 逻辑删除属性
        attributeMapper.delete(new LambdaUpdateWrapper<ProductAttribute>()
                .eq(ProductAttribute::getProductId, productId));
    }
}
```

#### 5.5.4 `ProductSkuServiceImpl.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductSkuServiceImpl.java`

关键方法实现要点：

- `saveSkus`：事务内先 `deleteByProductId` 逻辑删除旧 SKU，再遍历插入新 SKU。`skuCode` 为空时自动生成（如 `颜色:曜石黑-版本:旗舰版`）。
- `deductStock`：使用 MyBatis-Plus 乐观锁 `UPDATE t_product_sku SET stock = stock - #{quantity} WHERE id = #{skuId} AND stock >= #{quantity}`，返回受影响行数 > 0 表示成功。
- `restoreStock`：`UPDATE t_product_sku SET stock = stock + #{quantity} WHERE id = #{skuId}`。
- `calculateMinPrice` / `calculateMaxPrice` / `calculateTotalStock`：查询启用 SKU 列表后内存聚合。

```java
package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.dto.ProductSkuDTO;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.mapper.ProductSkuMapper;
import com.seckill.mall.service.ProductSkuService;
import com.seckill.mall.vo.ProductSkuVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductSkuServiceImpl implements ProductSkuService {

    private final ProductSkuMapper skuMapper;

    @Override
    public List<ProductSkuVO> listByProductId(Long productId) {
        List<ProductSku> skus = skuMapper.selectList(
                new LambdaQueryWrapper<ProductSku>()
                        .eq(ProductSku::getProductId, productId)
                        .orderByAsc(ProductSku::getId));
        return skus.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<ProductSkuVO> listEnabledByProductId(Long productId) {
        List<ProductSku> skus = skuMapper.selectEnabledByProductId(productId);
        return skus.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSkus(Long productId, List<ProductSkuDTO> skus) {
        deleteByProductId(productId);
        if (skus == null || skus.isEmpty()) return;
        for (ProductSkuDTO dto : skus) {
            ProductSku sku = new ProductSku();
            sku.setProductId(productId);
            sku.setSkuCode(dto.getSkuCode() != null ? dto.getSkuCode() : generateSkuCode(dto.getAttributes()));
            sku.setPrice(dto.getPrice());
            sku.setStock(dto.getStock());
            sku.setMainImage(dto.getMainImage());
            sku.setAttributes(dto.getAttributes());
            sku.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
            skuMapper.insert(sku);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProductId(Long productId) {
        skuMapper.delete(new LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getProductId, productId));
    }

    @Override
    public ProductSku getByIdEnabled(Long skuId) {
        ProductSku sku = skuMapper.selectById(skuId);
        if (sku == null || sku.getStatus() == null || sku.getStatus() != 1) {
            return null;
        }
        return sku;
    }

    @Override
    public boolean deductStock(Long skuId, Integer quantity) {
        int rows = skuMapper.update(null, new LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .ge(ProductSku::getStock, quantity)
                .setSql("stock = stock - " + quantity));
        return rows > 0;
    }

    @Override
    public void restoreStock(Long skuId, Integer quantity) {
        skuMapper.update(null, new LambdaUpdateWrapper<ProductSku>()
                .eq(ProductSku::getId, skuId)
                .setSql("stock = stock + " + quantity));
    }

    @Override
    public BigDecimal calculateMinPrice(Long productId) {
        List<ProductSkuVO> skus = listEnabledByProductId(productId);
        return skus.stream().map(ProductSkuVO::getPrice).min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateMaxPrice(Long productId) {
        List<ProductSkuVO> skus = listEnabledByProductId(productId);
        return skus.stream().map(ProductSkuVO::getPrice).max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public Integer calculateTotalStock(Long productId) {
        List<ProductSkuVO> skus = listEnabledByProductId(productId);
        return skus.stream().mapToInt(ProductSkuVO::getStock).sum();
    }

    private ProductSkuVO toVO(ProductSku sku) {
        ProductSkuVO vo = new ProductSkuVO();
        vo.setId(sku.getId());
        vo.setSkuCode(sku.getSkuCode());
        vo.setPrice(sku.getPrice());
        vo.setStock(sku.getStock());
        vo.setMainImage(sku.getMainImage());
        vo.setAttributes(sku.getAttributes());
        vo.setStatus(sku.getStatus());
        return vo;
    }

    private String generateSkuCode(String attributesJson) {
        // 简易生成：取 attributes JSON 的 hashCode
        return "SKU-" + Integer.toHexString(attributesJson.hashCode());
    }
}
```

### 5.6 修改 Controller

#### 5.6.1 新增 `ProductSkuController.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/controller/ProductSkuController.java`

提供按商品查 SKU 列表的公开接口（前台商品详情页调用）：

```java
package com.seckill.mall.controller;

import com.seckill.mall.common.Result;
import com.seckill.mall.service.ProductSkuService;
import com.seckill.mall.vo.ProductSkuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品 SKU 控制器
 * 前缀 /api/v1/products/{productId}/skus，公开接口（商品详情页加载 SKU）
 */
@Tag(name = "商品SKU", description = "查询商品SKU列表")
@RestController
@RequestMapping("/api/v1/products/{productId}/skus")
@RequiredArgsConstructor
public class ProductSkuController {

    private final ProductSkuService productSkuService;

    @Operation(summary = "查询商品所有启用SKU（前台用）")
    @GetMapping
    public Result<List<ProductSkuVO>> listEnabled(@PathVariable Long productId) {
        return Result.success(productSkuService.listEnabledByProductId(productId));
    }
}
```

#### 5.6.2 修改 `ProductController.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/controller/ProductController.java`

`detail` 接口返回的 `ProductVO` 已包含 `attributes` / `skus` / `hasSku` / `minPrice` / `maxPrice` / `totalStock` 字段（由 `ProductServiceImpl.getProductDetail` 填充），Controller 层无需改动签名，仅依赖 Service 层填充。

#### 5.6.3 修改 `CartController.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/controller/CartController.java`

修改内嵌类 `AddToCartRequest`，新增 `skuId` 字段：

```java
@Data
public static class AddToCartRequest {
    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    /** SKU ID（可选，null 表示无规格商品） */
    private Long skuId;

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer quantity;
}
```

修改 `add` 方法调用，传入 `skuId`：

```java
public Result<Void> add(@Validated @RequestBody AddToCartRequest req) {
    Long userId = securityUtils.getCurrentUserId();
    return cartService.addToCart(userId, req.getProductId(), req.getSkuId(), req.getQuantity());
}
```

#### 5.6.4 修改 `ProductReviewController.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/controller/ProductReviewController.java`

修改内嵌类 `ReviewCreateRequest`，新增 `skuId` 字段：

```java
@Data
public static class ReviewCreateRequest {
    @NotNull(message = "商品 ID 不能为空")
    private Long productId;

    /** SKU ID（可选，null 表示无规格评论） */
    private Long skuId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论内容最大 1000 字符")
    private String content;

    @NotNull(message = "评分不能为空")
    private Integer rating;

    private String images;
}
```

修改 `create` 方法调用，传入 `skuId`：

```java
public Result<ProductReviewVO> create(@Validated @RequestBody ReviewCreateRequest req) {
    Long userId = securityUtils.getCurrentUserId();
    String safeContent = HtmlUtils.htmlEscape(req.getContent());
    return Result.success(productReviewService.create(
            userId, req.getProductId(), req.getSkuId(), safeContent, req.getRating(), req.getImages()));
}
```

### 5.7 修改现有 Service

#### 5.7.1 修改 `ProductService.java` / `ProductServiceImpl.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/ProductService.java` 与 `impl/ProductServiceImpl.java`

**`getProductDetail(Long id)` 方法修改**：在原有逻辑基础上，调用 `ProductAttributeService.listByProductId(id)` 与 `ProductSkuService.listEnabledByProductId(id)` 填充 `ProductVO` 的 `attributes` / `skus` / `hasSku` / `minPrice` / `maxPrice` / `totalStock` 字段。

```java
@Override
public ProductVO getProductDetail(Long id) {
    Product product = productMapper.selectById(id);
    if (product == null) throw new BusinessException("商品不存在");
    ProductVO vo = convertToVO(product);

    // 加载属性与 SKU
    List<ProductAttributeVO> attributes = productAttributeService.listByProductId(id);
    List<ProductSkuVO> skus = productSkuService.listEnabledByProductId(id);
    vo.setAttributes(attributes);
    vo.setSkus(skus);
    vo.setHasSku(!skus.isEmpty());
    if (!skus.isEmpty()) {
        vo.setMinPrice(skus.stream().map(ProductSkuVO::getPrice).min(BigDecimal::compareTo).orElse(product.getOriginalPrice()));
        vo.setMaxPrice(skus.stream().map(ProductSkuVO::getPrice).max(BigDecimal::compareTo).orElse(product.getOriginalPrice()));
        vo.setTotalStock(skus.stream().mapToInt(ProductSkuVO::getStock).sum());
    } else {
        vo.setMinPrice(product.getOriginalPrice());
        vo.setMaxPrice(product.getOriginalPrice());
        vo.setTotalStock(product.getStock());
    }
    return vo;
}
```

**`createProduct(ProductCreateRequest req)` 方法修改**：在原有插入 `t_product` 后，若 `req.getAttributes()` 与 `req.getSkus()` 非空，调用 `productAttributeService.saveAttributes(productId, req.getAttributes())` 与 `productSkuService.saveSkus(productId, req.getSkus())`。同时若有 SKU，将 `t_product.original_price` 设为最低 SKU 价格、`t_product.stock` 设为 SKU 库存之和，保持列表页展示一致性。

**`updateProduct(Long id, ProductUpdateRequest req)` 方法修改**：同上，更新商品后调用 `saveAttributes` / `saveSkus`（内部会先逻辑删除旧数据再插入新数据）。

**`deleteProduct(Long id)` 方法修改**：删除商品时同步调用 `productAttributeService.deleteByProductId(id)` 与 `productSkuService.deleteByProductId(id)`。

#### 5.7.2 修改 `CartService.java` / `CartServiceImpl.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/CartService.java` 与 `impl/CartServiceImpl.java`

**`addToCart` 方法签名修改**：

```java
Result<Void> addToCart(Long userId, Long productId, Long skuId, Integer quantity);
```

**实现要点**：
1. 校验商品状态为 `ON_SALE`。
2. 若 `skuId != null`，调用 `productSkuService.getByIdEnabled(skuId)` 校验 SKU 存在且启用，且 SKU 的 `productId` 与传入 `productId` 一致；取 SKU 价格 / 库存用于后续展示。
3. 若 `skuId == null`，按原逻辑使用 `t_product.original_price` / `stock`。
4. 查询购物车项时使用 `(userId, productId, skuId)` 三列匹配（`LambdaQueryWrapper.eq(Cart::getUserId).eq(Cart::getProductId).eq(Cart::getSkuId)`，注意 `skuId` 为 null 时 MyBatis-Plus `eq` 会查 `sku_id = null`，需用 `Objects.isNull` 判断改用 `.isNull`）。
5. 已存在则数量累加，否则新建购物车项并设置 `skuId`。
6. 同步递增 `t_product.cart_count`。

**`getCartList` 方法修改**：组装 `CartItemVO` 时，若购物车项 `skuId != null`，查询 SKU 并填充 `skuId` / `skuAttributes`（将 SKU 的 `attributes` JSON 转为可读字符串「颜色: 曜石黑 / 版本: 旗舰版」）/ `skuMainImage`，价格取 SKU 价格，库存取 SKU 库存，主图优先取 SKU 主图（空则取商品主图）。

#### 5.7.3 修改 `OrderService.java` / `OrderServiceImpl.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/OrderService.java` 与 `impl/OrderServiceImpl.java`

**`createNormalOrder` 方法签名修改**：

```java
NormalOrderDetailVO createNormalOrder(Long userId, Long productId, Long skuId,
                                      Integer quantity, Long addressId, String remark);
```

**实现要点**：
1. 若 `skuId != null`，校验 SKU 启用且库存 ≥ quantity，调用 `productSkuService.deductStock(skuId, quantity)` 扣减 SKU 库存（乐观锁，失败抛异常）；价格取 SKU 价格；SKU 属性快照由 SKU 的 `attributes` JSON 转可读字符串。
2. 若 `skuId == null`，按原逻辑扣减 `t_product.stock`，价格取 `t_product.original_price`，`skuAttributes` 设为 null。
3. 创建 `NormalOrderItem` 时设置 `skuId` 与 `skuAttributes`。
4. 同步递增 `t_product.sales_count`。

**`createOrderFromCart` 方法修改**：遍历待结算购物车项时，读取每项的 `skuId`，按上述逻辑扣减对应 SKU 库存（或商品库存），写入订单明细的 `skuId` / `skuAttributes`。

**`cancelNormalOrder` / `timeoutCancelNormalOrder` 方法修改**：回补库存时，若订单明细 `skuId != null`，调用 `productSkuService.restoreStock(skuId, quantity)` 回补 SKU 库存；否则按原逻辑回补 `t_product.stock`。

#### 5.7.4 修改 `ProductReviewService.java` / `ProductReviewServiceImpl.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/service/ProductReviewService.java` 与 `impl/ProductReviewServiceImpl.java`

**`create` 方法签名修改**：

```java
ProductReviewVO create(Long userId, Long productId, Long skuId,
                       String content, Integer rating, String images);
```

**实现要点**：
1. 若 `skuId != null`，查询 SKU 并将 `attributes` JSON 转可读字符串作为 `skuAttributes` 快照。
2. 创建 `ProductReview` 实体时设置 `skuId` 与 `skuAttributes`。

**`listByProductId` 方法修改**：组装 `ProductReviewVO` 时填充 `skuId` / `skuAttributes`（直接从实体读取，无需再查 SKU 表，因为已快照）。

### 5.8 修改 DTO

#### 5.8.1 修改 `ProductCreateRequest.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/dto/ProductCreateRequest.java`

新增字段：

```java
/** 商品属性列表（可选，无规格时为 null） */
@Valid
private List<ProductAttributeDTO> attributes;

/** 商品 SKU 列表（可选，无规格时为 null） */
@Valid
private List<ProductSkuDTO> skus;
```

同时将 `originalPrice` 与 `stock` 的 `@NotNull` 改为可选（有 SKU 时可由 SKU 数据推导），但保留校验：若无 SKU 则仍需 `originalPrice` / `stock`。该条件校验在 Service 层判断。

#### 5.8.2 修改 `ProductUpdateRequest.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/dto/ProductUpdateRequest.java`

同 `ProductCreateRequest`，新增 `attributes` / `skus` 字段。

#### 5.8.3 修改 `BuyNowRequest.java`

文件：`seckill-mall/src/main/java/com/seckill/mall/dto/BuyNowRequest.java`

新增字段：

```java
/** SKU ID（可选，null 表示无规格商品立即购买） */
private Long skuId;
```

### 5.9 后端改动汇总

| 改动类型 | 文件数 | 关键改动 |
| --- | --- | --- |
| 新增实体 | 6 | `CategoryAttribute` / `CategoryAttributeValue` / `ProductAttribute` / `ProductAttributeValue` / `ProductSku` / `AttributeType` + `AttributeInputType` |
| 修改实体 | 3 | `Cart` / `NormalOrderItem` / `ProductReview` 加 SKU 字段 |
| 新增 Mapper | 5 | `CategoryAttributeMapper` / `CategoryAttributeValueMapper` / `ProductAttributeMapper` / `ProductAttributeValueMapper` / `ProductSkuMapper` + XML |
| 新增 Service | 6 | `CategoryAttributeService` + Impl / `CategoryAttributeValueService` + Impl / `ProductAttributeService` + Impl / `ProductSkuService` + Impl |
| 修改 Service | 4 | `ProductService` / `CartService` / `OrderService` / `ProductReviewService` + 各自 Impl |
| 新增 Controller | 2 | `CategoryAttributeController`（分类规格模板后台管理） / `ProductSkuController` |
| 修改 Controller | 3 | `ProductController` / `CartController` / `ProductReviewController` |
| 新增 DTO | 3 | `CategoryAttributeDTO` / `ProductAttributeDTO` / `ProductSkuDTO` |
| 修改 DTO | 3 | `ProductCreateRequest` / `ProductUpdateRequest` / `BuyNowRequest` |
| 新增 VO | 3 | `CategoryAttributeVO` / `ProductAttributeVO` / `ProductSkuVO` |
| 修改 VO | 3 | `ProductVO` / `CartItemVO` / `ProductReviewVO` |
| 新增 SQL | 1 | `migration_v7_sku.sql`（含分类规格模板表 + SKU 表 + ALTER） |

---

## 6. 前端后台改动方案

### 6.0 分类规格模板管理

本节为分类规格模板机制新增的前端后台代码，独立于商品编辑流程，但被 `ProductEdit.vue` 选择分类时调用以自动带出模板属性。

#### 6.0.1 新增管理页面 `CategoryAttributeManage.vue`

文件：`frontend/src/views/admin/CategoryAttributeManage.vue`

页面布局：
- 顶部：分类选择器（`el-select` 树形选择 `t_category`），选择分类后调用 `getCategoryAttributes(categoryId)` 加载该分类的规格属性列表。
- 中部：属性列表，每个属性一张卡片：
  - 属性名输入框（`el-input`）
  - 类型下拉（IMAGE / TEXT）
  - 录入方式下拉（SELECT / INPUT）
  - 是否必选开关（`el-switch`）
  - 排序输入框（`el-input-number`）
  - 预设值列表（`inputType=SELECT` 时显示）：`el-tag` 可增删，图片型属性值附带色块上传
  - 操作按钮：保存属性 / 删除属性
- 底部：「新增属性」按钮，调用 `createCategoryAttribute`。

页面骨架示例：

```vue
<template>
  <div class="category-attribute-manage">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分类规格模板管理</span>
          <el-select v-model="selectedCategoryId" placeholder="请选择分类" @change="loadAttributes">
            <el-option v-for="c in categoryList" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </div>
      </template>

      <div v-if="selectedCategoryId">
        <div v-for="attr in attributeList" :key="attr.id" class="attribute-card">
          <el-input v-model="attr.name" placeholder="属性名（如：颜色）" style="width: 160px" />
          <el-select v-model="attr.type" style="width: 110px">
            <el-option label="图片型" value="IMAGE" />
            <el-option label="文字型" value="TEXT" />
          </el-select>
          <el-select v-model="attr.inputType" style="width: 130px">
            <el-option label="预设值选择" value="SELECT" />
            <el-option label="自由输入" value="INPUT" />
          </el-select>
          <el-switch v-model="attr.isRequired" :active-value="1" :inactive-value="0" active-text="必选" />
          <el-input-number v-model="attr.sortOrder" :min="0" size="small" />

          <div v-if="attr.inputType === 'SELECT'" class="preset-values">
            <el-tag v-for="(v, i) in attr.values" :key="i" closable @close="attr.values.splice(i, 1)">
              {{ v.value }}
            </el-tag>
            <el-button size="small" @click="addPresetValue(attr)">+ 添加预设值</el-button>
          </div>

          <el-button type="primary" size="small" @click="saveAttribute(attr)">保存</el-button>
          <el-button type="danger" plain size="small" @click="deleteAttribute(attr)">删除</el-button>
        </div>

        <el-button type="primary" plain @click="addNewAttribute">+ 新增属性</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getCategoryAttributes,
  createCategoryAttribute,
  updateCategoryAttribute,
  deleteCategoryAttribute
} from '@/api/categoryAttribute'
import type { CategoryAttribute } from '@/types'

const selectedCategoryId = ref<number | undefined>(undefined)
const attributeList = ref<CategoryAttribute[]>([])

async function loadAttributes(): Promise<void> {
  if (!selectedCategoryId.value) return
  const res = await getCategoryAttributes(selectedCategoryId.value)
  attributeList.value = res.data || []
}

async function saveAttribute(attr: CategoryAttribute): Promise<void> {
  if (attr.id) {
    await updateCategoryAttribute(attr.id, attr)
  } else {
    attr.categoryId = selectedCategoryId.value!
    const res = await createCategoryAttribute(attr)
    attr.id = res.data.id
  }
  ElMessage.success('保存成功')
}

async function deleteAttribute(attr: CategoryAttribute): Promise<void> {
  await deleteCategoryAttribute(attr.id!)
  await loadAttributes()
  ElMessage.success('删除成功')
}

function addNewAttribute(): void {
  attributeList.value.push({
    categoryId: selectedCategoryId.value!,
    name: '',
    type: 'TEXT',
    inputType: 'SELECT',
    isRequired: 0,
    sortOrder: attributeList.value.length,
    values: []
  })
}

function addPresetValue(attr: CategoryAttribute): void {
  const val = prompt('请输入预设值')
  if (val) {
    attr.values.push({ value: val, imageUrl: attr.type === 'IMAGE' ? '' : undefined, sortOrder: attr.values.length })
  }
}
</script>
```

#### 6.0.2 新增路由配置

在 `frontend/src/router/index.ts`（如存在）后台路由组中新增：

```ts
{
  path: '/admin/category-attribute',
  name: 'CategoryAttributeManage',
  component: () => import('@/views/admin/CategoryAttributeManage.vue'),
  meta: { title: '分类规格模板', requiresAuth: true, role: 'admin' }
}
```

#### 6.0.3 新增 API `categoryAttribute.ts`

文件：`frontend/src/api/categoryAttribute.ts`

```ts
/**
 * 分类规格模板 API - 对接后端 /api/v1/admin/category
 */
import { get, post, put, del } from './request'
import type { Result, CategoryAttribute, CategoryAttributeValue } from '@/types'

/** 获取分类的规格模板（含预设值） */
export function getCategoryAttributes(categoryId: number | string): Promise<Result<CategoryAttribute[]>> {
  return get<CategoryAttribute[]>(`/api/v1/admin/category/${categoryId}/attributes`)
}

/** 创建分类规格属性 */
export function createCategoryAttribute(data: CategoryAttribute): Promise<Result<CategoryAttribute>> {
  return post<CategoryAttribute>('/api/v1/admin/category/attributes', data)
}

/** 更新分类规格属性 */
export function updateCategoryAttribute(id: number | string, data: CategoryAttribute): Promise<Result<CategoryAttribute>> {
  return put<CategoryAttribute>(`/api/v1/admin/category/attributes/${id}`, data)
}

/** 删除分类规格属性 */
export function deleteCategoryAttribute(id: number | string): Promise<Result<void>> {
  return del<void>(`/api/v1/admin/category/attributes/${id}`)
}

/** 为分类规格属性添加预设值 */
export function addCategoryAttributeValue(
  attributeId: number | string,
  data: CategoryAttributeValue
): Promise<Result<CategoryAttribute>> {
  return post<CategoryAttribute>(`/api/v1/admin/category/attributes/${attributeId}/values`, data)
}
```

#### 6.0.4 新增类型定义

文件：`frontend/src/types/index.ts` 末尾新增：

```ts
/* ==================== 分类规格模板类型 ==================== */

/** 录入方式 */
export type AttributeInputType = 'SELECT' | 'INPUT'

/** 分类属性模板视图对象 */
export interface CategoryAttribute {
  id?: number | string
  categoryId: number | string
  name: string
  type: AttributeType
  inputType: AttributeInputType
  isRequired: number  // 0-否 / 1-是
  sortOrder: number
  values: CategoryAttributeValue[]
}

/** 分类属性预设值 */
export interface CategoryAttributeValue {
  id?: number | string
  attributeId?: number | string
  value: string
  imageUrl?: string | null
  sortOrder: number
}
```

#### 6.0.5 修改 `ProductEdit.vue` 选择分类自动带出模板

**template 修改**：规格区域改为「模板驱动 + 自定义扩展」模式，每个属性行显示「模板属性」/「自定义」标签：

```vue
<el-form-item label="商品规格" class="form-item-full">
  <div class="sku-attribute-area">
    <el-alert v-if="templateAttributes.length > 0" type="info" :closable="false"
              title="以下属性来自分类规格模板，可在此基础上增删" />
    <el-button type="primary" plain size="small" @click="addAttribute">+ 添加自定义属性</el-button>
    <div v-for="(attr, idx) in formData.attributes" :key="idx" class="attribute-row">
      <el-tag v-if="attr.categoryAttributeId" type="success" size="small">模板</el-tag>
      <el-tag v-else type="info" size="small">自定义</el-tag>
      <el-input v-model="attr.name" placeholder="属性名（如：颜色）" style="width: 120px" />
      <!-- 其余属性配置同 v1.0 -->
    </div>
  </div>
</el-form-item>
```

**script 修改**：watch 分类变化，调用 `getCategoryAttributes` 加载模板并自动填充到 `formData.attributes`：

```ts
import { watch } from 'vue'
import { getCategoryAttributes } from '@/api/categoryAttribute'

const templateAttributes = ref<CategoryAttribute[]>([])

/* === 选择分类后自动带出规格模板 === */
watch(() => formData.categoryId, async (newCategoryId) => {
  if (!newCategoryId) {
    templateAttributes.value = []
    return
  }
  try {
    const res = await getCategoryAttributes(newCategoryId as number)
    templateAttributes.value = res.data || []
    // 仅在 attributes 为空（首次选择分类）或用户确认覆盖时自动带出
    if (formData.attributes.length === 0 && templateAttributes.value.length > 0) {
      formData.attributes = templateAttributes.value.map(t => ({
        categoryAttributeId: t.id,
        name: t.name,
        type: t.type,
        sortOrder: t.sortOrder,
        values: t.values.map(v => ({
          value: v.value,
          imageUrl: v.imageUrl,
          sortOrder: v.sortOrder
        })),
        valueInputVisible: false,
        valueInput: ''
      }))
      ElMessage.info(`已带出分类模板 ${templateAttributes.value.length} 个属性`)
    }
  } catch (e) {
    templateAttributes.value = []
  }
})
```

**修改 `addAttribute`**：新增的属性默认为「自定义属性」（`categoryAttributeId = undefined`）：

```ts
function addAttribute(): void {
  formData.attributes.push({
    categoryAttributeId: undefined,  // 自定义属性
    name: '',
    type: 'TEXT',
    sortOrder: formData.attributes.length,
    values: [],
    valueInputVisible: false,
    valueInput: ''
  })
}
```

**修改 `handleSubmit`**：提交 `payload` 时携带 `categoryAttributeId`：

```ts
attributes: formData.attributes.length > 0 ? formData.attributes.map(a => ({
  categoryAttributeId: a.categoryAttributeId || null,  // 新增：关联模板
  name: a.name,
  type: a.type,
  sortOrder: a.sortOrder,
  values: a.values.map(v => ({ value: v.value, imageUrl: v.imageUrl, sortOrder: v.sortOrder }))
})) : undefined
```

### 6.1 `ProductEdit.vue` 改造

文件：`frontend/src/views/admin/ProductEdit.vue`

#### 6.1.1 template 修改

在「库存」`el-form-item`（36-40 行）之后、「状态」`el-form-item`（42-47 行）之前，新增「商品规格」区域。同时在「商品详情」富文本编辑器（70-76 行）之前新增「SKU 组合」表格。

**新增「商品规格」区域**（插入到左列 form-col-left 中库存之后）：

```vue
<el-form-item label="商品规格" class="form-item-full">
  <div class="sku-attribute-area">
    <el-button type="primary" plain size="small" @click="addAttribute">
      + 添加属性
    </el-button>
    <div v-for="(attr, idx) in formData.attributes" :key="idx" class="attribute-row">
      <el-input v-model="attr.name" placeholder="属性名（如：颜色）" style="width: 120px" />
      <el-select v-model="attr.type" placeholder="类型" style="width: 100px">
        <el-option label="图片型" value="IMAGE" />
        <el-option label="文字型" value="TEXT" />
      </el-select>
      <div class="attribute-values">
        <el-tag v-for="(val, vIdx) in attr.values" :key="vIdx" closable @close="removeAttrValue(idx, vIdx)">
          {{ val.value }}
        </el-tag>
        <el-input v-if="attr.valueInputVisible" v-model="attr.valueInput" size="small" style="width: 100px"
          @keyup.enter="confirmAttrValue(idx)" @blur="confirmAttrValue(idx)" />
        <el-button v-else size="small" @click="showAttrValueInput(idx)">+ 添加值</el-button>
      </div>
      <el-button type="danger" plain size="small" @click="removeAttribute(idx)">删除属性</el-button>
    </div>
    <el-button v-if="formData.attributes.length > 0" type="success" plain size="small" @click="generateSkus">
      生成 SKU 组合
    </el-button>
  </div>
</el-form-item>
```

**新增「SKU 组合」表格**（插入到富文本编辑器之前）：

```vue
<el-form-item v-if="formData.skus.length > 0" label="SKU组合" class="form-item-full">
  <el-table :data="formData.skus" border style="width: 100%">
    <el-table-column label="组合" min-width="200">
      <template #default="{ row }">{{ formatSkuAttributes(row.attributes) }}</template>
    </el-table-column>
    <el-table-column label="价格" width="120">
      <template #default="{ row }">
        <el-input-number v-model="row.price" :min="0.01" :precision="2" :controls="false" size="small" />
      </template>
    </el-table-column>
    <el-table-column label="库存" width="100">
      <template #default="{ row }">
        <el-input-number v-model="row.stock" :min="0" :controls="false" size="small" />
      </template>
    </el-table-column>
    <el-table-column label="主图" width="120">
      <template #default="{ row }">
        <ImageUploader v-model="row.mainImageList" :max-count="1" />
      </template>
    </el-table-column>
    <el-table-column label="编码" width="140">
      <template #default="{ row }">
        <el-input v-model="row.skuCode" size="small" placeholder="自动生成" />
      </template>
    </el-table-column>
    <el-table-column label="状态" width="100">
      <template #default="{ row }">
        <el-switch v-model="row.status" :active-value="1" :inactive-value="0" />
      </template>
    </el-table-column>
    <el-table-column label="操作" width="80">
      <template #default="{ $index }">
        <el-button type="danger" plain size="small" @click="removeSku($index)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</el-form-item>
```

#### 6.1.2 script 修改

**扩展 `ProductFormData` 接口**（131-140 行）：

```ts
interface ProductFormData {
  productName: string
  categoryId: number | string | undefined
  originalPrice: number
  stock: number
  description: string
  detailHtml: string
  images: string[]
  status: ProductStatus
  // 新增 SKU 相关字段
  attributes: ProductAttributeDTO[]
  skus: ProductSkuDTO[]
}
```

**初始化 `formData`**（142-151 行）新增：

```ts
attributes: [],
skus: []
```

**新增方法**：

```ts
/* === 添加属性 === */
function addAttribute(): void {
  formData.attributes.push({
    name: '',
    type: 'TEXT',
    sortOrder: formData.attributes.length,
    values: [],
    valueInputVisible: false,
    valueInput: ''
  })
}

/* === 删除属性 === */
function removeAttribute(idx: number): void {
  formData.attributes.splice(idx, 1)
}

/* === 显示属性值输入框 === */
function showAttrValueInput(idx: number): void {
  formData.attributes[idx].valueInputVisible = true
  formData.attributes[idx].valueInput = ''
}

/* === 确认添加属性值 === */
function confirmAttrValue(idx: number): void {
  const attr = formData.attributes[idx]
  const val = attr.valueInput.trim()
  if (val && !attr.values.some(v => v.value === val)) {
    attr.values.push({
      value: val,
      imageUrl: attr.type === 'IMAGE' ? '' : undefined,
      sortOrder: attr.values.length
    })
  }
  attr.valueInputVisible = false
  attr.valueInput = ''
}

/* === 删除属性值 === */
function removeAttrValue(idx: number, vIdx: number): void {
  formData.attributes[idx].values.splice(vIdx, 1)
}

/* === 笛卡尔积生成 SKU 组合 === */
function generateSkus(): void {
  if (formData.attributes.length === 0) {
    ElMessage.warning('请先添加属性')
    return
  }
  for (const attr of formData.attributes) {
    if (!attr.name || attr.values.length === 0) {
      ElMessage.warning('属性名和属性值不能为空')
      return
    }
  }
  // 笛卡尔积
  const valueLists = formData.attributes.map(a => a.values)
  const combinations = cartesianProduct(valueLists)
  formData.skus = combinations.map(combo => {
    const attrMap: Record<string, string> = {}
    formData.attributes.forEach((a, i) => {
      attrMap[a.name] = combo[i].value
    })
    return {
      skuCode: '',
      price: formData.originalPrice,
      stock: 0,
      mainImage: '',
      mainImageList: [],
      attributes: JSON.stringify(attrMap),
      status: 1
    }
  })
  ElMessage.success(`已生成 ${combinations.length} 个 SKU 组合`)
}

/* === 笛卡尔积工具函数 === */
function cartesianProduct<T>(arrays: T[][]): T[][] {
  if (arrays.length === 0) return [[]]
  const [first, ...rest] = arrays
  const restProduct = cartesianProduct(rest)
  return first.flatMap(x => restProduct.map(r => [x, ...r]))
}

/* === 格式化 SKU 属性为可读字符串 === */
function formatSkuAttributes(attributesJson: string): string {
  try {
    const obj = JSON.parse(attributesJson)
    return Object.entries(obj).map(([k, v]) => `${k}: ${v}`).join(' / ')
  } catch {
    return attributesJson
  }
}

/* === 删除 SKU 行 === */
function removeSku(idx: number): void {
  formData.skus.splice(idx, 1)
}
```

**修改 `handleSubmit`**（292-334 行）：提交 `payload` 时携带 `attributes` / `skus`：

```ts
const payload = {
  productName: formData.productName,
  categoryId: formData.categoryId as number,
  originalPrice: formData.originalPrice,
  stock: formData.stock,
  description: formData.description,
  detailHtml: formData.detailHtml,
  images: mergedImages,
  status: formData.status,
  // 新增：仅当有属性时才提交 SKU 数据
  attributes: formData.attributes.length > 0 ? formData.attributes.map(a => ({
    name: a.name,
    type: a.type,
    sortOrder: a.sortOrder,
    values: a.values.map(v => ({ value: v.value, imageUrl: v.imageUrl, sortOrder: v.sortOrder }))
  })) : undefined,
  skus: formData.skus.length > 0 ? formData.skus.map(s => ({
    skuCode: s.skuCode,
    price: s.price,
    stock: s.stock,
    mainImage: s.mainImageList && s.mainImageList.length > 0 ? s.mainImageList[0] : null,
    attributes: s.attributes,
    status: s.status
  })) : undefined
}
```

**修改 `fetchProductDetail`**（246-274 行）：加载商品时回填 `attributes` / `skus`：

```ts
formData.attributes = p.attributes ? p.attributes.map(a => ({
  ...a,
  values: a.values.map(v => ({ ...v })),
  valueInputVisible: false,
  valueInput: ''
})) : []
formData.skus = p.skus ? p.skus.map(s => ({
  ...s,
  mainImageList: s.mainImage ? [s.mainImage] : []
})) : []
```

#### 6.1.3 style 修改

新增样式（追加到 `<style scoped>` 末尾）：

```css
/* === 商品规格区域 === */
.sku-attribute-area {
  width: 100%;
}

.attribute-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.attribute-values {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}
```

### 6.2 新增 API `sku.ts`

文件：`frontend/src/api/sku.ts`

```ts
/**
 * 商品 SKU API - 对接后端 /api/v1/products/{productId}/skus
 */
import { get } from './request'
import type { Result, ProductSkuVO } from '@/types'

/** 查询商品所有启用 SKU（前台商品详情页用） */
export function getProductSkus(productId: number | string): Promise<Result<ProductSkuVO[]>> {
  return get<ProductSkuVO[]>(`/api/v1/products/${productId}/skus`)
}
```

### 6.3 新增类型定义

文件：`frontend/src/types/index.ts`

在文件末尾新增 SKU 相关接口，并扩展现有接口：

```ts
/* ==================== SKU 类型 ==================== */

/** 属性类型 */
export type AttributeType = 'IMAGE' | 'TEXT'

/** 商品属性视图对象 */
export interface ProductAttributeVO {
  id: number | string
  /** 关联分类属性模板 ID，null 表示商品自定义属性 */
  categoryAttributeId?: number | string | null
  name: string
  type: AttributeType
  sortOrder: number
  values: ProductAttributeValueVO[]
}

/** 商品属性值视图对象 */
export interface ProductAttributeValueVO {
  id: number | string
  value: string
  imageUrl: string | null
  sortOrder: number
}

/** 商品 SKU 视图对象 */
export interface ProductSkuVO {
  id: number | string
  skuCode: string
  price: number
  stock: number
  mainImage: string | null
  /** SKU 属性键值对 JSON 字符串 */
  attributes: string
  /** 状态：1-启用 / 0-禁用 */
  status: number
}

/** 商品属性 DTO（创建 / 编辑商品时提交） */
export interface ProductAttributeDTO {
  id?: number | string
  /** 关联分类属性模板 ID，null/undefined 表示商品自定义属性 */
  categoryAttributeId?: number | string | null
  name: string
  type: AttributeType
  sortOrder: number
  values: ProductAttributeValueDTO[]
  /** 前端临时字段：属性值输入框可见性 */
  valueInputVisible?: boolean
  /** 前端临时字段：属性值输入框内容 */
  valueInput?: string
}

/** 商品属性值 DTO */
export interface ProductAttributeValueDTO {
  id?: number | string
  value: string
  imageUrl?: string
  sortOrder: number
}

/** 商品 SKU DTO（创建 / 编辑商品时提交） */
export interface ProductSkuDTO {
  id?: number | string
  skuCode: string
  price: number
  stock: number
  mainImage: string | null
  /** 前端临时字段：主图列表（ImageUploader 用） */
  mainImageList?: string[]
  /** SKU 属性键值对 JSON 字符串 */
  attributes: string
  status: number
}
```

**扩展 `ProductVO`**（74-88 行）：

```ts
export interface ProductVO {
  id: number | string
  productName: string
  categoryId: number | string
  categoryName: string
  description: string
  detailHtml?: string
  originalPrice: number
  images: string[]
  stock: number
  salesCount: number
  status: ProductStatus
  createTime: string
  // 新增 SKU 相关字段
  attributes?: ProductAttributeVO[]
  skus?: ProductSkuVO[]
  hasSku?: boolean
  minPrice?: number
  maxPrice?: number
  totalStock?: number
  /** 分类规格模板（供前端区分模板属性 / 自定义属性） */
  categoryAttributes?: CategoryAttribute[]
}
```

**扩展 `CartItemVO`**（155-169 行）：

```ts
export interface CartItemVO {
  id: number | string
  productId: number | string
  quantity: number
  selected: boolean
  productName: string
  mainImage: string
  originalPrice: number
  stock: number
  productStatus: ProductStatus
  subtotal: number
  // 新增 SKU 相关字段
  skuId?: number | string | null
  skuAttributes?: string
  skuMainImage?: string | null
}
```

**扩展 `CartAddRequest`**（172-175 行）：

```ts
export interface CartAddRequest {
  productId: number | string
  skuId?: number | string | null
  quantity: number
}
```

**扩展 `ProductCreateRequest` / `ProductUpdateRequest`**（490-513 行）：

```ts
export interface ProductCreateRequest {
  productName: string
  categoryId: number | string
  originalPrice: number
  stock: number
  description?: string
  detailHtml?: string
  images?: string[]
  status?: ProductStatus
  // 新增
  attributes?: ProductAttributeDTO[]
  skus?: ProductSkuDTO[]
}

export interface ProductUpdateRequest {
  productName?: string
  categoryId?: number
  originalPrice?: number
  stock?: number
  description?: string
  detailHtml?: string
  images?: string[]
  status?: ProductStatus
  // 新增
  attributes?: ProductAttributeDTO[]
  skus?: ProductSkuDTO[]
}
```

**扩展 `ProductReviewVO`**（107-126 行）：

```ts
export interface ProductReviewVO {
  id: number | string
  productId: number | string
  userId: number | string
  userName: string
  orderId: number | string | null
  content: string
  rating: number
  images: string[]
  status: number
  replyContent: string | null
  replyTime: string | null
  createTime: string
  // 新增
  skuId?: number | string | null
  skuAttributes?: string
}
```

**扩展 `ReviewCreateRequest`**（129-135 行）：

```ts
export interface ReviewCreateRequest {
  productId: number | string
  skuId?: number | string | null
  content: string
  rating: number
  images?: string
}
```

**扩展 `BuyNowRequest`**（如存在）：

```ts
export interface BuyNowRequest {
  productId: number | string
  skuId?: number | string | null
  quantity: number
  addressId: number | string
  remark?: string
}
```

---

## 7. 前端前台改动方案

### 7.1 `ProductDetail.vue` 改造

文件：`frontend/src/views/front/ProductDetail.vue`

#### 7.1.1 SKU 选择器对接后端

**当前现状**：SKU 选择器（颜色 / 版本 / 表带）只是静态 HTML，未对接后端。

**改造点**：

1. **加载 SKU 数据**：`onMounted` 中调用 `getProductDetail(id)` 后，从返回的 `ProductVO.attributes` 与 `ProductVO.skus` 获取 SKU 数据，存入响应式变量：

```ts
import { getProductSkus } from '@/api/sku'

const attributes = ref<ProductAttributeVO[]>([])
const skus = ref<ProductSkuVO[]>([])
const hasSku = ref<boolean>(false)
const selectedAttributes = reactive<Record<string, string>>({})  // 用户选择的属性键值对
const currentSku = ref<ProductSkuVO | null>(null)  // 当前匹配的 SKU

async function loadProductDetail(id: string | number) {
  const res = await getProductDetail(id)
  const p = res.data
  // ...原有逻辑...
  attributes.value = p.attributes || []
  skus.value = p.skus || []
  hasSku.value = p.hasSku || false
  // 初始化选择：默认选第一个属性值
  attributes.value.forEach(attr => {
    if (attr.values.length > 0) {
      selectedAttributes[attr.name] = attr.values[0].value
    }
  })
  updateCurrentSku()
}
```

2. **渲染 SKU 选择器**：替换原静态 HTML，使用 `v-for` 渲染属性组：

```vue
<div class="sku" v-if="hasSku">
  <div class="sku__group" v-for="attr in attributes" :key="attr.id">
    <div class="sku__label">
      {{ attr.name }}
      <span class="selected">{{ selectedAttributes[attr.name] || '请选择' }}</span>
    </div>
    <div class="sku__values">
      <!-- 图片型：色块 -->
      <template v-if="attr.type === 'IMAGE'">
        <div v-for="val in attr.values" :key="val.id"
             class="sku-color"
             :class="{ active: selectedAttributes[attr.name] === val.value, disabled: isAttributeValueDisabled(attr.name, val.value) }"
             :style="{ background: val.imageUrl }"
             :title="val.value"
             @click="selectAttributeValue(attr.name, val.value)" />
      </template>
      <!-- 文字型：按钮 -->
      <template v-else>
        <button v-for="val in attr.values" :key="val.id"
                class="sku-text"
                :class="{ active: selectedAttributes[attr.name] === val.value, disabled: isAttributeValueDisabled(attr.name, val.value) }"
                :disabled="isAttributeValueDisabled(attr.name, val.value)"
                @click="selectAttributeValue(attr.name, val.value)">
          {{ val.value }}
        </button>
      </template>
    </div>
  </div>
</div>
```

3. **选择属性值**：

```ts
function selectAttributeValue(attrName: string, value: string): void {
  if (isAttributeValueDisabled(attrName, value)) return
  selectedAttributes[attrName] = value
  updateCurrentSku()
}

/* === 判断属性值是否禁用（无对应 SKU 或 SKU 库存为 0） === */
function isAttributeValueDisabled(attrName: string, value: string): boolean {
  // 临时选中该值，检查是否存在启用的 SKU 且库存 > 0
  const tempSelected = { ...selectedAttributes, [attrName]: value }
  const matched = skus.value.filter(sku => {
    if (sku.status !== 1) return false
    try {
      const attrs = JSON.parse(sku.attributes)
      return Object.keys(tempSelected).every(k => attrs[k] === tempSelected[k])
    } catch {
      return false
    }
  })
  return matched.length === 0 || matched.every(s => s.stock === 0)
}

/* === 更新当前匹配的 SKU === */
function updateCurrentSku(): void {
  // 检查是否所有属性都已选择
  const allSelected = attributes.value.every(a => selectedAttributes[a.name])
  if (!allSelected) {
    currentSku.value = null
    return
  }
  // 查找完全匹配的 SKU
  const matched = skus.value.find(sku => {
    if (sku.status !== 1) return false
    try {
      const attrs = JSON.parse(sku.attributes)
      return Object.keys(selectedAttributes).every(k => attrs[k] === selectedAttributes[k])
    } catch {
      return false
    }
  })
  currentSku.value = matched || null
}
```

4. **价格 / 库存 / 主图联动**：

```ts
import { computed } from 'vue'

/* === 当前展示价格 === */
const displayPrice = computed(() => {
  if (!hasSku.value) return product.value?.originalPrice || 0
  if (currentSku.value) return currentSku.value.price
  // 未完整选择时显示区间
  if (product.value?.minPrice && product.value?.maxPrice) {
    return product.value.minPrice === product.value.maxPrice
      ? product.value.minPrice
      : `${product.value.minPrice} - ${product.value.maxPrice}`
  }
  return 0
})

/* === 当前展示库存 === */
const displayStock = computed(() => {
  if (!hasSku.value) return product.value?.stock || 0
  if (currentSku.value) return currentSku.value.stock
  return product.value?.totalStock || 0
})

/* === 当前展示主图 === */
const displayMainImage = computed(() => {
  if (currentSku.value?.mainImage) return currentSku.value.mainImage
  return product.value?.images?.[0] || ''
})

/* === 是否可加购 / 购买 === */
const canAddToCart = computed(() => {
  if (hasSku.value && !currentSku.value) return false
  if (currentSku.value && currentSku.value.stock === 0) return false
  return true
})
```

在模板中将原静态价格 / 库存 / 主图替换为 `displayPrice` / `displayStock` / `displayMainImage`，加购 / 购买按钮的 `:disabled` 绑定 `!canAddToCart`。

#### 7.1.2 加入购物车 / 立即购买携带 `skuId`

**修改 `addToCart` 调用**：

```ts
import { addCart } from '@/api/cart'

async function handleAddToCart(): Promise<void> {
  if (!canAddToCart.value) {
    ElMessage.warning(hasSku.value ? '请选择完整规格' : '商品不可加购')
    return
  }
  const payload: CartAddRequest = {
    productId: product.value!.id,
    skuId: currentSku.value?.id || null,
    quantity: quantity.value
  }
  await addCart(payload)
  ElMessage.success('已加入购物车')
}
```

**修改 `handleBuyNow` 调用**：

```ts
async function handleBuyNow(): Promise<void> {
  if (!canAddToCart.value) {
    ElMessage.warning(hasSku.value ? '请选择完整规格' : '商品不可购买')
    return
  }
  const payload: BuyNowRequest = {
    productId: product.value!.id,
    skuId: currentSku.value?.id || null,
    quantity: quantity.value,
    addressId: selectedAddress.value,
    remark: remark.value
  }
  await buyNow(payload)
  // 跳转订单页...
}
```

#### 7.1.3 数量选择器上限

将数量选择器的 `:max` 绑定到 `displayStock`：

```vue
<el-input-number v-model="quantity" :min="1" :max="displayStock" />
```

### 7.2 购物车改造

文件：`frontend/src/views/front/Cart.vue`（或购物车组件）

**展示 SKU 信息**：在购物车项的商品名称下方展示 `skuAttributes`：

```vue
<div class="cart-item">
  <img :src="item.skuMainImage || item.mainImage" class="cart-item__image" />
  <div class="cart-item__info">
    <div class="cart-item__name">{{ item.productName }}</div>
    <div v-if="item.skuAttributes" class="cart-item__sku">{{ item.skuAttributes }}</div>
    <div class="cart-item__price">¥{{ item.originalPrice }}</div>
  </div>
</div>
```

样式参考设计稿 1415 行 `.review-item__sku`：

```css
.cart-item__sku {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}
```

### 7.3 订单改造

文件：`frontend/src/views/front/OrderDetail.vue`（或订单详情组件）

**展示订单项 SKU 信息**：在订单明细的商品名称下方展示 `skuAttributes`：

```vue
<div v-for="item in order.items" :key="item.id" class="order-item">
  <img :src="item.productImage" class="order-item__image" />
  <div class="order-item__info">
    <div class="order-item__name">{{ item.productName }}</div>
    <div v-if="item.skuAttributes" class="order-item__sku">{{ item.skuAttributes }}</div>
  </div>
</div>
```

由于 `NormalOrderDetailVO` 当前直接暴露 `NormalOrderItem` 实体，新增的 `skuId` / `skuAttributes` 字段会自动序列化到前端，无需改 VO。

### 7.4 评论改造

#### 7.4.1 评论展示

文件：`frontend/src/views/front/ProductDetail.vue` 评论列表区域 或独立评论组件

**展示评论 SKU 信息**：在评论内容上方展示 `skuAttributes`（参考设计稿 2652 行）：

```vue
<div v-for="review in reviews" :key="review.id" class="review-item">
  <div class="review-item__header">
    <span class="review-item__user">{{ review.userName }}</span>
    <span class="review-item__rating">★★★★★</span>
  </div>
  <div v-if="review.skuAttributes" class="review-item__sku">{{ review.skuAttributes }}</div>
  <div class="review-item__content">{{ review.content }}</div>
</div>
```

样式参考设计稿 1415 行：

```css
.review-item__sku {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin: 4px 0;
}
```

#### 7.4.2 评论发表

文件：`frontend/src/api/review.ts`

修改 `createReview` 调用，携带 `skuId`：

```ts
export function createReview(data: ReviewCreateRequest): Promise<Result<ProductReviewVO>> {
  return post<ProductReviewVO>('/api/v1/reviews/create', data)
}
```

`ReviewCreateRequest` 已在 6.3 节扩展 `skuId` 字段。

在订单详情页「发表评论」入口处，从订单项的 `skuId` 自动带入：

```ts
async function submitReview(orderItem: NormalOrderItem): Promise<void> {
  const payload: ReviewCreateRequest = {
    productId: orderItem.productId,
    skuId: orderItem.skuId || null,
    content: reviewContent.value,
    rating: reviewRating.value,
    images: reviewImages.value
  }
  await createReview(payload)
  ElMessage.success('评论成功')
}
```

---

## 8. 实施计划

### 8.1 阶段划分

> **前置依赖说明**：分类规格模板（阶段 1A-3A）需在商品 SKU 管理（阶段 4-12）之前完成，因为 `ProductEdit.vue` 选择分类时需调用分类规格模板接口自动带出属性。各阶段编号 A 表示分类规格模板相关阶段。

| 阶段 | 内容 | 预估工时 | 依赖 |
| --- | --- | --- | --- |
| **阶段 1A：数据库（分类规格模板）** | 在 `migration_v7_sku.sql` 中创建 `t_category_attribute` 和 `t_category_attribute_value` 表，并在 `t_product_attribute` 上新增 `category_attribute_id` 字段，测试环境执行验证 | 0.5 天 | 无 |
| **阶段 2A：后端实体 / Mapper（分类规格模板）** | 新增 `CategoryAttribute` / `CategoryAttributeValue` 实体 + `AttributeInputType` 枚举 + 2 个 Mapper，修改 `ProductAttribute` 实体加 `categoryAttributeId` | 0.5 天 | 阶段 1A |
| **阶段 3A：后端 Service / Controller / 前端管理页面（分类规格模板）** | 新增 `CategoryAttributeService` + Impl + `CategoryAttributeValueService` + Impl + `CategoryAttributeController` + DTO/VO；前端新增 `CategoryAttributeManage.vue` + `api/categoryAttribute.ts` + 路由 + 类型定义，联调模板 CRUD | 1.5 天 | 阶段 2A |
| **阶段 1：数据库（SKU）** | 编写 `migration_v7_sku.sql` 的 SKU 部分（`t_product_attribute_value` / `t_product_sku`），ALTER `t_cart` / `t_normal_order_item` / `t_product_review`，在测试环境执行，验证表结构与索引 | 0.5 天 | 阶段 1A |
| **阶段 2：后端实体 / Mapper** | 新增 3 个实体 + 1 个枚举 + 3 个 Mapper + XML，修改 3 个实体加 SKU 字段 | 0.5 天 | 阶段 1、阶段 2A |
| **阶段 3：后端 DTO / VO** | 新增 2 个 DTO + 2 个 VO，修改 3 个 DTO + 3 个 VO（含 `ProductAttributeDTO` / `ProductAttributeVO` 加 `categoryAttributeId`，`ProductVO` 加 `categoryAttributes`） | 0.5 天 | 阶段 2 |
| **阶段 4：后端 Service** | 新增 `ProductAttributeService` + Impl + `ProductSkuService` + Impl | 1 天 | 阶段 3 |
| **阶段 5：后端现有 Service 修改** | 修改 `ProductService`（含 `categoryAttributeId` 校验逻辑）/ `CartService` / `OrderService` / `ProductReviewService` 及 Impl | 1.5 天 | 阶段 4、阶段 3A |
| **阶段 6：后端 Controller** | 新增 `ProductSkuController`，修改 `ProductController`（详情返回 `categoryAttributes`）/ `CartController` / `ProductReviewController` | 0.5 天 | 阶段 5 |
| **阶段 7：后端联调** | Postman / Swagger 联调所有新接口，验证分类模板 CRUD + SKU 创建 / 详情 / 加购 / 下单 / 评论全链路 | 0.5 天 | 阶段 6 |
| **阶段 8：前端类型 / API** | 扩展 `types/index.ts`，新增 `api/sku.ts`，修改 `api/cart.ts` / `api/review.ts` | 0.5 天 | 阶段 7 |
| **阶段 9：前端后台** | 改造 `ProductEdit.vue` 增加规格管理 + SKU 表格 + 选择分类自动带出模板（watch `categoryId`），联调创建 / 编辑商品接口 | 1.5 天 | 阶段 8、阶段 3A |
| **阶段 10：前端前台商品详情** | 改造 `ProductDetail.vue` SKU 选择器对接后端，价格 / 库存 / 主图联动 | 1.5 天 | 阶段 8 |
| **阶段 11：前端前台购物车 / 订单 / 评论** | 改造购物车 / 订单详情 / 评论展示 SKU 信息，加购 / 购买 / 评论携带 `skuId` | 1 天 | 阶段 10 |
| **阶段 12：端到端测试** | 完整流程测试：分类模板配置 → 后台建商品自动带出模板 → 前台选 SKU 加购 → 购物车展示 → 下单 → 订单展示 → 评论 | 0.5 天 | 阶段 11 |
| **合计** | | **12 天** | |

### 8.2 风险与注意事项

#### 8.2.1 数据兼容性

- **风险**：现有商品无 SKU 数据，改造后需保证旧商品仍能正常展示 / 加购 / 下单。
- **应对**：
  - `t_cart.sku_id` / `t_normal_order_item.sku_id` / `t_product_review.sku_id` 均允许 NULL，旧数据无需迁移。
  - 前端 `ProductDetail.vue` 通过 `hasSku` 判断：无 SKU 时回退到原 `originalPrice` / `stock` 展示，加购 / 购买不传 `skuId`。
  - 后端 `CartServiceImpl` / `OrderServiceImpl` 中所有 SKU 相关逻辑均以 `if (skuId != null)` 守卫，null 时走原逻辑。

#### 8.2.2 购物车唯一约束变更

- **风险**：`t_cart` 唯一约束从 `(user_id, product_id)` 改为 `(user_id, product_id, sku_id)`，需保证 MyBatis-Plus 查询时正确处理 `sku_id = null` 的情况。
- **应对**：
  - MySQL 中 `null != null`，因此 `(user_id=1, product_id=1, sku_id=null)` 与 `(user_id=1, product_id=1, sku_id=null)` 不会被唯一约束拦截（NULL 不参与唯一性比较）。这意味着同一无规格商品可能产生多条 `sku_id=null` 的购物车项。
  - 解决方案：`CartServiceImpl.addToCart` 中查询已有购物车项时，对 `skuId == null` 使用 `LambdaQueryWrapper.isNull(Cart::getSkuId)`，对 `skuId != null` 使用 `.eq(Cart::getSkuId, skuId)`，保证查到已有项后数量累加而非新建。
  - 若数据库层希望严格唯一，可考虑用 `0` 替代 NULL 作为无规格商品的默认 `sku_id`，但需同步修改唯一约束与查询逻辑。本方案选择保留 NULL + 应用层守卫，更符合「无规格」语义。

#### 8.2.3 SKU 库存扣减一致性

- **风险**：SKU 库存扣减需与商品总库存保持一致，避免列表页展示库存与实际可购库存不符。
- **应对**：
  - `t_product.stock` 在有 SKU 时由 Service 层动态聚合（`productSkuService.calculateTotalStock`），不直接扣减。
  - 列表页展示库存取 `totalStock`（有 SKU）或 `stock`（无 SKU）。
  - 详情页展示具体 SKU 库存。

#### 8.2.4 SKU 属性 JSON 查询性能

- **风险**：`t_product_sku.attributes` 是 JSON 字段，按 `attributes` 精确匹配 SKU 的查询（`selectByAttributes`）可能不走索引。
- **应对**：
  - 当前方案用 `attributes = CAST(#{attributesJson} AS JSON)` 精确匹配，数据量不大时性能可接受。
  - 若性能瓶颈，可考虑：
    1. 新增 `t_product_sku_attribute` 关联表，将 JSON 拆为多行 `(sku_id, attribute_name, attribute_value)`，按 `(product_id, attribute_name, attribute_value)` 建索引，查询时 INTERSECT 多个条件。
    2. 或在前端缓存商品所有 SKU 列表，前端完成属性组合到 SKU ID 的映射，加购 / 购买时直接传 `skuId`，后端无需按 `attributes` 查询。本方案推荐方案 2，前端已有完整 SKU 列表，无需后端再查。

#### 8.2.5 富文本与 XSS

- **风险**：`ProductEdit.vue` 新增的 SKU 属性名 / 属性值是用户输入，需防 XSS。
- **应对**：
  - 后端 `ProductAttributeDTO` 用 `@Size` 限制长度，属性名 / 值在 Service 层做 HTML 转义（`HtmlUtils.htmlEscape`）。
  - 前端 Vue 模板默认对插值做 HTML 转义，`{{ val.value }}` 安全。

#### 8.2.6 秒杀场景的 SKU

- **范围说明**：本次改造**不涉及秒杀场景**的 SKU。`t_seckill_goods` 与 `t_seckill_order` 保持原 `product_id` 关联，秒杀活动仍以商品为单位。
- **原因**：秒杀场景追求极致性能与一人一单约束，引入 SKU 会增加复杂度。若未来需要秒杀 SKU，可参考本方案扩展 `t_seckill_goods` 加 `sku_id`。

#### 8.2.7 并发库存扣减

- **风险**：SKU 库存扣减需保证并发安全。
- **应对**：`ProductSkuServiceImpl.deductStock` 使用乐观锁 `WHERE stock >= #{quantity}`，与现有 `t_product` 库存扣减策略一致。失败抛异常，由上层事务回滚。

#### 8.2.8 迁移脚本执行顺序

- **注意**：`migration_v7_sku.sql` 必须在 `migration_v2.sql`（创建 `t_product_review`）与 `migration_v3.sql`（创建 `t_cart`）与 `normal_order_tables.sql`（创建 `t_normal_order_item`）之后执行，否则 ALTER 会失败。
- **执行前备份**：执行前请备份 `t_cart` / `t_normal_order_item` / `t_product_review` 三张表。

#### 8.2.9 分类规格模板与商品属性的同步策略

- **风险**：分类模板变更后（如管理员在「颜色」属性中新增「玫瑰金」预设值），已使用该模板创建的商品属性是否需要同步更新？
- **应对**：本方案采用「**快照式**」策略——商品创建时将模板属性**复制**到 `t_product_attribute` / `t_product_attribute_value`，并记录 `category_attribute_id` 关联。模板后续变更**不自动同步**到已创建商品，避免影响已上架商品的 SKU 配置。若需同步，可在 `CategoryAttributeManage.vue` 提供「批量同步到该分类下商品」按钮，由管理员显式触发。`category_attribute_id` 字段仅用于前端区分「模板属性 / 自定义属性」展示标签，不参与运行时模板查询。

#### 8.2.10 分类规格模板的向后兼容

- **风险**：分类未配置模板时，商家添加商品不应受影响。
- **应对**：`ProductEdit.vue` 的 `watch(categoryId)` 在 `getCategoryAttributes` 返回空列表时不填充任何属性，商家仍可像 v1.0 方案那样手动「+ 添加自定义属性」。`t_product_attribute.category_attribute_id` 允许 NULL，旧数据无需迁移。

---

## 附录 A：接口契约一览

### A.0 分类规格模板管理接口（新增）

#### A.0.1 获取分类规格模板

```
GET /api/v1/admin/category/{categoryId}/attributes
```

响应：

```json
{
  "code": 200,
  "data": [
    {
      "id": 1001,
      "categoryId": 5,
      "name": "颜色",
      "type": "IMAGE",
      "inputType": "SELECT",
      "isRequired": 1,
      "sortOrder": 0,
      "values": [
        {"id": 2001, "value": "曜石黑", "imageUrl": "#1f2937", "sortOrder": 0},
        {"id": 2002, "value": "银月白", "imageUrl": "#e5e7eb", "sortOrder": 1},
        {"id": 2003, "value": "赤霞红", "imageUrl": "#dc2626", "sortOrder": 2}
      ]
    },
    {
      "id": 1002,
      "categoryId": 5,
      "name": "版本",
      "type": "TEXT",
      "inputType": "SELECT",
      "isRequired": 1,
      "sortOrder": 1,
      "values": [
        {"id": 2004, "value": "标准版", "imageUrl": null, "sortOrder": 0},
        {"id": 2005, "value": "旗舰版", "imageUrl": null, "sortOrder": 1}
      ]
    },
    {
      "id": 1003,
      "categoryId": 5,
      "name": "表带",
      "type": "TEXT",
      "inputType": "SELECT",
      "isRequired": 0,
      "sortOrder": 2,
      "values": [
        {"id": 2006, "value": "氟橡胶表带", "imageUrl": null, "sortOrder": 0},
        {"id": 2007, "value": "米兰尼斯表带", "imageUrl": null, "sortOrder": 1},
        {"id": 2008, "value": "真皮表带", "imageUrl": null, "sortOrder": 2}
      ]
    }
  ]
}
```

#### A.0.2 创建分类规格属性

```
POST /api/v1/admin/category/attributes
```

请求：

```json
{
  "categoryId": 5,
  "name": "颜色",
  "type": "IMAGE",
  "inputType": "SELECT",
  "isRequired": 1,
  "sortOrder": 0,
  "values": [
    {"value": "曜石黑", "imageUrl": "#1f2937", "sortOrder": 0},
    {"value": "银月白", "imageUrl": "#e5e7eb", "sortOrder": 1}
  ]
}
```

响应同 A.0.1 中的单个属性对象。

#### A.0.3 更新分类规格属性

```
PUT /api/v1/admin/category/attributes/{id}
```

请求体同 A.0.2（不含 `categoryId`，由服务端保留原值）。

#### A.0.4 删除分类规格属性

```
DELETE /api/v1/admin/category/attributes/{id}
```

响应：

```json
{"code": 200, "data": null}
```

#### A.0.5 为分类规格属性添加预设值

```
POST /api/v1/admin/category/attributes/{id}/values
```

请求：

```json
{"value": "玫瑰金", "imageUrl": "#fda4af", "sortOrder": 3}
```

响应同 A.0.1 中的单个属性对象（含更新后的预设值列表）。

### A.1 商品详情接口（扩展）

```
GET /api/v1/products/{id}
```

响应新增字段：

```json
{
  "code": 200,
  "data": {
    "id": 1,
    "productName": "智能手表 Pro X",
    "originalPrice": 1299.00,
    "stock": 100,
    "hasSku": true,
    "minPrice": 1099.00,
    "maxPrice": 1899.00,
    "totalStock": 100,
    "attributes": [
      {
        "id": 1,
        "categoryAttributeId": 1001,
        "name": "颜色",
        "type": "IMAGE",
        "sortOrder": 0,
        "values": [
          {"id": 1, "value": "深空灰", "imageUrl": "#4b5563", "sortOrder": 0},
          {"id": 2, "value": "曜石黑", "imageUrl": "#1f2937", "sortOrder": 1}
        ]
      },
      {
        "id": 2,
        "categoryAttributeId": 1002,
        "name": "版本",
        "type": "TEXT",
        "sortOrder": 1,
        "values": [
          {"id": 3, "value": "标准版", "imageUrl": null, "sortOrder": 0},
          {"id": 4, "value": "旗舰版", "imageUrl": null, "sortOrder": 1}
        ]
      }
    ],
    "categoryAttributes": [
      {
        "id": 1001,
        "categoryId": 5,
        "name": "颜色",
        "type": "IMAGE",
        "inputType": "SELECT",
        "isRequired": 1,
        "sortOrder": 0,
        "values": [
          {"id": 2001, "value": "曜石黑", "imageUrl": "#1f2937", "sortOrder": 0},
          {"id": 2002, "value": "银月白", "imageUrl": "#e5e7eb", "sortOrder": 1}
        ]
      }
    ],
    "skus": [
      {
        "id": 1,
        "skuCode": "SKU-abc123",
        "price": 1299.00,
        "stock": 20,
        "mainImage": null,
        "attributes": "{\"颜色\":\"深空灰\",\"版本\":\"标准版\"}",
        "status": 1
      },
      {
        "id": 2,
        "skuCode": "SKU-def456",
        "price": 1599.00,
        "stock": 15,
        "mainImage": null,
        "attributes": "{\"颜色\":\"深空灰\",\"版本\":\"旗舰版\"}",
        "status": 1
      }
    ]
  }
}
```

### A.2 SKU 列表接口（新增）

```
GET /api/v1/products/{productId}/skus
```

响应：

```json
{
  "code": 200,
  "data": [
    {"id": 1, "skuCode": "SKU-abc123", "price": 1299.00, "stock": 20, "mainImage": null, "attributes": "{\"颜色\":\"深空灰\",\"版本\":\"标准版\"}", "status": 1}
  ]
}
```

### A.3 加购接口（扩展）

```
POST /api/v1/cart/add
```

请求新增 `skuId`：

```json
{
  "productId": 1,
  "skuId": 2,
  "quantity": 1
}
```

### A.4 立即购买接口（扩展）

```
POST /api/v1/orders/buy-now
```

请求新增 `skuId`：

```json
{
  "productId": 1,
  "skuId": 2,
  "quantity": 1,
  "addressId": 100,
  "remark": ""
}
```

### A.5 发表评论接口（扩展）

```
POST /api/v1/reviews/create
```

请求新增 `skuId`：

```json
{
  "productId": 1,
  "skuId": 2,
  "content": "很好用",
  "rating": 5,
  "images": null
}
```

### A.6 创建商品接口（扩展）

```
POST /api/v1/products
```

请求新增 `attributes` / `skus`：

```json
{
  "productName": "智能手表 Pro X",
  "categoryId": 5,
  "originalPrice": 1299.00,
  "stock": 100,
  "description": "...",
  "detailHtml": "...",
  "images": ["url1", "url2"],
  "status": "ON_SALE",
  "attributes": [
    {
      "categoryAttributeId": 1001,
      "name": "颜色",
      "type": "IMAGE",
      "sortOrder": 0,
      "values": [
        {"value": "深空灰", "imageUrl": "#4b5563", "sortOrder": 0},
        {"value": "曜石黑", "imageUrl": "#1f2937", "sortOrder": 1}
      ]
    },
    {
      "categoryAttributeId": null,
      "name": "限量编号",
      "type": "TEXT",
      "sortOrder": 2,
      "values": []
    }
  ],
  "skus": [
    {
      "skuCode": "",
      "price": 1299.00,
      "stock": 50,
      "mainImage": null,
      "attributes": "{\"颜色\":\"深空灰\"}",
      "status": 1
    },
    {
      "skuCode": "",
      "price": 1399.00,
      "stock": 50,
      "mainImage": null,
      "attributes": "{\"颜色\":\"曜石黑\"}",
      "status": 1
    }
  ]
}
```

---

## 附录 C：典型分类规格模板示例

### C.1 智能手表分类模板

适用分类：智能手表（`category_id = 5`）

| 属性名 | 类型 | 录入方式 | 是否必选 | 排序 | 预设值 |
| --- | --- | --- | --- | --- | --- |
| 颜色 | IMAGE | SELECT | 是 | 0 | 曜石黑(#1f2937) / 银月白(#e5e7eb) / 赤霞红(#dc2626) / 冰川蓝(#3b82f6) / 森野绿(#16a34a) / 深空灰(#4b5563) |
| 版本 | TEXT | SELECT | 是 | 1 | 标准版 / 旗舰版 / 尊享版 / 运动版 |
| 表带 | TEXT | SELECT | 否 | 2 | 氟橡胶表带 / 米兰尼斯表带 / 真皮表带 |

### C.2 手机分类模板

适用分类：手机（`category_id = 6`）

| 属性名 | 类型 | 录入方式 | 是否必选 | 排序 | 预设值 |
| --- | --- | --- | --- | --- | --- |
| 颜色 | IMAGE | SELECT | 是 | 0 | 曜石黑(#1f2937) / 雪域白(#f9fafb) / 星河蓝(#1e40af) / 沙漠金(#d4a574) |
| 内存 | TEXT | SELECT | 是 | 1 | 8GB / 12GB / 16GB / 24GB |
| 存储 | TEXT | SELECT | 是 | 2 | 128GB / 256GB / 512GB / 1TB |

### C.3 服装分类模板

适用分类：服装（`category_id = 7`）

| 属性名 | 类型 | 录入方式 | 是否必选 | 排序 | 预设值 |
| --- | --- | --- | --- | --- | --- |
| 颜色 | IMAGE | SELECT | 是 | 0 | 黑色(#111827) / 白色(#f9fafb) / 灰色(#6b7280) / 藏青(#1e3a8a) / 卡其(#d4d4aa) |
| 尺码 | TEXT | SELECT | 是 | 1 | S / M / L / XL / XXL / XXXL |

### C.4 电脑分类模板

适用分类：电脑（`category_id = 8`）

| 属性名 | 类型 | 录入方式 | 是否必选 | 排序 | 预设值 |
| --- | --- | --- | --- | --- | --- |
| 颜色 | IMAGE | SELECT | 是 | 0 | 深空灰(#4b5563) / 银色(#d1d5db) / 星光金(#fbbf24) |
| CPU | TEXT | SELECT | 是 | 1 | i5-13400 / i7-13700 / i9-13900 / R5-7600 / R7-7700 / R9-7900 |
| 内存 | TEXT | SELECT | 是 | 2 | 8GB / 16GB / 32GB / 64GB |
| 硬盘 | TEXT | SELECT | 是 | 3 | 512GB SSD / 1TB SSD / 2TB SSD / 4TB SSD |

### C.5 商品自定义属性示例

某款限量版智能手表（`product_id = 100`）在手表分类模板基础上新增「限量编号」自定义属性：

| 属性名 | 类型 | 录入方式 | 是否必选 | `category_attribute_id` | 说明 |
| --- | --- | --- | --- | --- | --- |
| 颜色 | IMAGE | SELECT | 是 | 1001 | 来自分类模板 |
| 版本 | TEXT | SELECT | 是 | 1002 | 来自分类模板 |
| 限量编号 | TEXT | INPUT | 是 | NULL | 商品自定义属性（自由输入） |

---

## 附录 B：设计稿 SKU 样式参考

设计稿 `docs/product-detail-taobao-design.html` 中 SKU 相关样式（864-968 行）：

- `.sku`：SKU 容器，`margin-bottom: 16px`
- `.sku__group`：属性组，`margin-bottom: 14px`
- `.sku__label`：属性标签，13px 灰色，含 `.selected` 子元素显示当前选中值
- `.sku__values`：属性值容器，flex 布局，`gap: 8px`
- `.sku-color`：图片型色块，30×30px 圆角，hover 边框变主色，`.active` 双边框 + 右下角圆点
- `.sku-text`：文字型按钮，padding 6×14px，hover 边框 + 文字变主色，`.active` 主色边框 + 浅主色背景，`.disabled` 灰色 + 删除线

前端实现时直接复用上述样式类名与设计稿保持一致。

---

**文档结束。**