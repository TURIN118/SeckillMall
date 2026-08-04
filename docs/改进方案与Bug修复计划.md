# 秒杀商城 改进方案与 Bug 修复计划

> 本文档基于对前后端代码的深度分析，针对 13 项需求/Bug 逐一给出根因、改进方案与争议选项。
> **请先审阅"第四部分：需要您决策的争议点汇总"，做出选择后我们再进入修改阶段。**

---

## 一、项目技术栈速览

| 层 | 技术 |
|---|---|
| 前端 | Vue 3 + TypeScript + Element Plus + Vite + Pinia（端口 5173） |
| 后端 | Spring Boot 3.2.5 + MyBatis-Plus + Redis + Spring Security + JWT（端口 8080） |
| 主键 | 雪花算法 `IdType.ASSIGN_ID`（19 位 Long） |
| 图片存储 | 本地存储，后端返回相对路径 `/images/...`，前端用 `formatImageUrl` 拼接 baseURL |

---

## 二、通用问题（影响多个需求，建议统一修复）

### 通用问题 A：ImageUploader 组件未拼接后端 baseURL（影响需求 8、9、11）

- **根因**：`frontend/src/components/ImageUploader.vue` 的 `watch` 生成 `fileList` 时，`url` 字段直接使用后端返回的相对路径 `/images/...`，未调用 `formatImageUrl` 拼接 `http://localhost:8080`。`el-upload` 缩略图请求 `http://localhost:5173/images/...` → 404。
- **对比**：列表/卡片层（BannerManage 表格、Home、ProductList、ProductDetail 等）都正确使用了 `formatImageUrl`，唯独 `ImageUploader` 组件内部漏用。
- **影响范围**：所有使用 ImageUploader 的弹窗——SeckillManage（需求9）、BannerManage 编辑弹窗（需求8）、ProductEdit 主图+多图（需求11），共 4 处。
- **统一修复方案**：在 `ImageUploader.vue` 中引入 `formatImageUrl`，`watch` 生成 `fileList` 时 `url: formatImageUrl(url)`；`handleHttpRequest` 返回 `formatImageUrl(url)` 供显示，`modelValue` 仍存相对路径保持纯净。**一处改，4 处弹窗全部生效，编辑回显自动修复。**

### 通用问题 B：雪花 Long ID 精度丢失（影响需求 7，潜在影响所有实体）

- **根因**：后端雪花算法生成 19 位 Long ID，后端无全局 Jackson `Long→String` 序列化配置，JSON 返回数字字面量。前端 `JSON.parse` 超过 `Number.MAX_SAFE_INTEGER`（16位）精度丢失，末尾几位变 0。编辑/删除时把精度丢失的 ID 传回后端，`selectById` 查不到记录 → "不存在"。
- **统一修复方案**：后端新增 `JacksonConfig`，把 `Long.class`/`Long.TYPE` 用 `ToStringSerializer` 序列化为字符串。前端相关 `id` 类型改为 `number | string`。**一处改，所有雪花 ID 实体（轮播图/商品/订单/用户...）全部受益。**

---

## 三、13 项需求逐一分析

### 需求1：搜索结果按价格筛选失效

- **根因**：前后端字段断层。前端 `ProductList.vue` 已发送 `minPrice/maxPrice`，但后端 `ProductQueryRequest.java` 无该字段（Spring MVC 忽略），`ProductServiceImpl` 未取价格，`ProductMapper.java` 签名无价格参数，`ProductMapper.xml` SQL 无价格 WHERE 条件。全链路缺失。
- **方案（后端 4 处，前端无需改）**：
  1. `ProductQueryRequest.java` 加 `BigDecimal minPrice/maxPrice`
  2. `ProductMapper.java` 签名加 `@Param("minPrice"/"maxPrice")`
  3. `ProductServiceImpl.listProducts` 透传价格
  4. `ProductMapper.xml` 加 `AND original_price >= #{minPrice}` / `<= #{maxPrice}` 的 `<if>` 条件

### 需求2：二级分类悬浮浮层鼠标移过去立马消失

- **根因**：`ProductList.vue` 的 `handleTreeLeave` 鼠标离开分类项**立即** `hoverCategoryId=null`，无延迟；且浮层 `.tree-panel` 未绑定 `mouseenter/mouseleave`，无法在鼠标移入时保持显示。对比 `Home.vue` 用了 200ms 延迟。
- **方案（前端）**：A) `handleTreeEnter/Leave` 加 200ms 延迟双定时器互斥（照搬 Home.vue）；B) 浮层 `.tree-panel` 绑定 `@mouseenter`（清除隐藏定时器）/`@mouseleave`（启动隐藏定时器）。推荐 A+B 结合。

