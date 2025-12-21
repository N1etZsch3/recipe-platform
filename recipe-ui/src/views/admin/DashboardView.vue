<script setup>
import { ref, onMounted, watch, computed, shallowRef } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboard, listAuditRecipes, auditRecipe, listCategories } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { useNotificationStore } from '@/stores/notification'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    GridComponent,
    GraphicComponent
} from 'echarts/components'
import VChart from 'vue-echarts'
import {
    Users,
    ChefHat,
    FolderOpen,
    MessageSquare,
    Clock,
    Check,
    X,
    ArrowUpRight
} from 'lucide-vue-next'

// 注册 ECharts 组件
use([
    CanvasRenderer,
    BarChart,
    LineChart,
    PieChart,
    TitleComponent,
    TooltipComponent,
    LegendComponent,
    GridComponent,
    GraphicComponent
])

const router = useRouter()
const { showToast } = useToast()
const { confirm, prompt } = useModal()
const notificationStore = useNotificationStore()

const loading = ref(false)
const categories = ref([])
const dashboard = ref({
    totalUsers: 0,
    todayNewUsers: 0,
    totalRecipes: 0,
    pendingRecipes: 0,
    publishedRecipes: 0,
    totalComments: 0,
    todayComments: 0,
    totalCategories: 0,
    // 新增：用于图表的真实数据
    monthlyRecipes: [],
    categoryStats: []
})
const pendingRecipes = ref([])

// 统计卡片配置 - 移除虚假百分比数据
const statCards = computed(() => [
    { 
        key: 'totalRecipes', 
        label: '菜谱总数', 
        value: dashboard.value.totalRecipes,
        subLabel: `已发布 ${dashboard.value.publishedRecipes}`,
        icon: ChefHat,
        iconBg: 'bg-gradient-to-br from-orange-400 to-amber-500',
        iconShadow: 'shadow-orange-200'
    },
    { 
        key: 'totalUsers', 
        label: '用户总数', 
        value: dashboard.value.totalUsers,
        subLabel: `今日新增 ${dashboard.value.todayNewUsers}`,
        icon: Users,
        iconBg: 'bg-gradient-to-br from-blue-400 to-blue-600',
        iconShadow: 'shadow-blue-200'
    },
    { 
        key: 'totalComments', 
        label: '评论总数', 
        value: dashboard.value.totalComments,
        subLabel: `今日新增 ${dashboard.value.todayComments}`,
        icon: MessageSquare,
        iconBg: 'bg-gradient-to-br from-purple-400 to-purple-600',
        iconShadow: 'shadow-purple-200'
    },
    { 
        key: 'pendingRecipes', 
        label: '待审核', 
        value: dashboard.value.pendingRecipes,
        subLabel: dashboard.value.pendingRecipes > 0 ? '需要处理' : '暂无待审',
        icon: Clock,
        iconBg: 'bg-gradient-to-br from-emerald-400 to-emerald-600',
        iconShadow: 'shadow-emerald-200'
    }
])

// 柱状图配置 - 使用真实数据
const barChartOption = computed(() => {
    const monthlyData = dashboard.value.monthlyRecipes || []
    const months = monthlyData.map(item => `${item.month}月`)
    const values = monthlyData.map(item => item.count)
    
    // 如果没有数据，显示占位
    if (months.length === 0) {
        // return empty axes to keep layout
    }
    
    return {
        graphic: [],
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e5e7eb',
            borderWidth: 1,
            textStyle: { color: '#374151', fontSize: 12 },
            axisPointer: { type: 'shadow' }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: '10%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            data: months,
            axisLine: { lineStyle: { color: '#e5e7eb' } },
            axisLabel: { color: '#9ca3af', fontSize: 11 },
            axisTick: { show: false }
        },
        yAxis: {
            type: 'value',
            axisLine: { show: false },
            axisLabel: { color: '#9ca3af', fontSize: 11 },
            splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
        },
        series: [{
            name: '菜谱发布',
            type: 'bar',
            barWidth: '50%',
            data: values,
            itemStyle: {
                borderRadius: [4, 4, 0, 0],
                color: {
                    type: 'linear',
                    x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [
                        { offset: 0, color: '#f97316' },
                        { offset: 1, color: '#fb923c' }
                    ]
                }
            }
        }]
    }
})

