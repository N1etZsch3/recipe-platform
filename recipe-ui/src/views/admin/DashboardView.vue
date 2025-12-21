<script setup>
import { ref, onMounted, watch, computed } from 'vue'
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
    MessageSquare,
    Clock,
    Check,
    X,
    ArrowUpRight,
    TrendingUp,
    TrendingDown,
    Eye,
    Activity,
    UserPlus,
    FileText
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
    monthlyRecipes: [],
    categoryStats: [],
    monthlyUsers: [],
    weeklyActiveUsers: 0,
    userGrowthRate: 0,
    totalViews: 0,
    todayViews: 0,
    topActiveUsers: [],
    recentActivities: []
})
const pendingRecipes = ref([])

// 格式化大数字
const formatNumber = (num) => {
    if (num >= 10000) {
        return (num / 10000).toFixed(1) + 'w'
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'k'
    }
    return num?.toString() || '0'
}

// 格式化时间
const formatTime = (time) => {
    if (!time) return ''
    const date = new Date(time)
    const now = new Date()
    const diff = now - date
    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)
    
    if (minutes < 1) return '刚刚'
    if (minutes < 60) return `${minutes}分钟前`
    if (hours < 24) return `${hours}小时前`
    if (days < 7) return `${days}天前`
    return date.toLocaleDateString()
}

// 统计卡片配置 - 优化后的4个指标
const statCards = computed(() => [
    { 
        key: 'totalRecipes', 
        label: '菜谱总数', 
        value: dashboard.value.totalRecipes,
        subValue: dashboard.value.publishedRecipes,
        subLabel: '已发布',
        trend: dashboard.value.monthlyRecipes?.length > 0 
            ? '+' + (dashboard.value.monthlyRecipes[dashboard.value.monthlyRecipes.length - 1]?.count || 0)
            : '+0',
        trendLabel: '本月新增',
        trendUp: true,
        icon: ChefHat,
        iconBg: 'bg-gradient-to-br from-orange-400 to-amber-500'
    },
    { 
        key: 'totalUsers', 
        label: '用户总数', 
        value: dashboard.value.totalUsers,
        subValue: dashboard.value.todayNewUsers,
        subLabel: '今日新增',
        trend: dashboard.value.userGrowthRate >= 0 
            ? `+${(dashboard.value.userGrowthRate || 0).toFixed(0)}%` 
            : `${(dashboard.value.userGrowthRate || 0).toFixed(0)}%`,
        trendLabel: '月环比',
        trendUp: dashboard.value.userGrowthRate >= 0,
        icon: Users,
        iconBg: 'bg-gradient-to-br from-blue-400 to-blue-600'
    },
    { 
        key: 'totalComments', 
        label: '评论总数', 
        value: dashboard.value.totalComments,
        subValue: dashboard.value.todayComments,
        subLabel: '今日新增',
        trend: '+' + (dashboard.value.todayComments || 0),
        trendLabel: '今日',
        trendUp: true,
        icon: MessageSquare,
        iconBg: 'bg-gradient-to-br from-purple-400 to-purple-600'
    },
    { 
        key: 'pendingRecipes', 
        label: '待审核', 
        value: dashboard.value.pendingRecipes,
        subValue: dashboard.value.totalCategories,
        subLabel: '分类数量',
        trend: dashboard.value.pendingRecipes > 0 ? '需处理' : '已清空',
        trendLabel: '',
        trendUp: dashboard.value.pendingRecipes === 0,
        icon: Clock,
        iconBg: 'bg-gradient-to-br from-emerald-400 to-emerald-600'
    }
])

// 用户增长趋势图配置
const userGrowthChartOption = computed(() => {
    const monthlyData = dashboard.value.monthlyUsers || []
    const months = monthlyData.map(item => `${item.month}月`)
    const values = monthlyData.map(item => item.count)
    
    return {
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e5e7eb',
            borderWidth: 1,
            textStyle: { color: '#374151', fontSize: 12 },
            formatter: '{b}: {c} 人'
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
            name: '新增用户',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 6,
            data: values,
            lineStyle: { 
                color: '#3b82f6',
                width: 2.5
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
                        { offset: 0, color: 'rgba(59, 130, 246, 0.25)' },
                        { offset: 1, color: 'rgba(59, 130, 246, 0.02)' }
                    ]
                }
            }
        }]
    }
})

// 柱状图配置 - 月度发布
const barChartOption = computed(() => {
    const monthlyData = dashboard.value.monthlyRecipes || []
    const months = monthlyData.map(item => `${item.month}月`)
    const values = monthlyData.map(item => item.count)
    
    return {
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
                        { offset: 0, color: '#3b82f6' },
                        { offset: 1, color: '#93c5fd' }
                    ]
                }
            }
        }]
    }
})

