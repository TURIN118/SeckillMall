<template>
  <!-- 严格对照 index.html .profile-layout 样式 -->
  <div class="profile-page">
    <!-- 加载骨架屏 -->
    <div v-if="loading" class="loading-wrap">
      <div v-for="i in 6" :key="i" class="skeleton-line"></div>
    </div>

    <div v-else-if="user" class="profile-layout">
      <!-- 左列: 用户信息卡片 -->
      <div class="profile-card">
        <div class="profile-avatar">{{ avatarText }}</div>
        <div class="profile-name">{{ user.nickname || user.username }}</div>
        <div class="profile-phone">{{ maskedPhone }}</div>
        <span class="role-badge" :class="roleClass">{{ roleLabel }}</span>
        <dl class="profile-meta">
          <div class="profile-meta-row">
            <dt>用户名</dt>
            <dd>{{ user.username }}</dd>
          </div>
          <div class="profile-meta-row">
            <dt>邮箱</dt>
            <dd>{{ user.email || '—' }}</dd>
          </div>
          <div class="profile-meta-row">
            <dt>注册时间</dt>
            <dd>{{ formatDate(user.createTime) }}</dd>
          </div>
          <div class="profile-meta-row">
            <dt>账号状态</dt>
            <dd>{{ user.status === 'ACTIVE' ? '正常' : '已禁用' }}</dd>
          </div>
        </dl>
        <!-- 快捷入口 -->
        <div class="profile-shortcuts">
          <div class="shortcut-item" @click="activeTab = 'wallet'">
            <el-icon>
              <Wallet />
            </el-icon>
            <span>我的钱包</span>
          </div>
          <div class="shortcut-item" @click="activeTab = 'coupons'">
            <el-icon>
              <Ticket />
            </el-icon>
            <span>我的优惠券</span>
          </div>
          <div class="shortcut-item" @click="activeTab = 'address'">
            <el-icon>
              <Location />
            </el-icon>
            <span>收货地址</span>
          </div>
          <div class="shortcut-item" @click="goTo('/user/orders')">
            <el-icon>
              <List />
            </el-icon>
            <span>我的订单</span>
          </div>
        </div>
      </div>

      <!-- 右列: 标签页内容 -->
      <div class="profile-main">
        <!-- 标签页 -->
        <div class="profile-tabs">
          <div class="profile-tab" :class="{ active: activeTab === 'info' }" @click="activeTab = 'info'">基本信息</div>
          <div class="profile-tab" :class="{ active: activeTab === 'password' }" @click="activeTab = 'password'">修改密码
          </div>
          <div class="profile-tab" :class="{ active: activeTab === 'wallet' }" @click="activeTab = 'wallet'">我的钱包
          </div>
          <div class="profile-tab" :class="{ active: activeTab === 'address' }" @click="activeTab = 'address'">收货地址
          </div>
          <div class="profile-tab" :class="{ active: activeTab === 'coupons' }" @click="activeTab = 'coupons'">我的优惠券
          </div>
        </div>

        <!-- Tab 1: 基本信息 -->
        <div v-if="activeTab === 'info'" class="profile-form">
          <div class="info-card">
            <!-- 只读展示模式 -->
            <template v-if="!editing">
              <div class="info-row">
                <div class="info-label">用户名</div>
                <div class="info-value">{{ user.username }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">昵称</div>
                <div class="info-value">{{ user.nickname || '—' }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">手机号</div>
                <div class="info-value">{{ maskedPhone }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">邮箱</div>
                <div class="info-value">{{ user.email || '—' }}</div>
              </div>
              <div class="info-row">
                <div class="info-label">注册时间</div>
                <div class="info-value">{{ formatTime(user.createTime) }}</div>
              </div>
              <div class="info-actions">
                <button class="btn-sm primary" type="button" @click="enterEdit">修改信息</button>
              </div>
            </template>

            <!-- 编辑模式 -->
            <template v-else>
              <div class="info-row">
                <div class="info-label">用户名</div>
                <div class="info-value readonly">{{ user.username }}</div>
              </div>
              <div class="info-row">
                <label class="info-label" for="edit-nickname">昵称</label>
                <input id="edit-nickname" v-model.trim="editForm.nickname" class="form-input" type="text"
                  placeholder="请输入昵称" maxlength="20" />
              </div>
              <div class="info-row">
                <label class="info-label" for="edit-phone">手机号</label>
                <div class="info-edit-cell">
                  <input id="edit-phone" v-model.trim="editForm.phone" class="form-input"
                    :class="{ error: editErrors.phone }" type="text" placeholder="请输入 11 位手机号" maxlength="11" />
                  <div v-if="editErrors.phone" class="form-error">{{ editErrors.phone }}</div>
                  <!-- 验证码输入 + 发送按钮 (仅手机号变更时需要) -->
                  <div v-if="isPhoneChanged" class="verify-row">
                    <input v-model.trim="editForm.phoneCode" class="form-input verify-input"
                      :class="{ error: editErrors.phoneCode }" type="text" placeholder="请输入短信验证码" maxlength="6" />
                    <button class="btn-sm primary verify-btn" type="button"
                      :disabled="phoneCountdown > 0 || sendingPhoneCode" @click="handleSendPhoneCode">
                      {{ phoneCountdown > 0 ? `${phoneCountdown}s` : (sendingPhoneCode ? '发送中' : '发送验证码') }}
                    </button>
                  </div>
                  <div v-if="editErrors.phoneCode" class="form-error">{{ editErrors.phoneCode }}</div>
                </div>
              </div>
              <div class="info-row">
                <label class="info-label" for="edit-email">邮箱</label>
                <div class="info-edit-cell">
                  <input id="edit-email" v-model.trim="editForm.email" class="form-input"
                    :class="{ error: editErrors.email }" type="text" placeholder="请输入邮箱 (可选)" />
                  <div v-if="editErrors.email" class="form-error">{{ editErrors.email }}</div>
                  <!-- 验证码输入 + 发送按钮 (仅邮箱变更时需要) -->
                  <div v-if="isEmailChanged" class="verify-row">
                    <input v-model.trim="editForm.emailCode" class="form-input verify-input"
                      :class="{ error: editErrors.emailCode }" type="text" placeholder="请输入邮箱验证码" maxlength="6" />
                    <button class="btn-sm primary verify-btn" type="button"
                      :disabled="emailCountdown > 0 || sendingEmailCode" @click="handleSendEmailCode">
                      {{ emailCountdown > 0 ? `${emailCountdown}s` : (sendingEmailCode ? '发送中' : '发送验证码') }}
                    </button>
                  </div>
                  <div v-if="editErrors.emailCode" class="form-error">{{ editErrors.emailCode }}</div>
                </div>
              </div>
              <div class="info-row">
                <div class="info-label">注册时间</div>
                <div class="info-value readonly">{{ formatTime(user.createTime) }}</div>
              </div>
              <div class="info-actions">
                <button class="btn-sm primary" type="button" :disabled="editLoading" @click="handleSaveProfile">
                  {{ editLoading ? '保存中...' : '保存' }}
                </button>
                <button class="btn-sm" type="button" :disabled="editLoading" @click="cancelEdit">取消</button>
              </div>
            </template>
          </div>
        </div>

        <!-- Tab 2: 修改密码 -->
        <div v-else-if="activeTab === 'password'" class="profile-form">
          <div class="pwd-card">
            <form @submit.prevent="handleChangePassword">
              <div class="form-group">
                <label class="form-label">旧密码</label>
                <input v-model.trim="pwdForm.oldPassword" class="form-input" :class="{ error: pwdErrors.oldPassword }"
                  type="password" placeholder="请输入当前密码" autocomplete="current-password" />
                <div v-if="pwdErrors.oldPassword" class="form-error">{{ pwdErrors.oldPassword }}</div>
              </div>

              <div class="form-group">
                <label class="form-label">新密码</label>
                <input v-model.trim="pwdForm.newPassword" class="form-input" :class="{ error: pwdErrors.newPassword }"
                  type="password" placeholder="请输入新密码 (6-20位)" autocomplete="new-password" />
                <!-- 密码强度指示器 -->
                <div class="strength-bars">
                  <div class="strength-bar" :class="{ active: passwordStrength >= 1, weak: passwordStrength === 1 }">
                  </div>
                  <div class="strength-bar" :class="{ active: passwordStrength >= 2, mid: passwordStrength === 2 }">
                  </div>
                  <div class="strength-bar" :class="{ active: passwordStrength >= 3, strong: passwordStrength === 3 }">
                  </div>
                </div>
                <div class="strength-text" :class="strengthClass">密码强度：{{ strengthLabel }}</div>
                <div v-if="pwdErrors.newPassword" class="form-error">{{ pwdErrors.newPassword }}</div>
              </div>

              <div class="form-group">
                <label class="form-label">确认新密码</label>
                <input v-model.trim="pwdForm.confirmPassword" class="form-input"
                  :class="{ error: pwdErrors.confirmPassword }" type="password" placeholder="请再次输入新密码"
                  autocomplete="new-password" />
                <div v-if="pwdErrors.confirmPassword" class="form-error">{{ pwdErrors.confirmPassword }}</div>
              </div>

              <!-- Bug2修复: 修改密码需验证码校验, 发送到当前用户邮箱 -->
              <div class="form-group">
                <label class="form-label">验证码</label>
                <div class="verify-row">
                  <input v-model.trim="pwdForm.code" class="form-input verify-input" :class="{ error: pwdErrors.code }"
                    type="text" placeholder="请输入邮箱验证码" maxlength="6" />
                  <button class="btn-sm primary verify-btn" type="button"
                    :disabled="pwdCodeCountdown > 0 || sendingPwdCode" @click="handleSendPwdCode">
                    {{ pwdCodeCountdown > 0 ? `${pwdCodeCountdown}s` : (sendingPwdCode ? '发送中' : '发送验证码') }}
                  </button>
                </div>
                <div v-if="pwdErrors.code" class="form-error">{{ pwdErrors.code }}</div>
                <div v-if="!user?.email" class="form-tip" style="color: #e6a23c;">当前账号未绑定邮箱，无法发送验证码</div>
              </div>

              <div class="form-actions">
                <button class="btn-sm primary" type="submit" :disabled="pwdLoading">
                  {{ pwdLoading ? '修改中...' : '修改密码' }}
                </button>
                <button class="btn-sm" type="button" @click="resetPwdForm">重置</button>
              </div>
              <div class="form-tip">修改成功后将自动退出登录，需重新登录</div>
            </form>
          </div>
        </div>

        <!-- Tab 3: 我的钱包 -->
        <div v-else-if="activeTab === 'wallet'" class="wallet-tab">
          <!-- 余额展示卡片 -->
          <div class="wallet-balance-card" v-loading="walletBalanceLoading">
            <div class="wallet-balance-icon">
              <el-icon>
                <Wallet />
              </el-icon>
            </div>
            <div class="wallet-balance-info">
              <div class="wallet-balance-label">我的余额 (元)</div>
              <div class="wallet-balance-amount">¥ {{ formatMoney(walletBalance) }}</div>
            </div>
            <div class="wallet-balance-actions">
              <button class="btn-sm primary wallet-recharge-btn" type="button" @click="openRechargeDialog">
                <el-icon class="btn-icon">
                  <Plus />
                </el-icon>充值
              </button>
            </div>
          </div>

          <!-- 交易记录 (前10条) -->
          <div class="wallet-records-section">
            <div class="wallet-section-header">
              <h3 class="wallet-section-title">交易记录</h3>
              <button class="btn-sm text" type="button" @click="loadWalletRecords">
                <el-icon>
                  <Refresh />
                </el-icon>刷新
              </button>
            </div>

            <!-- 加载中 -->
            <div v-if="walletRecordsLoading" class="wallet-loading-state">
              <el-icon class="is-loading">
                <Loading />
              </el-icon>
              <span class="wallet-loading-text">加载中...</span>
            </div>

            <!-- 空状态 -->
            <div v-else-if="walletRecords.length === 0" class="wallet-empty-state">
              <el-empty description="暂无交易记录" />
            </div>

            <!-- 记录表格 -->
            <div v-else class="wallet-records-table-wrap">
              <table class="wallet-records-table">
                <thead>
                  <tr>
                    <th>类型</th>
                    <th>金额</th>
                    <th>时间</th>
                    <th>备注</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="row in walletRecords.slice(0, 10)" :key="row.id">
                    <td>
                      <el-tag :type="recordTagType(row.type)" size="small" effect="plain">
                        {{ recordTypeLabel(row.type) }}
                      </el-tag>
                    </td>
                    <td>
                      <span class="wallet-amount-cell" :class="{ income: row.amount >= 0, expense: row.amount < 0 }">
                        {{ row.amount >= 0 ? '+' : '' }}¥ {{ formatMoney(row.amount) }}
                      </span>
                    </td>
                    <td class="wallet-time-cell">{{ formatTime(row.createTime) }}</td>
                    <td class="wallet-remark-cell">{{ row.remark || '—' }}</td>
                  </tr>
                </tbody>
              </table>
              <!-- 查看更多 -->
              <div v-if="walletRecords.length > 10" class="wallet-records-footer">
                <button class="btn-sm text" type="button" @click="goTo('/user/wallet')">
                  查看更多 <el-icon>
                    <ArrowRight />
                  </el-icon>
                </button>
              </div>
            </div>
          </div>

          <!-- 充值弹窗 -->
          <el-dialog v-model="rechargeDialogVisible" title="钱包充值" width="460px" :close-on-click-modal="false"
            destroy-on-close @closed="resetRechargeForm">
            <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeRules" label-width="80px">
              <el-form-item label="卡号" prop="cardNo">
                <el-input v-model.trim="rechargeForm.cardNo" placeholder="请输入充值卡卡号" maxlength="32" clearable />
              </el-form-item>
              <el-form-item label="卡密" prop="cardPassword">
                <el-input v-model.trim="rechargeForm.cardPassword" type="password" placeholder="请输入充值卡卡密" maxlength="64"
                  show-password clearable @keyup.enter="handleRecharge" />
              </el-form-item>
              <div class="recharge-tip">
                <el-icon>
                  <InfoFilled />
                </el-icon>
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

        <!-- Tab 4: 收货地址 -->
        <div v-else-if="activeTab === 'address'" class="address-tab">
          <!-- 页头：标题 + 新增按钮 -->
          <div class="address-header">
            <h3 class="address-title">收货地址管理</h3>
            <button class="btn-sm primary" type="button" @click="openAddDialog">
              <span class="btn-plus">+</span> 新增地址
            </button>
          </div>

          <!-- 加载中 -->
          <div v-if="addressLoading" class="address-loading-state">
            <el-icon class="is-loading">
              <Loading />
            </el-icon>
            <span class="address-loading-text">加载中...</span>
          </div>

          <!-- 空状态 -->
          <div v-else-if="addressList.length === 0" class="address-empty-state">
            <div class="address-empty-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="64" height="64">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0118 0z" />
                <circle cx="12" cy="10" r="3" />
              </svg>
            </div>
            <p class="address-empty-text">还没有收货地址，快去添加吧！</p>
            <button class="btn-sm primary" type="button" @click="openAddDialog">新增地址</button>
          </div>

          <!-- 地址列表：响应式卡片网格 -->
          <el-row v-else :gutter="16" class="address-grid">
            <el-col v-for="addr in addressList" :key="addr.id" :xs="24" :sm="24" :md="12" :lg="8" :xl="8"
              class="address-col">
              <div class="address-card" :class="{ 'address-default': addr.isDefault === 1 }">
                <!-- 顶部：姓名 + 手机号 + 默认标签 -->
                <div class="card-top">
                  <div class="card-name-row">
                    <span class="address-name">{{ addr.receiverName }}</span>
                    <span class="address-phone">{{ addr.receiverPhone }}</span>
                    <el-tag v-if="addr.isDefault === 1" type="danger" size="small" effect="dark"
                      class="default-tag">默认</el-tag>
                  </div>
                </div>

                <!-- 中部：详细地址 -->
                <div class="card-detail">
                  <el-icon class="detail-icon">
                    <Location />
                  </el-icon>
                  <span class="detail-text">
                    {{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detailAddress }}
                  </span>
                </div>

                <!-- 底部：操作按钮 -->
                <div class="card-actions">
                  <button v-if="addr.isDefault !== 1" class="btn-sm text" type="button"
                    @click="handleSetDefault(addr)">设为默认</button>
                  <button class="btn-sm text" type="button" @click="openEditDialog(addr)">编辑</button>
                  <button class="btn-sm text danger" type="button" @click="handleDeleteAddress(addr)">删除</button>
                </div>
              </div>
            </el-col>
          </el-row>

          <!-- 新增/编辑弹窗 -->
          <el-dialog v-model="addressDialogVisible" :title="addressDialogTitle" width="500px"
            :close-on-click-modal="false" append-to-body>
            <el-form ref="addressFormRef" :model="addressFormData" :rules="addressFormRules" label-width="100px">
              <el-form-item label="姓名" prop="receiverName">
                <el-input v-model="addressFormData.receiverName" placeholder="请输入收货人姓名" maxlength="20"
                  show-word-limit />
              </el-form-item>
              <el-form-item label="手机号" prop="receiverPhone">
                <el-input v-model="addressFormData.receiverPhone" placeholder="请输入 11 位手机号" maxlength="11" />
              </el-form-item>
              <el-form-item label="省份" prop="province">
                <el-input v-model="addressFormData.province" placeholder="如：北京市" maxlength="30" />
              </el-form-item>
              <el-form-item label="城市" prop="city">
                <el-input v-model="addressFormData.city" placeholder="如：北京市" maxlength="30" />
              </el-form-item>
              <el-form-item label="区/县" prop="district">
                <el-input v-model="addressFormData.district" placeholder="如：海淀区" maxlength="30" />
              </el-form-item>
              <el-form-item label="详细地址" prop="detailAddress">
                <el-input v-model="addressFormData.detailAddress" type="textarea" :rows="2"
                  placeholder="请输入详细地址（街道、门牌号等）" maxlength="200" show-word-limit />
              </el-form-item>
              <el-form-item label="设为默认">
                <el-switch v-model="addressFormData.isDefault" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="addressDialogVisible = false">取消</el-button>
              <el-button type="primary" :loading="addressSubmitting" @click="handleAddressSubmit">确定</el-button>
            </template>
          </el-dialog>
        </div>

        <!-- Tab 5: 我的优惠券 -->
        <div v-else-if="activeTab === 'coupons'" class="coupons-tab">
          <!-- 子Tab切换 -->
          <div class="coupon-tabs">
            <div v-for="tab in couponSubTabs" :key="tab.value" class="coupon-tab"
              :class="{ active: couponActiveSubTab === tab.value }" @click="switchCouponSubTab(tab.value)">
              {{ tab.label }}
              <span class="tab-count">({{ couponTabCountMap[tab.value] || 0 }})</span>
            </div>
          </div>

          <!-- 加载中 -->
          <div v-if="couponLoading" class="coupon-loading-state">
            <el-icon class="is-loading">
              <Loading />
            </el-icon>
            <span class="coupon-loading-text">加载中...</span>
          </div>

          <!-- 空状态 -->
          <div v-else-if="couponList.length === 0" class="coupon-empty-state">
            <el-empty :description="couponEmptyText" />
          </div>

          <!-- 优惠券卡片网格 -->
          <el-row v-else :gutter="16" class="coupon-grid">
            <el-col v-for="item in couponList" :key="item.id" :xs="24" :sm="12" :md="12" :lg="8" :xl="6"
              class="coupon-col">
              <div class="coupon-card" :class="couponCardClass(item)">
                <!-- 左侧面额区 -->
                <div class="coupon-left">
                  <template v-if="item.coupon.type === 'AMOUNT'">
                    <div class="coupon-value">
                      <span class="value-unit">¥</span>
                      <span class="value-num">{{ formatCouponAmount(item.coupon.amount) }}</span>
                    </div>
                    <div class="coupon-type-label">满减券</div>
                  </template>
                  <template v-else>
                    <div class="coupon-value">
                      <span class="value-num discount">{{ formatCouponDiscount(item.coupon.amount) }}</span>
                      <span class="value-unit">折</span>
                    </div>
                    <div class="coupon-type-label">折扣券</div>
                  </template>
                </div>

                <!-- 右侧信息区 -->
                <div class="coupon-right">
                  <div class="coupon-name">{{ item.coupon.name }}</div>
                  <div class="coupon-condition">
                    <template v-if="item.coupon.type === 'AMOUNT'">
                      满 {{ formatCouponMoney(item.coupon.minAmount) }} 元可用
                    </template>
                    <template v-else>
                      <span v-if="item.coupon.minAmount > 0">
                        满 {{ formatCouponMoney(item.coupon.minAmount) }} 元可用
                      </span>
                      <span v-else>无门槛</span>
                    </template>
                  </div>
                  <div class="coupon-time">
                    {{ formatCouponDate(item.coupon.startTime) }} ~ {{ formatCouponDate(item.coupon.endTime) }}
                  </div>
                  <div class="coupon-status">
                    <el-tag :type="couponStatusTagType(item.status)" size="small" effect="dark">
                      {{ couponStatusLabel(item.status) }}
                    </el-tag>
                  </div>
                </div>

                <!-- 已使用/已过期标记 (右上角) -->
                <div v-if="item.status !== 'UNUSED'" class="coupon-mark">
                  <span class="mark-text">{{ couponStatusLabel(item.status) }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * P09 个人中心
 * 严格对照 index.html .profile-layout / .profile-card / .profile-tabs 样式
 * 钱包 Tab 整合: 余额卡片 + 充值弹窗 + 交易记录前10条
 */
defineOptions({ name: 'UserProfile' })
import { ref, reactive, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  Wallet,
  Ticket,
  Location,
  List,
  Plus,
  Refresh,
  Loading,
  InfoFilled,
  ArrowRight
} from '@element-plus/icons-vue'
import { changePassword, updateProfile, updatePhone, updateEmail } from '@/api/auth'
import { sendSmsCode, sendEmailCode } from '@/api/verification'
import { getWalletBalance, rechargeWallet, getWalletRecords } from '@/api/wallet'
import {
  getAddressList,
  createAddress,
  updateAddress,
  deleteAddress,
  setDefaultAddress
} from '@/api/address'
import { getMyCoupons } from '@/api/coupon'
import { useUserStore } from '@/stores/user'
import type {
  WalletRecordVO,
  UserAddressVO,
  UserAddressRequest,
  UserCouponVO,
  UserCouponStatus
} from '@/types'
import dayjs from 'dayjs'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

/** 快捷入口跳转 */
function goTo(path: string): void {
  router.push(path)
}

const loading = ref<boolean>(false)
const activeTab = ref<string>('info')
const pwdLoading = ref<boolean>(false)

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  code: ''
})

const pwdErrors = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  code: ''
})

