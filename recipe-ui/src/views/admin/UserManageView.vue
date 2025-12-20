<script setup>
import { ref, onMounted, watch } from 'vue'
import { listUsers, updateUserStatus } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { Search, Ban, CheckCircle, ChevronLeft, ChevronRight } from 'lucide-vue-next'

const { showToast } = useToast()
const { confirm } = useModal()

const loading = ref(false)
const users = ref([])
const keyword = ref('')
const pagination = ref({
    current: 1,
    size: 10,
    total: 0
})

const fetchUsers = async () => {
    loading.value = true
    try {
        const res = await listUsers({
            page: pagination.value.current,
            size: pagination.value.size,
            keyword: keyword.value || undefined
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

const toggleUserStatus = async (user) => {
    // 0=正常, 1=封禁。切换状态
    const newStatus = user.status === 0 ? 1 : 0
    const action = newStatus === 1 ? '封禁' : '解封'
    
    const confirmed = await confirm(`确定要${action}用户 "${user.nickname}" 吗？`, { danger: newStatus === 1 })
    if (!confirmed) return

    try {
        await updateUserStatus(user.id, { status: newStatus })
        user.status = newStatus
        showToast(`${action}成功`)
    } catch (error) {
        console.error(error)
        showToast('操作失败')
    }
}

const handleSearch = () => {
    pagination.value.current = 1
    fetchUsers()
}

const handlePageChange = (page) => {
    pagination.value.current = page
    fetchUsers()
}

const totalPages = () => Math.ceil(pagination.value.total / pagination.value.size)

onMounted(() => {
    fetchUsers()
})

const formatDate = (dateStr) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleDateString('zh-CN')
}
</script>

<template>
    <div class="flex flex-col h-full">
        <!-- 搜索栏 (固定) -->
        <div class="bg-white rounded-xl shadow-sm p-4 flex-shrink-0">
            <div class="flex gap-4">
                <div class="flex-1 relative">
                    <Search class="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                        v-model="keyword"
                        @keyup.enter="handleSearch"
                        type="text"
                        placeholder="搜索用户名或昵称..."
                        class="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none"
                    />
                </div>
                <button 
                    @click="handleSearch"
                    class="px-6 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition font-medium"
                >
                    搜索
                </button>
            </div>
        </div>

        <!-- 用户列表 (可滚动) -->
        <div class="flex-1 mt-4 bg-white rounded-xl shadow-sm overflow-hidden flex flex-col min-h-0">
            <div class="flex-1 overflow-auto">
                <table class="w-full">
                    <thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">账号</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">角色</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">注册时间</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                        <tr v-if="loading" class="text-center">
                            <td colspan="6" class="px-6 py-12 text-gray-400">加载中...</td>
                        </tr>
                        <tr v-else-if="users.length === 0" class="text-center">
                            <td colspan="6" class="px-6 py-12 text-gray-400">暂无数据</td>
                        </tr>
                        <tr v-else v-for="user in users" :key="user.id" class="hover:bg-gray-50">
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center gap-3">
                                    <img 
                                        :src="user.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=' + user.id" 
                                        class="w-10 h-10 rounded-full bg-gray-200"
                                    />
                                    <div>
                                        <div class="font-medium text-gray-900">{{ user.nickname }}</div>
                                        <div class="text-sm text-gray-500">ID: {{ user.id }}</div>
                                    </div>
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-600">{{ user.username }}</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span :class="[
                                    'px-2 py-1 text-xs rounded-full',
                                    user.role === 'admin' ? 'bg-purple-100 text-purple-700' : 'bg-gray-100 text-gray-600'
                                ]">
                                    {{ user.role === 'admin' ? '管理员' : '普通用户' }}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-600">{{ formatDate(user.createTime) }}</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span :class="[
                                    'px-2 py-1 text-xs rounded-full',
                                    user.status === 0 ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                                ]">
                                    {{ user.status === 0 ? '正常' : '已封禁' }}
                                </span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <button 
                                    v-if="user.role !== 'admin'"
                                    @click="toggleUserStatus(user)"
                                    :class="[
                                        'flex items-center gap-1 px-3 py-1.5 text-sm rounded-lg transition',
                                        user.status === 0 
                                            ? 'text-red-600 hover:bg-red-50' 
                                            : 'text-green-600 hover:bg-green-50'
                                    ]"
                                >
                                    <Ban v-if="user.status === 0" class="w-4 h-4" />
                                    <CheckCircle v-else class="w-4 h-4" />
                                    {{ user.status === 0 ? '封禁' : '解封' }}
                                </button>
                                <span v-else class="text-gray-400 text-sm">-</span>
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
