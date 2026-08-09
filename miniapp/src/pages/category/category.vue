<!--
  分类页 Category（T2.2，tabBar 分类 tab）
  对齐 spec.md 3.1（首页分类导航扩展）+ tasks.md T2.2：
  - 分类列表展示（调用 categoryApi.list()）
  - 左侧一级分类 + 右侧二级分类（经典电商分类布局）
  - 点击分类跳转商品列表（navigate.to，带 categoryId 参数）
  - 使用 stores/category.ts 缓存
-->
<template>
  <view class="category-page">
    <!-- 顶部自定义导航栏 -->
    <view class="custom-navbar" :style="{ paddingTop: statusBarHeight + 'px' }">
      <view class="navbar-content">
        <text class="navbar-title">商品分类</text>
      </view>
    </view>
    <view class="navbar-placeholder" :style="{ height: (statusBarHeight + 44) + 'px' }" />

    <!-- 主体：左侧一级 + 右侧二级 -->
    <view class="category-body">
      <!-- 左侧一级分类 -->
      <scroll-view scroll-y class="left-panel">
        <view
          class="left-item"
          :class="{ active: activeFirstId === '' }"
          @click="handleSelectFirst('')"
        >
          <text class="left-text">全部</text>
        </view>
        <view
          class="left-item"
          :class="{ active: activeFirstId === category.id }"
          v-for="category in firstLevelCategories"
          :key="category.id"
          @click="handleSelectFirst(category.id)"
        >
          <text class="left-text">{{ category.name }}</text>
        </view>
      </scroll-view>

      <!-- 右侧二级分类 -->
      <scroll-view scroll-y class="right-panel">
        <!-- 当前一级分类标题 -->
        <view class="right-header" v-if="currentFirstCategory">
          <text class="right-title">{{ currentFirstCategory.name }}</text>
        </view>
        <view class="right-header" v-else>
          <text class="right-title">全部分类</text>
        </view>

        <!-- 二级分类网格 -->
        <view class="sub-category-grid" v-if="currentSubCategories.length > 0">
          <view
            class="sub-item"
            v-for="sub in currentSubCategories"
            :key="sub.id"
            @click="handleSubClick(sub)"
          >
            <view class="sub-icon-wrap">
              <u-image
                v-if="sub.icon"
                :src="sub.icon"
                mode="aspectFill"
                width="100rpx"
                height="100rpx"
                radius="8rpx"
                :lazy-load="true"
              />
              <view v-else class="sub-icon-default">
                <u-icon name="grid" size="40rpx" color="#ff4d4f" />
              </view>
            </view>
            <text class="sub-name ellipsis-1">{{ sub.name }}</text>
          </view>
        </view>

        <!-- 当无二级分类时，显示一级分类下的"查看全部" -->
        <view class="view-all" v-if="currentFirstCategory && currentSubCategories.length === 0">
          <u-button
            type="error"
            text="查看该分类下全部商品"
            @click="handleViewAll"
          />
        </view>

        <!-- 空状态 -->
        <u-empty
          v-if="!currentFirstCategory && firstLevelCategories.length === 0 && !loading"
          text="暂无分类"
          mode="data"
          margin-top="120"
        />
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useCategoryStore } from '@/stores/category'
import { navigate } from '@/utils/navigate'
import { ensureStringId } from '@/utils/snowflake'
import { showToast } from '@/utils/toast'
import type { CategoryVO } from '@/types'

// ============ 状态栏高度 ============
const statusBarHeight = ref<number>(20)

// ============ 分类 store ============
const categoryStore = useCategoryStore()
const loading = computed(() => categoryStore.loading)

// ============ 当前选中的一级分类 ID ============
const activeFirstId = ref<string>('')

// ============ 一级分类列表（level=1 或 parentId='0'/'') ============
const firstLevelCategories = computed<CategoryVO[]>(() => {
  const list = categoryStore.categoryList
  // 取 level=1 或 parentId 为空/'0' 的分类作为一级
  return list.filter(c => c.level === 1 || c.parentId === '' || c.parentId === '0')
})