/* === Bug2修复: 修改密码验证码倒计时 === */
const pwdCodeCountdown = ref<number>(0)
const sendingPwdCode = ref<boolean>(false)
let pwdCodeTimer: ReturnType<typeof setInterval> | null = null

/* === 基本信息: 编辑模式 === */
const editing = ref<boolean>(false)
const editLoading = ref<boolean>(false)
const editForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  phoneCode: '',
  emailCode: ''
})
const editErrors = reactive({
  nickname: '',
  phone: '',
  email: '',
  phoneCode: '',
  emailCode: ''
})

/* === 验证码倒计时 (60s) === */
const phoneCountdown = ref<number>(0)
const emailCountdown = ref<number>(0)
const sendingPhoneCode = ref<boolean>(false)
const sendingEmailCode = ref<boolean>(false)
let phoneTimer: ReturnType<typeof setInterval> | null = null
let emailTimer: ReturnType<typeof setInterval> | null = null

/** 手机号是否变更 (变更才需要验证码) */
const isPhoneChanged = computed<boolean>(() => {
  return editForm.phone.trim() !== (user.value?.phone || '')
})

/** 邮箱是否变更 (变更才需要验证码) */
const isEmailChanged = computed<boolean>(() => {
  return editForm.email.trim() !== (user.value?.email || '')
})

