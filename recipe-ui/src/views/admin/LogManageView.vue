<script setup>
import { ref, onMounted, computed } from 'vue'
import { listLogs } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { 
    Search, ChevronLeft, ChevronRight, Calendar, Filter, X,
    LogIn, Ban, CheckCircle, Trash2, Plus, Edit, FileCheck, FileX, ClipboardList,
    RefreshCw, Settings
} from 'lucide-vue-next'

const { showToast } = useToast()

const loading = ref(false)
const logs = ref([])

// Search state
const searchForm = ref({
    operationType: '',
    startDate: '',
    endDate: ''
})
const showFilterPanel = ref(false)

const pagination = ref({
    current: 1,
    size: 20,
    total: 0
})

const operationTypes = [
    { value: '', label: '全部操作' },
    { value: 'ADMIN_LOGIN', label: '管理员登录' },
    { value: 'USER_BAN', label: '封禁用户' },
    { value: 'USER_UNBAN', label: '解封用户' },
    { value: 'RECIPE_APPROVE', label: '审核通过菜谱' },
    { value: 'RECIPE_REJECT', label: '驳回菜谱' },
    { value: 'RECIPE_DELETE', label: '删除菜谱' },
    { value: 'CATEGORY_ADD', label: '新增分类' },
    { value: 'CATEGORY_UPDATE', label: '修改分类' },
    { value: 'CATEGORY_DELETE', label: '删除分类' },
    { value: 'COMMENT_DELETE', label: '删除评论' }
]

const getOperationIcon = (type) => {
    const icons = {
        'ADMIN_LOGIN': LogIn,
        'USER_BAN': Ban,
        'USER_UNBAN': CheckCircle,
        'RECIPE_APPROVE': FileCheck,
        'RECIPE_REJECT': FileX,
        'RECIPE_DELETE': Trash2,
        'CATEGORY_ADD': Plus,
        'CATEGORY_UPDATE': Edit,
        'CATEGORY_DELETE': Trash2,
        'COMMENT_DELETE': Trash2
    }
    return icons[type] || Filter
}

const getOperationConfig = (type) => {
    if (type.includes('DELETE') || type.includes('BAN') || type.includes('REJECT')) {
        return { bg: 'bg-red-50', text: 'text-red-600', dot: 'bg-red-500' }
    }
    if (type.includes('ADD') || type.includes('APPROVE') || type.includes('UNBAN')) {
        return { bg: 'bg-emerald-50', text: 'text-emerald-600', dot: 'bg-emerald-500' }
    }
    if (type.includes('UPDATE') || type.includes('LOGIN')) {
        return { bg: 'bg-blue-50', text: 'text-blue-600', dot: 'bg-blue-500' }
    }
    return { bg: 'bg-gray-100', text: 'text-gray-600', dot: 'bg-gray-400' }
}

const fetchLogs = async () => {
    loading.value = true
    try {
        const res = await listLogs({
            page: pagination.value.current,
            size: pagination.value.size,
            operationType: searchForm.value.operationType || undefined,
            startDate: searchForm.value.startDate || undefined,
            endDate: searchForm.value.endDate || undefined
        })
        if (res) {
            logs.value = res.records || []
            pagination.value.total = res.total || 0
        }
    } catch (error) {
        console.error('Failed to fetch logs', error)
        showToast('获取操作日志失败')
    } finally {
        loading.value = false
    }
}

const handleSearch = () => {
    pagination.value.current = 1
    fetchLogs()
}

const handleReset = () => {
    searchForm.value = {
        operationType: '',
        startDate: '',
        endDate: ''
    }
    handleSearch()
}

const handlePageChange = (page) => {
    pagination.value.current = page
    fetchLogs()
}

