// components/load-more/load-more.js
// 封装加载更多状态：loading / nomore / error
Component({
  properties: {
    // 状态：loading 加载中 | nomore 没有更多 | error 加载失败
    status: {
      type: String,
      value: 'loading'
    },
    // loading 文案
    loadingText: {
      type: String,
      value: '加载中...'
    },
    // nomore 文案
    nomoreText: {
      type: String,
      value: '没有更多了'
    },
    // error 文案
    errorText: {
      type: String,
      value: '加载失败，点击重试'
    }
  },
  methods: {
    /** 点击重试 */
    onTapRetry() {
      if (this.data.status === 'error') {
        this.triggerEvent('retry')
      }
    }
  }
})