const user = computed(() => userStore.userInfo)

const avatarText = computed<string>(() => {
  const name = user.value?.nickname || user.value?.username || ''
  return name.charAt(0).toUpperCase()
})

const maskedPhone = computed<string>(() => {
  const phone = user.value?.phone || ''
  if (phone.length === 11) {
    return phone.substring(0, 3) + '****' + phone.substring(7)
  }
  return phone || '—'
})

const roleLabel = computed<string>(() => {
  const map: Record<string, string> = {
    BUYER: '普通买家',
    SELLER: '卖家',
    ADMIN: '管理员'
  }
  return map[user.value?.role || ''] || '用户'
})

/** 角色徽章 class：admin 红 / seller 橙 / buyer 蓝 */
const roleClass = computed<string>(() => {
  const map: Record<string, string> = {
    BUYER: 'buyer',
    SELLER: 'seller',
    ADMIN: 'admin'
  }
  return map[user.value?.role || ''] || 'buyer'
})

/* === 密码强度 === */
const passwordStrength = computed<number>(() => {
  const pwd = pwdForm.newPassword
  if (!pwd) return 0
  let score = 0
  if (pwd.length >= 6) score++
  if (pwd.length >= 10) score++
  const hasLower = /[a-z]/.test(pwd)
  const hasUpper = /[A-Z]/.test(pwd)
  const hasNumber = /\d/.test(pwd)
  const hasSpecial = /[^a-zA-Z0-9]/.test(pwd)
  const complexity = [hasLower, hasUpper, hasNumber, hasSpecial].filter(Boolean).length
  if (complexity >= 2) score++
  if (complexity >= 3) score++
  return Math.min(3, score)
})

