# 库存 Ownership 分析报告

> 本报告基于当前 HEAD `8d7ac2b2e2c2f58c469fc7a878792f84b6b0cac6`（分支 `main`，与 `github/main` 同步）生成。
> 本轮只分析，不修改代码。

---

## 基线确认

- **当前 HEAD**：`8d7ac2b2e2c2f58c469fc7a878792f84b6b0cac6`
- **分支**：`main`（与 `github/main` 同步）
- **工作区**：仅有少量未跟踪文档和 `.trae`/`.vscode` 删除，不影响代码基线
- **最近重构**：Pragmatic DDD + Modular Monolith（Phase 0-18 + P0/P1/P2），所有 God Service ≤ 500 行

---

## 1. 当前库存 Ownership Map

### Product Entity（`t_product`）

| 字段 | 类型 | 语义 | 真实 Ownership |
|---|---|---|---|
| `stock` | Integer | **双重语义**：①无规格商品的真实库存（单一真值源）；②有 SKU 时的冗余聚合字段（= Σ SKU.stock） | **混乱**：①由 `InventoryService` 增减；②由 `ProductService.saveAttributesAndSkus` / `ProductSkuService.refreshTotalStock` 刷新 |
| `totalStock` | Integer | 冗余字段：有 SKU 时 = Σ 启用 SKU.stock，无 SKU 时 = stock | `ProductSkuService.refreshTotalStock` / `ProductService.saveAttributesAndSkus` |
| `salesCount` | Integer | 销量计数：扣减 +qty，回补 -qty | `InventoryService`（随 stock 同步增减） |
| `minPrice` / `maxPrice` | BigDecimal | 价格冗余字段 | `ProductSkuService.refreshTotalStock` / `ProductService.saveAttributesAndSkus` |
| `cartCount` / `favoriteCount` | Integer | 加购/收藏冗余计数（非库存） | `ProductService.updateCartCount` / `updateFavoriteCount` |

### ProductSku Entity（`t_product_sku`）

| 字段 | 类型 | 语义 | 真实 Ownership |
|---|---|---|---|
| `stock` | Integer | SKU 真实库存（有规格商品的单一真值源） | `InventoryService.deductSkuStock` / `rollbackSkuStock`；定义时由 `ProductSkuService.saveSkus` 初始写入 |

### Order / Seckill / Payment / Coupon / Promotion

- **Order**：仅通过 `OrderServiceImpl` / `OrderLifecycleServiceImpl` 调用 `InventoryService` 触发库存增减，不直接写 stock
- **Seckill**：独立库存体系（`t_seckill_goods.stock_count`，由 `SeckillGoodsMapper.deductStockOptimistic` / `restoreStockOptimistic` + Redis Lua 维护），**与普通库存隔离**
- **Payment / Coupon / Promotion**：不触碰 stock

---

## 2. 所有 stock 写入点（全项目搜索结果）

### A. 真正的 stock mutation（运行时库存增减，业务语义）

| 位置 | 操作 | SQL |
|---|---|---|
| `InventoryServiceImpl.deductProductStock` (L35-43) | t_product.stock - qty, sales_count + qty | `stock = stock - qty, sales_count = sales_count + qty` WHERE stock>=qty AND status=ON_SALE（乐观锁） |
| `InventoryServiceImpl.rollbackProductStock` (L46-56) | t_product.stock + qty, sales_count - qty | `stock = stock + qty, sales_count = sales_count - qty` WHERE status=ON_SALE |
| `InventoryServiceImpl.deductSkuStock` (L59-70) | t_product_sku.stock - qty | `stock = stock - {0}` WHERE id=? AND stock>=?（参数绑定，乐观锁） |
| `InventoryServiceImpl.rollbackSkuStock` (L73-81) | t_product_sku.stock + qty | `stock = stock + {0}` WHERE id=? |

### B. 商品/SKU 定义修改（创建/编辑时设置初始库存）

| 位置 | 操作 | 说明 |
|---|---|---|
| `ProductServiceImpl.createProduct` (L234-235) | product.setStock(req.getStock()), setSalesCount(0) → insert | 创建无规格商品的初始库存 |
| `ProductServiceImpl.updateProduct` (L277-279) | product.setStock(req.getStock()) → updateById | **风险点**：对有 SKU 商品会覆盖聚合值 |
| `ProductSkuServiceImpl.saveSkus` (L91) | sku.setStock(dto.getStock()) → insert | 创建/编辑 SKU 的初始库存（先物理删除旧 SKU 再插入） |
| `AdminProductSkuController.generateSkus` (L106) | dto.setStock(0) | 笛卡尔积生成时占位，非 DB 写 |

