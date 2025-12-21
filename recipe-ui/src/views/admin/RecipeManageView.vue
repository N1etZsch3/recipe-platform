<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listAllRecipes, auditRecipe, deleteRecipe } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
    Search, Eye, Check, X, Trash2, 
    ChevronLeft, ChevronRight, Filter, ChefHat
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const { showToast } = useToast()
const { confirm, prompt } = useModal()

const loading = ref(false)
const recipes = ref([])
const keyword = ref('')
const statusFilter = ref(null)
const pagination = ref({
    current: 1,
    size: 10,
    total: 0
})

const statusOptions = [
    { value: null, label: '全部状态' },
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
    try {
        const res = await listAllRecipes({
            page: pagination.value.current,
            size: pagination.value.size,
            status: statusFilter.value,
            keyword: keyword.value || undefined
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

const handleStatusChange = () => {
    pagination.value.current = 1
    fetchRecipes()
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
    return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
    if (route.query.status !== undefined) {
        statusFilter.value = parseInt(route.query.status)
    }
    fetchRecipes()
})
</script>

<template>
    <div class="space-y-6 p-5 h-full overflow-y-auto">
        <!-- 页面标题 -->
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-2xl font-bold text-gray-800">菜谱管理</h1>
                <p class="text-sm text-gray-500 mt-1">审核和管理平台菜谱内容</p>
            </div>
            <div class="flex items-center gap-3">
                <div class="flex items-center gap-2 px-4 py-2 bg-orange-50 text-orange-600 rounded-xl">
                    <ChefHat class="w-4 h-4" />
                    <span class="text-sm font-medium">{{ pagination.total }} 道菜谱</span>
                </div>
            </div>
        </div>

        <!-- 搜索和筛选 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-50 p-5">
            <div class="flex flex-wrap gap-4">
                <div class="flex-1 min-w-[200px] relative">
                    <Search class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2" />
                    <input
                        v-model="keyword"
                        @keyup.enter="handleSearch"
                        type="text"
                        placeholder="搜索菜谱标题..."
                        class="w-full pl-11 pr-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition"
                    />
                </div>
                <div class="relative">
                    <Filter class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none z-10" />
                    <select
                        v-model="statusFilter"
                        @change="handleStatusChange"
                        class="appearance-none pl-11 pr-10 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition cursor-pointer min-w-[140px]"
                    >
                        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
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
            </div>
        </div>

        <!-- 菜谱列表 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-50 overflow-hidden">
            <div class="overflow-x-auto">
                <table class="w-full">
                    <thead>
                        <tr class="border-b border-gray-100">
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">菜谱信息</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">作者</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">分类</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">发布时间</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">状态</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-50">
                        <tr v-if="loading" class="text-center">
                            <td colspan="6" class="px-6 py-16 text-gray-400">
                                <div class="flex flex-col items-center">
                                    <div class="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
                                    <span class="mt-3">加载中...</span>
                                </div>
                            </td>
                        </tr>
                        <tr v-else-if="recipes.length === 0" class="text-center">
                            <td colspan="6" class="px-6 py-16 text-gray-400">
                                <div class="flex flex-col items-center">
                                    <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-3">
                                        <ChefHat class="w-8 h-8 text-gray-300" />
                                    </div>
                                    <span>暂无菜谱数据</span>
                                </div>
                            </td>
                        </tr>
                        <tr v-else v-for="recipe in recipes" :key="recipe.id" class="hover:bg-gray-50/50 transition group">
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center gap-3">
                                    <img 
                                        :src="recipe.coverImage" 
                                        class="w-16 h-12 rounded-xl object-cover bg-gray-100"
                                        @error="e => e.target.src = 'https://via.placeholder.com/64'"
                                    />
                                    <div>
                                        <div class="font-medium text-gray-800 max-w-[200px] truncate">{{ recipe.title }}</div>
                                        <div class="text-xs text-gray-400">ID: {{ recipe.id }}</div>
                                    </div>
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-600 text-sm">{{ recipe.authorName || '-' }}</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="inline-flex items-center px-2.5 py-1 text-xs font-medium rounded-lg bg-blue-50 text-blue-600">
                                    {{ recipe.categoryName || '未分类' }}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-500 text-sm">{{ formatDate(recipe.createTime) }}</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span :class="[
                                    'inline-flex items-center px-2.5 py-1 text-xs font-medium rounded-lg',
                                    getStatusConfig(recipe.status).bg,
                                    getStatusConfig(recipe.status).text
                                ]">
                                    <span :class="['w-1.5 h-1.5 rounded-full mr-1.5', getStatusConfig(recipe.status).dot]"></span>
                                    {{ getStatusConfig(recipe.status).label }}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center gap-1">
                                    <button 
                                        @click="viewRecipe(recipe.id)"
                                        class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition"
                                        title="查看"
                                    >
                                        <Eye class="w-4 h-4" />
                                    </button>
                                    <template v-if="recipe.status === 0">
                                        <button 
                                            @click="passRecipe(recipe.id)"
                                            class="p-2 text-emerald-500 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg transition"
                                            title="通过"
                                        >
                                            <Check class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="rejectRecipe(recipe.id)"
                                            class="p-2 text-amber-500 hover:text-amber-700 hover:bg-amber-50 rounded-lg transition"
                                            title="驳回"
                                        >
                                            <X class="w-4 h-4" />
                                        </button>
                                    </template>
                                    <button 
                                        @click="handleDelete(recipe)"
                                        class="p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition opacity-0 group-hover:opacity-100"
                                        title="删除"
                                    >
                                        <Trash2 class="w-4 h-4" />
                                    </button>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 分页 -->
            <div v-if="totalPages > 1" class="px-6 py-4 border-t border-gray-100 flex items-center justify-between bg-gray-50/50">
                <span class="text-sm text-gray-500">
                    第 {{ pagination.current }} 页，共 {{ pagination.total }} 条记录
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
