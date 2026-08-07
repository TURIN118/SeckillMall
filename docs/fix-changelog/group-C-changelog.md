# 组C 修复变更日志（后端契约 + 数据访问 + 充值卡）

> **修复人**: 组C（coding-engineer）
> **修复时间**: 2026-08-07
> **Bug 清单**: C4, H-D1, M-D2, M-D3, M-D4, M-D5, M-D6, M-D7

---

## C4 — 充值卡生成接口无法返回明文卡密

### 根因分析
`RechargeCardVO.cardPassword` 标注了 `@JsonIgnore`（用于列表查询防泄露）。
`RechargeCardGenerateVO` 继承自 `RechargeCardVO` 并仅重写 `getCardPassword()` getter，
但 Jackson 的 `@JsonIgnore` 是字段级注解，子类重写 getter **无法取消**父类字段的忽略标记。
同时 `AdminRechargeCardController.generate` 返回类型仍为 `Result<List<RechargeCardVO>>`，
导致生成接口同样屏蔽卡密。新生成的充值卡明文卡密永久丢失，DB BCrypt 单向加密不可逆，无补救机制。

### 修复策略
1. `RechargeCardGenerateVO` 不再继承 `RechargeCardVO`，独立声明所有字段，
   `cardPassword` 不带 `@JsonIgnore`，Jackson 默认序列化该字段。
2. `AdminRechargeCardController.generate` 返回类型改为 `Result<List<RechargeCardGenerateVO>>`。
3. `RechargeCardService.generate` 签名改为 `List<RechargeCardGenerateVO>`，实现同步修改。
4. 列表查询仍使用 `RechargeCardVO`（保留 `@JsonIgnore`），卡密屏蔽不变。

### 修改文件清单
- `seckill-mall/src/main/java/com/seckill/mall/vo/RechargeCardGenerateVO.java`
- `seckill-mall/src/main/java/com/seckill/mall/controller/AdminRechargeCardController.java`
- `seckill-mall/src/main/java/com/seckill/mall/service/RechargeCardService.java`
- `seckill-mall/src/main/java/com/seckill/mall/service/impl/RechargeCardServiceImpl.java`

### 预期效果
- 生成充值卡接口返回明文卡密，新卡可正常充值。
- 列表查询接口仍屏蔽卡密，无泄露风险。

### 潜在风险
- `RechargeCardGenerateVO` 不再继承 `RechargeCardVO`，如有代码依赖继承关系需调整（已排查无此依赖）。

### 回归测试用例设计
1. 调用 `POST /api/v1/admin/recharge-cards/generate`，断言响应体中每条记录包含非空 `cardPassword`。
2. 调用 `GET /api/v1/admin/recharge-cards/list`，断言响应体中每条记录不含 `cardPassword` 字段。
3. 用生成返回的卡号+卡密调用充值接口，断言充值成功。

---

## H-D1 — 未启用防全表更新拦截器

### 根因分析
`MybatisPlusConfig` 仅配置了 `PaginationInnerInterceptor` + `OptimisticLockerInnerInterceptor`，
缺少 `BlockAttackInnerInterceptor`。项目存在 Controller 直调 Mapper 的写法，
一旦 `UpdateWrapper`/`LambdaUpdateWrapper` 漏写 `eq` 条件即触发全表 update/delete，无兜底防线。

### 修复策略
在 `MybatisPlusInterceptor` 中追加 `new BlockAttackInnerInterceptor()`，
阻止无 where 条件的 update/delete 操作。

### 修改文件清单
- `seckill-mall/src/main/java/com/seckill/mall/config/MybatisPlusConfig.java`

### 预期效果
- 任何无 where 条件的 update/delete 将抛出 `MybatisPlusException`，阻止全表误操作。

### 潜在风险
- 若现有代码中存在合法的"全表更新"场景（如批量初始化），会被拦截。已 review 项目内所有 update/delete 均带主键或唯一条件，无影响。

