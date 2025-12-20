<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard, listAuditRecipes, auditRecipe } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { useNotificationStore } from '@/stores/notification'
import {
    Users,
    ChefHat,
    FolderOpen,
    MessageSquare,
    TrendingUp,
    Clock,
    Check,
    X,
    Eye
} from 'lucide-vue-next'

const router = useRouter()
const { showToast } = useToast()
const { confirm, prompt } = useModal()
const notificationStore = useNotificationStore()

const loading = ref(false)
const dashboard = ref({
    totalUsers: 0,
    todayNewUsers: 0,
    totalRecipes: 0,
    pendingRecipes: 0,
    publishedRecipes: 0,
    totalComments: 0,
    todayComments: 0,
    totalCategories: 0
})
const pendingRecipes = ref([])

const statCards = [
    { key: 'totalUsers', label: '用户总数', icon: Users, color: 'blue' },
    { key: 'totalRecipes', label: '菜谱总数', icon: ChefHat, color: 'green' },
    { key: 'pendingRecipes', label: '待审核', icon: Clock, color: 'orange' },
    { key: 'totalComments', label: '评论总数', icon: MessageSquare, color: 'purple' }
]

const fetchDashboard = async () => {
    loading.value = true
    try {
        const res = await getDashboard()
        if (res) {
            dashboard.value = res
        }
    } catch (error) {
        console.error('Failed to fetch dashboard', error)
        showToast('获取统计数据失败')
    } finally {
        loading.value = false
    }
}

const fetchPendingRecipes = async () => {
    try {
        const res = await listAuditRecipes({ page: 1, size: 5 })
        if (res && res.records) {
            pendingRecipes.value = res.records
        }
    } catch (error) {
        console.error('Failed to fetch pending recipes', error)
    }
}

const handleAudit = async (id, action, reason = '') => {
    try {
        await auditRecipe({ recipeId: id, action, reason })
        pendingRecipes.value = pendingRecipes.value.filter(r => r.id !== id)
        dashboard.value.pendingRecipes--
        showToast(action === 'pass' ? '已通过审核' : '已驳回')
    } catch (error) {
        console.error(error)
        showToast('操作失败')
    }
}

const rejectRecipe = async (id) => {
    const reason = await prompt('请输入驳回原因:', { placeholder: '请输入驳回原因...' })
    if (reason) handleAudit(id, 'reject', reason)
}

const passRecipe = async (id) => {
    const confirmed = await confirm('确定通过该菜谱审核吗？')
    if (confirmed) {
        handleAudit(id, 'pass')
    }
}

const viewRecipe = (id) => {
    router.push(`/recipe/${id}`)
}

// 监听新菜谱待审核通知，实时更新列表
watch(() => notificationStore.latestNotification, (notification) => {
    if (!notification) return
    
    if (notification.type === 'NEW_RECIPE_PENDING') {
        console.log('DashboardView: 收到新菜谱待审核通知，刷新数据')
        // 刷新待审核列表和统计数据
        fetchDashboard()
        fetchPendingRecipes()
    }
})

onMounted(() => {
    fetchDashboard()
    fetchPendingRecipes()
})

const getColorClass = (color) => {
    const colors = {
        blue: 'bg-blue-500',
        green: 'bg-green-500',
        orange: 'bg-orange-500',
        purple: 'bg-purple-500'
    }
    return colors[color] || 'bg-gray-500'
}
</script>

