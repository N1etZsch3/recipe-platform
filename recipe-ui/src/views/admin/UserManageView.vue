<script setup>
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { listUsers, updateUserStatus, batchUpdateUserStatus, getUsersOnlineStatus, kickUser as kickUserApi } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import UserModal from './components/UserModal.vue'
import { 
    Search, Ban, CheckCircle, ChevronLeft, ChevronRight, 
    Users, UserPlus, Filter, Edit, Trash2, 
    RefreshCw, ArrowUpDown, Maximize, Settings, MoreHorizontal,
    ChevronDown, Shield, FileText, Lock, LogOut, Circle
} from 'lucide-vue-next'

const { showToast } = useToast()
const { confirm } = useModal()

const loading = ref(false)
const users = ref([])
const selectedIds = ref([])

// 搜索字段
const searchForm = ref({
    keyword: '',
    role: '',
    sortBy: ''
})

const showFilterPanel = ref(false)

const pagination = ref({
    current: 1,
    size: 20,
    total: 0
})

const showModal = ref(false)
const editingUser = ref(null)
const onlineStatus = ref({})

// 状态配置
const getStatusConfig = (status) => {
    const configs = {
        0: { bg: 'bg-red-50', text: 'text-red-600', label: '封禁' },
        1: { bg: 'bg-emerald-50', text: 'text-emerald-600', label: '正常' }
    }
    return configs[status] || configs[1]
}

const getRoleConfig = (role) => {
    if (role === 'admin') return { bg: 'bg-purple-50', text: 'text-purple-600', label: '超级管理员' }
    if (role === 'common_admin') return { bg: 'bg-blue-50', text: 'text-blue-600', label: '管理员' }
    return { bg: 'bg-gray-50', text: 'text-gray-600', label: '普通用户' }
}

const fetchUsers = async () => {
    loading.value = true
    // Reset selection on page change or refresh
    selectedIds.value = []
    
    try {
        const res = await listUsers({
            page: pagination.value.current,
            size: pagination.value.size,
            keyword: searchForm.value.keyword || undefined,
            role: searchForm.value.role || undefined,
            sortBy: searchForm.value.sortBy || undefined
        })
        if (res) {
            users.value = res.records || []
            pagination.value.total = res.total || 0
        }
    } catch (error) {
        console.error('Failed to fetch users', error)
        showToast('获取用户列表失败')
    } finally {
        loading.value = false
    }
}

// 获取用户在线状态
const fetchOnlineStatus = async () => {
    if (users.value.length === 0) return
    try {
        const userIds = users.value.map(u => u.id)
        const res = await getUsersOnlineStatus(userIds)
        if (res) {
            onlineStatus.value = res
        }
    } catch (error) {
        console.error('Failed to fetch online status', error)
    }
}

// 踢用户下线
const handleKickUser = async (user) => {
    const confirmed = await confirm(`确定要将用户 "${user.nickname}" 踢下线吗？`, { danger: true })
    if (!confirmed) return
    try {
        await kickUserApi(user.id)
        onlineStatus.value[user.id] = false
        showToast('已将用户踢下线')
    } catch (error) {
        showToast('操作失败')
    }
}

const toggleUserStatus = async (user) => {
    const newStatus = user.status === 1 ? 0 : 1
    const action = newStatus === 0 ? '封禁' : '解封'
    
    const confirmed = await confirm(`确定要${action}用户 "${user.nickname}" 吗？`, { danger: newStatus === 0 })
    if (!confirmed) return

    try {
        // 封禁用户时，如果用户在线，先踢下线
        if (newStatus === 0 && onlineStatus.value[user.id]) {
            await kickUserApi(user.id)
            onlineStatus.value[user.id] = false
        }
        await updateUserStatus(user.id, { status: newStatus })
        user.status = newStatus
        showToast(`${action}成功`)
    } catch (error) {
        showToast('操作失败')
    }
}