### 回归测试用例设计
1. 构造无 where 条件的 `update(null, wrapper)` 调用，断言抛出异常。
2. 构造带 `eq(id, ...)` 条件的正常 update，断言成功。

---

## M-D2 — Controller 直调 Mapper 绕过 Service

### 根因分析
`WalletController` 直接注入 `RechargeCardMapper` 和 `UserMapper`，在 `records()` 方法中直接调用 Mapper 查询。
`UserController` 直接注入 `UserMapper`，在 `updatePhone`/`updateEmail` 中直接调用 `findByPhone`/`updateById`/`selectById`。
违反分层架构：Controller 应仅编排 Service 并返回 `Result<VO>`，数据访问逻辑应在 Service 层。

### 修复策略
1. 新建 `WalletService` 接口 + `WalletServiceImpl`，封装 `getBalance` 和 `listRecords`（含 RechargeCard→WalletRecordVO 转换与卡号脱敏）。
2. 新建 `UserService` 接口 + `UserServiceImpl`，封装 `updatePhone` 和 `updateEmail`（含唯一性校验、更新、Entity→VO 转换）。
3. `WalletController` 移除 `RechargeCardMapper`/`UserMapper` 依赖，改为注入 `WalletService`。
4. `UserController` 移除 `UserMapper` 依赖，改为注入 `UserService`，验证码校验保留在 Controller 编排层。

### 修改文件清单
- `seckill-mall/src/main/java/com/seckill/mall/service/WalletService.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/service/impl/WalletServiceImpl.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/service/UserService.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/service/impl/UserServiceImpl.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/controller/WalletController.java`
- `seckill-mall/src/main/java/com/seckill/mall/controller/UserController.java`

### 预期效果
- Controller 不再依赖 Mapper，分层清晰。
- 数据访问逻辑集中在 Service，便于事务管理和复用。

### 潜在风险
- `UserService` 与 `AdminUserService` 职责不同（前者面向当前用户个人信息，后者面向后台管理），无冲突。
- `WalletService` 与 `RechargeCardService` 职责不同（前者聚合钱包视图，后者管充值卡生命周期），无冲突。

### 回归测试用例设计
1. 登录后调用 `GET /api/v1/wallet/records`，断言返回交易记录列表且卡号脱敏。
2. 调用 `PUT /api/v1/users/profile/phone`（带正确验证码），断言手机号更新成功。
3. 调用 `PUT /api/v1/users/profile/email`（带已占用邮箱），断言返回 EMAIL_EXISTS 错误。

---

## M-D3 — 多处 DTO 已声明校验但 Controller 未触发

### 根因分析
`CategoryController.update`、`AdminUserController.list`、`AdminOrderController.list`、`SystemController.operationLogs`
的请求参数虽声明了校验注解，但 Controller 方法参数前未加 `@Valid`，导致校验不触发，脏数据可直达 Service。

### 修复策略
在以下方法参数前加 `@Valid`：
- `CategoryController.update` 的 `CategoryUpdateRequest`
- `AdminUserController.list` 的 `UserListRequest`
- `AdminOrderController.list` 的 `AdminOrderQueryRequest`
- `SystemController.operationLogs` 的 `OperationLogQueryRequest`

### 修改文件清单
- `seckill-mall/src/main/java/com/seckill/mall/controller/CategoryController.java`
- `seckill-mall/src/main/java/com/seckill/mall/controller/AdminUserController.java`
- `seckill-mall/src/main/java/com/seckill/mall/controller/AdminOrderController.java`
- `seckill-mall/src/main/java/com/seckill/mall/controller/SystemController.java`

### 预期效果
- 违反 DTO 校验约束的请求在 Controller 层即被拒绝（返回 400），无需到 Service 层才报错。

### 潜在风险
- 若现有前端传参不满足新增校验约束，会收到 400。需确认前端传参已符合（本次仅触发已声明的校验，未新增约束）。

### 回归测试用例设计
1. 对 `PUT /api/v1/categories/{id}` 传入超长 `categoryName`（>32），断言返回 400。
2. 对 `GET /api/v1/admin/orders` 传入非法 `pageNum`（-1），断言返回 400。

