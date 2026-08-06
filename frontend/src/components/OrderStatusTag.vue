<template>
  <el-tag :type="tagType" effect="light" :size="size">{{ tagLabel }}</el-tag>
</template>

<script setup lang="ts">
/**
 * 订单状态标签组件 C04
 * 参照 10-ai-design-spec.md C04 规范
 * UNPAID→待支付(warning), PAID→已支付(success), CANCELLED→已取消(info),
 * TIMEOUT→已超时(danger), COMPLETED→已完成(primary)
 */
import { computed } from 'vue'
import type { OrderStatus } from '@/types'

interface Props {
  status: OrderStatus
  size?: 'large' | 'default' | 'small'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'default'
})

const statusMap: Record<OrderStatus, { type: 'primary' | 'success' | 'warning' | 'info' | 'danger'; label: string }> = {
  UNPAID: { type: 'warning', label: '待支付' },
  PAID: { type: 'success', label: '已支付' },
  SHIPPED: { type: 'primary', label: '已发货' },
  CANCELLED: { type: 'info', label: '已取消' },
  TIMEOUT: { type: 'danger', label: '已超时' },
  COMPLETED: { type: 'primary', label: '已完成' }
}

const tagType = computed(() => statusMap[props.status]?.type ?? 'info')
const tagLabel = computed(() => statusMap[props.status]?.label ?? props.status)
</script>