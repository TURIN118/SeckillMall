# AI P0 上线 Go/No-Go 检查清单

> 秒杀商城 AI 现代化改造 P0 阶段上线 Gate 检查清单
> 
> 生成时间：2026-08-14 | 负责人：@WNJ | 阶段：T20 上线准备
> 
> **使用说明**：逐项检查，全部为 ✅ 通过后方可放行上线；任一 ⬜ 或 ❌ 不得放行。
> 状态图例：⬜ 待检查 / ✅ 通过 / ❌ 失败 / ⚠️ 风险接受

---

## 1. 单元测试全部通过

- **检查项**：`mvn test` 执行
- **通过标准**：0 失败，0 跳过（除非显式 `@Disabled` 并注明原因）
- **状态**：⬜ 待检查
- **备注**：重点关注 `ai-gateway` 模块单测（RateLimitAdvisor/SemanticCacheAdvisor/AuditAdvisor/FallbackAdvisor/BudgetAdvisor）

---

## 2. 覆盖率达标

- **检查项**：JaCoCo 覆盖率报告（`target/site/jacoco/index.html`）
- **通过标准**：`ai/**` 包行覆盖率 ≥ 78%
- **状态**：⬜ 待检查
- **备注**：核心 Advisor 链路（限流/缓存/审计/降级/预算）应达 85%+，DTO/Config 可放宽

---

## 3. 集成测试全部通过

- **检查项**：`mvn test -Dtest="*IntegrationTest"` 执行（需 Docker 环境）
- **通过标准**：6 套件 0 失败（I-GW-01 ~ I-GW-06，含 `@Disabled` 项）
- **状态**：⬜ 待检查
- **备注**：
  - I-GW-01 正常调用返回 LLM 响应
  - I-GW-02 限流触发降级（`@Disabled`：耗时过长，单测覆盖）
  - I-GW-03 语义缓存命中
  - I-GW-04 LLM 超时降级（`@Disabled`：耗时过长，单测覆盖）
  - I-GW-05 审计记录写入 t_ai_audit
  - I-GW-06 日预算超限降级
  - 运行需 Docker（Testcontainers 编排 MySQL 8.0 / Redis 7 / RabbitMQ 3.13）

---

## 4. E2E 场景全通过

- **检查项**：`SeckillE2ETest` 执行（需 Docker 环境）
- **通过标准**：秒杀完整链路（注册→登录→令牌→下单→轮询→支付→校验）全通过
- **状态**：⬜ 待检查
- **备注**：AI 改造不应破坏现有秒杀主链路，E2E 回归是底线

---

## 5. 压测达标

- **检查项**：WRK/JMeter 压测 AI 网关接口 `/api/v1/admin/ai-gateway/test`
- **通过标准**：
  - P99 延迟 < 3s（含 LLM 调用）
  - 错误率 < 0.1%
  - 限流触发后降级响应 < 50ms
- **状态**：⬜ 待检查
- **备注**：压测时 LLM 出口应 mock 或用低配模型，避免产生真实费用

---

## 6. 安全测试通过

- **检查项**：安全测试套件执行
- **通过标准**：
  - 越权：非 ADMIN 用户调 `/api/v1/admin/ai-gateway/**` 返回 403
  - Prompt Injection：系统提示词不被用户输入覆盖
  - 内容安全：LLM 响应经敏感词过滤，违规内容拦截
- **状态**：⬜ 待检查
- **备注**：参考 `SecurityConfig` 中 `@PreAuthorize("hasRole('ADMIN')")` 配置

---

## 7. 监控指标就绪

- **检查项**：Prometheus + Grafana + 告警规则
- **通过标准**：
  - `/actuator/prometheus` 暴露 AI 指标（ai_call_total/ai_tokens_total/ai_cost_total/ai_cache_hit_rate）
  - Grafana Dashboard 已导入 AI 网关面板
  - 告警规则：5 分钟错误率 > 1% / 日 token > 90% 预算 / 月成本 > 90% 预算
- **状态**：⬜ 待检查
- **备注**：生产 `application-prod.yml` 已暴露 `health,info,metrics,prometheus` 端点

---

## 8. 灰度配置就绪

- **检查项**：灰度发布配置
- **通过标准**：
  - 按用户 ID 取模 5% 灰度（如 `userId % 100 < 5`）
  - 一键回滚开关就绪（Feature Flag 或配置中心切换）
  - 灰度白名单/黑名单可动态调整
- **状态**：⬜ 待检查
- **备注**：灰度期间重点观测 AI 调用成功率、降级率、用户反馈

---

## 9. 数据库迁移完成

- **检查项**：`02_ai_tables.sql` 在生产执行成功
- **通过标准**：
  - 4 张表创建成功：`t_user_event` / `t_ai_audit` / `t_ai_conversation` / `t_ai_message`
  - 表结构验证：字段类型/索引/字符集与 DDL 一致
  - 表空间初始为空（无脏数据）
- **状态**：⬜ 待检查
- **备注**：DDL 使用 `CREATE TABLE IF NOT EXISTS`，可安全重跑；回滚脚本需准备 `DROP TABLE` 语句

---

## 10. LLM 预算配置就绪

- **检查项**：LLM 出口与预算配置
- **通过标准**：
  - `DEEPSEEK_API_KEY` 已注入生产环境（K8s Secret / 配置中心）
  - `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` 已配置
  - 日预算上限 `ai.gateway.budget.daily-token-limit=2000000` 已生效
  - 月预算上限 `ai.gateway.budget.monthly-cost-limit=5000.00` 已生效
  - 预算告警阈值 `degrade-threshold=0.9`（90%）已配置
- **状态**：⬜ 待检查
- **备注**：API Key 严禁硬编码或入仓，`application-prod.yml` 使用 `${DEEPSEEK_API_KEY:?required}` 强制注入

---

## 11. 降级预案演练

- **检查项**：手动降级演练
- **通过标准**：
  - 手动关闭 LLM 出口（网络隔离 / Mock 故障）后，AI 接口返回降级文案而非 500
  - 降级文案按 caller 区分：导购/AIGC/客服/默认
  - 降级后业务主链路（秒杀/下单/支付）不受影响
- **状态**：⬜ 待检查
- **备注**：`FallbackAdvisor` 按 caller 返回兜底文案，`AiGatewayService` try-catch 保证不抛 500

---

## 12. 文档更新

- **检查项**：API 文档与运维 Runbook
- **通过标准**：
  - Swagger/Knife4j API 文档已更新（AI 网关/导购/AIGC/客服接口）
  - 运维 Runbook 已更新（含降级处置/预算超限/限流配置/告警响应）
  - 本 checklist 已逐项检查并标记状态
- **状态**：⬜ 待检查
- **备注**：生产环境 Swagger 关闭（`springdoc.api-docs.enabled=false`），文档以预发/UAT 环境为准

---

## 上线决策

| 决策项 | 结果 |
|--------|------|
| 检查项总数 | 12 |
| 通过数 | ⬜ |
| 失败数 | ⬜ |
| 风险接受数 | ⬜ |
| **最终决策** | **⬜ GO / ❌ NO-GO** |

> **签字**：技术负责人 _________ | 测试负责人 _________ | 运维负责人 _________ | 产品负责人 _________
> 
> **日期**：______ 年 ____ 月 ____ 日