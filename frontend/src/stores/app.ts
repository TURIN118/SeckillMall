/**
 * 应用 Store - 参照 10-ai-design-spec.md "State Management / appStore"
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  /* === State === */
  const sidebarCollapsed = ref<boolean>(false)
  const theme = ref<'light' | 'dark'>('light')
  const globalLoading = ref<boolean>(false)

  /* === Actions === */
  function toggleSidebar(): void {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setSidebarCollapsed(value: boolean): void {
    sidebarCollapsed.value = value
  }

  function setLoading(value: boolean): void {
    globalLoading.value = value
  }

  function setTheme(value: 'light' | 'dark'): void {
    theme.value = value
  }

  return {
    sidebarCollapsed,
    theme,
    globalLoading,
    toggleSidebar,
    setSidebarCollapsed,
    setLoading,
    setTheme
  }
})