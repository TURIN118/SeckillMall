<template>
  <view class="home-page">
    <view class="placeholder">
      <text class="title">秒杀商城</text>
      <text class="desc">首页（占位，阶段 2 填充）</text>
    </view>
  </view>
</template>

<script setup lang="ts">
// 首页占位（阶段 2 T2.1 实现：Banner 轮播 + 秒杀入口 + 分类导航 + 猜你喜欢）
</script>

<style lang="scss" scoped>
.home-page {
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
      color: #ff4d4f;
      margin-bottom: 24rpx;
    }
    .desc {
      font-size: 28rpx;
      color: #909399;
    }
  }
}
</style>