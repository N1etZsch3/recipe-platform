<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { listAuditRecipes } from '@/api/admin'
import {
    LayoutDashboard,
    Users,
    ChefHat,
    FolderOpen,
    MessageSquare,
    ClipboardList,
    LogOut,
    Menu,
    ChevronRight,
    ChevronDown,
    Lock,
    X
} from 'lucide-vue-next'
import NotificationCenter from '@/components/NotificationCenter.vue'
import { changePassword } from '@/api/auth'
import { useToast } from '@/components/Toast.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const sidebarCollapsed = ref(false)
const showUserMenu = ref(false)
const showNotificationPanel = ref(false)
const todoList = ref([])
const todoCount = ref(0)

const menuItems = [
    { 
        path: '/backstage-m9x2k7', 
        name: '仪表盘', 
        icon: LayoutDashboard,
        exact: true 
    },
    { 
        path: '/backstage-m9x2k7/users', 
        name: '用户管理', 
        icon: Users 
    },
    { 
        path: '/backstage-m9x2k7/recipes', 
        name: '菜谱管理', 
        icon: ChefHat 
    },
    { 
        path: '/backstage-m9x2k7/categories', 
        name: '分类管理', 
        icon: FolderOpen 
    },
    { 
        path: '/backstage-m9x2k7/comments', 
        name: '评论管理', 
        icon: MessageSquare 
    },
    { 
        path: '/backstage-m9x2k7/team', 
        name: '团队管理', 
        icon: Users 
    },
    { 
        path: '/backstage-m9x2k7/logs', 
        name: '操作日志', 
        icon: ClipboardList 
    }
]

// Fetch Pending Todos
const fetchTodos = async () => {
    try {
        const res = await listAuditRecipes({ page: 1, size: 5, status: 0 })
        if (res && res.records) {
            todoList.value = res.records
            todoCount.value = parseInt(res.total || res.records.length)
        }
    } catch (error) {
        console.error('Failed to fetch todo list', error)
    }
}

watch(showNotificationPanel, (val) => {
    if (val) {
        fetchTodos()
    }
})

const handleTodoClick = (item) => {
    showNotificationPanel.value = false
    // Go to recipe management with status=0 (pending)
    router.push('/backstage-m9x2k7/recipes?status=0')
}

const handleViewAll = (tab) => {
    showNotificationPanel.value = false
    if (tab === 'todo') {
        router.push('/backstage-m9x2k7/recipes?status=0')
    } else if (tab === 'message') {
        router.push('/messages')
    } else {
        router.push('/messages')
    }
}

// Notifications
const notifications = computed(() => {
    return notificationStore.notifications.slice(0, 5)
})

const unreadCount = computed(() => notificationStore.unreadCount)

const formatNotificationTime = (time) => {
    if (!time) return ''
    const date = new Date(time)
    const now = new Date()
    const diff = now - date
    
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
    return `${date.getMonth() + 1}-${date.getDate()} ${date.getHours()}:${String(date.getMinutes()).padStart(2, '0')}`
}

const isActive = (item) => {
    if (item.exact) {
        return route.path === item.path
    }
    return route.path.startsWith(item.path)
}

const navigateTo = (path) => {
    router.push(path)
}

const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
}

const handleLogout = () => {
    userStore.logout()
    router.push('/backstage-m9x2k7/login')
}

const markAllAsRead = () => {
    notificationStore.markAllAsRead()
}

// 修改密码相关
const { showToast } = useToast()
const showPasswordModal = ref(false)
const changingPassword = ref(false)
const passwordForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const openPasswordModal = () => {
    showUserMenu.value = false
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    showPasswordModal.value = true
}

const handlePasswordChange = async () => {
    if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
        showToast('请填写完整信息', 'error')
        return
    }
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        showToast('两次输入的密码不一致', 'error')
        return
    }
    if (passwordForm.value.newPassword.length < 6) {
        showToast('新密码长度不能少于6位', 'error')
        return
    }
    
    changingPassword.value = true
    try {
        await changePassword({
            oldPassword: passwordForm.value.oldPassword,
            newPassword: passwordForm.value.newPassword
        })
        showToast('密码修改成功，请重新登录', 'success')
        showPasswordModal.value = false
        handleLogout()
    } catch (error) {
        console.error(error)
        showToast(error.message || '密码修改失败', 'error')
    } finally {
        changingPassword.value = false
    }
}