### 需求3：底部分页取消固定，改为随页面滚动

- **根因**：当前为"视口固定 + 商品区独立滚动 + 分页钉底"布局。`.category-page` `height:calc(100vh-60px)` 占满视口不滚动；`.product-scroll` `overflow-y:auto` 商品独立滚动；`.pagination-bar` `flex-shrink:0` 钉在底部。
- **方案（前端仅 CSS）**：`.category-page` 的 `height` 改 `min-height` 允许撑高整页滚动；`.product-scroll` 去掉 `overflow-y:auto` 自然撑高；保留面包屑/筛选栏/左侧分类树的 `sticky` 吸顶。

### 需求4：选择一级分类商品为空，需显示其下所有二级分类商品

- **根因**：`ProductMapper.xml` 用 `AND category_id = #{categoryId}` 精确等值匹配。商品只挂在二级分类下，选一级分类 id 查不到商品。
- **方案（后端）**：推荐方案A（Service 层展开）——`ProductServiceImpl.listProducts` 判断 `categoryId` 的 `parentId`，若为一级（`parentId=0`）则查其所有二级 id 列表传给 Mapper 用 `IN` 查询，若为二级保持等值。需 `ProductMapper.java` 加 `@Param("categoryIds") List<Long>`，`ProductMapper.xml` 用 `<foreach>` 拼 `IN`。

### 需求5：商品详情"立即购买"跳转到全部商品界面

- **根因（三层）**：①前端 `ProductDetail.vue` 第170行"立即购买"按钮 `@click` 错误绑定到 `goProductList`（仅 `router.push('/products')`）；②后端 `OrderController` 无创建订单接口，`OrderService.createSeckillOrder` 是秒杀专用；③`payOrder` 未扣减钱包余额，`WalletController` 无消费扣款接口。
- **方案**：
  - 前端：按钮改绑 `handleBuyNow`；新增 `handleBuyNow`（登录校验→拉钱包余额→弹窗确认商品+总价+余额→`createOrder`→`payOrder(orderId,'WALLET')`→跳订单详情）；`api/order.ts` 新增 `createOrder`。
  - 后端：`OrderController` 新增 `POST /api/v1/orders`；`OrderService` 新增 `createNormalOrder`（校验商品/库存→扣库存→建订单UNPAID）；`payOrder` 当 `payMethod="WALLET"` 时 `UserMapper.deductBalance` 原子扣减（`WHERE balance>=amount`）+记流水+更新PAID。

### 需求6：商品详情"收藏""分享"按钮无响应

- **根因**：①`ProductDetail.vue` 第73-89行收藏/分享按钮是纯静态 `<span>`，无 `@click` 绑定；②收藏后端接口完整（`UserFavoriteController`）但前端未接入；③分享功能完全缺失。
- **方案（前端，后端无需改）**：收藏按钮绑 `handleFavorite`（登录校验→`checkFavorite`初始化状态→已收藏则`removeFavorite`/未收藏则`addFavorite`→切换红心+文字）；分享按钮绑 `handleShare`（优先 `navigator.share` 降级 `clipboard.writeText` 复制链接）。

### 需求7：轮播图编辑/删除报"轮播图不存在"，但添加正常

- **根因**：雪花 Long ID 精度丢失（通用问题 B）。列表 JSON 中 `id` 是 19 位数字，前端 `JSON.parse` 精度丢失末尾变 0，编辑/删除传回错误 ID → `selectById` 查不到 → "轮播图不存在"。添加不传 ID 故不受影响。
- **方案**：后端新增 `JacksonConfig` 全局 `Long→String` 序列化（通用问题 B 统一修复）；前端 `BannerVO.id` 等类型改为 `number | string`。

### 需求8：轮播图编辑弹窗缩略图不显示

- **根因**：通用问题 A。列表用了 `formatImageUrl` 能显示，编辑弹窗用 `ImageUploader` 未拼接 baseURL。
- **方案**：通用问题 A 统一修复（ImageUploader 内部引入 `formatImageUrl`）。

### 需求9：秒杀创建活动失败 + 弹窗图片不显示

