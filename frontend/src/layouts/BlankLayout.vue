<template>
  <div class="blank-layout">
    <!--
      登录/注册页直接渲染, 不使用 transition.
      原实现 <transition mode="out-in"> 会导致切换时旧组件先淡出再新组件淡入,
      中间产生白屏间隙 (约 0.4s), 表现为卡顿空白. 移除 transition 后切换瞬时完成.
      :key 绑定 route.path 确保登录/注册切换时组件强制重新渲染 (不复用).
    -->
    <router-view v-slot="{ Component }">
      <component :is="Component" :key="route.path" />
    </router-view>
  </div>
</template>

<script setup lang="ts">
/**
 * 空白布局 - 用于登录/注册页
 * 渐变背景, 纯 router-view 包装
 *
 * 性能优化说明:
 *   登录/注册是独立顶级路由 (非 FrontLayout 子路由), 切换时整个布局重新挂载.
 *   原实现使用 <transition name="fade" mode="out-in"> 包裹, out-in 模式会:
 *     1. 先让旧组件完全淡出 (0.2s) —— 此期间页面空白
 *     2. 再让新组件淡入 (0.2s)
 *   总计约 0.4s 白屏间隙, 叠加组件 chunk 加载延迟, 表现为明显卡顿.
 *   修复: 移除 transition, 直接渲染; 添加 :key 保证组件切换时正确重建.
 */
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<style scoped>
.blank-layout {
  min-height: 100vh;
  background: linear-gradient(135deg, #fff5f5 0%, #fff3e0 100%);
}
</style>
