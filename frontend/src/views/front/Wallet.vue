<template>
  <!-- 我的钱包页面：余额展示 + 充值弹窗 + 交易记录列表 -->
  <div class="wallet-page">
    <!-- 余额展示卡片 -->
    <div class="balance-card" v-loading="balanceLoading">
      <div class="balance-icon">
        <el-icon><Wallet /></el-icon>
      </div>
      <div class="balance-info">
        <div class="balance-label">我的余额 (元)</div>
        <div class="balance-amount">¥ {{ formatMoney(balance) }}</div>
      </div>
      <div class="balance-actions">
        <button class="btn-sm primary" type="button" @click="openRechargeDialog">
          <el-icon class="btn-icon"><Plus /></el-icon>充值
        </button>
      </div>
    </div>

    <!-- 交易记录 -->
    <div class="records-section">
      <div class="section-header">
        <h3 class="section-title">交易记录</h3>
        <button class="btn-sm text" type="button" @click="loadRecords">
          <el-icon><Refresh /></el-icon>刷新
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="recordsLoading" class="loading-state">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span class="loading-text">加载中...</span>
      </div>

      <!-- 空状态 -->
      <div v-else-if="records.length === 0" class="empty-state">
        <el-empty description="暂无交易记录" />
      </div>

      <!-- 记录表格 -->
      <div v-else class="records-table-wrap">
        <table class="records-table">
          <thead>
            <tr>
              <th>类型</th>
              <th>金额</th>
              <th>时间</th>
              <th>备注</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in records" :key="row.id">
              <td>
                <el-tag :type="recordTagType(row.type)" size="small" effect="plain">
                  {{ recordTypeLabel(row.type) }}
                </el-tag>
              </td>
              <td>
                <span class="amount-cell" :class="{ income: row.amount >= 0, expense: row.amount < 0 }">
                  {{ row.amount >= 0 ? '+' : '' }}¥ {{ formatMoney(row.amount) }}
                </span>
              </td>
              <td class="time-cell">{{ formatTime(row.createTime) }}</td>
              <td class="remark-cell">{{ row.remark || '—' }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 充值弹窗 -->
    <el-dialog
      v-model="rechargeDialogVisible"
      title="钱包充值"
      width="460px"
      :close-on-click-modal="false"
      destroy-on-close
      @closed="resetRechargeForm"
    >
      <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeRules" label-width="80px">
        <el-form-item label="卡号" prop="cardNo">
          <el-input
            v-model.trim="rechargeForm.cardNo"
            placeholder="请输入充值卡卡号"
            maxlength="32"
            clearable
          />
        </el-form-item>
        <el-form-item label="卡密" prop="cardPassword">
          <el-input
            v-model.trim="rechargeForm.cardPassword"
            type="password"
            placeholder="请输入充值卡卡密"
            maxlength="64"
            show-password
            clearable
            @keyup.enter="handleRecharge"
          />
        </el-form-item>
        <div class="recharge-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>请输入正确的充值卡卡号和卡密，充值后卡将作废</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rechargeSubmitting" @click="handleRecharge">
          确认充值
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
/**
 * 我的钱包页面 (前台)
 * 余额展示 + 充值卡充值 + 交易记录列表
 * 数据全部从后端 API 获取，无模拟数据。
 */
defineOptions({ name: 'Wallet' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { Wallet, Plus, Refresh, Loading, InfoFilled } from '@element-plus/icons-vue'
import { getWalletBalance, rechargeWallet, getWalletRecords } from '@/api/wallet'
import type { WalletRecordVO } from '@/types'
import dayjs from 'dayjs'

/* === 余额 === */
const balance = ref<number>(0)
const balanceLoading = ref<boolean>(false)

async function loadBalance(): Promise<void> {
  balanceLoading.value = true
  try {
    const res = await getWalletBalance()
    balance.value = res.data?.balance ?? 0
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    balanceLoading.value = false
  }
}

/* === 交易记录 === */
const records = ref<WalletRecordVO[]>([])
const recordsLoading = ref<boolean>(false)

async function loadRecords(): Promise<void> {
  recordsLoading.value = true
  try {
    const res = await getWalletRecords()
    records.value = res.data ?? []
  } catch {
    // 错误已由全局拦截器提示
    records.value = []
  } finally {
    recordsLoading.value = false
  }
}

/* === 充值弹窗 === */
const rechargeDialogVisible = ref<boolean>(false)
const rechargeSubmitting = ref<boolean>(false)
const rechargeFormRef = ref<FormInstance | null>(null)

const rechargeForm = reactive({
  cardNo: '',
  cardPassword: ''
})

const rechargeRules: FormRules = {
  cardNo: [{ required: true, message: '请输入充值卡卡号', trigger: 'blur' }],
  cardPassword: [{ required: true, message: '请输入充值卡卡密', trigger: 'blur' }]
}

function openRechargeDialog(): void {
  resetRechargeForm()
  rechargeDialogVisible.value = true
}

function resetRechargeForm(): void {
  rechargeForm.cardNo = ''
  rechargeForm.cardPassword = ''
  rechargeFormRef.value?.clearValidate()
}

async function handleRecharge(): Promise<void> {
  if (!rechargeFormRef.value) return
  try {
    await rechargeFormRef.value.validate()
  } catch {
    return
  }
  rechargeSubmitting.value = true
  try {
    await rechargeWallet({
      cardNo: rechargeForm.cardNo.trim(),
      cardPassword: rechargeForm.cardPassword.trim()
    })
    ElMessage.success('充值成功')
    rechargeDialogVisible.value = false
    // 充值成功后刷新余额和记录
    await Promise.all([loadBalance(), loadRecords()])
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    rechargeSubmitting.value = false
  }
}

/* === 工具函数 === */
function formatMoney(value: number): string {
  const num = Number(value || 0)
  return num.toFixed(2)
}

function formatTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/** 交易类型标签文本 */
function recordTypeLabel(type: string): string {
  const map: Record<string, string> = {
    RECHARGE: '充值',
    CONSUME: '消费',
    REFUND: '退款',
    WITHDRAW: '提现'
  }
  return map[type] || type || '未知'
}

/** 交易类型标签样式 */
function recordTagType(type: string): 'success' | 'warning' | 'danger' | 'info' {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    RECHARGE: 'success',
    CONSUME: 'danger',
    REFUND: 'warning',
    WITHDRAW: 'info'
  }
  return map[type] || 'info'
}

onMounted(() => {
  loadBalance()
  loadRecords()
})
</script>

<style scoped>
.wallet-page {
  padding: 24px;
}

/* === 余额展示卡片 === */
.balance-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: linear-gradient(135deg, var(--color-primary), #d32f2f);
  color: #fff;
  border-radius: 12px;
  padding: 28px 32px;
  margin-bottom: 24px;
  box-shadow: 0 6px 20px rgba(229, 57, 53, 0.25);
}

.balance-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
}

.balance-info {
  flex: 1;
  min-width: 0;
}

.balance-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}

