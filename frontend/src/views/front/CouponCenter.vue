<template>
  <!-- 领券中心页面：6列网格展示可领取优惠券 -->
  <div class="coupon-center-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">🎫 领券中心</h1>
      <p class="page-subtitle">领取优惠券，下单更划算</p>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-state">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span class="loading-text">加载中...</span>
    </div>

    <!-- 空状态 -->
    <div v-else-if="couponList.length === 0" class="empty-state">
      <el-empty description="暂无可领取的优惠券" :image-size="120" />
      <button class="btn-go-products" type="button" @click="router.push('/products')">去逛逛</button>
    </div>

    <!-- 优惠券网格 -->
    <template v-else>
      <div class="coupon-count-bar">共 {{ couponList.length }} 张可领取优惠券</div>

      <div class="coupon-grid">
        <div v-for="coupon in couponList" :key="coupon.id" class="coupon-card">
          <!-- 顶部面额区 -->
          <div class="coupon-card-top">
            <div class="coupon-value">
              <template v-if="coupon.type === 'AMOUNT'">
                <span class="value-unit">¥</span>
                <span class="value-num">{{ formatAmount(coupon.amount) }}</span>
              </template>
              <template v-else>
                <span class="value-num discount">{{ formatDiscount(coupon.amount) }}</span>
                <span class="value-unit">折</span>
              </template>
            </div>
            <div class="coupon-type-tag">
              {{ coupon.type === 'AMOUNT' ? '满减券' : '折扣券' }}
            </div>
          </div>

          <!-- 中部信息区 -->
          <div class="coupon-card-body">
            <!-- 使用门槛 -->
            <div class="coupon-condition">
              <template v-if="coupon.minAmount > 0">满 {{ formatMoney(coupon.minAmount) }} 元可用</template>
              <template v-else>无门槛</template>
            </div>

            <!-- 适用范围标签 (通用券不显示) -->
            <div v-if="coupon.scopeLabel" class="coupon-scope-tag">{{ coupon.scopeLabel }}</div>

            <!-- 有效期 -->
            <div class="coupon-time">
              {{ formatDate(coupon.startTime) }} ~ {{ formatDate(coupon.endTime) }}
            </div>

            <!-- 剩余数量 -->
            <div class="coupon-remain" :class="{ low: remainCount(coupon) <= 10 }">
              <template v-if="remainCount(coupon) <= 10">仅剩 {{ remainCount(coupon) }} 张</template>
              <template v-else>剩余 {{ remainCount(coupon) }} 张</template>
            </div>
          </div>

          <!-- 底部领取按钮 -->
          <div class="coupon-card-footer">
            <button
              class="btn-receive"
              :class="{ received: receivedMap[coupon.id] }"
              :disabled="receivedMap[coupon.id] || receivingId === coupon.id"
              @click="handleReceive(coupon)"
            >
              <template v-if="receivedMap[coupon.id]">已领取 ✓</template>
              <template v-else-if="receivingId === coupon.id">领取中...</template>
              <template v-else>立即领取</template>
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * 领券中心页面
 * - 调用 GET /api/v1/coupons/available 获取全部可领取优惠券
 * - 调用 POST /api/v1/coupons/{id}/receive 领取优惠券
 * - 6列网格展示，响应式: 1024px→4列, 768px→2列
 */
defineOptions({ name: 'CouponCenter' })
import { ref, onMounted, onActivated } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { getAvailableCoupons, receiveCoupon } from '@/api/coupon'
import dayjs from 'dayjs'
import type { CouponVO } from '@/types'

const router = useRouter()

/** 可领取优惠券列表 */
const couponList = ref<CouponVO[]>([])

/** 列表加载中 */
const loading = ref<boolean>(false)

/** 正在领取的优惠券 ID (按钮 loading 态) */
const receivingId = ref<number | string | null>(null)

/** 已领取标记 Map (key: couponId, value: true) */
const receivedMap = ref<Record<string, boolean>>({})

/* === 工具函数 === */

/** 格式化满减金额 (整数显示不带小数, 非整数保留两位) */
function formatAmount(value: number): string {
  const n = Number(value || 0)
  return Number.isInteger(n) ? String(n) : n.toFixed(2)
}

/** 格式化折扣率 (0.85 → 8.5) */
function formatDiscount(value: number): string {
  const n = Number(value || 0) * 10
  return n.toFixed(1).replace(/\.0$/, '')
}

/** 格式化金额 (保留两位小数) */
function formatMoney(value: number): string {
  return Number(value || 0).toFixed(2)
}

/** 格式化日期 (仅年月日) */
function formatDate(time: string | null | undefined): string {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD')
}

/** 计算剩余数量 */
function remainCount(coupon: CouponVO): number {
  return Math.max(0, (coupon.totalCount || 0) - (coupon.receivedCount || 0))
}

/* === 数据加载 === */

/** 加载可领取优惠券列表 */
async function loadCouponList(): Promise<void> {
  loading.value = true
  try {
    const res = await getAvailableCoupons()
    couponList.value = res.data ?? []
  } catch {
    couponList.value = []
  } finally {
    loading.value = false
  }
}

/* === 事件处理 === */

