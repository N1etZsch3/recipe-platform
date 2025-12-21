<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listAllRecipes, auditRecipe, deleteRecipe, batchAuditRecipes, batchUpdateRecipeStatus } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
    Search, Eye, Check, X, Trash2, 
    ChevronLeft, ChevronRight, Filter, ChefHat,
    RefreshCw, Settings, Ban, CheckCircle, ArrowDownCircle
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const { showToast } = useToast()
const { confirm, prompt } = useModal()

const loading = ref(false)
const recipes = ref([])
const selectedIds = ref([])

// 搜索表单
const searchForm = ref({
    keyword: '',
    status: ''
})

const showFilterPanel = ref(false)

const pagination = ref({
    current: 1,
    size: 20,
    total: 0
})

const statusOptions = [
    { value: '', label: '全部状态' },
    { value: 0, label: '待审核' },
    { value: 1, label: '已发布' },
    { value: 2, label: '已驳回' }
]

const getStatusConfig = (status) => {
    const configs = {
        0: { bg: 'bg-amber-50', text: 'text-amber-600', dot: 'bg-amber-500', label: '待审核' },
        1: { bg: 'bg-emerald-50', text: 'text-emerald-600', dot: 'bg-emerald-500', label: '已发布' },
        2: { bg: 'bg-red-50', text: 'text-red-600', dot: 'bg-red-500', label: '已驳回' }
    }
    return configs[status] || { bg: 'bg-gray-100', text: 'text-gray-600', dot: 'bg-gray-400', label: '未知' }
}

const fetchRecipes = async () => {
    loading.value = true
    selectedIds.value = [] // Reset
    try {
        const res = await listAllRecipes({
            page: pagination.value.current,
            size: pagination.value.size,
            status: searchForm.value.status === '' ? undefined : searchForm.value.status,
            keyword: searchForm.value.keyword || undefined
        })
        if (res) {
            recipes.value = res.records || []
            pagination.value.total = res.total || 0
        }
    } catch (error) {
        console.error('Failed to fetch recipes', error)
        showToast('获取菜谱列表失败')
    } finally {
        loading.value = false
    }
}

const handleSearch = () => {
    pagination.value.current = 1
    fetchRecipes()
}

const handleReset = () => {
    searchForm.value = {
        keyword: '',
        status: ''
    }
    handleSearch()
}

const handlePageChange = (page) => {
    pagination.value.current = page
    fetchRecipes()
}

const viewRecipe = (id) => {
    window.open(`/recipe/${id}`, '_blank')
}

const handleAudit = async (id, action, reason = '') => {
    try {
        await auditRecipe({ recipeId: id, action, reason })
        const recipe = recipes.value.find(r => r.id === id)
        if (recipe) {
            recipe.status = action === 'pass' ? 1 : 2
        }
        showToast(action === 'pass' ? '已通过审核' : '已驳回')
    } catch (error) {
        console.error(error)
        showToast('操作失败')
    }
}

const passRecipe = async (id) => {
    const confirmed = await confirm('确定通过该菜谱审核吗？')
    if (confirmed) {
        handleAudit(id, 'pass')
    }
}

const rejectRecipe = async (id) => {
    const reason = await prompt('请输入驳回原因:', { placeholder: '请输入驳回原因...' })
    if (reason) handleAudit(id, 'reject', reason)
}

const handleDelete = async (recipe) => {
    const confirmed = await confirm(`确定要删除菜谱 "${recipe.title}" 吗？此操作不可恢复！`, { danger: true })
    if (!confirmed) return

    try {
        await deleteRecipe(recipe.id)
        recipes.value = recipes.value.filter(r => r.id !== recipe.id)
        pagination.value.total--
        showToast('删除成功')
    } catch (error) {
        console.error(error)
        showToast('删除失败')
    }
}

// Selection Logic
const isAllSelected = computed(() => {
    return recipes.value.length > 0 && recipes.value.every(r => selectedIds.value.includes(r.id))
})

