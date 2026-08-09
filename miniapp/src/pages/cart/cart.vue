<template>
  <view class="cart-page">
    <view class="placeholder">
      <text class="title">购物车</text>
      <text class="desc">购物车页（占位，阶段 3 填充）</text>
    </view>
  </view>
</template>

<script setup lang="ts">
// 购物车页占位（阶段 3 T3.1 实现：列表 + 左滑删除 + 数量修改 + 批量选中 + 底部结算栏）
</script>

<style lang="scss" scoped>
.cart-page {
  min-height: 100vh;
  background-color: #f8f8f8;
  .placeholder {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding-top: 200rpx;
    .title {
      font-size: 48rpx;
      font-weight: bold;
      color: #303133;
      margin-bottom: 24rpx;
    }
    .desc {
      font-size: 28rpx;
      color: #909399;
    }
  }
}
</style>