<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getRecipeDetail } from '@/api/recipe'
import { useToast } from '@/components/Toast.vue'
import { 
    ArrowLeft, 
    Clock, 
    User, 
    ChefHat,
    Check,
    X,
    AlertCircle
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const { showToast } = useToast()

const loading = ref(true)
const recipe = ref(null)
const error = ref(null)

// 状态映射
const statusMap = {
    0: { label: '待审核', class: 'bg-yellow-100 text-yellow-700 border-yellow-200' },
    1: { label: '已发布', class: 'bg-green-100 text-green-700 border-green-200' },
    2: { label: '已驳回', class: 'bg-red-100 text-red-700 border-red-200' },
    3: { label: '草稿', class: 'bg-gray-100 text-gray-600 border-gray-200' },
    4: { label: '处理中', class: 'bg-blue-100 text-blue-700 border-blue-200' }
}

const statusInfo = computed(() => statusMap[recipe.value?.status] || statusMap[0])

const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

const fetchRecipe = async () => {
    loading.value = true
    error.value = null
    try {
        const res = await getRecipeDetail(route.params.id)
        if (res) {
            recipe.value = res
        } else {
            error.value = '菜谱不存在'
        }
    } catch (e) {
        console.error('Failed to fetch recipe', e)
        error.value = e.message || '获取菜谱详情失败'
    } finally {
        loading.value = false
    }
}

const goBack = () => {
    router.back()
}

onMounted(() => {
    fetchRecipe()
})
</script>

<template>
    <div class="h-full overflow-y-auto bg-gray-50/50">
        <!-- 顶部导航栏 -->
        <div class="sticky top-0 z-10 bg-white border-b border-gray-200 px-6 py-4">
            <div class="flex items-center justify-between">
                <div class="flex items-center gap-4">
                    <button 
                        @click="goBack"
                        class="flex items-center gap-2 text-gray-600 hover:text-gray-800 transition"
                    >
                        <ArrowLeft class="w-5 h-5" />
                        <span class="text-sm font-medium">返回</span>
                    </button>
                    <div class="h-5 w-px bg-gray-300"></div>
                    <h1 class="text-lg font-semibold text-gray-800">菜谱预览</h1>
                </div>
                <span 
                    v-if="recipe" 
                    :class="['px-3 py-1 text-sm font-medium rounded-full border', statusInfo.class]"
                >
                    {{ statusInfo.label }}
                </span>
            </div>
        </div>

        <!-- 加载状态 -->
        <div v-if="loading" class="flex items-center justify-center h-64">
            <div class="flex flex-col items-center gap-3">
                <div class="w-10 h-10 border-3 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
                <span class="text-gray-500 text-sm">加载中...</span>
            </div>
        </div>

        <!-- 错误状态 -->
        <div v-else-if="error" class="flex flex-col items-center justify-center h-64">
            <AlertCircle class="w-12 h-12 text-red-400 mb-3" />
            <p class="text-gray-600">{{ error }}</p>
            <button 
                @click="goBack"
                class="mt-4 px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition text-sm"
            >
                返回列表
            </button>
        </div>

        <!-- 菜谱内容 -->
        <div v-else-if="recipe" class="max-w-4xl mx-auto p-6 space-y-6">
            <!-- 封面图 -->
            <div class="relative aspect-video rounded-xl overflow-hidden bg-gray-200 shadow-lg">
                <img 
                    v-if="recipe.coverImage"
                    :src="recipe.coverImage" 
                    :alt="recipe.title"
                    class="w-full h-full object-cover"
                    @error="e => e.target.src = 'https://via.placeholder.com/800x450?text=暂无图片'"
                />
                <div v-else class="w-full h-full flex items-center justify-center text-gray-400">
                    <ChefHat class="w-16 h-16" />
                </div>
            </div>

            <!-- 标题和基本信息 -->
            <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
                <h2 class="text-2xl font-bold text-gray-800 mb-4">{{ recipe.title }}</h2>
                
                <div class="flex flex-wrap items-center gap-4 text-sm text-gray-500">
                    <div class="flex items-center gap-1.5">
                        <User class="w-4 h-4" />
                        <span>{{ recipe.authorName || '未知作者' }}</span>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <ChefHat class="w-4 h-4" />
                        <span>{{ recipe.categoryName || '未分类' }}</span>
                    </div>
                    <div class="flex items-center gap-1.5">
                        <Clock class="w-4 h-4" />
                        <span>{{ formatDate(recipe.createTime) }}</span>
                    </div>
                </div>

                <!-- 驳回原因 -->
                <div 
                    v-if="recipe.status === 2 && recipe.rejectReason"
                    class="mt-4 p-4 bg-red-50 border border-red-200 rounded-lg"
                >
                    <p class="text-sm text-red-700">
                        <strong>驳回原因：</strong>{{ recipe.rejectReason }}
                    </p>
                </div>

                <!-- 描述 -->
                <div v-if="recipe.description" class="mt-4 pt-4 border-t border-gray-100">
                    <h3 class="text-sm font-medium text-gray-500 mb-2">简介</h3>
                    <p class="text-gray-700 leading-relaxed">{{ recipe.description }}</p>
                </div>
            </div>

            <!-- 食材列表 -->
            <div v-if="recipe.ingredients?.length" class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
                <h3 class="text-lg font-semibold text-gray-800 mb-4">食材清单</h3>
                <div class="grid grid-cols-2 md:grid-cols-3 gap-3">
                    <div 
                        v-for="(ing, index) in recipe.ingredients" 
                        :key="index"
                        class="flex items-center justify-between p-3 bg-orange-50 rounded-lg"
                    >
                        <span class="text-gray-800 font-medium">{{ ing.name }}</span>
                        <span class="text-orange-600 text-sm">{{ ing.amount }}</span>
                    </div>
                </div>
            </div>

            <!-- 步骤列表 -->
            <div v-if="recipe.steps?.length" class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
                <h3 class="text-lg font-semibold text-gray-800 mb-4">制作步骤</h3>
                <div class="space-y-6">
                    <div 
                        v-for="(step, index) in recipe.steps" 
                        :key="index"
                        class="flex gap-4"
                    >
                        <div class="flex-shrink-0 w-8 h-8 bg-orange-500 text-white rounded-full flex items-center justify-center font-bold text-sm">
                            {{ step.stepNo || index + 1 }}
                        </div>
                        <div class="flex-1">
                            <p class="text-gray-700 leading-relaxed mb-3">{{ step.description }}</p>
                            <img 
                                v-if="step.imageUrl"
                                :src="step.imageUrl" 
                                :alt="`步骤 ${step.stepNo || index + 1}`"
                                class="w-full max-w-md rounded-lg shadow-sm"
                                @error="e => e.target.style.display = 'none'"
                            />
                        </div>
                    </div>
                </div>
            </div>

            <!-- 底部元信息 -->
            <div class="bg-white rounded-xl p-6 shadow-sm border border-gray-100">
                <h3 class="text-sm font-medium text-gray-500 mb-3">其他信息</h3>
                <div class="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                    <div>
                        <span class="text-gray-400">菜谱ID</span>
                        <p class="font-medium text-gray-700">{{ recipe.id }}</p>
                    </div>
                    <div>
                        <span class="text-gray-400">作者ID</span>
                        <p class="font-medium text-gray-700">{{ recipe.userId }}</p>
                    </div>
                    <div>
                        <span class="text-gray-400">浏览量</span>
                        <p class="font-medium text-gray-700">{{ recipe.viewCount || 0 }}</p>
                    </div>
                    <div>
                        <span class="text-gray-400">收藏数</span>
                        <p class="font-medium text-gray-700">{{ recipe.favoriteCount || 0 }}</p>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.border-3 {
    border-width: 3px;
}
</style>