const hasActiveFilters = computed(() => {
    return searchForm.value.operationType || searchForm.value.startDate || searchForm.value.endDate
})

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
    return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`
}

onMounted(() => {
    fetchLogs()
})
</script>

<template>
    <div class="flex flex-col h-full p-5 space-y-4" @click="showFilterPanel = false">
        <!-- 顶部搜索栏 -->
        <div class="flex-shrink-0 bg-white rounded-xl p-5 border border-gray-100 shadow-sm">
            <div class="flex flex-wrap items-center gap-6">
                <!-- 操作类型筛选 (Primary) -->
                <div class="flex items-center gap-3">
                    <label class="text-sm font-medium text-gray-600 whitespace-nowrap">操作类型</label>
                    <div class="relative">
                        <select
                            v-model="searchForm.operationType"
                            @change="handleSearch"
                            class="w-48 appearance-none pl-3 pr-10 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400 transition cursor-pointer"
                        >
                            <option v-for="opt in operationTypes" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                            </option>
                        </select>
                        <div class="absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none">
                            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                            </svg>
                        </div>
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
                    <div class="text-sm font-medium text-gray-500 flex items-center gap-2">
                        <ClipboardList class="w-4 h-4" />
                        日志列表
                    </div>
                </div>
                
                <div class="flex items-center gap-2">
                    <button class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" title="刷新" @click="fetchLogs">
                        <RefreshCw class="w-4 h-4" />
                    </button>
                    <div class="relative">
                        <button 
                            class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" 
                            title="高级筛选"
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
                            class="absolute right-0 top-full mt-2 w-72 bg-white border border-gray-100 rounded-lg shadow-xl p-4 space-y-4 z-30"
                            @click.stop
                        >
                            <div>
                                <label class="text-xs font-medium text-gray-500 mb-1.5 block">开始日期</label>
                                <input
                                    v-model="searchForm.startDate"
                                    type="date"
                                    class="w-full px-3 py-2 bg-white border border-gray-200 rounded-md text-sm focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                                />
                            </div>
                            <div>
                                <label class="text-xs font-medium text-gray-500 mb-1.5 block">结束日期</label>
                                <input
                                    v-model="searchForm.endDate"
                                    type="date"
                                    class="w-full px-3 py-2 bg-white border border-gray-200 rounded-md text-sm focus:ring-1 focus:ring-blue-500 focus:border-blue-500 outline-none transition"
                                />
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
                            <th class="p-4 px-6 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">时间</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">管理员</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">操作</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">目标</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">详情</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">IP</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-50 text-sm">
                        <tr v-if="loading">
                             <td colspan="6" class="p-12 text-center text-gray-400">
                                 <div class="flex flex-col items-center">
                                     <div class="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                                     <span class="mt-2 text-xs">加载中...</span>
                                 </div>
                             </td>
                        </tr>
                        <tr v-else-if="logs.length === 0">
                             <td colspan="6" class="p-12 text-center text-gray-400">暂无操作日志</td>
                        </tr>
                        <tr 
                            v-else
                            v-for="log in logs" 
                            :key="log.id" 
                            class="hover:bg-gray-50/80 transition"
                        >
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                {{ formatDate(log.createTime) }}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="text-sm font-medium text-gray-800">{{ log.adminName }}</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span :class="[
                                    'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium',
                                    getOperationConfig(log.operationType).bg,
                                    getOperationConfig(log.operationType).text
                                ]">
                                    <component :is="getOperationIcon(log.operationType)" class="w-3.5 h-3.5" />
                                    {{ log.operationTypeName }}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div v-if="log.targetName" class="text-sm text-gray-800">
                                    {{ log.targetName }}
                                    <span class="text-gray-400 text-xs ml-1">
                                        ({{ log.targetType }}#{{ log.targetId }})
                                    </span>
                                </div>
                                <span v-else class="text-gray-300 text-sm">-</span>
                            </td>
                            <td class="px-6 py-4">
                                <span class="text-sm text-gray-500 max-w-xs truncate block" :title="log.detail">
                                    {{ log.detail || '-' }}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-400">
                                {{ log.ipAddress || '-' }}
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
</style>