### C. 库存聚合字段刷新（冗余字段同步）

| 位置 | 操作 | 说明 |
|---|---|---|
| `ProductSkuServiceImpl.refreshTotalStock` (L146-159) | productMapper.update set totalStock/minPrice/maxPrice | **不刷新 stock**，导致有 SKU 时 stock 与 totalStock 可能不一致 |
| `ProductServiceImpl.saveAttributesAndSkus` 有 SKU 分支 (L444-453) | product.setStock(totalStock)/setTotalStock/setMinPrice/setMaxPrice → updateById | 商品定义路径写库存聚合字段 |
| `ProductServiceImpl.saveAttributesAndSkus` 无 SKU 分支 (L455-460) | product.setTotalStock(stock)/setMinPrice/setMaxPrice → updateById | 对齐冗余字段 |

### D. 纯查询（VO 赋值，非 DB 写）

- `ProductServiceImpl.toProductVO` (L416-417)、`enrichWithSkuInfo` (L480,484)
- `ProductSkuServiceImpl.toVO` (L166)
- `UserFavoriteServiceImpl` (L178)、`CartServiceImpl` (L345)：VO 赋值

### E. 冗余计数维护（非库存）

- `ProductServiceImpl.updateCartCount` (L351-355)：`cart_count = cart_count + delta`
- `ProductServiceImpl.updateFavoriteCount` (L363-367)：`favorite_count = favorite_count + delta`

### Seckill 库存（独立体系，不在本次修改范围）

- `SeckillGoodsMapper.deductStockOptimistic` / `restoreStockOptimistic`
- `SeckillLuaService.deductStock`（Redis Lua）
- `SeckillDbStrategy.deductStock`、`SeckillOrderProducer` / `SeckillOrderConsumer`

---

## 3. ProductService 中应该迁移的具体方法

### 当前 ProductService 涉及 stock 的代码分类

| 方法 / 行号 | 分类 | 说明 |
|---|---|---|
| `createProduct` L234-235 | **B 商品定义修改** | 创建时设置初始 stock 和 salesCount=0 |
| `createProduct` → `saveAttributesAndSkus` L444-453 | **C 聚合字段刷新** | 有 SKU 时把 stock 设为 SKU 之和（应迁移） |
| `createProduct` → `saveAttributesAndSkus` L455-460 | **C 聚合字段刷新** | 无 SKU 时对齐 totalStock（可保留或迁移） |
| `updateProduct` L277-279 | **B 商品定义修改** | 编辑时 setStock（**需加守卫**，非迁移） |
| `updateProduct` → `saveAttributesAndSkus` L444-460 | **C 聚合字段刷新** | 同 createProduct（应迁移） |
| `updateCartCount` L351-355 | **E 冗余计数** | cart_count，非库存，不迁移 |
| `updateFavoriteCount` L363-367 | **E 冗余计数** | favorite_count，非库存，不迁移 |
| `toProductVO` L416-417 | **D 纯查询** | VO 赋值，不迁移 |
| `enrichWithSkuInfo` L480,484 | **D 纯查询** | VO 赋值，不迁移 |

### 应迁移的具体逻辑

**`saveAttributesAndSkus` 中有 SKU 分支的聚合刷新（L444-453）**：

```java
// 当前：ProductService 直接写 t_product.stock/totalStock/minPrice/maxPrice
BigDecimal minPrice = productSkuService.calculateMinPrice(productId);
Integer totalStock = productSkuService.calculateTotalStock(productId);
BigDecimal maxPrice = productSkuService.calculateMaxPrice(productId);
product.setOriginalPrice(minPrice);
product.setStock(totalStock);        // ← 库存聚合字段，应迁移
product.setMinPrice(minPrice);
product.setMaxPrice(maxPrice);
product.setTotalStock(totalStock);   // ← 聚合字段，应迁移
productMapper.updateById(product);
```

**迁移目标**：改为调用 `productSkuService.refreshTotalStock(productId)`（或 `inventoryService.refreshProductStock`），由聚合服务统一刷新 stock/totalStock/minPrice/maxPrice。ProductService 只保留 `product.setOriginalPrice(minPrice)` 这类商品定义字段更新。

---

## 4. InventoryService 应该新增/调整的具体 API

### 当前 InventoryService 职责

