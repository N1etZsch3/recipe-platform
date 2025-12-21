<script setup>
import { ref, onMounted, computed } from 'vue'
import { listLogs } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { 
    Search, ChevronLeft, ChevronRight, Calendar, Filter, X,
    LogIn, Ban, CheckCircle, Trash2, Plus, Edit, FileCheck, FileX, ClipboardList
} from 'lucide-vue-next'

const { showToast } = useToast()

const loading = ref(false)
const logs = ref([])
const operationType = ref('')
const startDate = ref('')
const endDate = ref('')

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
            operationType: operationType.value || undefined,
            startDate: startDate.value || undefined,
            endDate: endDate.value || undefined
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

const clearFilters = () => {
    operationType.value = ''
    startDate.value = ''
    endDate.value = ''
    pagination.value.current = 1
    fetchLogs()
}

const handlePageChange = (page) => {
    pagination.value.current = page
    fetchLogs()
}

const hasActiveFilters = computed(() => {
    return operationType.value || startDate.value || endDate.value
})

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
    return `${date.toLocaleDateString('zh-CN')} ${date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`
}

onMounted(() => {
    fetchLogs()
})
</script>

<template>
    <div class="space-y-6 p-5 h-full overflow-y-auto">
        <!-- 页面标题 -->
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-2xl font-bold text-gray-800">操作日志</h1>
                <p class="text-sm text-gray-500 mt-1">查看管理员操作记录</p>
            </div>
            <div class="flex items-center gap-2 px-4 py-2 bg-amber-50 text-amber-600 rounded-xl">
                <ClipboardList class="w-4 h-4" />
                <span class="text-sm font-medium">{{ pagination.total }} 条记录</span>
            </div>
        </div>

        <!-- 搜索和筛选 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-50 p-5">
            <div class="flex flex-wrap gap-4 items-end">
                <!-- 操作类型筛选 -->
                <div class="min-w-[180px]">
                    <label class="block text-sm text-gray-500 mb-2">操作类型</label>
                    <div class="relative">
                        <Filter class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none z-10" />
                        <select
                            v-model="operationType"
                            @change="handleSearch"
                            class="appearance-none w-full pl-11 pr-10 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition cursor-pointer"
                        >
                            <option v-for="opt in operationTypes" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                            </option>
                        </select>
                        <div class="absolute right-4 top-1/2 -translate-y-1/2 pointer-events-none">
                            <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                            </svg>
                        </div>
                    </div>
                </div>

                <!-- 开始日期 -->
                <div class="min-w-[160px]">
                    <label class="block text-sm text-gray-500 mb-2">开始日期</label>
                    <div class="relative">
                        <Calendar class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
                        <input
                            v-model="startDate"
                            type="date"
                            class="w-full pl-11 pr-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition text-sm"
                        />
                    </div>
                </div>

                <!-- 结束日期 -->
                <div class="min-w-[160px]">
                    <label class="block text-sm text-gray-500 mb-2">结束日期</label>
                    <div class="relative">
                        <Calendar class="w-4 h-4 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none" />
                        <input
                            v-model="endDate"
                            type="date"
                            class="w-full pl-11 pr-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition text-sm"
                        />
                    </div>
                </div>

                <button 
                    @click="handleSearch"
                    class="px-6 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl hover:from-orange-600 hover:to-amber-600 transition font-medium shadow-md shadow-orange-200"
                >
                    查询
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

        <!-- 日志列表 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-50 overflow-hidden">
            <div v-if="loading" class="p-16 text-center text-gray-400">
                <div class="flex flex-col items-center">
                    <div class="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
                    <span class="mt-3">加载中...</span>
                </div>
            </div>
            <div v-else-if="logs.length === 0" class="p-16 text-center text-gray-400">
                <div class="flex flex-col items-center">
                    <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-3">
                        <ClipboardList class="w-8 h-8 text-gray-300" />
                    </div>
                    <span>暂无操作日志</span>
                </div>
            </div>
            
            <template v-else>
                <div class="overflow-x-auto">
                    <table class="w-full">
                        <thead>
                            <tr class="border-b border-gray-100">
                                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">时间</th>
                                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">管理员</th>
                                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">操作</th>
                                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">目标</th>
                                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">详情</th>
                                <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">IP</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-50">
                            <tr v-for="log in logs" :key="log.id" class="hover:bg-gray-50/50 transition">
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
                                    <span class="text-sm text-gray-500 max-w-xs truncate block">
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
            </template>
        </div>
    </div>
</template>
