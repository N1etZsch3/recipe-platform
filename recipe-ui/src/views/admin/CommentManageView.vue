<script setup>
import { ref, onMounted, computed } from 'vue'
import { listComments, deleteComment, listUsers, listAllRecipes } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
    Search, Trash2, ExternalLink, ChevronLeft, ChevronRight, 
    Filter, ArrowUpDown, User, ChefHat, X, MessageSquare
} from 'lucide-vue-next'

const { showToast } = useToast()
const { confirm } = useModal()

const loading = ref(false)
const comments = ref([])
const keyword = ref('')
const userIdFilter = ref(null)
const recipeIdFilter = ref(null)
const sortBy = ref('newest')

const users = ref([])
const recipes = ref([])

const pagination = ref({
    current: 1,
    size: 10,
    total: 0
})

const sortOptions = [
    { value: 'newest', label: '最新优先' },
    { value: 'oldest', label: '最早优先' }
]

const fetchComments = async () => {
    loading.value = true
    try {
        const res = await listComments({
            page: pagination.value.current,
            size: pagination.value.size,
            keyword: keyword.value || undefined,
            userId: userIdFilter.value || undefined,
            recipeId: recipeIdFilter.value || undefined,
            sortBy: sortBy.value === 'oldest' ? 'oldest' : undefined
        })
        if (res) {
            comments.value = res.records || []
            pagination.value.total = res.total || 0
        }
    } catch (error) {
        console.error('Failed to fetch comments', error)
        showToast('获取评论列表失败')
    } finally {
        loading.value = false
    }
}

const fetchFilterOptions = async () => {
    try {
        const userRes = await listUsers({ page: 1, size: 100 })
        if (userRes?.records) {
            users.value = userRes.records
        }
        const recipeRes = await listAllRecipes({ page: 1, size: 100 })
        if (recipeRes?.records) {
            recipes.value = recipeRes.records
        }
    } catch (error) {
        console.error('Failed to fetch filter options', error)
    }
}

const handleSearch = () => {
    pagination.value.current = 1
    fetchComments()
}

const handleFilterChange = () => {
    pagination.value.current = 1
    fetchComments()
}

const clearFilters = () => {
    keyword.value = ''
    userIdFilter.value = null
    recipeIdFilter.value = null
    sortBy.value = 'newest'
    pagination.value.current = 1
    fetchComments()
}

const handlePageChange = (page) => {
    pagination.value.current = page
    fetchComments()
}

const handleDelete = async (comment) => {
    const confirmed = await confirm('确定要删除这条评论吗？', { danger: true })
    if (!confirmed) return

    try {
        await deleteComment(comment.id)
        comments.value = comments.value.filter(c => c.id !== comment.id)
        pagination.value.total--
        showToast('删除成功')
    } catch (error) {
        console.error(error)
        showToast('删除失败')
    }
}

const viewRecipe = (recipeId) => {
    window.open(`/recipe/${recipeId}`, '_blank')
}

const totalPages = computed(() => Math.ceil(pagination.value.total / pagination.value.size))

const visiblePages = computed(() => {
    const pages = []
    const total = totalPages.value
    const current = pagination.value.current
    
    if (total <= 5) {
        for (let i = 1; i <= total; i++) pages.push(i)
    } else {
        if (current <= 3) {
            pages.push(1, 2, 3, 4, '...', total)
        } else if (current >= total - 2) {
            pages.push(1, '...', total - 3, total - 2, total - 1, total)
        } else {
            pages.push(1, '...', current - 1, current, current + 1, '...', total)
        }
    }
    return pages
})