// ============ 当前一级分类对象 ============
const currentFirstCategory = computed<CategoryVO | undefined>(() => {
  if (!activeFirstId.value) return undefined
  return firstLevelCategories.value.find(c => c.id === activeFirstId.value)
})

// ============ 当前一级下的二级分类 ============
const currentSubCategories = computed<CategoryVO[]>(() => {
  if (!currentFirstCategory.value) {
    // 全部：展示所有一级分类作为入口
    return firstLevelCategories.value
  }
  // 当前一级的 children 或同级 parentId 匹配
  if (currentFirstCategory.value.children && currentFirstCategory.value.children.length > 0) {
    return currentFirstCategory.value.children
  }
  // 从全列表中查找 parentId 等于当前一级 id 的子分类
  return categoryStore.categoryList.filter(c => c.parentId === currentFirstCategory.value!.id)
})

// ============ 初始化 ============
onMounted(() => {
  const sysInfo = uni.getSystemInfoSync()
  statusBarHeight.value = sysInfo.statusBarHeight || 20

  if (categoryStore.categoryList.length === 0) {
    categoryStore.fetchCategories().catch((e) => {
      console.error('[category] fetchCategories error:', e)
      showToast('加载分类失败', 'error')
    })
  }
})

// ============ 事件处理 ============

/** 选择一级分类 */
function handleSelectFirst(id: string) {
  activeFirstId.value = id
}

/** 点击二级分类 → 跳转商品列表 */
function handleSubClick(sub: CategoryVO) {
  navigate.to('pages-product/pages/product-list/product-list', {
    categoryId: ensureStringId(sub.id),
    categoryName: sub.name
  })
}

/** 查看当前一级分类下全部商品 */
function handleViewAll() {
  if (!currentFirstCategory.value) return
  navigate.to('pages-product/pages/product-list/product-list', {
    categoryId: ensureStringId(currentFirstCategory.value.id),
    categoryName: currentFirstCategory.value.name
  })
}
</script>

<style lang="scss" scoped>
.category-page {
  min-height: 100vh;
  background-color: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 自定义导航栏 */
.custom-navbar {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 999;
  background-color: #ffffff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);

  .navbar-content {
    height: 88rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 0 24rpx;

    .navbar-title {
      font-size: 36rpx;
      font-weight: bold;
      color: #303133;
    }
  }
}

.navbar-placeholder {
  width: 100%;
}

/* 主体 */
.category-body {
  flex: 1;
  display: flex;
  background-color: #ffffff;

  .left-panel {
    width: 200rpx;
    height: calc(100vh - 88rpx);
    background-color: #f5f5f5;
    flex-shrink: 0;

    .left-item {
      padding: 32rpx 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;

      .left-text {
        font-size: 26rpx;
        color: #606266;
      }

      &.active {
        background-color: #ffffff;

        .left-text {
          color: #ff4d4f;
          font-weight: bold;
        }

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 6rpx;
          height: 36rpx;
          background-color: #ff4d4f;
          border-radius: 0 4rpx 4rpx 0;
        }
      }
    }
  }

  .right-panel {
    flex: 1;
    height: calc(100vh - 88rpx);
    padding: 24rpx;

    .right-header {
      padding: 16rpx 0 24rpx;

      .right-title {
        font-size: 30rpx;
        font-weight: bold;
        color: #303133;
      }
    }

    .sub-category-grid {
      display: flex;
      flex-wrap: wrap;
      gap: 24rpx;

      .sub-item {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12rpx;
        width: calc((100% - 48rpx) / 3);
        padding: 16rpx 0;
        background-color: #fafafa;
        border-radius: 12rpx;

        .sub-icon-wrap {
          width: 100rpx;
          height: 100rpx;
          display: flex;
          align-items: center;
          justify-content: center;

          .sub-icon-default {
            width: 100rpx;
            height: 100rpx;
            border-radius: 8rpx;
            background-color: #fff5f5;
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }

        .sub-name {
          font-size: 24rpx;
          color: #606266;
          max-width: 180rpx;
        }
      }
    }

    .view-all {
      margin-top: 48rpx;
      padding: 0 24rpx;
    }
  }
}

/* 单行省略 */
.ellipsis-1 {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}
</style>