const strengthLabel = computed<string>(() => {
  const labels = ['', '弱', '中', '强']
  return labels[passwordStrength.value] || ''
})

const strengthClass = computed<string>(() => {
  const classes = ['', 'weak', 'mid', 'strong']
  return classes[passwordStrength.value] || ''
})

/** 表单校验 */
function validatePwdForm(): boolean {
  pwdErrors.oldPassword = ''
  pwdErrors.newPassword = ''
  pwdErrors.confirmPassword = ''
  pwdErrors.code = ''
  let valid = true
  if (!pwdForm.oldPassword) {
    pwdErrors.oldPassword = '请输入当前密码'
    valid = false
  }
  if (!pwdForm.newPassword) {
    pwdErrors.newPassword = '请输入新密码'
    valid = false
  } else if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 20) {
    pwdErrors.newPassword = '密码长度为 6-20 位'
    valid = false
  }
  if (!pwdForm.confirmPassword) {
    pwdErrors.confirmPassword = '请再次输入新密码'
    valid = false
  } else if (pwdForm.confirmPassword !== pwdForm.newPassword) {
    pwdErrors.confirmPassword = '两次输入的密码不一致'
    valid = false
  }
  // Bug2修复: 校验验证码
  if (!pwdForm.code) {
    pwdErrors.code = '请输入验证码'
    valid = false
  }
  return valid
}

/* === 工具函数 === */
function formatDate(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD')
}

function formatTime(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

/* === 修改密码 === */
async function handleChangePassword(): Promise<void> {
  if (!validatePwdForm()) return
  pwdLoading.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword,
      code: pwdForm.code
    })
    ElMessage.success('密码修改成功，请重新登录')
    await userStore.logout()
    router.push('/login')
  } catch {
    // 错误已由拦截器处理
  } finally {
    pwdLoading.value = false
  }
}

/** 重置密码表单 */
function resetPwdForm(): void {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirmPassword = ''
  pwdForm.code = ''
  pwdErrors.oldPassword = ''
  pwdErrors.newPassword = ''
  pwdErrors.confirmPassword = ''
  pwdErrors.code = ''
}

/** Bug2修复: 发送修改密码验证码 (发送到当前用户邮箱) */
async function handleSendPwdCode(): Promise<void> {
  const email = user.value?.email || ''
  if (!email) {
    ElMessage.warning('当前账号未绑定邮箱，无法发送验证码')
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }
  sendingPwdCode.value = true
  try {
    await sendEmailCode({ target: email })
    ElMessage.success('邮箱验证码已发送')
    startPwdCodeCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    sendingPwdCode.value = false
  }
}

/** Bug2修复: 修改密码验证码倒计时 (60s) */
function startPwdCodeCountdown(): void {
  pwdCodeCountdown.value = 60
  if (pwdCodeTimer) clearInterval(pwdCodeTimer)
  pwdCodeTimer = setInterval(() => {
    if (pwdCodeCountdown.value > 0) {
      pwdCodeCountdown.value--
    } else {
      if (pwdCodeTimer) {
        clearInterval(pwdCodeTimer)
        pwdCodeTimer = null
      }
    }
  }, 1000)
}