// 折线图配置 - 基于真实数据
const lineChartOption = computed(() => {
    const monthlyData = dashboard.value.monthlyRecipes || []
    const months = monthlyData.map(item => `${item.month}月`)
    const values = monthlyData.map(item => item.count)
    
    if (months.length === 0) {
        // empty
    }
    
    return {
        graphic: [],
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e5e7eb',
            borderWidth: 1,
            textStyle: { color: '#374151', fontSize: 12 }
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            top: '10%',
            containLabel: true
        },
        xAxis: {
            type: 'category',
            boundaryGap: false,
            data: months,
            axisLine: { lineStyle: { color: '#e5e7eb' } },
            axisLabel: { color: '#9ca3af', fontSize: 11 },
            axisTick: { show: false }
        },
        yAxis: {
            type: 'value',
            axisLine: { show: false },
            axisLabel: { color: '#9ca3af', fontSize: 11 },
            splitLine: { lineStyle: { color: '#f3f4f6', type: 'dashed' } }
        },
        series: [{
            name: '菜谱发布趋势',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            data: values,
            lineStyle: { 
                color: '#3b82f6',
                width: 2
            },
            itemStyle: { 
                color: '#3b82f6',
                borderColor: '#fff',
                borderWidth: 2
            },
            areaStyle: {
                color: {
                    type: 'linear',
                    x: 0, y: 0, x2: 0, y2: 1,
                    colorStops: [
                        { offset: 0, color: 'rgba(59, 130, 246, 0.2)' },
                        { offset: 1, color: 'rgba(59, 130, 246, 0.02)' }
                    ]
                }
            }
        }]
    }
})

// 饼图配置 - 使用真实分类数据
const pieChartOption = computed(() => {
    const categoryData = dashboard.value.categoryStats || []
    
    if (categoryData.length === 0 && categories.value.length > 0) {
       // empty
    }
    
    if (categoryData.length === 0) {
        // empty
    }
    
    const colors = ['#f97316', '#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899', '#6b7280']
    
    return {
        graphic: [],
        tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e5e7eb',
            borderWidth: 1,
            textStyle: { color: '#374151', fontSize: 12 },
            formatter: '{b}: {c} ({d}%)'
        },
        legend: {
            orient: 'vertical',
            right: '5%',
            top: 'center',
            textStyle: { color: '#6b7280', fontSize: 12 }
        },
        series: [{
            name: '分类占比',
            type: 'pie',
            radius: ['40%', '65%'],
            center: ['35%', '50%'],
            avoidLabelOverlap: false,
            label: { show: false },
            labelLine: { show: false },
            data: categoryData.map((item, index) => ({
                value: item.count,
                name: item.name,
                itemStyle: { color: colors[index % colors.length] }
            }))
        }]
    }
})

const fetchDashboard = async () => {
    loading.value = true
    try {
        const res = await getDashboard()
        if (res) {
            dashboard.value = {
                ...dashboard.value,
                ...res
            }
        }
    } catch (error) {
        console.error('Failed to fetch dashboard', error)
        showToast('获取统计数据失败')
    } finally {
        loading.value = false
    }
}

