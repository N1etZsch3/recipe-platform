<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { listComments, deleteComment, listUsers, listAllRecipes } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
    Search, Trash2, ExternalLink, ChevronLeft, ChevronRight, 
    Filter, ArrowUpDown, User, ChefHat, X, MessageSquare,
    RefreshCw, Settings, MoreHorizontal
} from 'lucide-vue-next'

const { showToast } = useToast()
const { confirm } = useModal()

const loading = ref(false)
const comments = ref([])
const users = ref([])
const recipes = ref([])

// 搜索表单
const searchForm = ref({
    keyword: '',
    userId: '',
    recipeId: '',
    sortBy: 'newest'
})

const showFilterPanel = ref(false)
const selectedIds = ref([])

const pagination = ref({
    current: 1,
    size: 20,
    total: 0
})

const sortOptions = [
    { value: 'newest', label: '最新优先' },
    { value: 'oldest', label: '最早优先' }
]

const fetchComments = async () => {
    loading.value = true
    selectedIds.value = [] // Reset selection
    try {
        const res = await listComments({
            page: pagination.value.current,
            size: pagination.value.size,
            keyword: searchForm.value.keyword || undefined,
            userId: searchForm.value.userId || undefined,
            recipeId: searchForm.value.recipeId || undefined,
            sortBy: searchForm.value.sortBy === 'oldest' ? 'oldest' : undefined
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

const handleReset = () => {
    searchForm.value = {
        keyword: '',
        userId: '',
        recipeId: '',
        sortBy: 'newest'
    }
    handleSearch()
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
    window.open(`/backstage-m9x2k7/recipe/${recipeId}`, '_blank')
}

// Selection Logic
const isAllSelected = computed(() => {
    return comments.value.length > 0 && comments.value.every(c => selectedIds.value.includes(c.id))
})

const toggleSelectAll = (e) => {
    if (e.target.checked) {
        selectedIds.value = comments.value.map(c => c.id)
    } else {
        selectedIds.value = []
    }
}

const toggleSelect = (comment) => {
    const id = comment.id
    if (selectedIds.value.includes(id)) {
        selectedIds.value = selectedIds.value.filter(i => i !== id)
    } else {
        selectedIds.value.push(id)
    }
}

const totalPages = computed(() => Math.ceil(pagination.value.total / pagination.value.size))
const visiblePages = computed(() => {
    const pages = []
    const total = totalPages.value
    const current = pagination.value.current
    if (total <= 7) {
        for (let i = 1; i <= total; i++) pages.push(i)
    } else {
        if (current <= 4) {
            pages.push(1, 2, 3, 4, 5, '...', total)
        } else if (current >= total - 3) {
            pages.push(1, '...', total - 4, total - 3, total - 2, total - 1, total)
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
    return searchForm.value.userId || searchForm.value.recipeId || searchForm.value.sortBy !== 'newest'
})

onMounted(() => {
    fetchComments()
    fetchFilterOptions()
})
</script>

<template>
    <div class="flex flex-col h-full p-5 space-y-4" @click="showFilterPanel = false">
        <!-- 顶部搜索栏 -->
        <div class="flex-shrink-0 bg-white rounded-xl p-5 border border-gray-100 shadow-sm">
            <div class="flex flex-wrap items-center gap-6">
                <!-- 搜索关键词 -->
                <div class="flex items-center gap-3">
                    <label class="text-sm font-medium text-gray-600 whitespace-nowrap">搜索内容</label>
                    <div class="relative">
                        <input 
                            v-model="searchForm.keyword"
                            type="text" 
                            placeholder="搜索评论内容..." 
                            class="w-64 px-3 py-2 pl-9 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400 transition"
                            @keyup.enter="handleSearch"
                        >
                        <Search class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    </div>
                </div>

                <!-- 操作按钮组 -->
                <div class="flex items-center gap-3 ml-auto">
                    <button 
                        @click="handleReset"
                        class="px-4 py-2 text-sm font-medium text-blue-500 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition"
                    >
                        重置
                    </button>
                    <button 
                        @click="handleSearch"
                        class="px-5 py-2 text-sm font-medium text-white bg-blue-500 rounded-lg hover:bg-blue-600 shadow-sm shadow-blue-200 transition"
                    >
                        查询
                    </button>
                </div>
            </div>
        </div>

        <!-- 表格主体区域 -->
        <div class="flex-1 min-h-0 bg-white rounded-xl border border-gray-100 shadow-sm flex flex-col relative">
            <!-- 工具栏 -->
            <div class="flex-shrink-0 p-4 border-b border-gray-100 flex items-center justify-between z-20 bg-white rounded-t-xl">
                <div class="flex items-center gap-3">
                    <!-- 批量操作 placeholder -->
                    <div v-if="selectedIds.length > 0" class="flex items-center gap-2 animate-fade-in">
                        <span class="text-xs text-gray-500">已选 {{ selectedIds.length }} 项</span>
                        <button 
                            class="px-3 py-1.5 text-xs font-medium text-red-600 bg-red-50 border border-red-100 rounded-lg hover:bg-red-100 transition flex items-center gap-1"
                        >
                            <Trash2 class="w-3.5 h-3.5" />
                            批量删除
                        </button>
                    </div>
                    <div v-else class="text-sm font-medium text-gray-500 flex items-center gap-2">
                        <MessageSquare class="w-4 h-4" />
                        评论列表
                    </div>
                </div>
                
                <div class="flex items-center gap-2">
                    <button class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" title="刷新" @click="fetchComments">
                        <RefreshCw class="w-4 h-4" />
                    </button>
                    <div class="relative">
                        <button 
                            class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" 
                            title="筛选"
                            @click.stop="showFilterPanel = !showFilterPanel"
                        >
                            <div class="relative">
                                <Filter class="w-4 h-4" :class="hasActiveFilters ? 'text-blue-500' : ''" />
                                <span v-if="hasActiveFilters" class="absolute -top-1 -right-1 w-2 h-2 bg-blue-500 rounded-full"></span>
                            </div>
                        </button>
                        <!-- 筛选下拉面板 -->
                        <div 
                            v-if="showFilterPanel"
                            class="absolute right-0 top-full mt-2 w-64 bg-white border border-gray-100 rounded-lg shadow-xl p-4 space-y-4 z-30"
                            @click.stop
                        >
                            <div>
                                <label class="text-xs font-medium text-gray-500 mb-1.5 block">用户筛选</label>
                                <select v-model="searchForm.userId" class="w-full text-sm border-gray-200 rounded-md focus:border-blue-500 focus:ring-1 focus:ring-blue-500">
                                    <option value="">全部用户</option>
                                    <option v-for="user in users" :key="user.id" :value="user.id">
                                        {{ user.nickname }}
                                    </option>
                                </select>
                            </div>
                            <div>
                                <label class="text-xs font-medium text-gray-500 mb-1.5 block">菜谱筛选</label>
                                <select v-model="searchForm.recipeId" class="w-full text-sm border-gray-200 rounded-md focus:border-blue-500 focus:ring-1 focus:ring-blue-500">
                                    <option value="">全部菜谱</option>
                                    <option v-for="recipe in recipes" :key="recipe.id" :value="recipe.id">
                                        {{ recipe.title }}
                                    </option>
                                </select>
                            </div>
                            <div>
                                <label class="text-xs font-medium text-gray-500 mb-1.5 block">排序方式</label>
                                <select v-model="searchForm.sortBy" class="w-full text-sm border-gray-200 rounded-md focus:border-blue-500 focus:ring-1 focus:ring-blue-500">
                                    <option v-for="opt in sortOptions" :key="opt.value" :value="opt.value">
                                        {{ opt.label }}
                                    </option>
                                </select>
                            </div>
                            <div class="flex gap-2 pt-2">
                                <button 
                                    @click="handleReset(); showFilterPanel=false"
                                    class="flex-1 py-1.5 text-xs font-medium text-gray-600 bg-gray-100 rounded hover:bg-gray-200"
                                >
                                    重置
                                </button>
                                <button 
                                    @click="handleSearch(); showFilterPanel=false"
                                    class="flex-1 py-1.5 text-xs font-medium text-white bg-blue-500 rounded hover:bg-blue-600"
                                >
                                    应用
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 表格滚动区域 -->
            <div class="flex-1 overflow-auto custom-scrollbar">
                <table class="w-full text-left">
                    <thead class="bg-gray-50 sticky top-0 z-10 text-xs font-semibold text-gray-500 uppercase">
                        <tr class="border-b border-gray-100">
                            <th class="p-4 w-12 text-center">
                                <input 
                                    type="checkbox" 
                                    :checked="isAllSelected"
                                    @change="toggleSelectAll"
                                    class="rounded border-gray-300 text-blue-500 focus:ring-blue-200 cursor-pointer" 
                                />
                            </th>
                            <th class="px-4 py-3 w-20 text-center">ID</th>
                            <th class="p-4 font-medium w-1/3">评论内容</th>
                            <th class="p-4 font-medium">用户</th>
                            <th class="p-4 font-medium">关联菜谱</th>
                            <th class="p-4 font-medium">评论时间</th>
                            <th class="p-4 font-medium text-right pr-8">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-50 text-sm">
                        <tr v-if="loading">
                             <td colspan="7" class="p-12 text-center text-gray-400">
                                 <div class="flex flex-col items-center">
                                     <div class="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                                     <span class="mt-2 text-xs">加载中...</span>
                                 </div>
                             </td>
                        </tr>
                        <tr v-else-if="comments.length === 0">
                             <td colspan="7" class="p-12 text-center text-gray-400">暂无评论数据</td>
                        </tr>
                        <tr 
                            v-else
                            v-for="(comment, index) in comments" 
                            :key="comment.id" 
                            class="hover:bg-gray-50/80 transition group"
                        >
                            <td class="p-4 text-center">
                                <input 
                                    type="checkbox" 
                                    :checked="selectedIds.includes(comment.id)"
                                    @change="toggleSelect(comment)"
                                    class="rounded border-gray-300 text-blue-500 focus:ring-blue-200 cursor-pointer" 
                                />
                            </td>
                            <td class="px-4 py-3 text-center">
                                <span class="text-gray-500 font-mono text-xs">{{ comment.id }}</span>
                            </td>
                            <td class="p-4">
                                <div class="text-gray-800 line-clamp-2" :title="comment.content">
                                    {{ comment.content }}
                                </div>
                            </td>
                            <td class="p-4">
                                <div class="flex items-center gap-2">
                                    <img 
                                        :src="comment.userAvatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + comment.userId" 
                                        class="w-6 h-6 rounded-full object-cover bg-gray-100"
                                    />
                                    <span class="text-gray-700 font-medium text-xs">{{ comment.userNickname || 'User' + comment.userId }}</span>
                                </div>
                            </td>
                            <td class="p-4">
                                <button 
                                    @click="viewRecipe(comment.recipeId)"
                                    class="text-blue-500 hover:text-blue-700 hover:underline flex items-center gap-1 text-xs"
                                >
                                    {{ comment.recipeTitle || 'Recipe #' + comment.recipeId }}
                                    <ExternalLink class="w-3 h-3" />
                                </button>
                            </td>
                            <td class="p-4 text-gray-500 text-xs">
                                {{ formatDate(comment.createTime) }}
                            </td>
                            <td class="p-4 text-right pr-6">
                                <button 
                                    @click="handleDelete(comment)"
                                    class="p-1.5 text-red-500 bg-red-50 hover:bg-red-100 rounded-lg transition opacity-0 group-hover:opacity-100"
                                    title="删除"
                                >
                                    <Trash2 class="w-4 h-4" />
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 底部主要分页 -->
            <div class="flex-shrink-0 p-4 border-t border-gray-100 flex items-center justify-between">
                <div class="text-gray-500 text-sm">
                    共 {{ pagination.total }} 条
                </div>
                
                <div class="flex items-center gap-2">
                    <button 
                        class="w-8 h-8 flex items-center justify-center rounded border border-gray-200 text-gray-400 hover:border-blue-500 hover:text-blue-500 disabled:opacity-50 transition"
                        :disabled="pagination.current <= 1"
                        @click="handlePageChange(pagination.current - 1)"
                    >
                        <ChevronLeft class="w-4 h-4" />
                    </button>
                    
                    <template v-for="page in visiblePages" :key="page">
                        <span v-if="page === '...'" class="px-1 text-gray-400">...</span>
                        <button 
                            v-else
                            @click="handlePageChange(page)"
                            :class="[
                                'min-w-[32px] h-8 px-2 rounded border text-sm transition',
                                pagination.current === page 
                                    ? 'bg-blue-500 border-blue-500 text-white' 
                                    : 'border-gray-200 text-gray-600 hover:border-blue-500 hover:text-blue-500'
                            ]"
                        >
                            {{ page }}
                        </button>
                    </template>
                    
                    <button 
                        class="w-8 h-8 flex items-center justify-center rounded border border-gray-200 text-gray-400 hover:border-blue-500 hover:text-blue-500 disabled:opacity-50 transition"
                        :disabled="pagination.current >= totalPages"
                        @click="handlePageChange(pagination.current + 1)"
                    >
                        <ChevronRight class="w-4 h-4" />
                    </button>
                    
                    <select 
                        v-model="pagination.size"
                        @change="handleSearch"
                        class="h-8 px-2 border border-gray-200 rounded text-sm text-gray-600 focus:border-blue-500 outline-none ml-2"
                    >
                        <option :value="10">10条/页</option>
                        <option :value="20">20条/页</option>
                        <option :value="50">50条/页</option>
                    </select>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
    height: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
    background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #e2e8f0;
    border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: #cbd5e1;
}
.animate-fade-in {
    animation: fadeIn 0.3s ease;
}
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(-5px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