/* === 基本信息: 编辑模式 === */
/** 进入编辑模式: 用当前 user 值初始化表单 */
function enterEdit(): void {
  if (!user.value) return
  editForm.nickname = user.value.nickname || ''
  editForm.phone = user.value.phone || ''
  editForm.email = user.value.email || ''
  editForm.phoneCode = ''
  editForm.emailCode = ''
  editErrors.nickname = ''
  editErrors.phone = ''
  editErrors.email = ''
  editErrors.phoneCode = ''
  editErrors.emailCode = ''
  editing.value = true
}

/** 取消编辑: 退出编辑模式, 清理验证码与倒计时 */
function cancelEdit(): void {
  editing.value = false
  editErrors.nickname = ''
  editErrors.phone = ''
  editErrors.email = ''
  editErrors.phoneCode = ''
  editErrors.emailCode = ''
  editForm.phoneCode = ''
  editForm.emailCode = ''
  stopPhoneCountdown()
  stopEmailCountdown()
}

/** 启动手机号倒计时 */
function startPhoneCountdown(): void {
  phoneCountdown.value = 60
  stopPhoneCountdown()
  phoneTimer = setInterval(() => {
    phoneCountdown.value--
    if (phoneCountdown.value <= 0) {
      stopPhoneCountdown()
    }
  }, 1000)
}

/** 停止手机号倒计时 */
function stopPhoneCountdown(): void {
  if (phoneTimer) {
    clearInterval(phoneTimer)
    phoneTimer = null
  }
  phoneCountdown.value = 0
}

/** 启动邮箱倒计时 */
function startEmailCountdown(): void {
  emailCountdown.value = 60
  stopEmailCountdown()
  emailTimer = setInterval(() => {
    emailCountdown.value--
    if (emailCountdown.value <= 0) {
      stopEmailCountdown()
    }
  }, 1000)
}

/** 停止邮箱倒计时 */
function stopEmailCountdown(): void {
  if (emailTimer) {
    clearInterval(emailTimer)
    emailTimer = null
  }
  emailCountdown.value = 0
}

/** 发送短信验证码 */
async function handleSendPhoneCode(): Promise<void> {
  const phone = editForm.phone.trim()
  if (!phone) {
    editErrors.phone = '请先输入手机号'
    return
  }
  if (!/^1\d{10}$/.test(phone)) {
    editErrors.phone = '请输入正确的 11 位手机号'
    return
  }
  editErrors.phone = ''
  sendingPhoneCode.value = true
  try {
    await sendSmsCode({ target: phone })
    ElMessage.success('短信验证码已发送')
    startPhoneCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    sendingPhoneCode.value = false
  }
}

/** 发送邮箱验证码 */
async function handleSendEmailCode(): Promise<void> {
  const email = editForm.email.trim()
  if (!email) {
    editErrors.email = '请先输入邮箱'
    return
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    editErrors.email = '邮箱格式不正确'
    return
  }
  editErrors.email = ''
  sendingEmailCode.value = true
  try {
    await sendEmailCode({ target: email })
    ElMessage.success('邮箱验证码已发送')
    startEmailCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    sendingEmailCode.value = false
  }
}

/** 校验编辑表单: 手机号 11 位数字, 邮箱格式可选, 变更字段需验证码 */
function validateEditForm(): boolean {
  editErrors.nickname = ''
  editErrors.phone = ''
  editErrors.email = ''
  editErrors.phoneCode = ''
  editErrors.emailCode = ''
  let valid = true

  const phone = editForm.phone.trim()
  if (!phone) {
    editErrors.phone = '请输入手机号'
    valid = false
  } else if (!/^1\d{10}$/.test(phone)) {
    editErrors.phone = '请输入正确的 11 位手机号'
    valid = false
  }

  const email = editForm.email.trim()
  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    editErrors.email = '邮箱格式不正确'
    valid = false
  }

  // 手机号变更必须填写验证码
  if (isPhoneChanged.value && !editForm.phoneCode.trim()) {
    editErrors.phoneCode = '手机号已变更，请输入验证码'
    valid = false
  }

  // 邮箱变更必须填写验证码
  if (isEmailChanged.value && !editForm.emailCode.trim()) {
    editErrors.emailCode = '邮箱已变更，请输入验证码'
    valid = false
  }

  return valid
}

/** 保存个人信息 (区分变更字段, 走对应验证码接口) */
async function handleSaveProfile(): Promise<void> {
  if (!validateEditForm()) return
  editLoading.value = true
  try {
    const nickname = editForm.nickname.trim()
    const phone = editForm.phone.trim()
    const email = editForm.email.trim()
    const phoneChanged = isPhoneChanged.value
    const emailChanged = isEmailChanged.value

    // 1. 若手机号变更 -> 走验证码接口
    if (phoneChanged) {
      await updatePhone({ phone, code: editForm.phoneCode.trim() })
    }
    // 2. 若邮箱变更 -> 走验证码接口
    if (emailChanged) {
      await updateEmail({ email, code: editForm.emailCode.trim() })
    }
    // 3. 昵称变更 (或未变更但需刷新) -> 走通用 profile 接口 (只传昵称, 避免覆盖手机号/邮箱)
    //    注意: 若手机号/邮箱已通过上面接口更新, 这里只更新昵称即可
    if (nickname !== (user.value?.nickname || '')) {
      await updateProfile({ nickname })
    }
    // 拉取最新用户信息
    await userStore.fetchUserInfo()
    ElMessage.success('修改成功')
    editing.value = false
    stopPhoneCountdown()
    stopEmailCountdown()
  } catch {
    // 错误已由拦截器处理
  } finally {
    editLoading.value = false
  }
}

/* === 钱包 Tab: 余额 + 交易记录 + 充值弹窗 === */
const walletBalance = ref<number>(0)
const walletBalanceLoading = ref<boolean>(false)
const walletRecords = ref<WalletRecordVO[]>([])
const walletRecordsLoading = ref<boolean>(false)
/** 钱包数据是否已加载 (避免重复加载) */
const walletLoaded = ref<boolean>(false)

/** 加载钱包余额 */
async function loadWalletBalance(): Promise<void> {
  walletBalanceLoading.value = true
  try {
    const res = await getWalletBalance()
    // 后端返回 Result<BigDecimal>，res.data 直接是数字
    walletBalance.value = res.data ?? 0
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    walletBalanceLoading.value = false
  }
}

/** 加载钱包交易记录 */
async function loadWalletRecords(): Promise<void> {
  walletRecordsLoading.value = true
  try {
    const res = await getWalletRecords()
    walletRecords.value = res.data ?? []
  } catch {
    // 错误已由全局拦截器提示
    walletRecords.value = []
  } finally {
    walletRecordsLoading.value = false
  }
}

/** 加载钱包全部数据 (余额 + 记录) */
async function loadWalletData(): Promise<void> {
  await Promise.all([loadWalletBalance(), loadWalletRecords()])
}

/** 金额格式化 (保留2位小数) */
function formatMoney(value: number): string {
  const num = Number(value || 0)
  return num.toFixed(2)
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

/** 确认充值 */
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
    await loadWalletData()
  } catch {
    // 错误已由全局拦截器提示
  } finally {
    rechargeSubmitting.value = false
  }
}

