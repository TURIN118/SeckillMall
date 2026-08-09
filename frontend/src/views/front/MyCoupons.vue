<template>
  <!-- 我的优惠券页面：Tab 切换 + 优惠券卡片展示 -->
  <div class="coupons-page">
    <!-- 页头 -->
    <div class="page-header">
      <h2 class="page-title">我的优惠券</h2>
    </div>

    <!-- Tab 切换 -->
    <div class="coupon-tabs">
      <div v-for="tab in tabs" :key="tab.value" class="coupon-tab" :class="{ active: activeTab === tab.value }"
        @click="switchTab(tab.value)">
        {{ tab.label }}
        <span class="tab-count">({{ tabCountMap[tab.value] || 0 }})</span>
      </div>
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
      <el-empty :description="emptyText" />
    </div>

    <!-- 优惠券卡片网格 -->
    <el-row v-else :gutter="16" class="coupon-grid">
      <el-col v-for="item in couponList" :key="item.id" :xs="24" :sm="12" :md="12" :lg="8" :xl="6" class="coupon-col">
        <div class="coupon-card" :class="couponCardClass(item)">
          <!-- 左侧面额区 -->
          <div class="coupon-left">
            <template v-if="item.couponType === 'AMOUNT'">
              <div class="coupon-value">
                <span class="value-unit">¥</span>
                <span class="value-num">{{ formatAmount(item.couponAmount) }}</span>
              </div>
              <div class="coupon-type-label">满减券</div>
            </template>
            <template v-else>
              <div class="coupon-value">
                <span class="value-num discount">{{ formatDiscount(item.couponAmount) }}</span>
                <span class="value-unit">折</span>
              </div>
              <div class="coupon-type-label">折扣券</div>
            </template>
          </div>

          <!-- 右侧信息区 -->
          <div class="coupon-right">
            <div class="coupon-name">{{ item.couponName }}</div>
            <div class="coupon-condition">
              <template v-if="item.couponType === 'AMOUNT'">
                满 {{ formatMoney(item.minAmount) }} 元可用
              </template>
              <template v-else>
                <span v-if="item.minAmount > 0">
                  满 {{ formatMoney(item.minAmount) }} 元可用
                </span>
                <span v-else>无门槛</span>
              </template>
            </div>
            <div class="coupon-time">
              {{ formatDate(item.couponStartTime) }} ~ {{ formatDate(item.couponEndTime) }}
            </div>
            <div class="coupon-status">
              <el-tag :type="statusTagType(item.status)" size="small" effect="dark">
                {{ statusLabel(item.status) }}
              </el-tag>
            </div>
          </div>

          <!-- 已使用/已过期标记 (右上角) -->
          <div v-if="item.status !== 'UNUSED'" class="coupon-mark">
            <span class="mark-text">{{ statusLabel(item.status) }}</span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
/**
 * 我的优惠券页面 (前台)
 * Tab 切换: 可用(UNUSED) / 已用(USED) / 已过期(EXPIRED)
 * 数据从 /api/v1/coupons/mine 获取，无模拟数据。
 */
defineOptions({ name: 'MyCoupons' })
import { ref, onMounted } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import { getMyCoupons } from '@/api/coupon'
import type { UserCouponVO, UserCouponStatus } from '@/types'
import dayjs from 'dayjs'

/* === Tab 配置 === */
interface TabItem {
  label: string
  value: UserCouponStatus
}

