<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
  ChefHat, 
  Compass, 
  PenSquare, 
  MessageSquare, 
  MoreHorizontal,
  Info,
  Mail,
  LogOut,
  X
} from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { showToast } = useToast()
const { confirm } = useModal()

// 更多菜单状态
const showMoreMenu = ref(false)

// 导航项配置
const navItems = [
  { id: 'home', name: '精选菜谱', icon: Compass, path: '/home' },
  { id: 'create', name: '发布菜谱', icon: PenSquare, path: '/create', requiresAuth: true },
  { id: 'messages', name: '消息中心', icon: MessageSquare, path: '/messages', requiresAuth: true },
]

// 判断当前路由是否激活
const isActive = (path) => {
  if (path === '/home') return route.path === '/home' || route.path.startsWith('/recipe/')
  return route.path.startsWith(path)
}

// 处理导航点击
const handleNavClick = (item) => {
  if (item.requiresAuth && !userStore.user) {
    router.push('/login')
    return
  }
  router.push(item.path)
}

// 处理个人中心点击
const handleProfileClick = () => {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  router.push('/profile')
}

// 更多菜单项
const handleAbout = () => {
  showMoreMenu.value = false
  showToast('三食六记 - 记录每一餐的美好', 'info')
}

const handleContact = () => {
  showMoreMenu.value = false
  showToast('请发送邮件至：admin@sanshiliuji.com', 'info')
}

const handleLogout = async () => {
  showMoreMenu.value = false
  if (await confirm('确定要退出登录吗？')) {
    userStore.logout()
    router.push('/login')
  }
}

// 点击外部关闭更多菜单
const closeMoreMenu = () => {
  showMoreMenu.value = false
}

// 获取头像
const getAvatarUrl = computed(() => {
  if (userStore.user?.avatar) return userStore.user.avatar
  const name = userStore.user?.nickname || userStore.user?.username || 'U'
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(name)}`
})
</script>

<template>
  <aside class="w-[200px] h-screen bg-white border-r border-gray-100 flex flex-col flex-shrink-0 sticky top-0">
    <!-- Logo + 项目名称 -->
    <div 
      class="p-5 flex items-center gap-3 cursor-pointer group"
      @click="router.push('/')"
    >
      <div class="relative">
        <div class="absolute inset-0 bg-gradient-to-br from-orange-400 to-red-500 rounded-xl blur opacity-40 group-hover:opacity-60 transition"></div>
        <div class="relative bg-gradient-to-br from-orange-500 to-red-500 p-2.5 rounded-xl">
          <ChefHat class="w-6 h-6 text-white" />
        </div>
      </div>
      <span class="font-bold text-lg bg-gradient-to-r from-orange-500 via-red-500 to-pink-500 bg-clip-text text-transparent">
        三食六记
      </span>
    </div>

    <!-- 导航菜单 -->
    <nav class="flex-1 px-3 py-4 space-y-1">
      <button
        v-for="item in navItems"
        :key="item.id"
        @click="handleNavClick(item)"
        :class="[
          'w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all',
          isActive(item.path)
            ? 'bg-orange-50 text-orange-600'
            : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
        ]"
      >
        <component :is="item.icon" class="w-5 h-5" />
        {{ item.name }}
      </button>

      <!-- 个人中心/登录入口 -->
      <button
        @click="handleProfileClick"
        :class="[
          'w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all mt-2',
          isActive('/profile')
            ? 'bg-orange-50 text-orange-600'
            : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
        ]"
      >
        <template v-if="userStore.user">
          <img 
            :src="getAvatarUrl" 
            class="w-6 h-6 rounded-full object-cover ring-2 ring-gray-100"
          >
          <span class="truncate">{{ userStore.user.nickname || userStore.user.username }}</span>
        </template>
        <template v-else>
          <div class="w-6 h-6 rounded-full bg-gray-200 flex items-center justify-center">
            <span class="text-xs text-gray-400">?</span>
          </div>
          <span>登录 / 注册</span>
        </template>
      </button>
    </nav>

    <!-- 底部更多按钮 -->
    <div class="p-3 border-t border-gray-100 relative">
      <button
        @click="showMoreMenu = !showMoreMenu"
        :class="[
          'w-full flex items-center gap-3 px-4 py-3 rounded-xl text-sm font-medium transition-all',
          showMoreMenu
            ? 'bg-gray-100 text-gray-800'
            : 'text-gray-500 hover:bg-gray-50 hover:text-gray-700'
        ]"
      >
        <MoreHorizontal class="w-5 h-5" />
        更多
      </button>

      <!-- 更多菜单弹窗 -->
      <Transition
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="opacity-0 scale-95 translate-y-2"
        enter-to-class="opacity-100 scale-100 translate-y-0"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100 scale-100 translate-y-0"
        leave-to-class="opacity-0 scale-95 translate-y-2"
      >
        <div 
          v-if="showMoreMenu"
          class="absolute bottom-full left-3 right-3 mb-2 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden z-50"
        >
          <!-- 关于三食六记 -->
          <button
            @click="handleAbout"
            class="w-full flex items-center gap-3 px-4 py-3 text-sm text-gray-600 hover:bg-gray-50 transition"
          >
            <Info class="w-4 h-4" />
            关于三食六记
          </button>
          
          <!-- 联系管理员 -->
          <button
            @click="handleContact"
            class="w-full flex items-center gap-3 px-4 py-3 text-sm text-gray-600 hover:bg-gray-50 transition border-t border-gray-50"
          >
            <Mail class="w-4 h-4" />
            联系管理员
          </button>
          
          <!-- 退出登录（仅登录用户显示） -->
          <button
            v-if="userStore.user"
            @click="handleLogout"
            class="w-full flex items-center gap-3 px-4 py-3 text-sm text-red-500 hover:bg-red-50 transition border-t border-gray-100"
          >
            <LogOut class="w-4 h-4" />
            退出登录
          </button>
        </div>
      </Transition>

      <!-- 点击外部关闭 -->
      <div 
        v-if="showMoreMenu" 
        @click="closeMoreMenu" 
        class="fixed inset-0 z-40"
      ></div>
    </div>
  </aside>
</template>