- **根因（创建失败，致命）**：前端 `el-date-picker` `value-format="YYYY-MM-DD HH:mm:ss"` 提交带空格时间串，后端 `SeckillCreateRequest` 的 `LocalDateTime` 字段无 `@JsonFormat` 注解；`spring.jackson.date-format` 只对 `java.util.Date` 生效，对 `LocalDateTime` 无效，Jackson 按 ISO-8601（带T）反序列化失败 → HTTP 400。
- **根因（创建失败，次生）**：`SeckillGoods` entity 无 `seckillName/perLimit/images/description` 列，service 层静默丢弃这 4 个字段。
- **根因（图片不显示）**：通用问题 A。
- **方案**：①后端 `SeckillCreateRequest` 的 `startTime/endTime` 加 `@JsonFormat(pattern="yyyy-MM-dd HH:mm:ss")`；②（建议）`t_seckill_goods` 表加 `seckill_name/per_limit/images/description` 列并落库；③通用问题 A 修复图片。

### 需求10：后台分类树商品总数显示为 0

- **根因**：后端 `CategoryVO` 无 `productCount` 字段，`CategoryServiceImpl.getCategoryTree()` 完全没有查询商品数量统计，前端 `node.productCount` 永远 `undefined` → 显示 0。
- **方案（后端）**：`CategoryVO` 加 `productCount`；`CategoryServiceImpl.getCategoryTree()` 一次性 `GROUP BY category_id` 查商品数 → `Map<Long,Long>`，`buildChildren` 时为每个节点设置 `productCount = 直接商品数 + 所有子孙 productCount 之和`；**缓存副作用修复**：`ProductServiceImpl` 增删改时调用 `evictCategoryCache()` 失效分类树缓存。

### 需求11：商品编辑弹窗主图和图片不显示

- **根因**：通用问题 A。`ProductManage` 列表用了 `formatImageUrl` 能显示，`ProductEdit` 用 `ImageUploader` 未拼接 baseURL。
- **方案**：通用问题 A 统一修复。

### 需求12：购物车商品图片不显示

- **根因**：`Cart.vue` 第58-64行 `<img :src="item.mainImage">` 直接绑定相对路径，未调用 `formatImageUrl`。全项目其他 8 个页面都用了，唯独 Cart.vue 漏用。
- **方案（前端单点）**：`Cart.vue` import `formatImageUrl`，模板改为 `:src="formatImageUrl(item.mainImage)"`。

### 需求13：购物车"去结算"显示"结算功能开发中"

- **根因（四层缺失）**：①前端 `handleCheckout` 仅弹提示；②前端 `api/order.ts` 无"从购物车创建订单"接口；③后端 `OrderController` 无创建订单接口；④`SeckillOrder` 模型为秒杀设计（`seckillId` 必填、无 `addressId`、`uk_user_seckill` 一人一单索引、`quantity=1` 硬编码），不支持购物车多商品结算。
- **方案（前后端协同）**：
  - 后端：新建 `NormalOrder`(`t_normal_order`) + `NormalOrderItem`(`t_normal_order_item`) 两张表（含 `addressId`、多商品明细、多数量）；`OrderController` 新增 `POST /api/v1/orders`（从购物车创建订单，事务：校验→建订单+明细→扣库存→清购物车项）；复用现有模拟支付与超时取消机制。
  - 前端：`api/order.ts` 新增 `createOrderFromCart`；新建 `Checkout.vue` 结算确认页 + 路由 `/checkout`（收货地址卡+商品清单卡+支付方式卡+提交按钮）；改造 `handleCheckout` 跳转 `/checkout`。

---

## 四、需要您决策的争议点汇总

> 以下是对各项需求分析中出现的"多方案/不确定/更优选项"的汇总，**请您逐项选择，我们按您的选择进入修改阶段**。

### 决策1【需求5】立即购买的交互流程
- **A（推荐）**：当前页弹窗确认（显示商品+数量+总价+钱包余额，确认即下单+支付，一步到位）
- **B**：跳转独立结算确认页 `/checkout`（可扩展选地址/优惠券，但"立即购买"通常单商品即时，弹窗更符合直觉）
- **C**：加入购物车→购物车结算（最重，不符合"立即购买"语义）

### 决策2【需求5】钱包余额不足时的处理
- **A（推荐）**：弹窗提示"余额不足，请充值" + 提供"去充值"按钮跳转充值页
- **B**：允许切换其他支付方式（支付宝/微信模拟）
- **C**：直接拦截，仅提示"余额不足"