// 饼图配置 - 分类分布
const pieChartOption = computed(() => {
    const categoryData = dashboard.value.categoryStats || []
    const colors = ['#f97316', '#3b82f6', '#8b5cf6', '#10b981', '#f59e0b', '#ec4899', '#6b7280']
    
    return {
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
            textStyle: { color: '#6b7280', fontSize: 12 },
            itemWidth: 10,
            itemHeight: 10
        },
        series: [{
            name: '分类占比',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['35%', '50%'],
            avoidLabelOverlap: false,
            label: { show: false },
            labelLine: { show: false },
            emphasis: {
                itemStyle: {
                    shadowBlur: 10,
                    shadowOffsetX: 0,
                    shadowColor: 'rgba(0, 0, 0, 0.2)'
                }
            },
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
            dashboard.value = { ...dashboard.value, ...res }
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
        if (res) categories.value = res
    } catch (error) {
        console.error('Failed to fetch categories', error)
    }
}

const fetchPendingRecipes = async () => {
    try {
        const res = await listAuditRecipes({ page: 1, size: 5 })
        if (res?.records) pendingRecipes.value = res.records
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
    if (confirmed) handleAudit(id, 'pass')
}

// 获取动态图标
const getActivityIcon = (action) => {
    switch (action) {
        case 'PUBLISH_RECIPE': return FileText
        case 'COMMENT': return MessageSquare
        case 'REGISTER': return UserPlus
        default: return Activity
    }
}

// 获取动态图标颜色
const getActivityColor = (action) => {
    switch (action) {
        case 'PUBLISH_RECIPE': return 'text-orange-500 bg-orange-50'
        case 'COMMENT': return 'text-blue-500 bg-blue-50'
        case 'REGISTER': return 'text-green-500 bg-green-50'
        default: return 'text-gray-500 bg-gray-50'
    }
}

// 监听新菜谱待审核通知
watch(() => notificationStore.latestNotification, (notification) => {
    if (!notification) return
    if (notification.type === 'NEW_RECIPE_PENDING') {
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
    <div class="space-y-5 p-5 h-full overflow-y-auto bg-gray-50/30">
        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div 
                v-for="stat in statCards" 
                :key="stat.key"
                class="bg-white rounded-xl p-5 shadow-sm hover:shadow-md transition-all duration-200 border border-gray-100"
            >
                <div class="flex items-start justify-between">
                    <div class="flex-1">
                        <p class="text-sm text-gray-500 mb-1">{{ stat.label }}</p>
                        <p class="text-2xl font-bold text-gray-800">{{ formatNumber(stat.value) }}</p>
                        <div class="flex items-center gap-2 mt-2">
                            <span class="text-xs text-gray-400">{{ stat.subLabel }}</span>
                            <span class="text-xs font-medium text-gray-600">{{ stat.subValue }}</span>
                        </div>
                    </div>
                    <div :class="['w-11 h-11 rounded-xl flex items-center justify-center shadow-lg', stat.iconBg]">
                        <component :is="stat.icon" class="w-5 h-5 text-white" />
                    </div>
                </div>
                <!-- 趋势标签 -->
                <div class="mt-3 pt-3 border-t border-gray-100 flex items-center gap-1.5">
                    <component 
                        :is="stat.trendUp ? TrendingUp : TrendingDown" 
                        :class="['w-3.5 h-3.5', stat.trendUp ? 'text-green-500' : 'text-red-500']" 
                    />
                    <span :class="['text-xs font-medium', stat.trendUp ? 'text-green-600' : 'text-red-600']">
                        {{ stat.trend }}
                    </span>
                    <span v-if="stat.trendLabel" class="text-xs text-gray-400">{{ stat.trendLabel }}</span>
                </div>
            </div>
        </div>

        <!-- 图表区域 - 左边柱状图+用户概况，右边折线图 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-4">
            <!-- 左侧：月度菜谱发布 + 用户概况 -->
            <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                <!-- 柱状图 -->
                <div class="p-5">
                    <div class="flex items-center justify-between mb-3">
                        <div>
                            <h3 class="text-base font-semibold text-gray-800">月度菜谱发布</h3>
                            <p class="text-xs text-gray-400 mt-0.5">每月新增菜谱统计</p>
                        </div>
                    </div>
                    <div class="relative h-48">
                        <v-chart class="w-full h-full" :option="barChartOption" autoresize />
                        <div v-if="!dashboard.monthlyRecipes?.length" class="absolute inset-0 flex items-center justify-center bg-white/50">
                            <span class="text-gray-400 text-sm">暂无数据</span>
                        </div>
                    </div>
                </div>
                
                <!-- 用户概况区域 -->
                <div class="px-5 pb-5 pt-2 border-t border-gray-100 bg-gray-50/50">
                    <div class="flex items-center justify-between mb-3">
                        <h4 class="text-sm font-medium text-gray-700">用户概况</h4>
                        <span :class="[
                            'text-xs font-medium px-2 py-0.5 rounded-full',
                            dashboard.userGrowthRate >= 0 ? 'text-green-600 bg-green-100' : 'text-red-600 bg-red-100'
                        ]">
                            {{ dashboard.userGrowthRate >= 0 ? '+' : '' }}{{ (dashboard.userGrowthRate || 0).toFixed(1) }}% 月环比
                        </span>
                    </div>
                    <div class="grid grid-cols-3 gap-4">
                        <div class="text-center">
                            <p class="text-xl font-bold text-gray-800">{{ formatNumber(dashboard.weeklyActiveUsers) }}</p>
                            <p class="text-xs text-gray-500 mt-1">周活跃用户</p>
                        </div>
                        <div class="text-center">
                            <p class="text-xl font-bold text-gray-800">{{ formatNumber(dashboard.totalViews) }}</p>
                            <p class="text-xs text-gray-500 mt-1">总浏览量</p>
                        </div>
                        <div class="text-center">
                            <p class="text-xl font-bold text-gray-800">{{ formatNumber(dashboard.todayNewUsers) }}</p>
                            <p class="text-xs text-gray-500 mt-1">今日新增</p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 右侧：用户增长趋势 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <div class="flex items-center justify-between mb-3">
                    <div>
                        <h3 class="text-base font-semibold text-gray-800">用户增长趋势</h3>
                        <p class="text-xs text-gray-400 mt-0.5">每月新增注册用户</p>
                    </div>
                    <div class="flex items-center gap-1.5 text-sm text-blue-500">
                        <TrendingUp class="w-4 h-4" />
                        <span class="font-medium">{{ (dashboard.userGrowthRate || 0).toFixed(1) }}%</span>
                    </div>
                </div>
                <div class="relative h-64">
                    <v-chart class="w-full h-full" :option="userGrowthChartOption" autoresize />
                    <div v-if="!dashboard.monthlyUsers?.length" class="absolute inset-0 flex items-center justify-center bg-white/50">
                        <span class="text-gray-400 text-sm">暂无数据</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 下方三栏区域 -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-4">
            <!-- 活跃用户列表 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <div class="flex items-center justify-between mb-4">
                    <h3 class="text-base font-semibold text-gray-800">活跃用户</h3>
                    <span class="text-xs text-gray-400">TOP 5</span>
                </div>
                
                <div v-if="!dashboard.topActiveUsers?.length" class="flex flex-col items-center justify-center py-8 text-gray-400">
                    <Users class="w-10 h-10 mb-2 opacity-40" />
                    <p class="text-sm">暂无活跃用户数据</p>
                </div>

                <div v-else class="space-y-3">
                    <div 
                        v-for="(user, index) in dashboard.topActiveUsers" 
                        :key="user.id"
                        class="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 transition"
                    >
                        <div class="relative">
                            <img 
                                :src="user.avatar || 'https://via.placeholder.com/40'" 
                                :alt="user.nickname"
                                class="w-9 h-9 rounded-full object-cover ring-2 ring-gray-100"
                                @error="e => e.target.src = 'https://via.placeholder.com/40'"
                            />
                            <span v-if="index < 3" :class="[
                                'absolute -top-1 -right-1 w-4 h-4 rounded-full flex items-center justify-center text-[10px] font-bold text-white',
                                index === 0 ? 'bg-yellow-400' : index === 1 ? 'bg-gray-400' : 'bg-amber-600'
                            ]">
                                {{ index + 1 }}
                            </span>
                        </div>
                        <div class="flex-1 min-w-0">
                            <p class="font-medium text-gray-800 truncate text-sm">{{ user.nickname }}</p>
                            <p class="text-xs text-gray-400">{{ user.recipeCount }} 菜谱</p>
                        </div>
                        <div class="w-14">
                            <div class="h-1.5 bg-gray-100 rounded-full overflow-hidden">
                                <div 
                                    class="h-full bg-gradient-to-r from-blue-400 to-blue-500 rounded-full"
                                    :style="{ width: user.activityScore + '%' }"
                                ></div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 分类分布饼图 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <h3 class="text-base font-semibold text-gray-800 mb-3">分类分布</h3>
                <div class="relative h-52">
                    <v-chart class="w-full h-full" :option="pieChartOption" autoresize />
                    <div v-if="!dashboard.categoryStats?.length" class="absolute inset-0 flex items-center justify-center bg-white/50">
                        <span class="text-gray-400 text-sm">暂无数据</span>
                    </div>
                </div>
            </div>

            <!-- 最新动态 -->
            <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
                <div class="flex items-center justify-between mb-4">
                    <h3 class="text-base font-semibold text-gray-800">最新动态</h3>
                    <Activity class="w-4 h-4 text-gray-400" />
                </div>
                
                <div v-if="!dashboard.recentActivities?.length" class="flex flex-col items-center justify-center py-8 text-gray-400">
                    <Activity class="w-10 h-10 mb-2 opacity-40" />
                    <p class="text-sm">暂无动态</p>
                </div>

                <div v-else class="space-y-3 max-h-52 overflow-y-auto custom-scrollbar">
                    <div 
                        v-for="(activity, index) in dashboard.recentActivities?.slice(0, 6)" 
                        :key="index"
                        class="flex items-start gap-2.5 text-sm"
                    >
                        <div :class="['w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0', getActivityColor(activity.action)]">
                            <component :is="getActivityIcon(activity.action)" class="w-3.5 h-3.5" />
                        </div>
                        <div class="flex-1 min-w-0">
                            <p class="text-gray-600 text-xs leading-relaxed">
                                <span class="font-medium text-gray-800">{{ activity.userName }}</span>
                                {{ activity.actionText }}
                                <span class="font-medium text-gray-700">{{ activity.targetTitle }}</span>
                            </p>
                            <p class="text-[11px] text-gray-400 mt-0.5">{{ formatTime(activity.time) }}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 待审核菜谱列表 -->
        <div class="bg-white rounded-xl p-5 shadow-sm border border-gray-100">
            <div class="flex items-center justify-between mb-4">
                <div class="flex items-center gap-2">
                    <h3 class="text-base font-semibold text-gray-800">待审核菜谱</h3>
                    <span v-if="pendingRecipes.length > 0" class="bg-orange-100 text-orange-600 text-xs px-2 py-0.5 rounded-full font-medium">
                        {{ pendingRecipes.length }}
                    </span>
                </div>
                <button 
                    @click="router.push('/backstage-m9x2k7/recipes?status=0')"
                    class="flex items-center gap-1 text-sm text-orange-500 hover:text-orange-600 font-medium transition"
                >
                    查看全部
                    <ArrowUpRight class="w-4 h-4" />
                </button>
            </div>

            <div v-if="pendingRecipes.length === 0" class="flex flex-col items-center justify-center py-8 text-gray-400">
                <div class="w-12 h-12 bg-green-50 rounded-full flex items-center justify-center mb-3">
                    <Check class="w-6 h-6 text-green-500" />
                </div>
                <p class="text-sm font-medium text-gray-500">暂无待审核菜谱</p>
                <p class="text-xs text-gray-400 mt-1">所有菜谱已处理完毕</p>
            </div>

            <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-3">
                <div 
                    v-for="recipe in pendingRecipes.slice(0, 4)" 
                    :key="recipe.id"
                    class="flex items-center gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition"
                >
                    <img 
                        :src="recipe.coverImage" 
                        class="w-12 h-12 rounded-lg object-cover bg-gray-200 flex-shrink-0"
                        @error="e => e.target.src = 'https://via.placeholder.com/48'"
                    />
                    <div class="flex-1 min-w-0">
                        <h4 class="font-medium text-gray-800 truncate text-sm">{{ recipe.title }}</h4>
                        <p class="text-xs text-gray-400 truncate">{{ recipe.authorName }}</p>
                        <div class="flex gap-1.5 mt-2">
                            <button 
                                @click="passRecipe(recipe.id)"
                                class="px-2.5 py-1 text-xs bg-green-500 text-white rounded hover:bg-green-600 transition font-medium"
                            >
                                通过
                            </button>
                            <button 
                                @click="rejectRecipe(recipe.id)"
                                class="px-2.5 py-1 text-xs bg-red-500 text-white rounded hover:bg-red-600 transition font-medium"
                            >
                                驳回
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.echarts {
    width: 100%;
    min-height: 180px;
}

.custom-scrollbar::-webkit-scrollbar {
    width: 3px;
}
.custom-scrollbar::-webkit-scrollbar-track {
    background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #e5e7eb;
    border-radius: 2px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: #d1d5db;
}
</style>
