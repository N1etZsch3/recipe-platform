<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { listRecipies } from '@/api/recipe'
import { Eye, MessageCircle, Frown, Loader2, Heart } from 'lucide-vue-next'
import { useToast } from '@/components/Toast.vue'


const router = useRouter()
const route = useRoute()
const { showToast } = useToast()

import { RECIPE_CATEGORIES } from '@/utils/constants'

const searchQuery = ref('')
const filterCategory = ref('全部')
const categories = ['全部', ...RECIPE_CATEGORIES]
const mainScrollRef = inject('mainScrollRef', null)
const scrollContainer = computed(() => {
  return mainScrollRef?.value
})

// 分页状态
const currentPage = ref(1)
const pageSize = 12
const hasMore = ref(true)
const loadingMore = ref(false)

// 防抖控制
const isLoadingDebounced = ref(false)
const DEBOUNCE_DELAY = 300

// 手动瀑布流逻辑
const columns = ref([])
const columnCount = ref(4)

const updateColumnCount = () => {
  const width = window.innerWidth
  if (width < 640) {
    columnCount.value = 1
  } else if (width < 768) {
    columnCount.value = 2
  } else if (width < 1024) {
    columnCount.value = 3
  } else {
    columnCount.value = 4
  }
}

// 初始化列数组
const initColumns = () => {
  columns.value = Array.from({ length: columnCount.value }, () => [])
}

// 分发数据到列中 (Round-Robin)
const distributeRecipes = (newItems) => {
  newItems.forEach((item, index) => {
    // 简单的轮询分发，保证顺序和位置固定
    // 为了接在之前的末尾，我们需要知道全局索引，但这里只处理增量
    // 如果是追加，我们其实需要知道当前所有 items 的总数来决定起始 colIndex 吗？
    // 不需要，Masonry 的通常做法是 "Add to shortest column" (不稳定) 或 "Available slots"
    // 用户想要 "增量渲染有问题，排列的顺序不同"， round-robin 是最稳定的。
    // 但是 round-robin 在追加时，如果不知道 offset，会从第0列开始放。
    // 假设之前有 12 个元素 (4列 x 3行)，刚好填满。
    // 如果之前有 13 个 (4列: 4, 3, 3, 3)，下一个应该放在第 1 列 (index 1)。
    
    // 计算当前总元素数量，用于确定下一个元素的放置位置
    const currentTotal = columns.value.reduce((sum, col) => sum + col.length, 0)
    const targetColIndex = currentTotal % columnCount.value
    columns.value[targetColIndex].push(item)
  })
}

// 重新分发所有数据 (用于 resize 或 reset)
const reDistributeAll = (allRecipes) => {
  initColumns()
  allRecipes.forEach((item, index) => {
    const targetColIndex = index % columnCount.value
    columns.value[targetColIndex].push(item)
  })
}

// 监听窗口大小变化
const handleResize = () => {
  const oldCols = columnCount.value
  updateColumnCount()
  if (oldCols !== columnCount.value) {
    // 重新排版
    reDistributeAll(recipes.value)
  }
}

// 选择分类
const selectCategory = (cat) => {
  filterCategory.value = cat
  resetAndFetch()
}

const recipes = ref([]) // 保持源数据，用于 resize 时重新计算
const loading = ref(false)

// 重置并重新获取
const resetAndFetch = () => {
  currentPage.value = 1
  recipes.value = []
  initColumns() // 清空列
  hasMore.value = true
  isLoadingDebounced.value = false
  fetchRecipes()
}

