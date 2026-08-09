<!--
  收货地址编辑/新增页（对齐 spec.md 3.10 / tasks.md T3.3 + 阶段5复用）
  - onLoad 读取 id 参数：有 id 走编辑（拉取详情），无 id 走新增
  - 表单：收件人 / 手机号 / 省市区 / 详情 / 设为默认
  - 省市区：uni.chooseLocation（可选）或 u-picker 简化输入
  - 调用 addressApi.addAddress / updateAddress
-->
<template>
  <view class="address-edit-page">
    <u-form
      ref="formRef"
      :model="form"
      :rules="rules"
      labelPosition="top"
      :labelWidth="100"
      errorType="toast"
    >
      <u-form-item label="收件人" prop="receiver" required>
        <u-input
          v-model="form.receiver"
          placeholder="请输入收件人姓名"
          maxlength="20"
          clearable
        />
      </u-form-item>

      <u-form-item label="手机号" prop="phone" required>
        <u-input
          v-model="form.phone"
          type="number"
          placeholder="请输入手机号"
          maxlength="11"
          clearable
        />
      </u-form-item>

      <u-form-item label="所在地区" prop="region" required>
        <view class="region-row" @tap="chooseLocation">
          <text
            class="region-text"
            :class="{ placeholder: !regionText }"
          >{{ regionText || '点击选择省市区' }}</text>
          <u-icon name="arrow-right" size="28" color="#909399" />
        </view>
      </u-form-item>

      <u-form-item label="详细地址" prop="detail" required>
        <u-input
          v-model="form.detail"
          type="textarea"
          placeholder="请输入详细地址（楼栋/门牌号等）"
          maxlength="100"
          :autoHeight="true"
        />
      </u-form-item>

      <u-form-item label="地址标签" prop="tag">
        <view class="tag-row">
          <view
            v-for="t in tagOptions"
            :key="t"
            class="tag-item"
            :class="{ active: form.tag === t }"
            @tap="form.tag = form.tag === t ? '' : t"
          >
            <text>{{ t }}</text>
          </view>
        </view>
      </u-form-item>

      <u-form-item label="设为默认地址">
        <u-switch v-model="form.isDefault" />
      </u-form-item>
    </u-form>

    <!-- 底部保存按钮 -->
    <view class="footer-bar">
      <u-button
        type="error"
        shape="circle"
        :loading="submitting"
        @click="onSubmit"
      >{{ isEdit ? '保存修改' : '保存地址' }}</u-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import * as addressApi from '@/api/address'
import { requireAuthAsync } from '@/utils/authGuard'
import { navigate } from '@/utils/navigate'
import { showToast, showLoading, hideLoading } from '@/utils/toast'
import { ensureStringId } from '@/utils/snowflake'
import type { AddressRequest, AddressVO } from '@/types'

const formRef = ref<any>(null)
const submitting = ref<boolean>(false)
const addressId = ref<string>('')
const isEdit = computed(() => !!addressId.value)

/** 表单数据 */
const form = reactive<AddressRequest & { region: string }>({
  receiver: '',
  phone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
  tag: '',
  region: ''
})

/** 标签选项 */
const tagOptions = ['家', '公司', '学校', '其他']

/** 校验规则 */
const rules = {
  receiver: [
    { required: true, message: '请输入收件人姓名', trigger: ['blur', 'change'] }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: ['blur', 'change'] },
    {
      validator: (val: string) => /^1[3-9]\d{9}$/.test(val),
      message: '手机号格式不正确',
      trigger: ['blur']
    }
  ],
  region: [
    { required: true, message: '请选择省市区', trigger: ['change'] }
  ],
  detail: [
    { required: true, message: '请输入详细地址', trigger: ['blur', 'change'] }
  ]
}

/** 省市区显示文本 */
const regionText = computed(() => {
  if (form.province || form.city || form.district) {
    return `${form.province}${form.city}${form.district}`
  }
  return ''
})

onLoad(async (options) => {
  if (!requireAuthAsync()) return
  if (options && options.id) {
    addressId.value = ensureStringId(options.id)
    uni.setNavigationBarTitle({ title: '编辑地址' })
    await fetchDetail()
  } else {
    uni.setNavigationBarTitle({ title: '新增地址' })
  }
})