/** 领取优惠券 */
async function handleReceive(coupon: CouponVO): Promise<void> {
  if (receivedMap.value[coupon.id]) return
  receivingId.value = coupon.id
  try {
    await receiveCoupon(coupon.id)
    // 标记为已领取 (按钮变灰色态)
    receivedMap.value[coupon.id] = true
    // 乐观更新剩余数量
    coupon.receivedCount = (coupon.receivedCount || 0) + 1
    ElMessage.success('优惠券领取成功')
  } catch {
    // 错误已由请求拦截器统一提示
  } finally {
    receivingId.value = null
  }
}

/* === 生命周期 === */

onMounted(() => {
  loadCouponList()
})

// keep-alive 缓存后, 再次激活时刷新列表 (保证从其他页面返回时数据新鲜)
onActivated(() => {
  loadCouponList()
})
</script>

<style scoped>
/* === 页面容器 (与首页一致: padding 24px, 无 max-width) === */
.coupon-center-page {
  padding: 24px;
  padding-bottom: 80px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  color: var(--color-text-primary, #1a1a2e);
}

/* === 页面头部 === */
.page-header {
  margin-bottom: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, var(--color-primary, #e53935), #ff6d00);
  border-radius: var(--radius-lg, 12px);
  color: #fff;
  box-shadow: 0 4px 16px rgba(229, 57, 53, 0.2);
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  opacity: 0.9;
}

/* === 加载中状态 === */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: var(--color-text-muted, #9ca3af);
  gap: 12px;
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-lg, 12px);
}

.loading-text {
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
}

/* === 空状态 === */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px 40px;
  text-align: center;
  gap: 16px;
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-lg, 12px);
}

.btn-go-products {
  padding: 8px 20px;
  border-radius: var(--radius-md, 6px);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
  background: var(--color-primary, #e53935);
  color: #fff;
  letter-spacing: 0.02em;
  transition: background 0.15s;
}

.btn-go-products:hover {
  background: var(--btn-hover, #c62828);
}

/* === 数量提示条 === */
.coupon-count-bar {
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
  margin-bottom: 16px;
}

/* === 优惠券网格 (6列) === */
.coupon-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}

/* === 优惠券卡片 === */
.coupon-card {
  background: var(--color-bg-card, #fff);
  border: 1px solid var(--color-border, #e5e7eb);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: box-shadow 0.2s, transform 0.2s;
}

.coupon-card:hover {
  box-shadow: 0 8px 24px rgba(229, 57, 53, 0.12);
  transform: translateY(-4px);
}

/* === 顶部面额区 (红色渐变背景) === */
.coupon-card-top {
  background: linear-gradient(135deg, var(--color-primary, #e53935), #d32f2f);
  color: #fff;
  padding: 16px 12px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.coupon-value {
  display: flex;
  align-items: baseline;
  gap: 2px;
  line-height: 1;
}

.value-unit {
  font-size: 14px;
  font-weight: 600;
}

.value-num {
  font-size: 32px;
  font-weight: 800;
  line-height: 1;
}

.value-num.discount {
  font-size: 36px;
}

.coupon-type-tag {
  font-size: 12px;
  opacity: 0.9;
  letter-spacing: 0.04em;
  padding: 2px 10px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 10px;
}

/* === 中部信息区 === */
.coupon-card-body {
  padding: 12px 12px 8px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.coupon-condition {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary, #1f2937);
}

/* 适用范围标签 (红色小标签) */
.coupon-scope-tag {
  display: inline-block;
  align-self: flex-start;
  font-size: 11px;
  font-weight: 600;
  color: var(--color-primary, #e53935);
  background: var(--color-primary-light, rgba(229, 57, 53, 0.08));
  border-radius: 4px;
  padding: 2px 6px;
  line-height: 1.4;
}

.coupon-time {
  font-size: 12px;
  color: var(--color-text-muted, #9ca3af);
}

.coupon-remain {
  font-size: 12px;
  color: var(--color-text-secondary, #6b7280);
}

.coupon-remain.low {
  color: var(--color-primary, #e53935);
  font-weight: 600;
}

/* === 底部领取按钮 === */
.coupon-card-footer {
  padding: 8px 12px 12px;
}

.btn-receive {
  width: 100%;
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 600;
  color: #fff;
  background: var(--color-primary, #e53935);
  border: none;
  border-radius: var(--radius-md, 6px);
  cursor: pointer;
  transition: background 0.15s;
  letter-spacing: 0.02em;
}

.btn-receive:hover:not(:disabled) {
  background: var(--btn-hover, #c62828);
}

.btn-receive:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

/* 已领取按钮: 灰色禁用态 */
.btn-receive.received {
  background: var(--color-bg-subtle, #f5f5f5);
  color: var(--color-text-muted, #9ca3af);
  border: 1px solid var(--color-border, #e5e7eb);
  opacity: 1;
}

/* === 响应式 === */
@media (max-width: 1200px) {
  .coupon-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 1024px) {
  .coupon-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

@media (max-width: 768px) {
  .coupon-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }

  .page-title {
    font-size: 18px;
  }

  .value-num {
    font-size: 28px;
  }

  .value-num.discount {
    font-size: 32px;
  }
}

@media (max-width: 480px) {
  .coupon-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }
}
</style>