仅负责**运行时库存增减**（4 个方法）：`deductProductStock` / `rollbackProductStock` / `deductSkuStock` / `rollbackSkuStock`。

不负责：商品定义初始库存、冗余聚合字段刷新。

### 推荐方案 A（最小修改，不新增 InventoryService API）

**不新增 InventoryService API**，仅调整 `ProductSkuService.refreshTotalStock`：

```java
// ProductSkuServiceImpl.refreshTotalStock 扩展：有 SKU 时同时刷新 stock 字段
public void refreshTotalStock(Long productId) {
    Integer totalStock = calculateTotalStock(productId);
    BigDecimal minPrice = calculateMinPrice(productId);
    BigDecimal maxPrice = calculateMaxPrice(productId);
    productMapper.update(null, new LambdaUpdateWrapper<Product>()
            .eq(Product::getId, productId)
            .set(Product::getTotalStock, totalStock)
            .set(Product::getStock, totalStock)      // ← 新增：有 SKU 时 stock = SKU 之和
            .set(Product::getMinPrice, minPrice)
            .set(Product::getMaxPrice, maxPrice));
}
```

**理由**：改动最小，不新增接口，直接解决 stock/totalStock 不一致；InventoryService 继续专注运行时 mutation。

### 推荐方案 B（更彻底，InventoryService 成为库存统一入口）

**InventoryService 新增 1 个 API**：

```java
/**
 * 刷新商品库存冗余聚合字段（商品定义变更或 SKU 库存变更后调用）。
 * 有 SKU 时：stock/totalStock = Σ 启用 SKU.stock，minPrice/maxPrice = SKU 价格区间。
 * 无 SKU 时：totalStock = stock，minPrice = maxPrice = originalPrice。
 */
void refreshProductStock(Long productId);
```

并将 `ProductSkuService.refreshTotalStock` 标记 `@Deprecated`，内部委托 `inventoryService.refreshProductStock`。

**理由**：InventoryService 成为库存操作唯一入口（mutation + aggregate refresh），职责更清晰；但新增 1 个 API，改动略大。

### 推荐

**推荐方案 A**。理由：
- 约束明确要求"不创建无业务意义的 Facade / Manager / Port"、"不进行其他重构"、"最小修改"
- 方案 A 仅改 2 个文件，不新增接口，直接解决两个核心问题（stock/totalStock 不一致 + updateProduct 覆盖）
- InventoryService 当前 4 个方法已足够覆盖运行时 mutation，聚合刷新交给 ProductSkuService 符合现有模块边界

---

## 5. 受影响的测试

| 测试文件 | 影响点 | 调整说明 |
|---|---|---|
| `ProductServiceTest` | `createProduct` / `updateProduct` 测试 mock 了 `productSkuService.calculateTotalStock/calculateMinPrice/calculateMaxPrice` | 若 `saveAttributesAndSkus` 改为调用 `refreshTotalStock`，需把 mock 改为 `productSkuService.refreshTotalStock(productId)`（verify 调用次数） |
| `OrderServiceTest` | mock 了 `inventoryService.deductProductStock` | **不受影响**（InventoryService API 不变） |
| `ProductControllerTest` | 集成测试 create/update 商品 | 若 updateProduct 对有 SKU 商品忽略 req.stock，需新增测试用例验证守卫生效 |
| `OrderLifecycleServiceImpl` 相关测试 | 调用 `productSkuService.refreshTotalStock` | 若方案 A 扩展 refreshTotalStock 刷新 stock，回补后 stock 与 totalStock 一致，测试断言可能需调整 |

---

## 6. 预计修改文件列表（方案 A）

| 文件 | 修改内容 |
|---|---|
| `seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductSkuServiceImpl.java` | `refreshTotalStock` 扩展：有 SKU 时同时 `set(Product::getStock, totalStock)`，保证 stock 与 totalStock 一致 |
| `seckill-mall/src/main/java/com/seckill/mall/service/impl/ProductServiceImpl.java` | ① `saveAttributesAndSkus` 有 SKU 分支：移除显式 setStock/setTotalStock/setMinPrice/setMaxPrice，改为调用 `productSkuService.refreshTotalStock(productId)`；保留 `product.setOriginalPrice(minPrice)` 后 `productMapper.updateById(product)` 更新定义字段。② `updateProduct` L277-279：增加守卫——若商品有 SKU（`productSkuService.listEnabledByProductId(id)` 非空）则忽略 `req.getStock()`（或抛 `BusinessException(PARAM_ERROR, "SKU 商品库存由 SKU 维护，不可直接设置")`） |
| `seckill-mall/src/test/java/com/seckill/mall/service/ProductServiceTest.java` | 调整 createProduct/updateProduct 测试中 `productSkuService` 的 mock 与 verify |
| `seckill-mall/src/test/java/com/seckill/mall/controller/ProductControllerTest.java` | 新增 updateProduct 对有 SKU 商品忽略 req.stock 的测试用例 |