/** 切换到钱包 Tab 时加载数据 (仅首次切换加载) */
watch(activeTab, (tab) => {
  if (tab === 'wallet' && !walletLoaded.value) {
    walletLoaded.value = true
    loadWalletData()
  }
  if (tab === 'address' && !addressLoaded.value) {
    addressLoaded.value = true
    loadAddressList()
  }
  if (tab === 'coupons' && !couponsLoaded.value) {
    couponsLoaded.value = true
    initCoupons()
  }
})

/* === 收货地址 Tab: 列表 + 新增/编辑弹窗 === */
const addressList = ref<UserAddressVO[]>([])
const addressLoading = ref<boolean>(false)
const addressLoaded = ref<boolean>(false)

const addressDialogVisible = ref<boolean>(false)
const addressDialogTitle = ref<string>('新增地址')
const addressSubmitting = ref<boolean>(false)
const addressEditingId = ref<number | string | null>(null)
const addressFormRef = ref<FormInstance | null>(null)

const addressFormData = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detailAddress: '',
  isDefault: false
})

const addressFormRules: FormRules = {
  receiverName: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  receiverPhone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '请输入正确的 11 位手机号', trigger: 'blur' }
  ],
  province: [{ required: true, message: '请输入省份', trigger: 'blur' }],
  city: [{ required: true, message: '请输入城市', trigger: 'blur' }],
  district: [{ required: true, message: '请输入区/县', trigger: 'blur' }],
  detailAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

/** 加载地址列表 */
async function loadAddressList(): Promise<void> {
  addressLoading.value = true
  try {
    const res = await getAddressList()
    addressList.value = res.data ?? []
  } catch {
    // 错误信息已由请求拦截器统一提示
    addressList.value = []
  } finally {
    addressLoading.value = false
  }
}

/** 重置地址表单 */
function resetAddressForm(): void {
  addressFormData.receiverName = ''
  addressFormData.receiverPhone = ''
  addressFormData.province = ''
  addressFormData.city = ''
  addressFormData.district = ''
  addressFormData.detailAddress = ''
  addressFormData.isDefault = false
  addressEditingId.value = null
  addressFormRef.value?.clearValidate()
}

/** 打开新增地址弹窗 */
function openAddDialog(): void {
  addressDialogTitle.value = '新增地址'
  resetAddressForm()
  addressDialogVisible.value = true
}

/** 打开编辑地址弹窗 */
function openEditDialog(addr: UserAddressVO): void {
  addressDialogTitle.value = '编辑地址'
  addressEditingId.value = addr.id
  addressFormData.receiverName = addr.receiverName
  addressFormData.receiverPhone = addr.receiverPhone
  addressFormData.province = addr.province
  addressFormData.city = addr.city
  addressFormData.district = addr.district
  addressFormData.detailAddress = addr.detailAddress
  addressFormData.isDefault = addr.isDefault === 1
  addressFormRef.value?.clearValidate()
  addressDialogVisible.value = true
}

/** 构造地址提交载荷 (isDefault: boolean → number 0/1) */
function buildAddressPayload(): UserAddressRequest {
  return {
    receiverName: addressFormData.receiverName.trim(),
    receiverPhone: addressFormData.receiverPhone.trim(),
    province: addressFormData.province.trim(),
    city: addressFormData.city.trim(),
    district: addressFormData.district.trim(),
    detailAddress: addressFormData.detailAddress.trim(),
    isDefault: addressFormData.isDefault ? 1 : 0
  }
}

/** 提交地址表单 (新增 / 编辑) */
async function handleAddressSubmit(): Promise<void> {
  if (!addressFormRef.value) return
  try {
    await addressFormRef.value.validate()
  } catch {
    return
  }
  addressSubmitting.value = true
  try {
    const payload = buildAddressPayload()
    if (addressEditingId.value === null) {
      await createAddress(payload)
      ElMessage.success('地址添加成功')
    } else {
      await updateAddress(addressEditingId.value, payload)
      ElMessage.success('地址修改成功')
    }
    addressDialogVisible.value = false
    resetAddressForm()
    await loadAddressList()
  } catch {
    // 错误信息已由请求拦截器统一提示
  } finally {
    addressSubmitting.value = false
  }
}

/** 删除地址 */
async function handleDeleteAddress(addr: UserAddressVO): Promise<void> {
  try {
    await ElMessageBox.confirm('确定删除该收货地址吗？', '删除确认', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '再想想'
    })
  } catch {
    return
  }
  try {
    await deleteAddress(addr.id)
    ElMessage.success('地址已删除')
    await loadAddressList()
  } catch {
    // 错误信息已由请求拦截器统一提示
  }
}

/** 设为默认地址 */
async function handleSetDefault(addr: UserAddressVO): Promise<void> {
  try {
    await setDefaultAddress(addr.id)
    ElMessage.success('已设为默认地址')
    await loadAddressList()
  } catch {
    // 错误信息已由请求拦截器统一提示
  }
}

/* === 优惠券 Tab: 子Tab切换 + 卡片展示 === */
interface CouponSubTabItem {
  label: string
  value: UserCouponStatus
}

const couponSubTabs: CouponSubTabItem[] = [
  { label: '可用', value: 'UNUSED' },
  { label: '已使用', value: 'USED' },
  { label: '已过期', value: 'EXPIRED' }
]

const couponActiveSubTab = ref<UserCouponStatus>('UNUSED')
const couponList = ref<UserCouponVO[]>([])
const couponLoading = ref<boolean>(false)
const couponsLoaded = ref<boolean>(false)

const couponTabCountMap = ref<Record<UserCouponStatus, number>>({
  UNUSED: 0,
  USED: 0,
  EXPIRED: 0
})

const couponEmptyText = ref<string>('暂无可用优惠券')

/** 拉取优惠券列表 (按状态) */
async function loadCoupons(status: UserCouponStatus): Promise<void> {
  couponLoading.value = true
  try {
    const res = await getMyCoupons(status)
    couponList.value = res.data ?? []
    couponTabCountMap.value[status] = couponList.value.length
  } catch {
    // 错误已由全局拦截器提示
    couponList.value = []
  } finally {
    couponLoading.value = false
  }
}

/** 切换优惠券子Tab */
async function switchCouponSubTab(status: UserCouponStatus): Promise<void> {
  if (status === couponActiveSubTab.value) return
  couponActiveSubTab.value = status
  couponEmptyText.value = status === 'UNUSED' ? '暂无可用优惠券' : status === 'USED' ? '暂无已使用优惠券' : '暂无已过期优惠券'
  await loadCoupons(status)
}

/** 初始化优惠券数据: 拉取三个状态数量 + 默认Tab数据 */
async function initCoupons(): Promise<void> {
  const statuses: UserCouponStatus[] = ['UNUSED', 'USED', 'EXPIRED']
  await Promise.all(statuses.map((s) => loadCoupons(s)))
  await loadCoupons(couponActiveSubTab.value)
}

/** 优惠券金额格式化 */
function formatCouponMoney(value: number): string {
  return Number(value || 0).toFixed(2)
}

/** 优惠券面额格式化 (整数不带小数) */
function formatCouponAmount(value: number): string {
  const num = Number(value || 0)
  return num % 1 === 0 ? String(num) : num.toFixed(2)
}