const fetchCategories = async () => {
    try {
        const res = await listCategories()
        if (res) {
            categories.value = res
        }
    } catch (error) {
        console.error('Failed to fetch categories', error)
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

// 监听新菜谱待审核通知
watch(() => notificationStore.latestNotification, (notification) => {
    if (!notification) return
    
    if (notification.type === 'NEW_RECIPE_PENDING') {
        console.log('DashboardView: 收到新菜谱待审核通知，刷新数据')
        fetchDashboard()
        fetchPendingRecipes()
    }
})

onMounted(() => {
    fetchDashboard()
    fetchPendingRecipes()
    fetchCategories()
})
</script>

<template>
    <div class="space-y-5 p-5 h-full overflow-y-auto">
        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div 
                v-for="stat in statCards" 
                :key="stat.key"
                class="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-shadow border border-gray-50"
            >
                <div class="flex items-start justify-between">
                    <div>
                        <p class="text-sm text-gray-500 mb-1">{{ stat.label }}</p>
                        <p class="text-2xl font-semibold text-gray-800">{{ stat.value.toLocaleString() }}</p>
                        <p class="text-xs text-gray-400 mt-1.5">{{ stat.subLabel }}</p>
                    </div>
                    <div :class="[
                        'w-10 h-10 rounded-lg flex items-center justify-center shadow-md',
                        stat.iconBg,
                        stat.iconShadow
                    ]">
                        <component :is="stat.icon" class="w-5 h-5 text-white" />
                    </div>
                </div>
            </div>
        </div>

        <!-- 图表区域 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <!-- 柱状图 - 月度发布 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-50 relative">
                <div class="mb-3">
                    <h3 class="text-base font-semibold text-gray-800">月度菜谱发布</h3>
                    <p class="text-xs text-gray-400 mt-0.5">每月新增菜谱统计</p>
                </div>
                <div class="relative min-h-[16rem]">
                    <v-chart 
                        class="w-full h-64" 
                        :option="barChartOption" 
                        autoresize 
                    />
                    <div v-if="!dashboard.monthlyRecipes.length" class="absolute inset-0 flex items-center justify-center bg-white/50 z-10">
                        <span class="text-gray-400 text-sm">暂无数据</span>
                    </div>
                </div>
            </div>

            <!-- 折线图 - 发布趋势 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-50 relative">
                <div class="mb-3">
                    <h3 class="text-base font-semibold text-gray-800">发布趋势</h3>
                    <p class="text-xs text-gray-400 mt-0.5">菜谱发布数量变化</p>
                </div>
                <div class="relative min-h-[16rem]">
                    <v-chart 
                        class="w-full h-64" 
                        :option="lineChartOption" 
                        autoresize 
                    />
                    <div v-if="!dashboard.monthlyRecipes.length" class="absolute inset-0 flex items-center justify-center bg-white/50 z-10">
                        <span class="text-gray-400 text-sm">暂无数据</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 下方区域 -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <!-- 数据概览 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-50">
                <h3 class="text-base font-semibold text-gray-800 mb-4">数据概览</h3>
                <div class="grid grid-cols-2 gap-3">
                    <div class="text-center p-3 bg-gray-50 rounded-lg">
                        <p class="text-xl font-semibold text-gray-800">{{ dashboard.totalUsers.toLocaleString() }}</p>
                        <p class="text-xs text-gray-500 mt-0.5">总用户量</p>
                    </div>
                    <div class="text-center p-3 bg-gray-50 rounded-lg">
                        <p class="text-xl font-semibold text-gray-800">{{ dashboard.totalRecipes.toLocaleString() }}</p>
                        <p class="text-xs text-gray-500 mt-0.5">菜谱总数</p>
                    </div>
                    <div class="text-center p-3 bg-gray-50 rounded-lg">
                        <p class="text-xl font-semibold text-gray-800">{{ dashboard.todayNewUsers }}</p>
                        <p class="text-xs text-gray-500 mt-0.5">今日新增用户</p>
                    </div>
                    <div class="text-center p-3 bg-gray-50 rounded-lg">
                        <p class="text-xl font-semibold text-gray-800">{{ dashboard.totalCategories }}</p>
                        <p class="text-xs text-gray-500 mt-0.5">分类数量</p>
                    </div>
                </div>
            </div>

            <!-- 分类分布饼图 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-50 relative">
                <h3 class="text-base font-semibold text-gray-800 mb-3">分类分布</h3>
                <div class="relative min-h-[12rem]">
                    <v-chart 
                        class="w-full h-48" 
                        :option="pieChartOption" 
                        autoresize 
                    />
                    <div v-if="!dashboard.categoryStats.length" class="absolute inset-0 flex items-center justify-center bg-white/50 z-10">
                        <span class="text-gray-400 text-sm">暂无数据</span>
                    </div>
                </div>
            </div>

            <!-- 待审核列表 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-50">
                <div class="flex items-center justify-between mb-3">
                    <div class="flex items-center gap-2">
                        <h3 class="text-base font-semibold text-gray-800">待审核</h3>
                        <span v-if="pendingRecipes.length > 0" class="bg-orange-100 text-orange-600 text-xs px-1.5 py-0.5 rounded-full font-medium">
                            {{ pendingRecipes.length }}
                        </span>
                    </div>
                    <button 
                        @click="router.push('/backstage-m9x2k7/recipes?status=0')"
                        class="flex items-center gap-1 text-xs text-orange-500 hover:text-orange-600 font-medium transition"
                    >
                        查看全部
                        <ArrowUpRight class="w-3.5 h-3.5" />
                    </button>
                </div>

                <div v-if="pendingRecipes.length === 0" class="flex flex-col items-center justify-center py-6 text-gray-400">
                    <div class="w-12 h-12 bg-gray-100 rounded-full flex items-center justify-center mb-2">
                        <Check class="w-6 h-6" />
                    </div>
                    <p class="text-sm">暂无待审核菜谱</p>
                </div>

                <div v-else class="space-y-2">
                    <div 
                        v-for="recipe in pendingRecipes.slice(0, 4)" 
                        :key="recipe.id"
                        class="flex items-center gap-2.5 p-2.5 bg-gray-50 rounded-lg hover:bg-gray-100 transition group"
                    >
                        <img 
                            :src="recipe.coverImage" 
                            class="w-10 h-10 rounded-lg object-cover bg-gray-200"
                            @error="e => e.target.src = 'https://via.placeholder.com/48'"
                        />
                        <div class="flex-1 min-w-0">
                            <h4 class="font-medium text-gray-800 truncate text-sm">{{ recipe.title }}</h4>
                            <p class="text-xs text-gray-400 truncate">{{ recipe.authorName }}</p>
                        </div>
                        <div class="flex gap-0.5 opacity-0 group-hover:opacity-100 transition">
                            <button 
                                @click="passRecipe(recipe.id)"
                                class="p-1.5 text-emerald-500 hover:bg-emerald-100 rounded-lg transition"
                                title="通过"
                            >
                                <Check class="w-4 h-4" />
                            </button>
                            <button 
                                @click="rejectRecipe(recipe.id)"
                                class="p-1.5 text-red-500 hover:bg-red-100 rounded-lg transition"
                                title="驳回"
                            >
                                <X class="w-4 h-4" />
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* 图表容器自适应 */
.echarts {
    width: 100%;
    min-height: 180px;
}
</style>
