<template>
  <view class="profile-page">
    <view class="placeholder">
      <text class="title">我的</text>
      <text class="desc">个人中心（占位，阶段 5 填充）</text>
    </view>
  </view>
</template>

<script setup lang="ts">
// 我的页占位（阶段 5 实现：个人中心入口）
</script>

<style lang="scss" scoped>
.profile-page {
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