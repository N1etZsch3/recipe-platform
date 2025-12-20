<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { listRecipies } from '@/api/recipe'
import { Search, Eye, MessageCircle, Frown, Loader2, Heart } from 'lucide-vue-next'
import { useToast } from '@/components/Toast.vue'

const router = useRouter()
const { showToast } = useToast()


import { RECIPE_CATEGORIES } from '@/utils/constants'

const searchQuery = ref('')
const filterCategory = ref('全部')
const categories = ['全部', ...RECIPE_CATEGORIES]

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
  <div class="max-w-6xl mx-auto p-4 space-y-6 pt-4">
    <!-- 搜索与分类 -->
    <div class="bg-white p-4 rounded-xl shadow-sm space-y-4">
        <div class="relative">
            <Search class="absolute left-3 top-3 text-gray-400 w-5 h-5" />
            <input 
                type="text" 
                v-model="searchQuery"
                @keyup.enter="handleSearch"
                placeholder="搜索菜谱名称..." 
                class="w-full pl-10 pr-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-300 transition"
            >
        </div>
        <div class="flex gap-2 overflow-x-auto pb-2 scrollbar-hide">
            <button 
                v-for="cat in categories" 
                :key="cat"
                @click="filterCategory = cat"
                :class="['px-4 py-1.5 rounded-full text-sm whitespace-nowrap transition', filterCategory === cat ? 'bg-orange-500 text-white shadow-md' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
            >
                {{ cat }}
            </button>
        </div>
    </div>

    <!-- 菜谱列表 -->
    <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
        <div 
            v-for="recipe in filteredRecipes" 
            :key="recipe.id" 
            @click="router.push(`/recipe/${recipe.id}`)"
            class="bg-white rounded-xl shadow-sm hover:shadow-lg transition cursor-pointer overflow-hidden group border border-transparent hover:border-orange-100"
        >
            <div class="h-48 overflow-hidden relative">
                <img :src="recipe.image" :alt="recipe.title" class="w-full h-full object-cover group-hover:scale-105 transition duration-500">
                <div class="absolute top-2 right-2 bg-black/50 text-white text-xs px-2 py-1 rounded backdrop-blur-sm flex items-center gap-1">
                    <Eye class="w-3 h-3" /> {{ recipe.viewCount }}
                </div>
            </div>
            <div class="p-4">
                <div class="flex justify-between items-start mb-2">
                    <span class="text-xs bg-orange-100 text-orange-600 px-2 py-0.5 rounded font-medium">{{ recipe.category }}</span>
                    <div class="flex items-center text-gray-400 text-xs gap-3">
                        <span class="flex items-center gap-1">
                            <MessageCircle class="w-3 h-3" /> {{ recipe.commentCount }}
                        </span>
                        <span :class="['flex items-center gap-1', recipe.isFavorite ? 'text-red-500' : '']">
                            <Heart :class="['w-3 h-3', recipe.isFavorite ? 'fill-current' : '']" /> {{ recipe.favoriteCount }}
                        </span>
                    </div>
                </div>
                <h3 class="font-bold text-gray-800 mb-2 truncate text-lg">{{ recipe.title }}</h3>
                <div class="flex items-center gap-2 text-sm text-gray-500">
                    <div class="bg-gray-100 rounded-full w-6 h-6 overflow-hidden shadow-sm flex-shrink-0">
                        <img v-if="recipe.authorAvatar" :src="recipe.authorAvatar" class="w-full h-full object-cover">
                        <div v-else class="w-full h-full flex items-center justify-center text-xs text-gray-400 font-bold">{{ recipe.authorName?.charAt(0).toUpperCase() || 'U' }}</div>
                    </div>
                    <span class="truncate">{{ recipe.authorName }}</span>
                </div>
            </div>
        </div>
        <div v-if="filteredRecipes.length === 0" class="col-span-full text-center py-20 text-gray-400 bg-white rounded-xl border border-dashed border-gray-300">
            <Frown class="w-12 h-12 mx-auto mb-2 opacity-50" />
            <p>没有找到相关菜谱</p>
        </div>
    </div>
  </div>
</template>