**预计共 4 个文件**（2 主 + 2 测试）。

---

## 7. 风险

| 风险 | 等级 | 说明 | 缓解 |
|---|---|---|---|
| `refreshTotalStock` 刷新 stock 改变有 SKU 商品 t_product.stock 写入语义 | 中 | 原本由 `saveAttributesAndSkus` 显式 set，改为 `refreshTotalStock` 统一刷新，写入时机/顺序变化 | 两者都在同一事务内（`@Transactional`），最终态一致；回归测试覆盖 |
| `updateProduct` 忽略 req.stock 对有 SKU 商品影响前端 | 中 | 前端编辑商品时若传 stock 会被忽略，可能导致前端展示与后端不一致 | 前端配合：有 SKU 商品编辑界面不展示 stock 输入框；或后端返回警告信息 |
| `refreshTotalStock` 在无 SKU 分支也刷新 stock 可能误覆盖 | 低 | 无 SKU 时 totalStock = stock，若 refreshTotalStock 无条件 set(stock, totalStock) 会把 stock 设为自身（无害） | refreshTotalStock 仅在有启用 SKU 时刷新 stock；无 SKU 时只刷新 totalStock/minPrice/maxPrice 对齐 |
| `OrderLifecycleServiceImpl.rollbackStockByItems` 回补 SKU 后调 refreshTotalStock 现在会刷新 stock | 低 | 回补后 t_product.stock 会同步增加，行为正确（聚合值随 SKU 库存变化） | 这是期望行为，反而修复了原 stock 不更新的隐患 |
| ArchUnit 规则可能触发 | 低 | 改动未引入跨模块 Mapper 依赖，ProductSkuService→ProductMapper 已有 | 运行 `ArchitectureRulesTest` 验证 21/21 通过 |

---

## 8. 推荐的 Git commit 内容

```
refactor(inventory): clarify t_product.stock ownership for SKU products

Problem:
- t_product.stock has dual semantics: real stock for non-SKU products and
  aggregate field for SKU products, leading to ownership ambiguity.
- ProductSkuServiceImpl.refreshTotalStock did not sync t_product.stock,
  causing stock/totalStock inconsistency for SKU products after stock
  rollback.
- ProductServiceImpl.updateProduct overwrote aggregate stock when
  req.stock was provided for SKU products.

Fix (minimal, no schema/API change):
- ProductSkuServiceImpl.refreshTotalStock now also syncs t_product.stock
  with SKU aggregate when enabled SKUs exist, keeping stock/totalStock
  consistent.
- ProductServiceImpl.saveAttributesAndSkus delegates aggregate refresh to
  ProductSkuService.refreshTotalStock, removing duplicate stock/totalStock
  writes in product definition path.
- ProductServiceImpl.updateProduct guards req.stock: ignored for SKU
  products to prevent overwriting aggregate stock.

Ownership after fix:
- InventoryService: runtime stock mutation (deduct/rollback) only.
- ProductSkuService: SKU definition + aggregate refresh (refreshTotalStock).
- ProductService: product definition only (name/description/price/images/
  status/initial stock for non-SKU products).

No schema change, no Order/Payment/Coupon/Promotion/Seckill/Redis Lua
change, no new Facade/Manager/Port.
```

---

## 分析结论

本轮分析已完成，**未修改任何代码**。

**核心发现**：`t_product.stock` 字段承担双重语义（无规格商品真实库存 + 有 SKU 时聚合字段），当前 Ownership 混乱体现在两处：

1. `ProductSkuServiceImpl.refreshTotalStock` 不刷新 stock，导致 SKU 库存回补后 stock 与 totalStock 不一致
2. `ProductServiceImpl.updateProduct` 允许对有 SKU 商品直接 setStock，会覆盖聚合值

**最小修改方案（方案 A）**：仅改 2 个主文件 + 2 个测试文件，不新增 API、不改 schema、不触碰 Order/Seckill 等约束范围，即可理清 Ownership。