<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'

// App 入口（对齐 plan.md 第 1.4 节）
onLaunch(() => {
    console.log('App Launch')
    // 启动时恢复登录态（检查 Token 有效性）
    try {
        const userStore = useUserStore()
        userStore.restoreFromStorage()
    } catch (e) {
        console.error('恢复登录态失败', e)
    }
})

onShow(() => {
    console.log('App Show')
})

onHide(() => {
    console.log('App Hide')
})
</script>

<style lang="scss">
/* App.vue 全局样式（对齐 plan.md 第 6.2 节，不带 scoped 全局生效） */
@import 'uview-plus/theme.scss';
@import 'uview-plus/index.scss';

page {
    background-color: #f8f8f8;
    font-size: 28rpx;
    color: #303133;
    font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, sans-serif;
}

/* 全局工具类 */
.flex {
    display: flex;
}

.flex-center {
    display: flex;
    align-items: center;
    justify-content: center;
}

.flex-between {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.text-ellipsis {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.text-ellipsis-2 {
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
}
</style>