### 决策3【需求5】普通订单是否复用秒杀订单表 `t_seckill_order`
- **A（推荐）**：复用，`seckill_id` 允许 null（最小改动，需确认表结构允许）
- **B**：新建独立 `t_normal_order` 表（模型清晰，但与需求13的 NormalOrder 合并考虑）
- > 注：需求13已建议新建 NormalOrder，若需求5也用 NormalOrder 则统一。若您希望需求5和需求13统一为同一套普通订单模型，请选 B。

### 决策4【需求6】分享功能的实现方式
- **A（推荐）**：`navigator.clipboard` 复制商品链接（MVP 最简，纯前端零依赖）
- **B**：优先 `navigator.share`（移动端原生分享面板），降级 `clipboard`（渐进增强）
- **C**：生成海报（canvas 绘制商品图+二维码，体验好但工作量大）

### 决策5【需求7】Long ID 精度丢失的修复范围
- **A（推荐）**：后端全局 Jackson 配置，所有 Long→String 序列化（根治所有雪花 ID 精度问题，前端 id 类型改为 `number|string`）
- **B**：仅轮播图相关接口处理（局部修复，其他实体仍有隐患）

### 决策6【需求9】秒杀创建活动的字段丢失问题
- **A（推荐）**：后端 `t_seckill_goods` 表加 `seckill_name/per_limit/images/description` 列并落库（真正满足"完整输入完信息后创建"）
- **B**：不落库，前端删除弹窗中"活动名称/活动图片/活动描述/限购数量"表单项（复用商品信息，但误导用户）
- > 注：时间格式反序列化问题（加 `@JsonFormat`）是必须修的，此处仅决策字段丢失部分。

### 决策7【需求10】一级分类 productCount 的语义
- **A（推荐）**：该一级分类子树商品总数（含所有二级分类商品数之和 + 直接挂在一级分类下的商品数）
- **B**：仅直接挂在该分类下的商品数（若商品都挂二级，一级永远显示 0，不符合需求）

### 决策8【需求10】商品数是否只统计在售商品
- **A（推荐）**：统计所有未删除商品（与删除分类时的检查逻辑一致，不过滤 status）
- **B**：只统计在售（`status=ON_SALE`）商品

### 决策9【需求13】结算流程的交互形式
- **A（推荐）**：独立结算确认页 `/checkout`（与项目独立页风格一致，地址+商品+支付方式有充足展示空间）
- **B**：当前页弹窗确认（`el-dialog`，空间局促）

### 决策10【需求13】支付方式
- **A（推荐）**：模拟支付（复用现有 `payOrder` 逻辑，前端展示支付宝/微信 radio，点击直接模拟成功，与秒杀订单支付体验统一）
- **B**：钱包余额支付（需新增钱包扣款体系，与需求5的钱包支付统一，但工作量大）
- > 注：若选 B，则需求5和需求13的支付统一为钱包余额支付。

### 决策11【需求4】一级分类展开为子分类集合的实现方式
- **A（推荐）**：Service 层展开（`ProductServiceImpl` 判断一级/二级，查子分类 id 列表传 Mapper 用 IN，逻辑清晰可测可复用缓存）
- **B**：SQL 子查询（`ProductMapper.xml` 改为 `category_id IN (SELECT id FROM t_category WHERE id=#{categoryId} OR parent_id=#{categoryId})`，简洁但 SQL 耦合业务）

### 决策12【通用】图片 URL 拼接的修复位置
- **A（推荐）**：在 `ImageUploader.vue` 组件内部统一引入 `formatImageUrl`（一处改，需求8/9/11 全部生效，编辑回显自动修复）
- **B**：在每个调用方（BannerManage/SeckillManage/ProductEdit）分别处理（重复代码，易遗漏）

---

## 五、修改顺序建议（确认决策后执行）

1. **通用修复先行**：通用问题 A（ImageUploader）+ 通用问题 B（JacksonConfig）→ 一次性解决需求7/8/9图片/11图片
2. **后端接口补全**：需求1（价格筛选）+ 需求4（一级分类展开）+ 需求5（普通下单+钱包支付）+ 需求9（@JsonFormat+字段落库）+ 需求10（分类商品数）+ 需求13（NormalOrder 建表建接口）
3. **前端界面修复**：需求2（悬浮延迟）+ 需求3（分页滚动）+ 需求5（立即购买弹窗）+ 需求6（收藏分享）+ 需求12（购物车图片）+ 需求13（Checkout 页）
4. **联调验证**