.balance-amount {
  font-size: 32px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.balance-actions {
  flex-shrink: 0;
}

/* === 交易记录区 === */
.records-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

/* === 加载/空状态 === */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
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

/* === 记录表格 === */
.records-table-wrap {
  overflow-x: auto;
}

.records-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.records-table thead th {
  background: var(--color-bg-subtle);
  padding: 10px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 12px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  letter-spacing: 0.02em;
}

.records-table tbody td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
  color: var(--color-text-primary);
}

.records-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.records-table tbody tr:last-child td {
  border-bottom: none;
}

.amount-cell {
  font-weight: 700;
  font-size: 14px;
}

.amount-cell.income {
  color: var(--color-success);
}

.amount-cell.expense {
  color: var(--color-danger);
}

.time-cell {
  color: var(--color-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.remark-cell {
  color: var(--color-text-secondary);
  max-width: 240px;
  word-break: break-all;
}

/* === 充值提示 === */
.recharge-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  padding: 8px 12px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.recharge-tip .el-icon {
  color: var(--color-primary);
  font-size: 14px;
  flex-shrink: 0;
}

/* === 小按钮 === */
.btn-sm {
  padding: 8px 18px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  transition: background 0.2s, color 0.2s, border-color 0.2s;
}

.btn-sm.primary {
  background: #fff;
  color: var(--color-primary);
  border-color: #fff;
}

.btn-sm.primary:hover {
  background: rgba(255, 255, 255, 0.9);
}

.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
  padding: 6px 10px;
}

.btn-sm.text:hover {
  color: var(--color-primary);
}

.btn-icon {
  font-size: 14px;
}

/* === 响应式 === */
@media (max-width: 768px) {
  .wallet-page {
    padding: 16px;
  }

  .balance-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 20px;
    gap: 16px;
  }

  .balance-amount {
    font-size: 26px;
  }
}
</style>