// components/order-card/order-card.js
// 订单卡片组件：展示订单号/状态/商品缩略/金额，bindtap 跳详情
//
// 对齐：
//   - design.md 2.6 节 order-card
//   - spec.md 5.3 节（订单管理）
//   - tasks.md TR2

const { formatPrice, formatDate } = require('../../utils/format')

// 订单状态文案映射
const STATUS_TEXT = {
  PENDING_PAY: '待付款',
  PENDING_SHIP: '待发货',
  PENDING_RECEIVE: '待收货',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

// 订单类型文案映射
const TYPE_TEXT = {
  SECKILL: '秒杀',
  NORMAL: '普通'
}

Component({
  properties: {
    // 订单对象 OrderListItemVO
    // { orderId, orderType, status, totalAmount, createTime, items }
    order: {
      type: Object,
      value: null
    }
  },
  data: {
    // 派生展示字段
    statusText: '',
    typeText: '',
    formattedAmount: '0.00',
    formattedTime: '',
    // 商品缩略图列表（最多 3 张）
    thumbImages: [],
    // 商品总件数
    itemCount: 0,
    // 商品名摘要（无图时展示）
    itemSummary: ''
  },
  observers: {
    'order': function (order) {
      if (!order) {
        this.setData({
          statusText: '',
          typeText: '',
          formattedAmount: '0.00',
          formattedTime: '',
          thumbImages: [],
          itemCount: 0,
          itemSummary: ''
        })
        return
      }

      const status = order.status || ''
      const orderType = order.orderType || ''
      const items = Array.isArray(order.items) ? order.items : []

      // 缩略图取每个商品 productImage，最多 3 张
      const thumbImages = items
        .map((it) => it.productImage || it.image || '')
        .filter((url) => !!url)
        .slice(0, 3)

      // 商品件数：sum(quantity)
      const itemCount = items.reduce((sum, it) => {
        const q = parseInt(it.quantity, 10)
        return sum + (Number.isNaN(q) ? 1 : q)
      }, 0)

      // 商品名摘要：取第一个商品名
      const itemSummary = items.length > 0 ? (items[0].productName || '') : ''

      this.setData({
        statusText: STATUS_TEXT[status] || status,
        typeText: TYPE_TEXT[orderType] || orderType,
        formattedAmount: formatPrice(order.totalAmount),
        formattedTime: formatDate(order.createTime, 'YYYY-MM-DD HH:mm'),
        thumbImages: thumbImages,
        itemCount: itemCount,
        itemSummary: itemSummary
      })
    }
  },
  methods: {
    /** 点击卡片：triggerEvent('tap', { orderId, orderType })，由父页面处理跳转 */
    onTap() {
      const order = this.data.order
      if (!order || order.orderId == null) return
      this.triggerEvent('tap', {
        orderId: String(order.orderId),
        orderType: order.orderType || 'NORMAL'
      })
    },
    /** 图片加载失败兜底 */
    onImageError() {
      // 静默处理，保留占位
    }
  }
})