/** 优惠券折扣格式化 */
function formatCouponDiscount(value: number): string {
  const num = Number(value || 0)
  return num % 1 === 0 ? String(num) : num.toFixed(1)
}

/** 优惠券日期格式化 */
function formatCouponDate(time: string): string {
  if (!time) return '—'
  return dayjs(time).format('YYYY-MM-DD')
}

/** 优惠券状态文本 */
function couponStatusLabel(status: UserCouponStatus): string {
  const map: Record<UserCouponStatus, string> = {
    UNUSED: '未使用',
    USED: '已使用',
    EXPIRED: '已过期'
  }
  return map[status] || status
}

/** 优惠券状态标签样式 */
function couponStatusTagType(status: UserCouponStatus): 'success' | 'info' | 'warning' {
  const map: Record<UserCouponStatus, 'success' | 'info' | 'warning'> = {
    UNUSED: 'success',
    USED: 'info',
    EXPIRED: 'warning'
  }
  return map[status] || 'info'
}

/** 优惠券卡片样式: 已使用/已过期置灰 */
function couponCardClass(item: UserCouponVO): string {
  if (item.status === 'USED') return 'is-used'
  if (item.status === 'EXPIRED') return 'is-expired'
  return ''
}

/* === 拉取用户信息 === */
async function fetchUserInfo(): Promise<void> {
  loading.value = true
  try {
    await userStore.fetchUserInfo()
  } catch {
    // 错误已由拦截器处理
  } finally {
    loading.value = false
  }
}

/* === 根据路由 query.tab 切换标签页 (支持 /user/wallet /user/coupons /user/address 重定向) === */
const VALID_TABS = ['info', 'password', 'wallet', 'address', 'coupons'] as const
function applyTabFromQuery(): void {
  const tab = route.query.tab
  if (typeof tab === 'string' && VALID_TABS.includes(tab as typeof VALID_TABS[number])) {
    activeTab.value = tab
  }
}

onMounted(() => {
  applyTabFromQuery()
  fetchUserInfo()
})

/* keep-alive 缓存下, 路由 query.tab 变化时同步切换标签页 */
watch(
  () => route.query.tab,
  () => {
    applyTabFromQuery()
  }
)

/* === 组件卸载时清理倒计时定时器 === */
onUnmounted(() => {
  stopPhoneCountdown()
  stopEmailCountdown()
  if (pwdCodeTimer) {
    clearInterval(pwdCodeTimer)
    pwdCodeTimer = null
  }
})
</script>

<style scoped>
.profile-page {
  padding-bottom: 24px;
}

.loading-wrap {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-line {
  height: 20px;
  background: var(--color-bg-subtle);
  border-radius: 4px;
  background-image: linear-gradient(90deg, var(--color-bg-subtle) 25%, var(--color-bg-muted) 50%, var(--color-bg-subtle) 75%);
  background-size: 200% 100%;
  animation: skeleton-loading 1.4s ease infinite;
}

@keyframes skeleton-loading {
  0% {
    background-position: 200% 0;
  }

  100% {
    background-position: -200% 0;
  }
}

/* 严格对照 index.html .profile-layout 样式 */
.profile-layout {
  display: flex;
  gap: 24px;
  padding: 24px;
}

/* 左列用户卡片 */
.profile-card {
  width: 260px;
  flex-shrink: 0;
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 32px 24px;
  text-align: center;
}

/* 头像 80px 圆形渐变 */
.profile-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-accent));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 28px;
  font-weight: 700;
  cursor: pointer;
  position: relative;
}

.profile-name {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 4px;
  color: var(--color-text-primary);
}

.profile-phone {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

/* 角色徽章 */
.role-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 700;
}

.role-badge.admin {
  background: var(--tag-timeout-bg);
  color: var(--tag-timeout-fg);
}

.role-badge.seller {
  background: var(--tag-unpaid-bg);
  color: var(--tag-unpaid-fg);
}

.role-badge.buyer {
  background: var(--tag-completed-bg);
  color: var(--tag-completed-fg);
}

.profile-meta {
  font-size: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  text-align: left;
}

.profile-meta-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
}

.profile-meta-row dt {
  color: var(--color-text-secondary);
}

.profile-meta-row dd {
  font-weight: 600;
  color: var(--color-text-primary);
}

/* 右列主内容 */
.profile-main {
  flex: 1;
  min-width: 0;
}

/* 标签页 */
.profile-tabs {
  display: flex;
  gap: 0;
  border-bottom: 1px solid var(--color-border);
  margin-bottom: 24px;
}

.profile-tab {
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-secondary);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.profile-tab.active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

/* 表单：max-width 420px */
.profile-form {
  max-width: 420px;
}

/* 基本信息: 白底卡片 */
.info-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 24px;
}

/* 修改密码: 白底卡片 (与 .info-card 风格协调, 加轻微阴影提升层次感) */
.pwd-card {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* 字段行: label-value 横向布局, 行间分隔线 */
.info-row {
  display: flex;
  align-items: flex-start;
  padding: 12px 0;
  border-bottom: 1px solid var(--color-border);
}

.info-row:last-of-type {
  border-bottom: none;
}

.info-label {
  flex-shrink: 0;
  width: 88px;
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
  letter-spacing: 0.02em;
  line-height: 24px;
}

.info-value {
  flex: 1;
  min-width: 0;
  font-size: 15px;
  color: var(--color-text-primary);
  line-height: 24px;
  word-break: break-all;
}

.info-value.readonly {
  color: var(--color-text-secondary);
}

/* 编辑模式: input 占满 value 区 */
.info-row .form-input {
  width: 100%;
  height: 36px;
}

.info-edit-cell {
  flex: 1;
  min-width: 0;
}

/* 操作区 */
.info-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}

.form-value {
  font-size: 14px;
  padding: 8px 0;
  color: var(--color-text-primary);
}

.form-input {
  width: 100%;
  height: 40px;
  border: 1px solid var(--color-border);
  border-radius: 4px;
  padding: 0 12px;
  font-size: 13px;
  transition: border-color 0.2s;
  outline: none;
  box-sizing: border-box;
  background: #fff;
  color: var(--color-text-primary);
}

.form-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(229, 57, 53, 0.1);
}

.form-input.error {
  border-color: var(--color-danger);
}

.form-error {
  font-size: 11px;
  color: var(--color-danger);
  margin-top: 4px;
}

/* 密码强度条 */
.strength-bars {
  display: flex;
  gap: 4px;
  margin-top: 6px;
}

.strength-bar {
  flex: 1;
  height: 3px;
  background: var(--btn-disabled-bg);
  border-radius: 2px;
  transition: background 0.2s;
}

.strength-bar.active.weak {
  background: var(--color-danger);
}

.strength-bar.active.mid {
  background: var(--color-warning);
}

.strength-bar.active.strong {
  background: var(--color-success);
}

.strength-text {
  font-size: 10px;
  color: var(--color-text-secondary);
  margin-top: 2px;
}

.strength-text.weak {
  color: var(--color-danger);
}

.strength-text.mid {
  color: var(--color-warning);
}

.strength-text.strong {
  color: var(--color-success);
}

.form-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
}

.form-tip {
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-top: 8px;
}