const tabs: TabItem[] = [
  { label: '可用', value: 'UNUSED' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' }
]

const activeTab = ref<UserCouponStatus>('UNUSED')
const couponList = ref<UserCouponVO[]>([])
const loading = ref<boolean>(false)

/** 各 Tab 的数量统计 (后端未提供统计接口, 这里用切换时拉取的长度做简单展示) */
const tabCountMap = ref<Record<UserCouponStatus, number>>({
  UNUSED: 0,
  USED: 0,
  EXPIRED: 0
})

const emptyText = ref<string>('暂无可用优惠券')

/* === 拉取优惠券列表 === */
async function loadCoupons(status: UserCouponStatus): Promise<void> {
  loading.value = true
  try {
    const res = await getMyCoupons(status)
    couponList.value = res.data ?? []
    tabCountMap.value[status] = couponList.value.length
  } catch {
    // 错误已由全局拦截器提示
    couponList.value = []
  } finally {
    loading.value = false
  }
}

/* === 切换 Tab === */
async function switchTab(status: UserCouponStatus): Promise<void> {
  if (status === activeTab.value) return
  activeTab.value = status
  await loadCoupons(status)
}

/* === 初始化: 拉取三个 Tab 的数量 + 默认 Tab 数据 === */
async function init(): Promise<void> {
  // 并行拉取三个状态的数量，用于 Tab 角标显示
  const statuses: UserCouponStatus[] = ['UNUSED', 'USED', 'EXPIRED']
  await Promise.all(statuses.map((s) => loadCoupons(s)))
  // 重新加载当前 Tab 数据
  await loadCoupons(activeTab.value)
}

/* === 工具函数 === */
function formatMoney(value: number): string {
  return Number(value || 0).toFixed(2)
}

function formatAmount(value: number): string {
  const num = Number(value || 0)
  // 整数显示不带小数
  return num % 1 === 0 ? String(num) : num.toFixed(2)
}

function formatDiscount(value: number): string {
  // 折扣率: 8.5 表示 8.5 折
  const num = Number(value || 0)
  return num % 1 === 0 ? String(num) : num.toFixed(1)
}

function formatDate(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD')
}

function statusLabel(status: UserCouponStatus): string {
  const map: Record<UserCouponStatus, string> = {
    UNUSED: '未使用',
    USED: '已使用',
    EXPIRED: '已过期'
  }
  return map[status] || status
}

function statusTagType(status: UserCouponStatus): 'success' | 'info' | 'warning' {
  const map: Record<UserCouponStatus, 'success' | 'info' | 'warning'> = {
    UNUSED: 'success',
    USED: 'info',
    EXPIRED: 'warning'
  }
  return map[status] || 'info'
}

/** 卡片样式: 已使用/已过期置灰 */
function couponCardClass(item: UserCouponVO): string {
  if (item.status === 'USED') return 'is-used'
  if (item.status === 'EXPIRED') return 'is-expired'
  return ''
}

onMounted(() => {
  init()
})
</script>

<style scoped>
.coupons-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

/* === Tab 切换 === */
.coupon-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 20px;
  background: var(--color-bg-card);
  border-radius: 8px 8px 0 0;
  padding: 0 8px;
}

.coupon-tab {
  padding: 12px 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.coupon-tab:hover {
  color: var(--color-primary);
}

.coupon-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tab-count {
  font-size: 12px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.coupon-tab.active .tab-count {
  color: var(--color-primary);
  opacity: 0.8;
}

/* === 加载/空状态 === */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 24px;
  color: var(--color-text-muted);
  gap: 12px;
}

.loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.empty-state {
  padding: 40px 24px;
  display: flex;
  justify-content: center;
}

/* === 优惠券卡片网格 === */
.coupon-grid {
  margin-left: 0 !important;
  margin-right: 0 !important;
}

.coupon-col {
  margin-bottom: 16px;
}

/* === 优惠券卡片 (左面额 + 右信息) === */
.coupon-card {
  position: relative;
  display: flex;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.25s ease, transform 0.25s ease;
  height: 130px;
}

.coupon-card:hover {
  box-shadow: 0 8px 24px rgba(229, 57, 53, 0.15);
  transform: translateY(-2px);
}

/* 左侧面额区: 红色渐变背景 */
.coupon-left {
  width: 130px;
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-primary), #d32f2f);
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  position: relative;
}

/* 左右分隔的锯齿效果 (用伪元素) */
.coupon-left::after {
  content: '';
  position: absolute;
  right: -6px;
  top: 0;
  bottom: 0;
  width: 12px;
  background: radial-gradient(circle at 6px 8px, transparent 4px, #fff 4px) repeat-y;
  background-size: 12px 16px;
}

.coupon-value {
  display: flex;
  align-items: baseline;
  gap: 2px;
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

.coupon-type-label {
  font-size: 12px;
  opacity: 0.9;
  letter-spacing: 0.04em;
}

/* 右侧信息区 */
.coupon-right {
  flex: 1;
  min-width: 0;
  padding: 14px 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
}

.coupon-name {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.coupon-condition {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.coupon-time {
  font-size: 12px;
  color: var(--color-text-muted);
}

.coupon-status {
  margin-top: 2px;
}

/* === 已使用/已过期卡片置灰 === */
.coupon-card.is-used .coupon-left,
.coupon-card.is-expired .coupon-left {
  background: linear-gradient(135deg, #bdbdbd, #9e9e9e);
}

.coupon-card.is-used .coupon-name,
.coupon-card.is-expired .coupon-name {
  color: var(--color-text-secondary);
}

/* === 右上角状态标记 === */
.coupon-mark {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 2;
}

.mark-text {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
}

/* === 响应式 === */
@media (max-width: 768px) {
  .coupons-page {
    padding: 16px;
  }

  .coupon-card {
    height: 120px;
  }

  .coupon-left {
    width: 110px;
  }

  .value-num {
    font-size: 28px;
  }
}
</style>