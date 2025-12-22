<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listRecipies } from '@/api/recipe'
import { Search, Eye, MessageCircle, Frown, Loader2, Heart, ChevronDown, Filter } from 'lucide-vue-next'
import { useToast } from '@/components/Toast.vue'

const router = useRouter()
const { showToast } = useToast()


import { RECIPE_CATEGORIES } from '@/utils/constants'

const searchQuery = ref('')
const filterCategory = ref('全部')
const categories = ['全部', ...RECIPE_CATEGORIES]
const showCategoryDropdown = ref(false)

// 关闭下拉菜单
const closeCategoryDropdown = () => {
  showCategoryDropdown.value = false
}

// 选择分类
const selectCategory = (cat) => {
  filterCategory.value = cat
  showCategoryDropdown.value = false
}

const recipes = ref([])
const loading = ref(false)

const fetchRecipes = async () => {
  loading.value = true
  try {
    const params = {
      page: 1,
      size: 20,
      keyword: searchQuery.value || undefined,
      // categoryId: ... // Need mapping
    }
    const res = await listRecipies(params)
    // res is IPage: { records, total, ... }
    recipes.value = res.records.map(r => ({
       ...r,
       image: r.coverImage,
       category: r.categoryName || '美食',
       viewCount: r.viewCount || 0,
       commentCount: r.commentCount || 0,
       favoriteCount: r.favoriteCount || 0,
       isFavorite: r.isFavorite || false,
       publishTime: r.createTime
    }))
  } catch (error) {
    showToast('获取菜谱失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}


onMounted(() => {
  fetchRecipes()
})

const handleSearch = () => {
  fetchRecipes()
}

// Client-side filtering for category if we can't do server-side yet
const filteredRecipes = computed(() => {
    let list = recipes.value
    if (filterCategory.value !== '全部') {
        list = list.filter(r => r.category === filterCategory.value)
    }
    return list
})
</script>

<template>
  <div class="max-w-6xl mx-auto px-4 py-6">
    <!-- 搜索区域 - 下拉菜单和搜索框同一行 -->
    <div class="mb-8">
      <div class="relative max-w-3xl mx-auto">
        <div class="absolute inset-0 bg-gradient-to-r from-orange-400 to-red-400 rounded-2xl blur opacity-15"></div>
        <div class="relative bg-white rounded-2xl shadow-lg border border-gray-100 overflow-visible">
          <div class="flex items-center gap-3 p-2">
            <!-- 分类下拉菜单 -->
            <div class="relative flex-shrink-0">
              <button 
                @click="showCategoryDropdown = !showCategoryDropdown"
                class="flex items-center gap-2 px-4 py-2.5 bg-gray-50 hover:bg-gray-100 rounded-xl text-sm font-medium text-gray-700 transition-all"
              >
                <Filter class="w-4 h-4 text-orange-500" />
                <span class="hidden sm:inline">{{ filterCategory }}</span>
                <ChevronDown :class="['w-4 h-4 transition-transform text-gray-400', showCategoryDropdown ? 'rotate-180' : '']"/>
              </button>
              
              <!-- 下拉菜单 -->
              <Transition
                enter-active-class="transition duration-200 ease-out"
                enter-from-class="opacity-0 scale-95 -translate-y-2"
                enter-to-class="opacity-100 scale-100 translate-y-0"
                leave-active-class="transition duration-150 ease-in"
                leave-from-class="opacity-100 scale-100 translate-y-0"
                leave-to-class="opacity-0 scale-95 -translate-y-2"
              >
                <div 
                  v-if="showCategoryDropdown" 
                  class="absolute top-full left-0 mt-2 w-48 bg-white rounded-xl shadow-xl border border-gray-100 py-2 z-50"
                >
                  <div class="max-h-64 overflow-y-auto">
                    <button 
                      v-for="cat in categories" 
                      :key="cat"
                      @click="selectCategory(cat)"
                      :class="[
                        'w-full px-4 py-2.5 text-left text-sm transition-colors flex items-center justify-between',
                        filterCategory === cat 
                          ? 'bg-orange-50 text-orange-600 font-medium' 
                          : 'text-gray-600 hover:bg-gray-50'
                      ]"
                    >
                      {{ cat }}
                      <span v-if="filterCategory === cat" class="text-orange-500">✓</span>
                    </button>
                  </div>
                </div>
              </Transition>
              
              <!-- 点击外部关闭 -->
              <div v-if="showCategoryDropdown" @click="closeCategoryDropdown" class="fixed inset-0 z-40"></div>
            </div>

            <!-- 分隔线 -->
            <div class="w-px h-8 bg-gray-200 flex-shrink-0"></div>

            <!-- 搜索框 -->
            <div class="flex-1 flex items-center">
              <Search class="w-5 h-5 text-gray-400 flex-shrink-0" />
              <input 
                type="text" 
                v-model="searchQuery"
                @keyup.enter="handleSearch"
                placeholder="搜索你想要的菜谱..." 
                class="flex-1 px-3 py-2 text-gray-700 placeholder-gray-400 focus:outline-none text-base bg-transparent"
              >
            </div>

            <!-- 搜索按钮 -->
            <button 
              @click="handleSearch"
              class="px-5 py-2.5 bg-gradient-to-r from-orange-500 to-red-500 text-white rounded-xl font-medium text-sm hover:shadow-lg hover:shadow-orange-200 transition-all active:scale-95 flex-shrink-0"
            >
              搜索
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 菜谱列表 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-5">
      <div 
        v-for="recipe in filteredRecipes" 
        :key="recipe.id" 
        @click="router.push(`/recipe/${recipe.id}`)"
        class="bg-white rounded-2xl shadow-sm hover:shadow-xl transition-all duration-300 cursor-pointer overflow-hidden group"
      >
        <!-- 图片区域 -->
        <div class="aspect-[4/3] overflow-hidden relative">
          <img :src="recipe.image" :alt="recipe.title" class="w-full h-full object-cover group-hover:scale-105 transition duration-500">
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

      <!-- 空状态 -->
      <div v-if="filteredRecipes.length === 0 && !loading" class="col-span-full text-center py-20 text-gray-400">
        <Frown class="w-16 h-16 mx-auto mb-4 opacity-30" />
        <p class="text-lg font-medium mb-1">没有找到相关菜谱</p>
        <p class="text-sm">换个关键词或分类试试吧</p>
      </div>

      <!-- 加载状态 -->
      <template v-if="loading">
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
      </template>
    </div>
  </div>
</template>