<template>
    <div class="space-y-6">
        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            <div 
                v-for="stat in statCards" 
                :key="stat.key"
                class="bg-white rounded-xl shadow-sm p-6 flex items-center gap-4"
            >
                <div :class="['p-3 rounded-lg text-white', getColorClass(stat.color)]">
                    <component :is="stat.icon" class="w-6 h-6" />
                </div>
                <div>
                    <p class="text-sm text-gray-500">{{ stat.label }}</p>
                    <p class="text-2xl font-bold text-gray-800">{{ dashboard[stat.key] }}</p>
                </div>
            </div>
        </div>

        <!-- 详细统计 -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
            <!-- 菜谱统计 -->
            <div class="bg-white rounded-xl shadow-sm p-6">
                <h3 class="font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <ChefHat class="w-5 h-5 text-orange-500" />
                    菜谱统计
                </h3>
                <div class="space-y-3">
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">已发布</span>
                        <span class="font-semibold text-green-600">{{ dashboard.publishedRecipes }}</span>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">待审核</span>
                        <span class="font-semibold text-orange-600">{{ dashboard.pendingRecipes }}</span>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">分类数量</span>
                        <span class="font-semibold text-gray-800">{{ dashboard.totalCategories }}</span>
                    </div>
                </div>
            </div>

            <!-- 用户统计 -->
            <div class="bg-white rounded-xl shadow-sm p-6">
                <h3 class="font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <Users class="w-5 h-5 text-blue-500" />
                    用户统计
                </h3>
                <div class="space-y-3">
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">总注册用户</span>
                        <span class="font-semibold text-gray-800">{{ dashboard.totalUsers }}</span>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">今日新增</span>
                        <span class="font-semibold text-green-600">+{{ dashboard.todayNewUsers }}</span>
                    </div>
                </div>
            </div>

            <!-- 评论统计 -->
            <div class="bg-white rounded-xl shadow-sm p-6">
                <h3 class="font-semibold text-gray-800 mb-4 flex items-center gap-2">
                    <MessageSquare class="w-5 h-5 text-purple-500" />
                    互动统计
                </h3>
                <div class="space-y-3">
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">总评论数</span>
                        <span class="font-semibold text-gray-800">{{ dashboard.totalComments }}</span>
                    </div>
                    <div class="flex justify-between items-center">
                        <span class="text-gray-600">今日评论</span>
                        <span class="font-semibold text-green-600">+{{ dashboard.todayComments }}</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 待审核列表 -->
        <div class="bg-white rounded-xl shadow-sm p-6">
            <div class="flex justify-between items-center mb-4">
                <h3 class="font-semibold text-gray-800 flex items-center gap-2">
                    <Clock class="w-5 h-5 text-orange-500" />
                    待审核菜谱
                    <span v-if="pendingRecipes.length > 0" class="bg-orange-100 text-orange-600 text-xs px-2 py-0.5 rounded-full">
                        {{ pendingRecipes.length }}
                    </span>
                </h3>
                <button 
                    @click="router.push('/backstage-m9x2k7/recipes?status=0')"
                    class="text-sm text-orange-500 hover:text-orange-600"
                >
                    查看全部 →
                </button>
            </div>

            <div v-if="pendingRecipes.length === 0" class="text-center py-8 text-gray-400">
                暂无待审核菜谱 🎉
            </div>

            <div v-else class="space-y-3">
                <div 
                    v-for="recipe in pendingRecipes" 
                    :key="recipe.id"
                    class="flex items-center gap-4 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition"
                >
                    <img 
                        :src="recipe.coverImage" 
                        class="w-16 h-16 rounded-lg object-cover bg-gray-200"
                        @error="e => e.target.src = 'https://via.placeholder.com/64'"
                    />
                    <div class="flex-1 min-w-0">
                        <h4 class="font-medium text-gray-800 truncate">{{ recipe.title }}</h4>
                        <p class="text-sm text-gray-500">
                            {{ recipe.authorName }} · {{ recipe.categoryName || '未分类' }}
                        </p>
                    </div>
                    <div class="flex gap-2">
                        <button 
                            @click="viewRecipe(recipe.id)"
                            class="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-200 rounded-lg transition"
                            title="查看详情"
                        >
                            <Eye class="w-4 h-4" />
                        </button>
                        <button 
                            @click="passRecipe(recipe.id)"
                            class="p-2 text-green-500 hover:text-green-700 hover:bg-green-100 rounded-lg transition"
                            title="通过"
                        >
                            <Check class="w-4 h-4" />
                        </button>
                        <button 
                            @click="rejectRecipe(recipe.id)"
                            class="p-2 text-red-500 hover:text-red-700 hover:bg-red-100 rounded-lg transition"
                            title="驳回"
                        >
                            <X class="w-4 h-4" />
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
