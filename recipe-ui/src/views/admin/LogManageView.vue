<script setup>
import { ref, onMounted, computed } from 'vue'
import { listLogs } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { 
    Search, ChevronLeft, ChevronRight, Calendar, Filter, X,
    LogIn, Ban, CheckCircle, Trash2, Plus, Edit, FileCheck, FileX
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

const getOperationColor = (type) => {
    if (type.includes('DELETE') || type.includes('BAN') || type.includes('REJECT')) {
        return 'text-red-500 bg-red-50'
    }
    if (type.includes('ADD') || type.includes('APPROVE') || type.includes('UNBAN')) {
        return 'text-green-500 bg-green-50'
    }
    if (type.includes('UPDATE') || type.includes('LOGIN')) {
        return 'text-blue-500 bg-blue-50'
    }
    return 'text-gray-500 bg-gray-50'
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

const totalPages = () => Math.ceil(pagination.value.total / pagination.value.size)

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
    <div class="flex flex-col h-full">
        <!-- 搜索和筛选栏 (固定) -->
        <div class="bg-white rounded-xl shadow-sm p-4 flex-shrink-0">
            <div class="flex flex-wrap gap-4 items-end">
                <!-- 操作类型筛选 -->
                <div class="min-w-[180px]">
                    <label class="block text-sm text-gray-500 mb-1">操作类型</label>
                    <div class="relative">
                        <Filter class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                        <select
                            v-model="operationType"
                            @change="handleSearch"
                            class="custom-select w-full pl-9"
                        >
                            <option v-for="opt in operationTypes" :key="opt.value" :value="opt.value">
                                {{ opt.label }}
                            </option>
                        </select>
                    </div>
                </div>

                <!-- 开始日期 -->
                <div class="min-w-[160px]">
                    <label class="block text-sm text-gray-500 mb-1">开始日期</label>
                    <div class="relative">
                        <Calendar class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                        <input
                            v-model="startDate"
                            type="date"
                            class="w-full pl-9 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                        />
                    </div>
                </div>

                <!-- 结束日期 -->
                <div class="min-w-[160px]">
                    <label class="block text-sm text-gray-500 mb-1">结束日期</label>
                    <div class="relative">
                        <Calendar class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                        <input
                            v-model="endDate"
                            type="date"
                            class="w-full pl-9 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none text-sm"
                        />
                    </div>
                </div>

                <button 
                    @click="handleSearch"
                    class="px-6 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition font-medium"
                >
                    查询
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

        <!-- 日志列表 (可滚动) -->
        <div class="flex-1 mt-4 bg-white rounded-xl shadow-sm overflow-hidden flex flex-col min-h-0">
            <div v-if="loading" class="p-12 text-center text-gray-400">加载中...</div>
            <div v-else-if="logs.length === 0" class="p-12 text-center text-gray-400">暂无操作日志</div>
            
            <template v-else>
                <div class="flex-1 overflow-auto">
                    <table class="w-full">
                        <thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">时间</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">管理员</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">目标</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">详情</th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">IP</th>
                            </tr>
                        </thead>
                        <tbody class="divide-y divide-gray-200">
                            <tr v-for="log in logs" :key="log.id" class="hover:bg-gray-50">
                                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                    {{ formatDate(log.createTime) }}
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap">
                                    <span class="text-sm font-medium text-gray-900">{{ log.adminName }}</span>
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap">
                                    <span :class="['inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium', getOperationColor(log.operationType)]">
                                        <component :is="getOperationIcon(log.operationType)" class="w-3.5 h-3.5" />
                                        {{ log.operationTypeName }}
                                    </span>
                                </td>
                                <td class="px-6 py-4 whitespace-nowrap">
                                    <div v-if="log.targetName" class="text-sm text-gray-900">
                                        {{ log.targetName }}
                                        <span class="text-gray-400 text-xs ml-1">
                                            ({{ log.targetType }}#{{ log.targetId }})
                                        </span>
                                    </div>
                                    <span v-else class="text-gray-400 text-sm">-</span>
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
            </template>
        </div>
    </div>
</template>
