<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listAllRecipes, auditRecipe, deleteRecipe } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
    Search, Eye, Check, X, Trash2, 
    ChevronLeft, ChevronRight, Filter
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

const getStatusClass = (status) => {
    const classes = {
        0: 'bg-orange-100 text-orange-700',
        1: 'bg-green-100 text-green-700',
        2: 'bg-red-100 text-red-700'
    }
    return classes[status] || 'bg-gray-100 text-gray-600'
}

const getStatusText = (status) => {
    const texts = { 0: '待审核', 1: '已发布', 2: '已驳回' }
    return texts[status] || '未知'
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

const totalPages = () => Math.ceil(pagination.value.total / pagination.value.size)

const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('zh-CN')
}

onMounted(() => {
    // Check for status query param
    if (route.query.status !== undefined) {
        statusFilter.value = parseInt(route.query.status)
    }
    fetchRecipes()
})
</script>

<template>
    <div class="flex flex-col h-full">
        <!-- 搜索和筛选栏 (固定) -->
        <div class="bg-white rounded-xl shadow-sm p-4 flex-shrink-0">
            <div class="flex gap-4">
                <div class="flex-1 relative">
                    <Search class="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                        v-model="keyword"
                        @keyup.enter="handleSearch"
                        type="text"
                        placeholder="搜索菜谱标题..."
                        class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none"
                    />
                </div>
                <div class="relative">
                    <Filter class="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none z-10" />
                    <select
                        v-model="statusFilter"
                        @change="handleStatusChange"
                        class="custom-select pl-10"
                    >
                        <option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
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
            </div>
        </div>

        <!-- 菜谱列表 (可滚动) -->
        <div class="flex-1 mt-4 bg-white rounded-xl shadow-sm overflow-hidden flex flex-col min-h-0">
            <div class="flex-1 overflow-auto">
                <table class="w-full">
                    <thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">菜谱</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">作者</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">分类</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">发布时间</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                        <tr v-if="loading" class="text-center">
                            <td colspan="6" class="px-6 py-12 text-gray-400">加载中...</td>
                        </tr>
                        <tr v-else-if="recipes.length === 0" class="text-center">
                            <td colspan="6" class="px-6 py-12 text-gray-400">暂无数据</td>
                        </tr>
                        <tr v-else v-for="recipe in recipes" :key="recipe.id" class="hover:bg-gray-50">
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center gap-3">
                                    <img 
                                        :src="recipe.coverImage" 
                                        class="w-16 h-12 rounded-lg object-cover bg-gray-200"
                                        @error="e => e.target.src = 'https://via.placeholder.com/64'"
                                    />
                                    <div>
                                        <div class="font-medium text-gray-900 max-w-[200px] truncate">{{ recipe.title }}</div>
                                        <div class="text-sm text-gray-500">ID: {{ recipe.id }}</div>
                                    </div>
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-600">{{ recipe.authorName || '-' }}</td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-600">{{ recipe.categoryName || '未分类' }}</td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-600">{{ formatDate(recipe.createTime) }}</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span :class="['px-2 py-1 text-xs rounded-full', getStatusClass(recipe.status)]">
                                    {{ getStatusText(recipe.status) }}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center gap-1">
                                    <button 
                                        @click="viewRecipe(recipe.id)"
                                        class="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition"
                                        title="查看"
                                    >
                                        <Eye class="w-4 h-4" />
                                    </button>
                                    <template v-if="recipe.status === 0">
                                        <button 
                                            @click="passRecipe(recipe.id)"
                                            class="p-2 text-green-500 hover:text-green-700 hover:bg-green-50 rounded-lg transition"
                                            title="通过"
                                        >
                                            <Check class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="rejectRecipe(recipe.id)"
                                            class="p-2 text-orange-500 hover:text-orange-700 hover:bg-orange-50 rounded-lg transition"
                                            title="驳回"
                                        >
                                            <X class="w-4 h-4" />
                                        </button>
                                    </template>
                                    <button 
                                        @click="handleDelete(recipe)"
                                        class="p-2 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition"
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