const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const date = new Date(dateStr)
    return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })}`
}

const hasActiveFilters = computed(() => {
    return keyword.value || userIdFilter.value || recipeIdFilter.value || sortBy.value !== 'newest'
})

onMounted(() => {
    fetchComments()
    fetchFilterOptions()
})
</script>

<template>
    <div class="space-y-6 p-5 h-full overflow-y-auto">
        <!-- 页面标题 -->
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-2xl font-bold text-gray-800">评论管理</h1>
                <p class="text-sm text-gray-500 mt-1">管理和审核用户评论</p>
            </div>
            <div class="flex items-center gap-2 px-4 py-2 bg-purple-50 text-purple-600 rounded-xl">
                <MessageSquare class="w-4 h-4" />
                <span class="text-sm font-medium">{{ pagination.total }} 条评论</span>
            </div>
        </div>

        <!-- 搜索和筛选 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-50 p-5">
            <div class="flex flex-wrap gap-4">
                <!-- 关键词搜索 -->
                <div class="flex-1 min-w-[200px] relative">
                    <Search class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" />
                    <input
                        v-model="keyword"
                        @keyup.enter="handleSearch"
                        type="text"
                        placeholder="搜索评论内容..."
                        class="w-full pl-11 pr-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition"
                    />
                </div>

                <!-- 用户筛选 -->
                <div class="relative min-w-[150px]">
                    <User class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none z-10" />
                    <select
                        v-model="userIdFilter"
                        @change="handleFilterChange"
                        class="appearance-none w-full pl-11 pr-10 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition cursor-pointer"
                    >
                        <option :value="null">全部用户</option>
                        <option v-for="user in users" :key="user.id" :value="user.id">
                            {{ user.nickname }}
                        </option>
                    </select>
                    <div class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none">
                        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </div>
                </div>

                <!-- 菜谱筛选 -->
                <div class="relative min-w-[150px]">
                    <ChefHat class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none z-10" />
                    <select
                        v-model="recipeIdFilter"
                        @change="handleFilterChange"
                        class="appearance-none w-full pl-11 pr-10 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition cursor-pointer"
                    >
                        <option :value="null">全部菜谱</option>
                        <option v-for="recipe in recipes" :key="recipe.id" :value="recipe.id">
                            {{ recipe.title }}
                        </option>
                    </select>
                    <div class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none">
                        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </div>
                </div>

                <!-- 排序 -->
                <div class="relative min-w-[130px]">
                    <ArrowUpDown class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none z-10" />
                    <select
                        v-model="sortBy"
                        @change="handleFilterChange"
                        class="appearance-none w-full pl-11 pr-10 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition cursor-pointer"
                    >
                        <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
                            {{ opt.label }}
                        </option>
                    </select>
                    <div class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none">
                        <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                        </svg>
                    </div>
                </div>

                <button 
                    @click="handleSearch"
                    class="px-6 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl hover:from-orange-600 hover:to-amber-600 transition font-medium shadow-md shadow-orange-200"
                >
                    搜索
                </button>

                <!-- 清除筛选 -->
                <button 
                    v-if="hasActiveFilters"
                    @click="clearFilters"
                    class="px-4 py-2.5 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-xl transition flex items-center gap-1.5 font-medium"
                >
                    <X class="w-4 h-4" />
                    清除
                </button>
            </div>
        </div>

        <!-- 评论列表 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-50 overflow-hidden">
            <div v-if="loading" class="p-16 text-center text-gray-400">
                <div class="flex flex-col items-center">
                    <div class="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
                    <span class="mt-3">加载中...</span>
                </div>
            </div>
            <div v-else-if="comments.length === 0" class="p-16 text-center text-gray-400">
                <div class="flex flex-col items-center">
                    <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-3">
                        <MessageSquare class="w-8 h-8 text-gray-300" />
                    </div>
                    <span>暂无评论</span>
                </div>
            </div>
            
            <div v-else class="divide-y divide-gray-50">
                <div 
                    v-for="comment in comments" 
                    :key="comment.id"
                    class="p-5 hover:bg-gray-50/50 transition group"
                >
                    <div class="flex items-start gap-4">
                        <!-- 用户头像 -->
                        <img 
                            :src="comment.userAvatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + comment.userId" 
                            class="w-11 h-11 rounded-xl bg-gray-100 flex-shrink-0 object-cover"
                        />
                        
                        <!-- 评论内容 -->
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2 mb-1.5">
                                <span class="font-medium text-gray-800">{{ comment.userNickname || '用户' + comment.userId }}</span>
                                <span class="text-xs text-gray-400">{{ formatDate(comment.createTime) }}</span>
                            </div>
                            <p class="text-gray-600 mb-2.5 leading-relaxed">{{ comment.content }}</p>
                            <div class="flex items-center gap-2 text-sm">
                                <span class="text-gray-400">评论于</span>
                                <button 
                                    @click="viewRecipe(comment.recipeId)"
                                    class="text-orange-500 hover:text-orange-600 flex items-center gap-1 font-medium transition"
                                >
                                    {{ comment.recipeTitle || '菜谱 #' + comment.recipeId }}
                                    <ExternalLink class="w-3.5 h-3.5" />
                                </button>
                            </div>
                        </div>

                        <!-- 操作按钮 -->
                        <button 
                            @click="handleDelete(comment)"
                            class="p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition flex-shrink-0 opacity-0 group-hover:opacity-100"
                            title="删除评论"
                        >
                            <Trash2 class="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>

            <!-- 分页 -->
            <div v-if="totalPages > 1" class="px-6 py-4 border-t border-gray-100 flex items-center justify-between bg-gray-50/50">
                <span class="text-sm text-gray-500">
                    第 {{ pagination.current }} 页，共 {{ pagination.total }} 条评论
                </span>
                <div class="flex items-center gap-1">
                    <button 
                        @click="handlePageChange(pagination.current - 1)"
                        :disabled="pagination.current <= 1"
                        class="p-2 rounded-lg hover:bg-white hover:shadow-sm disabled:opacity-50 disabled:cursor-not-allowed transition"
                    >
                        <ChevronLeft class="w-4 h-4 text-gray-600" />
                    </button>
                    
                    <template v-for="page in visiblePages" :key="page">
                        <span v-if="page === '...'" class="px-2 text-gray-400">...</span>
                        <button 
                            v-else
                            @click="handlePageChange(page)"
                            :class="[
                                'min-w-[36px] h-9 rounded-lg text-sm font-medium transition',
                                pagination.current === page 
                                    ? 'bg-orange-500 text-white shadow-md shadow-orange-200' 
                                    : 'text-gray-600 hover:bg-white hover:shadow-sm'
                            ]"
                        >
                            {{ page }}
                        </button>
                    </template>
                    
                    <button 
                        @click="handlePageChange(pagination.current + 1)"
                        :disabled="pagination.current >= totalPages"
                        class="p-2 rounded-lg hover:bg-white hover:shadow-sm disabled:opacity-50 disabled:cursor-not-allowed transition"
                    >
                        <ChevronRight class="w-4 h-4 text-gray-600" />
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>
