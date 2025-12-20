<script setup>
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
    LayoutDashboard,
    Users,
    ChefHat,
    FolderOpen,
    MessageSquare,
    ClipboardList,
    LogOut,
    Menu,
    X
} from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const sidebarCollapsed = ref(false)

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
        path: '/backstage-m9x2k7/logs', 
        name: '操作日志', 
        icon: ClipboardList 
    }
]

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

const currentPageName = () => {
    const item = menuItems.find(item => isActive(item))
    return item?.name || '后台管理'
}
</script>

<template>
    <div class="min-h-screen bg-gray-900 flex">
        <!-- Sidebar -->
        <aside 
            :class="[
                'bg-gray-800 text-white transition-all duration-300 flex flex-col fixed h-full z-10',
                sidebarCollapsed ? 'w-16' : 'w-64'
            ]"
        >
            <!-- Logo区域 -->
            <div class="h-16 flex items-center justify-between px-4 border-b border-gray-700">
                <div v-if="!sidebarCollapsed" class="flex items-center gap-2">
                    <ChefHat class="w-8 h-8 text-orange-500" />
                    <span class="font-bold text-lg">管理后台</span>
                </div>
                <button 
                    @click="toggleSidebar"
                    class="p-2 hover:bg-gray-700 rounded-lg transition"
                >
                    <Menu v-if="sidebarCollapsed" class="w-5 h-5" />
                    <X v-else class="w-5 h-5" />
                </button>
            </div>

            <!-- 导航菜单 -->
            <nav class="flex-1 py-4 overflow-y-auto">
                <ul class="space-y-1 px-2">
                    <li v-for="item in menuItems" :key="item.path">
                        <button
                            @click="navigateTo(item.path)"
                            :class="[
                                'w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all',
                                isActive(item) 
                                    ? 'bg-orange-500 text-white' 
                                    : 'text-gray-300 hover:bg-gray-700 hover:text-white'
                            ]"
                        >
                            <component :is="item.icon" class="w-5 h-5 flex-shrink-0" />
                            <span v-if="!sidebarCollapsed" class="truncate">{{ item.name }}</span>
                        </button>
                    </li>
                </ul>
            </nav>

            <!-- 用户信息 -->
            <div class="border-t border-gray-700 p-4">
                <div :class="['flex items-center gap-3', sidebarCollapsed ? 'justify-center' : '']">
                    <img 
                        :src="userStore.user?.avatar || 'https://api.dicebear.com/7.x/avataaars/svg?seed=admin'" 
                        class="w-10 h-10 rounded-full bg-gray-600"
                    />
                    <div v-if="!sidebarCollapsed" class="flex-1 min-w-0">
                        <p class="text-sm font-medium truncate">{{ userStore.user?.nickname || '管理员' }}</p>
                        <p class="text-xs text-gray-400">管理员</p>
                    </div>
                    <button 
                        v-if="!sidebarCollapsed"
                        @click="handleLogout"
                        class="p-2 hover:bg-gray-700 rounded-lg transition text-gray-400 hover:text-white"
                        title="退出登录"
                    >
                        <LogOut class="w-5 h-5" />
                    </button>
                </div>
            </div>
        </aside>

        <!-- Main Content -->
        <main :class="['flex-1 flex flex-col h-screen transition-all duration-300', sidebarCollapsed ? 'ml-16' : 'ml-64']">
            <!-- Page Header (固定) -->
            <div class="bg-gray-800 border-b border-gray-700 px-6 py-4 flex-shrink-0">
                <h1 class="text-xl font-semibold text-white">{{ currentPageName() }}</h1>
            </div>

            <!-- Page Content (可滚动) -->
            <div class="flex-1 overflow-y-auto bg-gray-100 p-6">
                <router-view />
            </div>
        </main>
    </div>
</template>

