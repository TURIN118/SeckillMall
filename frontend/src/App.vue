<template>
  <!-- ElConfigProvider 注入 Element Plus 中文 locale, 替代原 main.ts 的 app.use(ElementPlus, { locale: zhCn }) -->
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
/**
 * 根组件
 */
// 显式引入 ElConfigProvider 与中文 locale, 确保按需引入后分页等组件仍显示中文
import { ElConfigProvider } from 'element-plus/es/components/config-provider/index'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
</script>
