<template>
  <view class="category-page">
    <view class="placeholder">
      <text class="title">分类</text>
      <text class="desc">分类页（占位，阶段 2 填充）</text>
    </view>
  </view>
</template>

<script setup lang="ts">
// 分类页占位（阶段 2 T2.2 实现：分类 tab + 商品列表）
</script>

<style lang="scss" scoped>
.category-page {
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