const handleBatchAction = async (status) => {
    if (selectedIds.value.length === 0) return
    const action = status === 0 ? '批量封禁' : '批量解封'
    
    const confirmed = await confirm(`确定要${action}选中的 ${selectedIds.value.length} 名用户吗？`, { danger: status === 0 })
    if (!confirmed) return
    
    try {
        await batchUpdateUserStatus({
            ids: selectedIds.value,
            status: status
        })
        showToast(`${action}成功`)
        fetchUsers() // Refresh
    } catch (error) {
        showToast('批量操作失败')
    }
}

// Selection Logic
const isAllSelected = computed(() => {
    return users.value.length > 0 && users.value.every(u => selectedIds.value.includes(u.id))
})

const toggleSelectAll = (e) => {
    if (e.target.checked) {
        selectedIds.value = users.value.map(u => u.id)
    } else {
        selectedIds.value = []
    }
}

const toggleSelect = (user) => {
    const id = user.id
    if (selectedIds.value.includes(id)) {
        selectedIds.value = selectedIds.value.filter(i => i !== id)
    } else {
        selectedIds.value.push(id)
    }
}

const openAddModal = () => {
    editingUser.value = null
    showModal.value = true
}

const openEditModal = (user) => {
    editingUser.value = { ...user } 
    showModal.value = true
}

const handleSearch = () => {
    pagination.value.current = 1
    fetchUsers()
}

const handleReset = () => {
    searchForm.value = {
        keyword: '',
        role: '',
        sortBy: ''
    }
    handleSearch()
}

const handlePageChange = (page) => {
    pagination.value.current = page
    fetchUsers()
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

onMounted(() => {
    fetchUsers().then(() => fetchOnlineStatus())
    // 监听用户在线状态变化事件
    window.addEventListener('admin-user-status', handleUserStatusChange)
})

onUnmounted(() => {
    // 清理事件监听器
    window.removeEventListener('admin-user-status', handleUserStatusChange)
})

// 处理用户状态变化事件（带防抖避免页面刷新闪烁）
let statusDebounceTimer = null
const handleUserStatusChange = (event) => {
    const { type, relatedId } = event.detail
    if (!relatedId) return
    
    // 清除之前的定时器
    if (statusDebounceTimer) {
        clearTimeout(statusDebounceTimer)
    }
    
    // 300ms 防抖，避免页面刷新时先离线再上线的闪烁
    statusDebounceTimer = setTimeout(() => {
        onlineStatus.value[relatedId] = type === 'USER_ONLINE'
    }, 300)
}

const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    const d = new Date(dateStr)
    return d.toISOString().replace('T', ' ').substring(0, 19)
}

