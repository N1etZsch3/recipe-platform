<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick, inject } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { listRecipies, getCategories } from '@/api/recipe'
import { Eye, MessageCircle, Frown, Loader2, Heart } from 'lucide-vue-next'
import { useToast } from '@/components/Toast.vue'
import UserAvatar from '@/components/UserAvatar.vue'


const router = useRouter()
const route = useRoute()
const { showToast } = useToast()

const searchQuery = ref('')
const categories = ref([{ id: 0, name: '全部' }])
const currentCategoryId = ref(0)

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
  } else if (width < 1280) {
    columnCount.value = 4
  } else {
    columnCount.value = 5
  }
}

// 初始化列数组
const initColumns = () => {
  columns.value = Array.from({ length: columnCount.value }, () => [])
}

// 分发数据到列中 (Round-Robin)
const distributeRecipes = (newItems) => {
  newItems.forEach((item, index) => {
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

// 加载分类
const loadCategories = async () => {
    try {
        const res = await getCategories()
        // 这里只取前8个或者全部，设计上可能需要 scroll
        // 假设 res 是 array
        categories.value = [{ id: 0, name: '全部' }, ...res]
    } catch (e) {
        console.error('Failed to load categories')
        // Fallback or retry?
    }
}

// 选择分类
const selectCategory = (cat) => {
  currentCategoryId.value = cat.id
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
      categoryId: currentCategoryId.value !== 0 ? currentCategoryId.value : undefined
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
  loadCategories() // Load dynamic categories
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
  <div class="p-8">
    <!-- 分类标签栏 -->
    <div class="mb-8 flex flex-nowrap items-center gap-3 overflow-x-auto scrollbar-hide pb-2">
      <button 
        v-for="cat in categories" 
        :key="cat.id"
        @click="selectCategory(cat)"
        :class="[
          'px-5 py-2.5 rounded-full text-sm font-medium transition-all duration-300 flex-shrink-0',
          currentCategoryId === cat.id
            ? 'bg-orange-500 text-white shadow-lg shadow-orange-200' 
            : 'bg-white text-gray-600 hover:bg-gray-100 border border-transparent shadow-sm hover:shadow'
        ]"
      >
        {{ cat.name }}
      </button>
    </div>

    <!-- 菜谱瀑布流列表 -->
    <div class="flex gap-6 items-start masonry-container">
      <div v-for="(col, colIndex) in columns" :key="colIndex" class="flex-1 flex flex-col gap-6">
        <div 
          v-for="recipe in col"
          :key="recipe.id" 
          :data-recipe-id="recipe.id"
          @click="router.push(`/recipe/${recipe.id}`)"
          class="bg-transparent group cursor-pointer"
        >
          <!-- 图片区域 -->
          <div class="overflow-hidden relative rounded-2xl mb-3 shadow-sm group-hover:shadow-md transition-shadow">
            <img :src="recipe.image" :alt="recipe.title" class="w-full group-hover:scale-105 transition-transform duration-500 will-change-transform">
            <!-- 仅保留视频标记等关键遮罩(如有) -->
          </div>
          
          <!-- 信息区域 -->
          <div class="px-1">
            <!-- 标题: 允许两行, 字体稍微改小加粗 -->
            <div class="font-bold text-gray-900 mb-2 line-clamp-2 text-[15px] leading-snug group-hover:text-gray-700 transition-colors">
               <span class="inline-block bg-orange-50 text-orange-600 text-[10px] px-1.5 py-0.5 rounded mr-1.5 align-middle border border-orange-100 font-medium h-[18px] leading-[16px]">
                  {{ recipe.category }}
               </span>
               <span class="align-middle">{{ recipe.title }}</span>
            </div>
            
            <div class="flex items-center justify-between">
              <!-- 作者信息 (更紧凑) -->
              <div class="flex items-center gap-2 min-w-0">
                <UserAvatar 
                  :src="recipe.authorAvatar" 
                  :name="recipe.authorName"
                  class="w-5 h-5 flex-shrink-0"
                />
                <span class="text-xs text-gray-500 truncate hover:text-gray-900 transition-colors">{{ recipe.authorName }}</span>
              </div>
              
              <!-- 互动数据: 评论 + 点赞 -->
              <div class="flex items-center gap-3 text-gray-400 text-xs">
                 <div class="flex items-center gap-0.5">
                    <MessageCircle class="w-3.5 h-3.5" />
                    <span class="font-medium">{{ recipe.commentCount }}</span>
                 </div>
                 <div class="flex items-center gap-0.5">
                    <Heart :class="['w-3.5 h-3.5 transition-colors', recipe.isFavorite ? 'fill-red-500 text-red-500' : 'text-gray-400 hover:text-gray-600']" /> 
                    <span class="font-medium">{{ recipe.favoriteCount }}</span>
                 </div>
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
