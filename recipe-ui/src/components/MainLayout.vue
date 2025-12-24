<script setup>
import { ref, computed, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import Sidebar from './Sidebar.vue'
import NotificationCenter from './NotificationCenter.vue'
import { Search, Bell, UserPlus } from 'lucide-vue-next'

const emit = defineEmits(['search'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()

const searchQuery = ref('')
const showNotifications = ref(false)
const mainScrollRef = ref(null)

provide('mainScrollRef', mainScrollRef)

// 判断当前页面是否需要显示搜索栏（只有首页需要）
const showSearch = computed(() => {
  return route.path === '/home' || route.name === 'home'
})

// 获取当前页面标题
const pageTitle = computed(() => {
  const titles = {
    '/messages': '消息中心',
    '/profile': '个人中心',
    '/create': '发布菜谱'
  }
  // 菜谱详情页
  if (route.path.startsWith('/recipe/')) return '菜谱详情'
  // 用户主页
  if (route.path.startsWith('/user/')) return '用户主页'
  return titles[route.path] || ''
})

// 未读通知数
const unreadCount = computed(() => notificationStore.unreadCount)

// 搜索处理 - 跳转到首页并带上搜索关键词
const handleSearch = () => {
  if (searchQuery.value.trim()) {
    router.push({ path: '/home', query: { keyword: searchQuery.value.trim() } })
  } else {
    router.push('/home')
  }
}

// 切换通知面板
const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value
}

// 关闭通知面板
const closeNotifications = () => {
  showNotifications.value = false
}

// 添加好友（跳转到消息页面或打开对话框）
const handleAddFriend = () => {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  // 暂时跳转到消息页面
  router.push('/messages')
}

// 点击外部关闭通知
const handleClickOutside = (e) => {
  const panel = document.getElementById('main-notification-panel')
  const btn = document.getElementById('main-notification-btn')
  if (panel && !panel.contains(e.target) && btn && !btn.contains(e.target)) {
    showNotifications.value = false
  }
}
</script>

<template>
  <div class="flex min-h-screen bg-[#FFFBF5]">
    <!-- 左侧侧边栏 -->
    <Sidebar />

    <!-- 右侧主内容区 -->
    <div class="flex-1 flex flex-col min-w-0">
      <!-- 顶部功能栏 -->
      <header class="h-16 bg-white/80 backdrop-blur-md border-b border-gray-100 flex items-center px-6 sticky top-0 z-40">
        <!-- 左侧占位 -->
        <div class="w-32 flex-shrink-0"></div>

        <!-- 中间内容区（支持 Teleport） -->
        <div id="header-center-content" class="flex-1 flex justify-center items-center">
          <!-- 首页显示搜索栏 -->
          <div v-if="showSearch" class="relative w-full max-w-xl">
            <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
              <Search class="w-4 h-4 text-gray-400" />
            </div>
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索菜谱..."
              class="w-full pl-11 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-orange-200 focus:border-orange-400 transition"
              @keyup.enter="handleSearch"
            >
          </div>
          <!-- 其他页面显示标题（如果没有 teleport 内容） -->
          <h1 v-else-if="pageTitle" class="text-lg font-semibold text-gray-800 header-default-title">{{ pageTitle }}</h1>
        </div>

        <!-- 右侧图标区 -->
        <div class="w-32 flex-shrink-0 flex items-center justify-end gap-2">
          <!-- 添加好友 -->
          <button
            v-if="userStore.user"
            @click="handleAddFriend"
            class="p-2.5 text-gray-500 hover:text-orange-500 hover:bg-orange-50 rounded-full transition"
            title="添加好友"
          >
            <UserPlus class="w-5 h-5" />
          </button>

          <!-- 通知铃铛 -->
          <div v-if="userStore.user" class="relative">
            <button
              id="main-notification-btn"
              @click="toggleNotifications"
              class="p-2.5 text-gray-500 hover:text-orange-500 hover:bg-orange-50 rounded-full transition relative"
              title="通知"
            >
              <Bell class="w-5 h-5" />
              <span
                v-if="unreadCount > 0"
                class="absolute top-1 right-1 w-4 h-4 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center font-bold"
              >
                {{ unreadCount > 9 ? '9+' : unreadCount }}
              </span>
            </button>

            <!-- 通知下拉面板 -->
            <Transition
              enter-active-class="transition duration-200 ease-out"
              enter-from-class="opacity-0 scale-95 -translate-y-2"
              enter-to-class="opacity-100 scale-100 translate-y-0"
              leave-active-class="transition duration-150 ease-in"
              leave-from-class="opacity-100 scale-100 translate-y-0"
              leave-to-class="opacity-0 scale-95 -translate-y-2"
            >
              <div
                v-if="showNotifications"
                id="main-notification-panel"
                class="absolute right-0 top-full mt-2 z-50"
              >
                <NotificationCenter
                  :show-todo="false"
                  @close="closeNotifications"
                  @view-all="router.push('/messages'); closeNotifications()"
                />
              </div>
            </Transition>
          </div>

          <!-- 未登录状态显示登录按钮 -->
          <button
            v-if="!userStore.user"
            @click="router.push('/login')"
            class="px-4 py-2 bg-gradient-to-r from-orange-500 to-red-500 text-white text-sm font-medium rounded-full hover:shadow-md transition"
          >
            登录
          </button>
        </div>
      </header>

      <!-- 内容区域 -->
      <main ref="mainScrollRef" class="flex-1 overflow-auto">
        <slot></slot>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 当 header-center-content 中有 Teleport 内容时，隐藏默认标题 */
#header-center-content:has(> :not(.header-default-title):not(div[class*="max-w-xl"])) .header-default-title {
  display: none;
}
</style>