/** 拉取地址详情 */
async function fetchDetail() {
  try {
    showLoading('加载中...')
    const detail = await addressApi.getAddressDetail(addressId.value)
    form.receiver = detail.receiver || ''
    form.phone = detail.phone || ''
    form.province = detail.province || ''
    form.city = detail.city || ''
    form.district = detail.district || ''
    form.detail = detail.detail || ''
    form.isDefault = !!detail.isDefault
    form.tag = detail.tag || ''
    form.region = `${form.province}${form.city}${form.district}`
  } catch (e) {
    console.error('拉取地址详情失败', e)
    showToast('地址详情加载失败', 'error')
  } finally {
    hideLoading()
  }
}

/** 选择省市区：优先 uni.chooseLocation，失败则降级为手动输入 */
function chooseLocation() {
  // #ifdef MP-WEIXIN
  uni.chooseLocation({
    success: (res) => {
      // chooseLocation 返回 address（含省市区）和 name（地点名）
      // 简化处理：将 address 拆分；如后端有省市区接口可改用 u-picker
      const address = res.address || ''
      // address 格式一般为 "XX省XX市XX区"，简化按省市区字符切分
      const provinceMatch = address.match(/^(.+?[省市区自治区])/)
      const cityMatch = address.match(/[省市区自治区](.+?[市州盟])/)
      const districtMatch = address.match(/[市州盟](.+?[区县旗市])/)
      form.province = provinceMatch ? provinceMatch[1] : address
      form.city = cityMatch ? cityMatch[1] : ''
      form.district = districtMatch ? districtMatch[1] : ''
      // 详细地址拼接：name + detail
      if (!form.detail) {
        form.detail = res.name || ''
      }
      form.region = `${form.province}${form.city}${form.district}`
    },
    fail: (err) => {
      console.warn('chooseLocation 失败，降级手动输入', err)
      // 降级：弹出简化输入（这里用 showToast 提示，实际可弹 u-picker）
      showToast('请在下方手动输入省市区', 'none')
      // 简化：直接让用户在详细地址中填写完整地址
      if (!form.province) {
        form.province = '请手动输入'
        form.city = ''
        form.district = ''
        form.region = '请手动输入'
      }
    }
  })
  // #endif

  // #ifndef MP-WEIXIN
  // 非微信小程序环境：简化为手动输入
  if (!form.province) {
    form.province = '请手动输入'
    form.region = '请手动输入'
  }
  // #endif
}

/** 提交保存 */
async function onSubmit() {
  // 表单校验
  try {
    await formRef.value?.validate()
  } catch (e) {
    // validate 失败已由 u-form toast 提示
    return
  }

  // 二次校验省市区
  if (!form.province) {
    showToast('请选择省市区', 'none')
    return
  }

  submitting.value = true
  try {
    showLoading('保存中...')
    const payload: AddressRequest = {
      receiver: form.receiver,
      phone: form.phone,
      province: form.province,
      city: form.city,
      district: form.district,
      detail: form.detail,
      isDefault: form.isDefault,
      tag: form.tag
    }

    if (isEdit.value) {
      await addressApi.updateAddress(addressId.value, payload)
      showToast('保存成功', 'success')
    } else {
      await addressApi.addAddress(payload)
      showToast('添加成功', 'success')
    }

    // 返回上一页
    setTimeout(() => {
      navigate.back()
    }, 800)
  } catch (e) {
    console.error('保存地址失败', e)
    showToast('保存失败，请重试', 'error')
  } finally {
    submitting.value = false
    hideLoading()
  }
}
</script>

<style lang="scss" scoped>
.address-edit-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  padding: 24rpx 32rpx 160rpx;
}

.region-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 64rpx;

  .region-text {
    font-size: 30rpx;
    color: #303133;

    &.placeholder {
      color: #c0c4cc;
    }
  }
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
  margin-top: 12rpx;

  .tag-item {
    padding: 8rpx 28rpx;
    border: 1rpx solid #dcdfe6;
    border-radius: 32rpx;
    background-color: #ffffff;

    text {
      font-size: 26rpx;
      color: #606266;
    }

    &.active {
      border-color: #ff4d4f;
      background-color: #fff1f0;

      text {
        color: #ff4d4f;
      }
    }
  }
}

.footer-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  padding: 24rpx 32rpx;
  padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.04);
}
</style>