const currentPageName = computed(() => {
    const item = menuItems.find(item => isActive(item))
    return item?.name || '后台管理'
})

const breadcrumbs = computed(() => {
    const crumbs = [{ name: '系统管理', path: '/backstage-m9x2k7' }]
    const current = menuItems.find(item => isActive(item))
    if (current) {
        crumbs.push({ name: current.name, path: current.path })
    }
    return crumbs
})
</script>

<template>
    <div class="h-screen bg-gray-50 flex font-sans antialiased overflow-hidden">
        <!-- Sidebar -->
        <aside 
            :class="[
                'bg-white border-r border-gray-100 transition-all duration-300 flex flex-col fixed h-full z-30',
                sidebarCollapsed ? 'w-16' : 'w-52'
            ]"
        >
            <!-- Logo区域 -->
            <div class="h-14 flex items-center justify-center px-3 border-b border-gray-100">
                <div v-if="!sidebarCollapsed" class="flex items-center gap-2">
                    <div class="w-8 h-8 bg-gradient-to-br from-orange-500 to-amber-500 rounded-lg flex items-center justify-center shadow-sm">
                        <ChefHat class="w-4 h-4 text-white" />
                    </div>
                    <span class="font-semibold text-gray-800 text-sm tracking-tight">三食六记</span>
                </div>
                <div v-else class="w-8 h-8 bg-gradient-to-br from-orange-500 to-amber-500 rounded-lg flex items-center justify-center shadow-sm">
                    <ChefHat class="w-4 h-4 text-white" />
                </div>
            </div>

            <!-- 导航菜单 -->
            <nav class="flex-1 py-3 overflow-y-auto">
                <ul class="space-y-0.5 px-2">
                    <li v-for="item in menuItems" :key="item.path">
                        <button
                            @click="navigateTo(item.path)"
                            :class="[
                                'w-full flex items-center gap-2.5 px-3 py-2 rounded-lg transition-all group text-sm',
                                isActive(item) 
                                    ? 'text-orange-600 font-medium' 
                                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-800'
                            ]"
                        >
                            <component 
                                :is="item.icon" 
                                :class="[
                                    'w-[18px] h-[18px] flex-shrink-0',
                                    isActive(item) ? 'text-orange-500' : 'text-gray-400 group-hover:text-gray-600'
                                ]" 
                            />
                            <span v-if="!sidebarCollapsed" class="truncate">{{ item.name }}</span>
                            <!-- 激活指示条 -->
                            <span 
                                v-if="isActive(item) && !sidebarCollapsed"
                                class="ml-auto w-1 h-4 bg-orange-500 rounded-full"
                            ></span>
                        </button>
                    </li>
                </ul>
            </nav>
        </aside>

        <!-- Main Content -->
        <main :class="['flex-1 flex flex-col h-full transition-all duration-300', sidebarCollapsed ? 'ml-16' : 'ml-52']">
            <!-- 顶部导航栏 -->
            <header class="h-14 bg-white border-b border-gray-100 flex items-center justify-between pl-4 pr-32 sticky top-0 z-50">
                <!-- 左侧：折叠按钮 + 面包屑 -->
                <div class="flex items-center gap-3">
                    <!-- 折叠按钮 -->
                    <button 
                        @click="toggleSidebar"
                        class="p-1.5 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition"
                    >
                        <Menu class="w-5 h-5" />
                    </button>

                    <!-- 分隔线 -->
                    <div class="h-4 w-px bg-gray-200"></div>

                    <!-- 面包屑 -->
                    <div class="flex items-center gap-1.5 text-sm">
                        <template v-for="(crumb, index) in breadcrumbs" :key="crumb.path">
                            <button 
                                @click="navigateTo(crumb.path)"
                                :class="[
                                    'hover:text-orange-500 transition',
                                    index === breadcrumbs.length - 1 ? 'text-gray-700 font-medium' : 'text-gray-400'
                                ]"
                            >
                                {{ crumb.name }}
                            </button>
                            <ChevronRight v-if="index < breadcrumbs.length - 1" class="w-3.5 h-3.5 text-gray-300" />
                        </template>
                    </div>
                </div>

                <!-- 右侧工具栏 -->
                <div class="flex items-center gap-2">
                    <!-- 通知按钮 -->
                    <div class="relative">
                        <button 
                            @click="showNotificationPanel = !showNotificationPanel"
                            class="relative p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition"
                        >
                            <Bell class="w-5 h-5" />
                            <span 
                                v-if="unreadCount > 0"
                                class="absolute top-1 right-1 min-w-[16px] h-4 bg-red-500 text-white text-xs rounded-full flex items-center justify-center px-1 font-medium"
                            >
                                {{ unreadCount > 99 ? '99+' : unreadCount }}
                            </span>
                        </button>

                        <!-- 通知面板 -->
                        <transition
                            enter-active-class="transition ease-out duration-150"
                            enter-from-class="transform opacity-0 scale-95 -translate-y-2"
                            enter-to-class="transform opacity-100 scale-100 translate-y-0"
                            leave-active-class="transition ease-in duration-100"
                            leave-from-class="transform opacity-100 scale-100"
                            leave-to-class="transform opacity-0 scale-95"
                        >
                            <div v-if="showNotificationPanel" class="absolute left-1/2 -translate-x-1/2 mt-2 z-50">
                                <NotificationCenter 
                                    :show-todo="true"
                                    :todo-count="todoCount"
                                    @close="showNotificationPanel = false"
                                    @mark-read="markAllAsRead"
                                    @view-all="handleViewAll"
                                >
                                    <template #todo>
                                        <div v-if="todoList.length === 0" class="py-12 text-center text-gray-400">
                                            <!-- NotificationCenter default slot logic will help but we can rely on TodoCount if it is right -->
                                        </div>
                                        <div v-else class="divide-y divide-gray-50">
                                            <div 
                                                v-for="item in todoList" 
                                                :key="item.id"
                                                @click="handleTodoClick(item)"
                                                class="flex items-start gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer transition"
                                            >
                                                <img 
                                                    :src="item.coverImage || 'https://via.placeholder.com/40'" 
                                                    class="w-10 h-10 rounded-lg object-cover flex-shrink-0 bg-gray-100"
                                                />
                                                <div class="flex-1 min-w-0">
                                                    <p class="text-sm font-medium text-gray-800 line-clamp-1">
                                                        {{ item.title }}
                                                    </p>
                                                    <p class="text-xs text-gray-500 mt-0.5">
                                                        {{ item.authorName }} · {{ formatNotificationTime(item.createTime || item.updateTime) }} · 申请发布
                                                    </p>
                                                </div>
                                                <div class="flex-shrink-0 text-xs text-orange-500 bg-orange-50 px-2 py-0.5 rounded-full">
                                                    待审核
                                                </div>
                                            </div>
                                        </div>
                                    </template>
                                </NotificationCenter>
                            </div>
                        </transition>
                    </div>

                    <!-- 分割线 -->
                    <div class="h-5 w-px bg-gray-200"></div>

                    <!-- 用户头像 -->
                    <div class="relative">
                        <button 
                            @click="showUserMenu = !showUserMenu"
                            class="flex items-center gap-2 p-1 hover:bg-gray-100 rounded-lg transition"
                        >
                            <img 
                                :src="userStore.user?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin'" 
                                class="w-8 h-8 rounded-lg bg-gray-100 object-cover"
                            />
                            <ChevronDown class="w-4 h-4 text-gray-400" />
                        </button>

                        <!-- 下拉菜单 -->
                        <transition
                            enter-active-class="transition ease-out duration-100"
                            enter-from-class="transform opacity-0 scale-95"
                            enter-to-class="transform opacity-100 scale-100"
                            leave-active-class="transition ease-in duration-75"
                            leave-from-class="transform opacity-100 scale-100"
                            leave-to-class="transform opacity-0 scale-95"
                        >
                            <div 
                                v-if="showUserMenu"
                                class="absolute right-0 mt-2 w-44 bg-white rounded-xl shadow-lg border border-gray-100 py-1.5 z-50"
                            >
                                <div class="px-3 py-2 border-b border-gray-100">
                                    <p class="text-sm font-medium text-gray-700">{{ userStore.user?.nickname || '管理员' }}</p>
                                    <p class="text-xs text-gray-400">超级管理员</p>
                                </div>
                                <button 
                                    @click="openPasswordModal"
                                    class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-600 hover:bg-gray-50 hover:text-orange-500 transition"
                                >
                                    <Lock class="w-4 h-4" />
                                    修改密码
                                </button>
                                <button 
                                    @click="handleLogout"
                                    class="w-full flex items-center gap-2 px-3 py-2 text-sm text-gray-600 hover:bg-gray-50 hover:text-red-500 transition"
                                >
                                    <LogOut class="w-4 h-4" />
                                    退出登录
                                </button>
                            </div>
                        </transition>
                    </div>
                </div>
            </header>

            <!-- Page Content -->
            <div class="flex-1 flex flex-col min-h-0 overflow-hidden bg-gray-50/50">
                <router-view />
            </div>
        </main>

        <!-- 点击外部关闭菜单 -->
        <div 
            v-if="showUserMenu || showNotificationPanel" 
            @click="showUserMenu = false; showNotificationPanel = false"
            class="fixed inset-0 z-40"
        ></div>

        <!-- 修改密码弹窗 -->
        <div v-if="showPasswordModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div class="bg-white rounded-2xl w-full max-w-md p-6 shadow-2xl animate-fade-in relative">
                <button @click="showPasswordModal = false" class="absolute right-4 top-4 p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition">
                    <X class="w-5 h-5" />
                </button>
                
                <h3 class="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
                    <div class="p-2 bg-orange-100 rounded-lg">
                        <Lock class="w-5 h-5 text-orange-500" />
                    </div>
                    修改密码
                </h3>
                
                <div class="space-y-4">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1.5">当前密码</label>
                        <input 
                            v-model="passwordForm.oldPassword" 
                            type="password" 
                            placeholder="请输入当前使用的密码"
                            class="w-full border border-gray-200 rounded-xl px-4 py-3 bg-gray-50 focus:bg-white focus:ring-4 focus:ring-orange-100 focus:border-orange-500 transition outline-none"
                        >
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1.5">新密码</label>
                        <input 
                            v-model="passwordForm.newPassword" 
                            type="password" 
                            placeholder="请输入新密码 (至少6位)"
                            class="w-full border border-gray-200 rounded-xl px-4 py-3 bg-gray-50 focus:bg-white focus:ring-4 focus:ring-orange-100 focus:border-orange-500 transition outline-none"
                        >
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1.5">确认新密码</label>
                        <input 
                            v-model="passwordForm.confirmPassword" 
                            type="password" 
                            placeholder="请再次输入新密码"
                            class="w-full border border-gray-200 rounded-xl px-4 py-3 bg-gray-50 focus:bg-white focus:ring-4 focus:ring-orange-100 focus:border-orange-500 transition outline-none"
                        >
                    </div>
                </div>
                
                <div class="flex gap-3 mt-8">
                    <button 
                        @click="showPasswordModal = false"
                        class="flex-1 py-3 text-gray-600 bg-gray-100 hover:bg-gray-200 font-bold rounded-xl transition"
                    >取消</button>
                    <button 
                        @click="handlePasswordChange"
                        :disabled="changingPassword"
                        class="flex-1 py-3 text-white bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 font-bold rounded-xl shadow-lg shadow-orange-200/50 transition flex items-center justify-center gap-2 disabled:opacity-70 disabled:grayscale"
                    >
                        <div v-if="changingPassword" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                        {{ changingPassword ? '修改中...' : '确认修改' }}
                    </button>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
/* 自定义滚动条 */
nav::-webkit-scrollbar {
    width: 3px;
}

nav::-webkit-scrollbar-track {
    background: transparent;
}

nav::-webkit-scrollbar-thumb {
    background: #e5e7eb;
    border-radius: 2px;
}

nav::-webkit-scrollbar-thumb:hover {
    background: #d1d5db;
}

/* 更清晰的字体渲染 */
.font-sans {
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    -webkit-font-smoothing: antialiased;
    -moz-osx-font-smoothing: grayscale;
}
</style>
