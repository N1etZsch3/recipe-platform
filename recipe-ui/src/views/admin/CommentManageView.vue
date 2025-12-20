<script setup>
import { ref, onMounted, computed } from 'vue'
import { listComments, deleteComment, listUsers, listAllRecipes } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
    Search, Trash2, ExternalLink, ChevronLeft, ChevronRight, 
    Filter, ArrowUpDown, User, ChefHat, X
} from 'lucide-vue-next'

const { showToast } = useToast()
const { confirm } = useModal()

const loading = ref(false)
const comments = ref([])
const keyword = ref('')
const userIdFilter = ref(null)
const recipeIdFilter = ref(null)
const sortBy = ref('newest')

// 筛选选项数据
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
        // 获取用户列表用于筛选
        const userRes = await listUsers({ page: 1, size: 100 })
        if (userRes?.records) {
            users.value = userRes.records
        }
        // 获取菜谱列表用于筛选
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

const totalPages = () => Math.ceil(pagination.value.total / pagination.value.size)

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
    <div class="flex flex-col h-full">
        <!-- 搜索和筛选栏 (固定) -->
        <div class="bg-white rounded-xl shadow-sm p-4 flex-shrink-0">
            <div class="flex flex-wrap gap-4">
                <!-- 关键词搜索 -->
                <div class="flex-1 min-w-[200px] relative">
                    <Search class="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                        v-model="keyword"
                        @keyup.enter="handleSearch"
                        type="text"
                        placeholder="搜索评论内容..."
                        class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none"
                    />
                </div>

                <!-- 用户筛选 -->
                <div class="relative min-w-[160px]">
                    <User class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <select
                        v-model="userIdFilter"
                        @change="handleFilterChange"
                        class="custom-select w-full pl-9"
                    >
                        <option :value="null">全部用户</option>
                        <option v-for="user in users" :key="user.id" :value="user.id">
                            {{ user.nickname }}
                        </option>
                    </select>
                </div>

                <!-- 菜谱筛选 -->
                <div class="relative min-w-[160px]">
                    <ChefHat class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <select
                        v-model="recipeIdFilter"
                        @change="handleFilterChange"
                        class="custom-select w-full pl-9"
                    >
                        <option :value="null">全部菜谱</option>
                        <option v-for="recipe in recipes" :key="recipe.id" :value="recipe.id">
                            {{ recipe.title }}
                        </option>
                    </select>
                </div>

                <!-- 排序 -->
                <div class="relative min-w-[120px]">
                    <ArrowUpDown class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <select
                        v-model="sortBy"
                        @change="handleFilterChange"
                        class="custom-select w-full pl-9"
                    >
                        <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
                            {{ opt.label }}
                        </option>
                    </select>
                </div>

                <button 
                    @click="handleSearch"
                    class="px-6 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition font-medium"
                >
                    搜索
                </button>

                <!-- 清除筛选 -->
                <button 
                    v-if="hasActiveFilters"
                    @click="clearFilters"
                    class="px-4 py-2 text-gray-600 hover:text-gray-800 hover:bg-gray-100 rounded-lg transition flex items-center gap-1"
                >
                    <X class="w-4 h-4" />
                    清除筛选
                </button>
            </div>
        </div>

        <!-- 评论列表 (可滚动) -->
        <div class="flex-1 mt-4 bg-white rounded-xl shadow-sm overflow-hidden flex flex-col min-h-0">
            <div v-if="loading" class="p-12 text-center text-gray-400">加载中...</div>
            <div v-else-if="comments.length === 0" class="p-12 text-center text-gray-400">暂无评论</div>
            
            <div v-else class="flex-1 overflow-y-auto divide-y divide-gray-100">
                <div 
                    v-for="comment in comments" 
                    :key="comment.id"
                    class="p-4 hover:bg-gray-50 transition"
                >
                    <div class="flex items-start gap-4">
                        <!-- 用户头像 -->
                        <img 
                            :src="comment.userAvatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + comment.userId" 
                            class="w-10 h-10 rounded-full bg-gray-200 flex-shrink-0"
                        />
                        
                        <!-- 评论内容 -->
                        <div class="flex-1 min-w-0">
                            <div class="flex items-center gap-2 mb-1">
                                <span class="font-medium text-gray-800">{{ comment.userNickname || '用户' + comment.userId }}</span>
                                <span class="text-sm text-gray-400">{{ formatDate(comment.createTime) }}</span>
                            </div>
                            <p class="text-gray-600 mb-2">{{ comment.content }}</p>
                            <div class="flex items-center gap-2 text-sm">
                                <span class="text-gray-400">评论于</span>
                                <button 
                                    @click="viewRecipe(comment.recipeId)"
                                    class="text-orange-500 hover:text-orange-600 flex items-center gap-1"
                                >
                                    {{ comment.recipeTitle || '菜谱 #' + comment.recipeId }}
                                    <ExternalLink class="w-3 h-3" />
                                </button>
                            </div>
                        </div>

                        <!-- 操作按钮 -->
                        <button 
                            @click="handleDelete(comment)"
                            class="p-2 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition flex-shrink-0"
                            title="删除评论"
                        >
                            <Trash2 class="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>

            <!-- 分页 -->
            <div v-if="totalPages() > 1" class="px-6 py-4 border-t border-gray-200 flex items-center justify-between flex-shrink-0">
                <span class="text-sm text-gray-500">
                    共 {{ pagination.total }} 条记录
                </span>
                <div class="flex items-center gap-2">
                    <button 
                        @click="handlePageChange(pagination.current - 1)"
                        :disabled="pagination.current <= 1"
                        class="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <ChevronLeft class="w-5 h-5" />
                    </button>
                    <span class="px-4 py-2 text-sm">
                        {{ pagination.current }} / {{ totalPages() }}
                    </span>
                    <button 
                        @click="handlePageChange(pagination.current + 1)"
                        :disabled="pagination.current >= totalPages()"
                        class="p-2 rounded-lg hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        <ChevronRight class="w-5 h-5" />
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