---

## M-D4 — CategoryUpdateRequest 全字段、LoginRequest 缺格式校验

### 根因分析
`CategoryUpdateRequest` 全字段无任何校验注解，脏数据可落库（负数排序值、超长名称等）。
`LoginRequest` 仅有 `@NotBlank`，缺长度/字符集约束，超长或含控制字符的入参可直达 Service。

### 修复策略
- `CategoryUpdateRequest`：
  - `categoryName`: `@Size(min=1, max=32)`
  - `parentId`: `@PositiveOrZero`
  - `sortOrder`: `@Min(0)`
  - `status`: `@Min(0)`
  - 字段语义为"可选更新"（PATCH），故不强制 `@NotBlank`/`@NotNull`，仅约束非 null 入参的取值范围。
- `LoginRequest`：
  - `username`: `@Size(min=3, max=32)`（与注册接口 4-20 对齐，登录放宽下界兼容历史 admin 账号）
  - `password`: `@Size(min=6, max=64)` + `@Pattern(可打印 ASCII)`（排除控制字符，防止注入）
  - `captchaKey`: `@Size(max=128)`
  - `captchaCode`: `@Size(max=16)`

### 修改文件清单
- `seckill-mall/src/main/java/com/seckill/mall/dto/CategoryUpdateRequest.java`
- `seckill-mall/src/main/java/com/seckill/mall/dto/LoginRequest.java`

### 预期效果
- 脏数据在 Controller 层被拦截，不落库。
- 超长/控制字符密码被拒绝，降低注入风险。

### 潜在风险
- 若现有用户密码含非可打印 ASCII 字符，登录会被拒绝。已排查种子数据密码均为可打印 ASCII，无影响。
- `username` 下界 3 兼容 admin（5 字符），无影响。

### 回归测试用例设计
1. `PUT /api/v1/categories/{id}` 传入 `sortOrder=-1`，断言 400。
2. `POST /api/v1/auth/login` 传入超长 password（>64），断言 400。
3. `POST /api/v1/auth/login` 传入含控制字符的 password，断言 400。

---

## M-D5 — 声明 MapStruct 却零使用

### 根因分析
`pom.xml` 已声明 MapStruct 依赖和 annotation processor，但全项目零使用，
所有 entity→VO 转换均为手工 `setXxx`，冗余且易漏字段/脱敏不一致。

### 修复策略
1. 新建 `com.seckill.mall.converter` 包，放 MapStruct mapper 接口。
2. `SeckillOrderConverter`：`SeckillOrder` → `SeckillOrderVO`，`@Mapper(componentModel="spring")`，
   `status` 枚举通过 `@AfterMapping` 转为 code + 中文描述，`isDeleted` 等 entity 内部字段不映射。
3. `RechargeCardConverter`：`RechargeCard` → `RechargeCardVO`，`cardPassword` 显式 ignore（脱敏），
   `status` 枚举通过 expression 转为 code。
4. `UserConverter`：`User` → `UserVO`，`avatarUrl`→`avatar` 字段名映射，
   `role`/`status` 枚举通过 `@AfterMapping` 转为 code，`phone`/`email` 脱敏在 `@AfterMapping` 中完成。
5. 由于 `SeckillOrderVO` 尚不存在（B4 由组B负责），创建基础占位版本，组B可在此基础上完善。

### 修改文件清单
- `seckill-mall/src/main/java/com/seckill/mall/converter/package-info.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/converter/SeckillOrderConverter.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/converter/RechargeCardConverter.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/converter/UserConverter.java`（新增）
- `seckill-mall/src/main/java/com/seckill/mall/vo/SeckillOrderVO.java`（新增，基础占位，组B可完善）

### 预期效果
- entity→VO 转换由 MapStruct 自动生成实现类，减少手工代码。
- 脱敏逻辑统一在 `@AfterMapping` 中，避免散落各处不一致。