const getAvatar = (user) => {
    return user.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${user.id}`
}
</script>

<template>
    <div class="flex flex-col h-full p-5 space-y-4" @click="showFilterPanel = false">
        <!-- 顶部搜索栏 -->
        <div class="flex-shrink-0 bg-white rounded-xl p-5 border border-gray-100 shadow-sm">
            <div class="flex flex-wrap items-center gap-6">
                <!-- 搜索关键词 -->
                <div class="flex items-center gap-3">
                    <label class="text-sm font-medium text-gray-600 whitespace-nowrap">搜索</label>
                    <div class="relative">
                        <input 
                            v-model="searchForm.keyword"
                            type="text" 
                            placeholder="搜索昵称或用户名" 
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
                    <button 
                        @click="openAddModal"
                        class="px-4 py-2 text-sm font-medium text-blue-500 border border-blue-200 rounded-lg hover:bg-blue-50 transition flex items-center gap-2"
                    >
                        <UserPlus class="w-4 h-4" />
                        新增用户
                    </button>

                    <!-- 批量操作 -->
                    <div v-if="selectedIds.length > 0" class="flex items-center gap-2 animate-fade-in">
                        <span class="text-xs text-gray-500">已选 {{ selectedIds.length }} 项</span>
                        <button 
                            @click="handleBatchAction(0)"
                            class="px-3 py-1.5 text-xs font-medium text-red-600 bg-red-50 border border-red-100 rounded-lg hover:bg-red-100 transition flex items-center gap-1"
                        >
                            <Ban class="w-3.5 h-3.5" />
                            批量封禁
                        </button>
                        <button 
                            @click="handleBatchAction(1)"
                            class="px-3 py-1.5 text-xs font-medium text-emerald-600 bg-emerald-50 border border-emerald-100 rounded-lg hover:bg-emerald-100 transition flex items-center gap-1"
                        >
                            <CheckCircle class="w-3.5 h-3.5" />
                            批量解封
                        </button>
                    </div>
                </div>
                
                <div class="flex items-center gap-2">
                    <button class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" title="刷新" @click="fetchUsers">
                        <RefreshCw class="w-4 h-4" />
                    </button>
                    <div class="relative">
                        <button 
                            class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" 
                            title="设置"
                            @click.stop="showFilterPanel = !showFilterPanel"
                        >
                            <Settings class="w-4 h-4" />
                        </button>
                        <!-- 筛选下拉面板 -->
                        <div 
                            v-if="showFilterPanel"
                            class="absolute right-0 top-full mt-2 w-48 bg-white border border-gray-100 rounded-lg shadow-xl p-3 space-y-3 z-30"
                            @click.stop
                        >
                            <div>
                                <label class="text-xs font-medium text-gray-500 mb-1 block">角色筛选</label>
                                <select v-model="searchForm.role" class="w-full text-sm border-gray-200 rounded-md focus:border-blue-500 focus:ring-1 focus:ring-blue-500">
                                    <option value="">全部角色</option>
                                    <option value="common_admin">普通管理员</option>
                                    <option value="member">普通用户</option>
                                </select>
                            </div>
                            <div>
                                <label class="text-xs font-medium text-gray-500 mb-1 block">排序方式</label>
                                <select v-model="searchForm.sortBy" class="w-full text-sm border-gray-200 rounded-md focus:border-blue-500 focus:ring-1 focus:ring-blue-500">
                                    <option value="">注册时间 (最新)</option>
                                    <option value="oldest">注册时间 (最早)</option>
                                </select>
                            </div>
                            <button 
                                @click="handleSearch(); showFilterPanel=false"
                                class="w-full py-1.5 text-xs font-medium text-white bg-blue-500 rounded hover:bg-blue-600"
                            >
                                应用
                            </button>
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
                            <th class="p-4 font-medium">用户信息</th>
                            <th class="p-4 font-medium">角色</th>
                            <th class="p-4 font-medium text-center">总发布菜谱数</th>
                            <th class="p-4 font-medium">状态</th>
                            <th class="p-4 font-medium text-center">在线</th>
                            <th class="p-4 font-medium">创建日期</th>
                            <th class="p-4 font-medium text-right pr-8">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-50 text-sm">
                        <tr v-if="loading">
                             <td colspan="8" class="p-12 text-center text-gray-400">
                                 <div class="flex flex-col items-center">
                                     <div class="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                                     <span class="mt-2 text-xs">加载中...</span>
                                 </div>
                             </td>
                        </tr>
                        <tr v-else-if="users.length === 0">
                             <td colspan="8" class="p-12 text-center text-gray-400">暂无数据</td>
                        </tr>
                        <tr 
                            v-else
                            v-for="(user, index) in users" 
                            :key="user.id" 
                            class="hover:bg-gray-50/80 transition group"
                        >
                            <td class="p-4 text-center">
                                <input 
                                    type="checkbox" 
                                    :checked="selectedIds.includes(user.id)"
                                    @change="toggleSelect(user)"
                                    class="rounded border-gray-300 text-blue-500 focus:ring-blue-200 cursor-pointer" 
                                />
                            </td>
                            <td class="px-4 py-3 text-center">
                                <span class="text-gray-500 font-mono text-xs">{{ user.id }}</span>
                            </td>
                            <td class="p-4">
                                <div class="flex items-center gap-3">
                                    <img 
                                        :src="getAvatar(user)" 
                                        class="w-10 h-10 rounded-full object-cover bg-gray-100 border border-gray-200"
                                    />
                                    <div>
                                        <div class="font-medium text-gray-800">{{ user.nickname || '未设置昵称' }}</div>
                                        <div class="text-xs text-gray-400">{{ user.username }}</div>
                                    </div>
                                </div>
                            </td>
                            <td class="p-4">
                                <span :class="[
                                    'inline-flex items-center gap-1 px-2 py-1 text-xs font-medium rounded',
                                    getRoleConfig(user.role).bg,
                                    getRoleConfig(user.role).text
                                ]">
                                    <Shield v-if="user.role==='admin' || user.role==='common_admin'" class="w-3 h-3" />
                                    {{ getRoleConfig(user.role).label }}
                                </span>
                            </td>
                            <td class="p-4 text-center">
                                <span class="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-orange-50 text-orange-600 text-xs font-medium">
                                    <FileText class="w-3 h-3" />
                                    {{ user.recipeCount || 0 }}
                                </span>
                            </td>
                            <td class="p-4">
                                <span :class="[
                                    'px-2 py-1 text-xs font-medium rounded',
                                    getStatusConfig(user.status).bg,
                                    getStatusConfig(user.status).text
                                ]">
                                    {{ getStatusConfig(user.status).label }}
                                </span>
                            </td>
                            <td class="p-4 text-center">
                                <span :class="[
                                    'inline-flex items-center gap-1 px-2 py-0.5 text-xs font-medium rounded-full',
                                    onlineStatus[user.id] ? 'bg-green-50 text-green-600' : 'bg-gray-100 text-gray-400'
                                ]">
                                    <Circle :class="['w-2 h-2', onlineStatus[user.id] ? 'fill-green-500 text-green-500' : 'fill-gray-300 text-gray-300']" />
                                    {{ onlineStatus[user.id] ? '在线' : '离线' }}
                                </span>
                            </td>
                            <td class="p-4">
                                <div class="text-gray-600">{{ formatDate(user.createTime).split(' ')[0] }}</div>
                            </td>
                            <td class="p-4 text-right pr-6">
                                <div class="flex items-center justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                    <button 
                                        @click="openEditModal(user)"
                                        class="p-1.5 text-blue-500 bg-blue-50 hover:bg-blue-100 rounded-lg transition"
                                        title="编辑用户信息"
                                    >
                                        <Edit class="w-4 h-4" />
                                    </button>
                                    <button 
                                        @click="toggleUserStatus(user)"
                                        :class="[
                                            'p-1.5 rounded-lg transition',
                                            user.status === 1 ? 'text-red-500 bg-red-50 hover:bg-red-100' : 'text-emerald-500 bg-emerald-50 hover:bg-emerald-100'
                                        ]"
                                        :title="user.status === 1 ? '封禁该用户' : '解除用户封禁'"
                                    >
                                        <component :is="user.status === 1 ? Ban : CheckCircle" class="w-4 h-4" />
                                    </button>
                                    <button 
                                        @click="handleKickUser(user)"
                                        :disabled="!onlineStatus[user.id]"
                                        :class="[
                                            'p-1.5 rounded-lg transition',
                                            onlineStatus[user.id] 
                                                ? 'text-orange-500 bg-orange-50 hover:bg-orange-100 cursor-pointer' 
                                                : 'text-gray-300 bg-gray-50 cursor-not-allowed'
                                        ]"
                                        :title="onlineStatus[user.id] ? '强制踢下线' : '用户已离线'"
                                    >
                                        <LogOut class="w-4 h-4" />
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

        <UserModal 
            v-model="showModal" 
            :edit-data="editingUser"
            @success="fetchUsers"
        />
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