const toggleSelectAll = (e) => {
    if (e.target.checked) {
        selectedIds.value = recipes.value.map(r => r.id)
    } else {
        selectedIds.value = []
    }
}

const toggleSelect = (recipe) => {
    const id = recipe.id
    if (selectedIds.value.includes(id)) {
        selectedIds.value = selectedIds.value.filter(i => i !== id)
    } else {
        selectedIds.value.push(id)
    }
}

// 批量审核通过
const handleBatchPass = async () => {
    if (selectedIds.value.length === 0) return
    const confirmed = await confirm(`确定批量通过选中的 ${selectedIds.value.length} 个菜谱吗？`)
    if (!confirmed) return
    try {
        await batchAuditRecipes({ ids: selectedIds.value, action: 'pass' })
        showToast('批量审核通过成功')
        fetchRecipes()
    } catch (error) {
        showToast('批量操作失败')
    }
}

// 批量下架
const handleBatchUnpublish = async () => {
    if (selectedIds.value.length === 0) return
    const confirmed = await confirm(`确定批量下架选中的 ${selectedIds.value.length} 个菜谱吗？`, { danger: true })
    if (!confirmed) return
    try {
        await batchUpdateRecipeStatus({ ids: selectedIds.value, status: 0 }) // 0 = pending
        showToast('批量下架成功')
        fetchRecipes()
    } catch (error) {
        showToast('批量操作失败')
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
    return new Date(dateStr).toLocaleDateString('zh-CN')
}

const hasActiveFilters = computed(() => {
    return searchForm.value.status !== ''
})

onMounted(() => {
    if (route.query.status !== undefined) {
        searchForm.value.status = parseInt(route.query.status)
    }
    fetchRecipes()
})
</script>

<template>
    <div class="flex flex-col h-full p-5 space-y-4" @click="showFilterPanel = false">
        <!-- 顶部搜索栏 -->
        <div class="flex-shrink-0 bg-white rounded-xl p-5 border border-gray-100 shadow-sm">
            <div class="flex flex-wrap items-center gap-6">
                <!-- 搜索关键词 -->
                <div class="flex items-center gap-3">
                    <label class="text-sm font-medium text-gray-600 whitespace-nowrap">搜索菜谱</label>
                    <div class="relative">
                        <input 
                            v-model="searchForm.keyword"
                            type="text" 
                            placeholder="搜索菜谱标题..." 
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
                    <!-- 批量操作 Placeholder -->
                    <div v-if="selectedIds.length > 0" class="flex items-center gap-2 animate-fade-in">
                        <span class="text-xs text-gray-500">已选 {{ selectedIds.length }} 项</span>
                        <button 
                            @click="handleBatchPass"
                            class="px-3 py-1.5 text-xs font-medium text-emerald-600 bg-emerald-50 border border-emerald-100 rounded-lg hover:bg-emerald-100 transition flex items-center gap-1"
                            title="将选中的待审核菜谱批量通过审核"
                        >
                            <CheckCircle class="w-3.5 h-3.5" />
                            批量通过
                        </button>
                        <button 
                            @click="handleBatchUnpublish"
                            class="px-3 py-1.5 text-xs font-medium text-amber-600 bg-amber-50 border border-amber-100 rounded-lg hover:bg-amber-100 transition flex items-center gap-1"
                            title="将选中的已发布菜谱批量下架"
                        >
                            <ArrowDownCircle class="w-3.5 h-3.5" />
                            批量下架
                        </button>
                    </div>
                    <div v-else class="text-sm font-medium text-gray-500 flex items-center gap-2">
                        <ChefHat class="w-4 h-4" />
                        菜谱列表
                    </div>
                </div>
                
                <div class="flex items-center gap-2">
                    <button class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" title="刷新" @click="fetchRecipes">
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
                                <label class="text-xs font-medium text-gray-500 mb-1.5 block">状态筛选</label>
                                <select v-model="searchForm.status" class="w-full text-sm border-gray-200 rounded-md focus:border-blue-500 focus:ring-1 focus:ring-blue-500">
                                    <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
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
                    <thead class="bg-white sticky top-0 z-10 text-xs font-semibold text-gray-500 uppercase">
                        <tr class="border-b border-gray-100">
                            <th class="p-4 w-12 text-center">
                                <input 
                                    type="checkbox" 
                                    :checked="isAllSelected"
                                    @change="toggleSelectAll"
                                    class="rounded border-gray-300 text-blue-500 focus:ring-blue-200 cursor-pointer" 
                                />
                            </th>
                            <th class="p-4 w-16 px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">菜谱信息</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">作者</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">分类</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">发布时间</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">状态</th>
                            <th class="px-6 py-4 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider pr-8">操作</th>
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
                        <tr v-else-if="recipes.length === 0">
                             <td colspan="7" class="p-12 text-center text-gray-400">暂无菜谱数据</td>
                        </tr>
                        <tr 
                            v-else
                            v-for="(recipe, index) in recipes" 
                            :key="recipe.id" 
                            class="hover:bg-gray-50/80 transition group"
                        >
                            <td class="p-4 text-center">
                                <input 
                                    type="checkbox" 
                                    :checked="selectedIds.includes(recipe.id)"
                                    @change="toggleSelect(recipe)"
                                    class="rounded border-gray-300 text-blue-500 focus:ring-blue-200 cursor-pointer" 
                                />
                            </td>
                            <td class="px-6 py-4">
                                <div class="flex items-center gap-3">
                                    <img 
                                        :src="recipe.coverImage" 
                                        class="w-14 h-10 rounded-lg object-cover bg-gray-100"
                                        @error="e => e.target.src = 'https://via.placeholder.com/64'"
                                    />
                                    <div>
                                        <div class="font-medium text-gray-800 max-w-[180px] truncate" :title="recipe.title">{{ recipe.title }}</div>
                                        <div class="text-xs text-gray-400">ID: {{ recipe.id }}</div>
                                    </div>
                                </div>
                            </td>
                            <td class="px-6 py-4 text-gray-600">{{ recipe.authorName || '-' }}</td>
                            <td class="px-6 py-4">
                                <span class="inline-flex items-center px-2.5 py-1 text-xs font-medium rounded-lg bg-blue-50 text-blue-600">
                                    {{ recipe.categoryName || '未分类' }}
                                </span>
                            </td>
                            <td class="px-6 py-4 text-gray-500">{{ formatDate(recipe.createTime) }}</td>
                            <td class="px-6 py-4">
                                <span :class="[
                                    'inline-flex items-center px-2.5 py-1 text-xs font-medium rounded-lg',
                                    getStatusConfig(recipe.status).bg,
                                    getStatusConfig(recipe.status).text
                                ]">
                                    <span :class="['w-1.5 h-1.5 rounded-full mr-1.5', getStatusConfig(recipe.status).dot]"></span>
                                    {{ getStatusConfig(recipe.status).label }}
                                </span>
                            </td>
                            <td class="px-6 py-4 text-right pr-6">
                                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button 
                                        @click="viewRecipe(recipe.id)"
                                        class="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition"
                                        title="在新窗口查看菜谱详情"
                                    >
                                        <Eye class="w-4 h-4" />
                                    </button>
                                    <template v-if="recipe.status === 0">
                                        <button 
                                            @click="passRecipe(recipe.id)"
                                            class="p-1.5 text-emerald-500 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg transition"
                                            title="审核通过并发布菜谱"
                                        >
                                            <CheckCircle class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="rejectRecipe(recipe.id)"
                                            class="p-1.5 text-amber-500 hover:text-amber-700 hover:bg-amber-50 rounded-lg transition"
                                            title="驳回菜谱申请"
                                        >
                                            <Ban class="w-4 h-4" />
                                        </button>
                                    </template>
                                    <button 
                                        @click="handleDelete(recipe)"
                                        class="p-1.5 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition"
                                        title="永久删除菜谱(不可恢复)"
                                    >
                                        <Trash2 class="w-4 h-4" />
                                    </button>
                                </div>
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