// 获取菜谱
const fetchRecipes = async (append = false) => {
  if (!append) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const params = {
      page: currentPage.value,
      size: pageSize,
      keyword: searchQuery.value || undefined,
      category: filterCategory.value !== '全部' ? filterCategory.value : undefined
    }
    const res = await listRecipies(params)
    
    const newRecipes = res.records.map(r => ({
      ...r,
      image: r.coverImage,
      category: r.categoryName || '美食',
      viewCount: r.viewCount || 0,
      commentCount: r.commentCount || 0,
      favoriteCount: r.favoriteCount || 0,
      isFavorite: r.isFavorite || false,
      publishTime: r.createTime
    }))
    
    if (append) {
      recipes.value.push(...newRecipes)       // 更新源数据
      distributeRecipes(newRecipes)           // 仅分发新增数据到列中
    } else {
      recipes.value = newRecipes              // 更新源数据
      reDistributeAll(newRecipes)             // 分发所有数据
    }
    
    // 判断是否还有更多
    hasMore.value = res.records.length >= pageSize && recipes.value.length < res.total
  } catch (error) {
    showToast('获取菜谱失败')
    console.error(error)
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

// 加载更多（带防抖）
const loadMore = () => {
  if (loadingMore.value || !hasMore.value || isLoadingDebounced.value) return
  
  // 启用防抖
  isLoadingDebounced.value = true
  setTimeout(() => {
    isLoadingDebounced.value = false
  }, DEBOUNCE_DELAY)
  
  currentPage.value++
  fetchRecipes(true)
}

// 滚动触发元素引用
const loadMoreTrigger = ref(null)
let observer = null

// 设置 IntersectionObserver
const setupIntersectionObserver = () => {
  if (observer) observer.disconnect()

  observer = new IntersectionObserver((entries) => {
    const entry = entries[0]
    // 只要可见，且有更多数据，不在加载中，就加载更多
    if (entry.isIntersecting && hasMore.value && !loadingMore.value && !loading.value) {
      loadMore()
    }
  }, {
    root: null, // 默认为视口，也可以设置为 scrollContainer.value
    rootMargin: '100px', // 提前 100px 触发
    threshold: 0.1
  })

  if (loadMoreTrigger.value) {
    observer.observe(loadMoreTrigger.value)
  }
}

// 监听 hasMore 变化，如果有更多数据，确保观察器处于激活状态
watch(hasMore, (val) => {
  if (val) {
    nextTick(() => {
      // 重新检查或者重新挂载观察（如果 DOM 变动）
      if (loadMoreTrigger.value && observer) {
        observer.observe(loadMoreTrigger.value)
      }
    })
  }
})

// 监听加载状态，结束后检查是否需要再次加载（此时内容可能还不足以撑开屏幕）
watch(loadingMore, (val) => {
  if (!val && hasMore.value) {
    nextTick(() => {
      // 这里的逻辑是：如果加载完一批数据后，触发元素仍然可见（例如屏幕很大，数据很少），
      // IntersectionObserver 不会再次触发（因为状态没有从不可见变为可见），
      // 所以我们手动检查一下位置或者简单地依赖下一次滚动
      // 但对于 IntersectionObserver，只要一直可见，它只触发一次 entry。
      // 所以如果内容太少，我们需要手动再次触发？
      // 实际上 standard IO 只在交叉状态变化时触发。
      // 如果加载后 loadMoreTrigger 依然在视口内，我们需要继续加载。
      // 可以通过 disconnect 再 observe 来强制触发，或者简单判断位置。
      // 更简单的做法是：不用处理，用户稍微滚一下就好。
      // 或者：disconnect -> observe hack
      if (observer && loadMoreTrigger.value) {
        // 强制重新观察以应对连续加载
        observer.unobserve(loadMoreTrigger.value)
        observer.observe(loadMoreTrigger.value)
      }
    })
  }
})

onMounted(() => {
  updateColumnCount()
  initColumns()
  window.addEventListener('resize', handleResize)
  
  setupIntersectionObserver()
  if (!route.query.keyword) {
    fetchRecipes()
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (observer) {
    observer.disconnect()
  }
})
</script>

<template>
  <div class="p-6">
    <!-- 分类标签栏 -->
    <div class="mb-6 flex flex-wrap items-center gap-2">
      <button 
        v-for="cat in categories" 
        :key="cat"
        @click="selectCategory(cat)"
        :class="[
          'px-4 py-2 rounded-full text-sm font-medium transition-all',
          filterCategory === cat 
            ? 'bg-orange-500 text-white shadow-md shadow-orange-200' 
            : 'bg-white text-gray-600 hover:bg-orange-50 hover:text-orange-600 border border-gray-200 hover:border-orange-300'
        ]"
      >
        {{ cat }}
      </button>
    </div>

    <!-- 菜谱瀑布流列表 -->
    <div class="flex gap-5 items-start masonry-container">
      <div v-for="(col, colIndex) in columns" :key="colIndex" class="flex-1 flex flex-col gap-5">
        <div 
          v-for="recipe in col"
          :key="recipe.id" 
          :data-recipe-id="recipe.id"
          @click="router.push(`/recipe/${recipe.id}`)"
          class="bg-white rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer overflow-hidden group"
        >
          <!-- 图片区域 - 保持原图比例实现瀑布流效果 -->
          <div class="overflow-hidden relative">
            <img :src="recipe.image" :alt="recipe.title" class="w-full object-cover group-hover:scale-105 transition duration-500">
            <!-- 浏览量角标 -->
            <div class="absolute top-3 right-3 bg-black/40 backdrop-blur-sm text-white text-xs px-2 py-1 rounded-lg flex items-center gap-1">
              <Eye class="w-3 h-3" /> {{ recipe.viewCount }}
            </div>
            <!-- 分类标签 -->
            <div class="absolute bottom-3 left-3">
              <span class="text-xs bg-white/95 backdrop-blur-sm text-orange-600 px-3 py-1 rounded-full font-medium shadow-sm">
                {{ recipe.category }}
              </span>
            </div>
          </div>
          <!-- 信息区域 -->
          <div class="p-4">
            <h3 class="font-semibold text-gray-800 mb-3 line-clamp-1 group-hover:text-orange-600 transition">{{ recipe.title }}</h3>
            <div class="flex items-center justify-between">
              <!-- 作者信息 -->
              <div class="flex items-center gap-2 min-w-0">
                <div class="w-6 h-6 rounded-full overflow-hidden bg-orange-100 flex-shrink-0">
                  <img v-if="recipe.authorAvatar" :src="recipe.authorAvatar" class="w-full h-full object-cover">
                  <div v-else class="w-full h-full flex items-center justify-center text-xs text-orange-500 font-bold">
                    {{ recipe.authorName?.charAt(0).toUpperCase() || 'U' }}
                  </div>
                </div>
                <span class="text-xs text-gray-500 truncate">{{ recipe.authorName }}</span>
              </div>
              <!-- 互动数据 -->
              <div class="flex items-center gap-2 text-gray-400 text-xs">
                <span class="flex items-center gap-0.5">
                  <MessageCircle class="w-3.5 h-3.5" /> {{ recipe.commentCount }}
                </span>
                <span :class="['flex items-center gap-0.5', recipe.isFavorite ? 'text-red-500' : '']">
                  <Heart :class="['w-3.5 h-3.5', recipe.isFavorite ? 'fill-current' : '']" /> {{ recipe.favoriteCount }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5 mt-5">
      <div v-for="n in 8" :key="'skeleton-' + n" class="bg-white rounded-2xl overflow-hidden">
        <div class="aspect-[4/3] skeleton"></div>
        <div class="p-4 space-y-3">
          <div class="h-4 skeleton rounded w-3/4"></div>
          <div class="flex items-center gap-2">
            <div class="w-6 h-6 skeleton rounded-full"></div>
            <div class="h-3 skeleton rounded w-16"></div>
          </div>
        </div>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="recipes.length === 0 && !loading" class="text-center py-20 text-gray-400">
      <Frown class="w-16 h-16 mx-auto mb-4 opacity-30" />
      <p class="text-lg font-medium mb-1">没有找到相关菜谱</p>
      <p class="text-sm">换个关键词或分类试试吧</p>
    </div>

    <!-- 加载状态指示器 -->
    <div ref="loadMoreTrigger" class="h-20 flex items-center justify-center">
      <div v-if="loadingMore" class="flex items-center gap-2 text-gray-400">
        <Loader2 class="w-5 h-5 animate-spin" />
        <span class="text-sm">加载中...</span>
      </div>
      <div v-else-if="!hasMore && recipes.length > 0" class="text-sm text-gray-400">
        — 已加载全部菜谱 —
      </div>
    </div>
  </div>
</template>