### 潜在风险
- `SeckillOrderVO` 为基础占位版本，组B后续可能修改字段。若组B修改字段名，需同步更新 `SeckillOrderConverter` 的 `@Mapping`。
- MapStruct 依赖已在 `pom.xml` 中，无需新增依赖。

### 回归测试用例设计
1. 单元测试 `SeckillOrderConverter.INSTANCE.toVO(entity)`，断言 `status` 为 code、`statusDescription` 为中文、`isDeleted` 不存在。
2. 单元测试 `UserConverter.INSTANCE.toVO(entity)`，断言 `phone` 脱敏为 `138****8000` 格式、`email` 脱敏为 `w***@ex.com` 格式。

---

## M-D6 — ProductSkuMapper.xml 使用 SELECT *

### 根因分析
`ProductSkuMapper.xml` 的 `selectEnabledByProductId` 和 `selectByAttributes` 使用 `SELECT *`，
表结构变更时结果集列数/顺序漂移，且可能查出不必要的列（如未来新增 LOB 字段）。

### 修复策略
新增 `<sql id="Base_Column_List">` 显式列出所有列，两个查询用 `<include refid="Base_Column_List"/>` 替代 `SELECT *`。

### 修改文件清单
- `seckill-mall/src/main/resources/mapper/ProductSkuMapper.xml`

### 预期效果
- 查询列固定，表结构变更（加列）不会影响现有查询结果集。
- 避免查出不必要的列，潜在性能提升。

### 潜在风险
- 若表新增列且该列在 entity 中有对应字段，需同步更新 `Base_Column_List`（当前列与 entity 字段完全对齐）。

### 回归测试用例设计
1. 调用商品详情接口，断言 SKU 列表正确返回（price/stock/attributes 等字段非 null）。

---

## M-D7 — PaginationWrapper total 类型不匹配

### 根因分析
`JacksonConfig` 全局将 `Long`/`long` 序列化为 String（防止雪花 ID 在 JS 端精度丢失）。
`PageResult.total` 为 `long` 类型，因此也被序列化为 String，但前端 `PaginationWrapper` 期望 `total` 为 Number，
导致前端分页组件计算异常。

### 修复策略
将 `PageResult.total` 字段从 `long` 改为 `int`。Jackson 不会对 `int`/`Integer` 应用 `ToStringSerializer`，
因此序列化为 JSON number。`of` 方法仍接受 `long` 入参以兼容 MyBatis-Plus `IPage.getTotal()`，内部窄化为 `int`。

### 修改文件清单
- `seckill-mall/src/main/java/com/seckill/mall/common/PageResult.java`
- `seckill-mall/src/test/java/com/seckill/mall/service/ProductServiceTest.java`（同步：`isEqualTo(1L)` → `isEqualTo(1)`）

### 预期效果
- `PageResult.total` 序列化为 JSON number，前端 `PaginationWrapper` 正常工作。

### 潜在风险
- `int` 最大值约 21 亿，分页总数远不会溢出。
- `pageNum`/`pageSize`/`pages` 仍为 `long`（序列化为 String），Bug 报告仅要求改 `total`，未改其余字段。若前端也期望它们为 Number，需后续迭代。

### 回归测试用例设计
1. 调用任意分页接口，断言响应 JSON 中 `total` 字段为 number 类型（无引号）。
2. 运行 `ProductServiceTest`，断言 `result.getTotal()` 断言通过。

---

## 依赖协调说明

### SeckillOrderVO（B4）
- `SeckillOrderVO.java` 由组B负责创建（B4 Bug 要求新增）。
- 组C 为完成 M-D5（MapStruct mapper）创建了基础占位版本，包含从 `SeckillOrder` entity 映射的全部业务字段。
- **组B后续可在此基础上修改/扩展**（如增删字段、调整脱敏）。若组B修改字段名，需同步更新 `SeckillOrderConverter` 的 `@Mapping`。

### MapStruct 依赖
- `pom.xml` 已声明 `mapstruct`（1.5.5.Final）+ `mapstruct-processor` + `lombok-mapstruct-binding`（0.2.0），无需新增依赖。