/* 小按钮 */
.btn-sm {
  padding: 10px 28px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid var(--color-border);
  background: #fff;
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.btn-sm.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.btn-sm.primary:hover {
  background: var(--btn-hover);
}

.btn-sm.primary:disabled {
  background: var(--btn-loading-bg);
  cursor: not-allowed;
}

/* === 左侧快捷入口 === */
.profile-shortcuts {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.shortcut-item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 4px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: var(--color-bg-subtle);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.shortcut-item:hover {
  color: var(--color-primary);
  background: rgba(229, 57, 53, 0.08);
}

.shortcut-item .el-icon {
  font-size: 16px;
  flex-shrink: 0;
}

/* === 验证码行 (输入框 + 发送按钮) === */
.verify-row {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  align-items: flex-start;
}

.verify-input {
  flex: 1;
  min-width: 0;
}

.verify-btn {
  flex-shrink: 0;
  padding: 0 14px;
  height: 36px;
  white-space: nowrap;
  min-width: 110px;
  justify-content: center;
  display: inline-flex;
  align-items: center;
}

/* === 钱包 Tab 样式 === */
.wallet-tab {
  /* 不限制 max-width，让钱包内容更饱满 */
}

/* 余额展示卡片 (渐变背景) */
.wallet-balance-card {
  display: flex;
  align-items: center;
  gap: 20px;
  background: linear-gradient(135deg, var(--color-primary), #d32f2f);
  color: #fff;
  border-radius: 12px;
  padding: 24px 28px;
  margin-bottom: 20px;
  box-shadow: 0 6px 20px rgba(229, 57, 53, 0.25);
}

.wallet-balance-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  flex-shrink: 0;
}

.wallet-balance-info {
  flex: 1;
  min-width: 0;
}

.wallet-balance-label {
  font-size: 13px;
  opacity: 0.9;
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}

.wallet-balance-amount {
  font-size: 28px;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: 0.02em;
}

.wallet-balance-actions {
  flex-shrink: 0;
}

.wallet-recharge-btn {
  background: #fff;
  color: var(--color-primary);
  border-color: #fff;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.wallet-recharge-btn:hover {
  background: rgba(255, 255, 255, 0.9);
}

/* 交易记录区 */
.wallet-records-section {
  background: var(--color-bg-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  overflow: hidden;
}

.wallet-section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border);
}

.wallet-section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

/* 加载/空状态 */
.wallet-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: var(--color-text-muted);
  gap: 12px;
}

.wallet-loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.wallet-empty-state {
  padding: 32px 24px;
  display: flex;
  justify-content: center;
}

/* 记录表格 (紧凑样式) */
.wallet-records-table-wrap {
  overflow-x: auto;
}

.wallet-records-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.wallet-records-table thead th {
  background: var(--color-bg-subtle);
  padding: 9px 16px;
  text-align: left;
  font-weight: 600;
  font-size: 12px;
  color: var(--color-text-secondary);
  border-bottom: 1px solid var(--color-border);
  letter-spacing: 0.02em;
}

.wallet-records-table tbody td {
  padding: 10px 16px;
  border-bottom: 1px solid var(--color-border);
  vertical-align: middle;
  color: var(--color-text-primary);
}

.wallet-records-table tbody tr:hover {
  background: var(--color-bg-subtle);
}

.wallet-records-table tbody tr:last-child td {
  border-bottom: none;
}

.wallet-amount-cell {
  font-weight: 700;
  font-size: 13px;
}

.wallet-amount-cell.income {
  color: var(--color-success);
}

.wallet-amount-cell.expense {
  color: var(--color-danger);
}

.wallet-time-cell {
  color: var(--color-text-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.wallet-remark-cell {
  color: var(--color-text-secondary);
  max-width: 220px;
  word-break: break-all;
}

/* 表格底部"查看更多" */
.wallet-records-footer {
  display: flex;
  justify-content: center;
  padding: 12px 0;
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-card);
}

.wallet-records-footer .btn-sm.text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

/* 充值提示 */
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

/* 按钮内图标 */
.btn-icon {
  font-size: 14px;
}

/* text 按钮 (用于刷新/查看更多) */
.btn-sm.text {
  border: none;
  background: none;
  color: var(--color-text-secondary);
  padding: 6px 10px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.btn-sm.text:hover {
  color: var(--color-primary);
}

/* === 收货地址 Tab 样式 === */
.address-tab {
  /* 不限制 max-width，让地址卡片网格更饱满 */
}

.address-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.address-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0;
}

.address-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: var(--color-text-muted);
  gap: 12px;
}

.address-loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.address-empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  text-align: center;
}

.address-empty-icon {
  color: var(--color-text-muted);
  margin-bottom: 16px;
}

.address-empty-text {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin-bottom: 20px;
}

/* 地址卡片网格 */
.address-grid {
  margin-left: 0 !important;
  margin-right: 0 !important;
}

.address-col {
  margin-bottom: 16px;
}

.address-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  transition: box-shadow 0.25s ease, transform 0.25s ease, border-color 0.25s ease;
  box-sizing: border-box;
}

.address-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.address-card.address-default {
  border-color: var(--color-primary);
  box-shadow: 0 2px 12px rgba(229, 57, 53, 0.15);
}

.address-card.address-default:hover {
  box-shadow: 0 8px 24px rgba(229, 57, 53, 0.25);
}

.card-top {
  display: flex;
  flex-direction: column;
}

.card-name-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.address-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.address-phone {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.default-tag {
  margin-left: auto;
  font-weight: 700;
}

.card-detail {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
  min-height: 42px;
}

.detail-icon {
  font-size: 15px;
  color: var(--color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.detail-text {
  flex: 1;
  word-break: break-all;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 4px;
  padding-top: 8px;
  border-top: 1px solid var(--color-border-light);
  margin-top: auto;
}

.btn-plus {
  font-size: 14px;
  font-weight: 700;
  line-height: 1;
}

.btn-sm.text.danger:hover {
  color: var(--color-primary);
}

/* === 优惠券 Tab 样式 === */
.coupons-tab {
  /* 不限制 max-width，让优惠券卡片网格更饱满 */
}

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

.coupon-loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 64px 24px;
  color: var(--color-text-muted);
  gap: 12px;
}

.coupon-loading-text {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.coupon-empty-state {
  padding: 40px 24px;
  display: flex;
  justify-content: center;
}

.coupon-grid {
  margin-left: 0 !important;
  margin-right: 0 !important;
}

.coupon-col {
  margin-bottom: 16px;
}

/* 优惠券卡片 (左面额 + 右信息) */
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

/* 已使用/已过期卡片置灰 */
.coupon-card.is-used .coupon-left,
.coupon-card.is-expired .coupon-left {
  background: linear-gradient(135deg, #bdbdbd, #9e9e9e);
}

.coupon-card.is-used .coupon-name,
.coupon-card.is-expired .coupon-name {
  color: var(--color-text-secondary);
}

/* 右上角状态标记 */
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

/* 响应式 */
@media (max-width: 768px) {
  .profile-layout {
    flex-direction: column;
  }

  .profile-card {
    width: 100%;
  }

  .profile-shortcuts {
    grid-template-columns: 1fr 1fr;
  }

  .wallet-balance-card {
    flex-direction: column;
    align-items: flex-start;
    padding: 18px;
    gap: 14px;
  }

  .wallet-balance-amount {
    font-size: 24px;
  }

  .address-col {
    margin-bottom: 12px;
  }

  .address-card {
    padding: 14px;
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
