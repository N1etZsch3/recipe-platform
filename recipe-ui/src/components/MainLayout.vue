<script setup>
import { ref, computed, provide, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import Sidebar from './Sidebar.vue'
import NotificationCenter from './NotificationCenter.vue'
import { Search, Bell, UserPlus, UserMinus, MessageCircle, Loader2 } from 'lucide-vue-next'
import { searchUsers, followUser, unfollowUser } from '@/api/social'
import UserAvatar from '@/components/UserAvatar.vue'
import { useToast } from '@/components/Toast.vue'

const emit = defineEmits(['search'])

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { showToast } = useToast()

const searchQuery = ref('')
const showNotifications = ref(false)
const mainScrollRef = ref(null)

// User Search State
const showUserSearch = ref(false)
const userSearchKeyword = ref('')
const userSearchResults = ref([])
const loadingUserSearch = ref(false)
let userSearchTimeout = null

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
  if (showNotifications.value) showUserSearch.value = false
}

// 关闭通知面板
const closeNotifications = () => {
  showNotifications.value = false
}

// User Search Logic
const toggleUserSearch = () => {
  showUserSearch.value = !showUserSearch.value
  if (showUserSearch.value) {
    showNotifications.value = false
    if (!userSearchKeyword.value) userSearchResults.value = []
    
    setTimeout(() => {
      const input = document.querySelector('#user-search-panel input')
      if (input) input.focus()
    }, 100)
  }
}

const handleUserSearch = () => {
  if (userSearchTimeout) clearTimeout(userSearchTimeout)

  if (!userSearchKeyword.value.trim()) {
    userSearchResults.value = []
    return
  }

  loadingUserSearch.value = true
  userSearchTimeout = setTimeout(async () => {
    try {
      const res = await searchUsers(userSearchKeyword.value)
      userSearchResults.value = res || []
    } catch (error) {
      console.error(error)
    } finally {
      loadingUserSearch.value = false
    }
  }, 300)
}

const toggleFollowUser = async (user) => {
  try {
    if (user.isFollow) {
      await unfollowUser(user.id)
      user.isFollow = false
      showToast('已取消关注')
    } else {
      await followUser(user.id)
      user.isFollow = true
      showToast('关注成功')
    }
  } catch (error) {
    // handled
  }
}

const sendMessageToUser = (user) => {
  showUserSearch.value = false
  router.push({
    path: '/messages',
    query: {
      chatWith: user.id,
      chatName: user.nickname || user.username
    }
  })
}

const goToProfile = (userId) => {
  showUserSearch.value = false
  router.push(`/user/${userId}`)
}

// 点击外部关闭通知
const handleClickOutside = (e) => {
  // Notifications
  const notifPanel = document.getElementById('main-notification-panel')
  const notifBtn = document.getElementById('main-notification-btn')
  if (showNotifications.value && notifPanel && !notifPanel.contains(e.target) && notifBtn && !notifBtn.contains(e.target)) {
    showNotifications.value = false
  }

  // User Search
  const searchPanel = document.getElementById('user-search-panel')
  const searchBtn = document.getElementById('user-search-btn')
  if (showUserSearch.value && searchPanel && !searchPanel.contains(e.target) && searchBtn && !searchBtn.contains(e.target)) {
    showUserSearch.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
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

          <!-- 添加好友 (点击显示搜索面板) -->
          <div class="relative">
            <button
              id="user-search-btn"
              v-if="userStore.user"
              @click="toggleUserSearch"
              class="p-2.5 text-gray-500 hover:text-orange-500 hover:bg-orange-50 rounded-full transition"
              title="添加好友"
            >
              <UserPlus class="w-5 h-5" />
            </button>

            <!-- 搜索用户下拉面板 -->
            <Transition
              enter-active-class="transition duration-200 ease-out"
              enter-from-class="opacity-0 scale-95 -translate-y-2"
              enter-to-class="opacity-100 scale-100 translate-y-0"
              leave-active-class="transition duration-150 ease-in"
              leave-from-class="opacity-100 scale-100 translate-y-0"
              leave-to-class="opacity-0 scale-95 -translate-y-2"
            >
              <div 
                v-if="showUserSearch"
                id="user-search-panel"
                class="absolute right-0 top-full mt-3 z-50 w-80 bg-white/90 backdrop-blur-xl rounded-2xl shadow-xl border border-gray-100/50 p-4 ring-1 ring-gray-900/5 origin-top-right overflow-hidden"
              >
                <!-- Header -->
                <div class="flex items-center justify-between mb-3 px-1">
                  <h3 class="font-semibold text-gray-800 text-sm">添加好友</h3>
                  <span class="text-[10px] text-gray-400 bg-gray-100 px-1.5 py-0.5 rounded">搜索</span>
                </div>

                <!-- Input -->
                <div class="relative mb-4 group">
                  <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-orange-500 transition-colors" />
                  <input 
                    v-model="userSearchKeyword"
                    @input="handleUserSearch"
                    type="text" 
                    placeholder="输入昵称或账号..." 
                    class="w-full pl-9 pr-3 py-2.5 bg-gray-50/50 border border-gray-200/60 rounded-xl text-sm focus:outline-none focus:bg-white focus:border-orange-200 focus:ring-4 focus:ring-orange-500/10 transition-all placeholder:text-gray-400"
                    autofocus
                  >
                </div>
                
                <!-- Loading -->
                <div v-if="loadingUserSearch" class="flex flex-col items-center justify-center py-8 text-orange-400">
                  <Loader2 class="w-6 h-6 animate-spin mb-2" />
                  <span class="text-xs text-gray-400">正在寻找...</span>
                </div>

                <!-- Results -->
                <div v-else-if="userSearchResults.length > 0" class="space-y-1 max-h-[320px] overflow-y-auto custom-scrollbar -mr-2 pr-2">
                  <div 
                    v-for="user in userSearchResults" 
                    :key="user.id" 
                    class="flex items-center justify-between p-2 rounded-xl hover:bg-orange-50/50 transition-colors group cursor-pointer"
                    @click="goToProfile(user.id)"
                  >
                    <div class="flex items-center gap-3 overflow-hidden">
                      <UserAvatar 
                        :src="user.avatar" 
                        :name="user.nickname || user.username"
                        class="w-10 h-10 ring-2 ring-white shadow-sm group-hover:scale-105 transition-transform duration-300"
                      />
                      <div class="flex flex-col overflow-hidden">
                        <span class="text-sm font-semibold text-gray-800 truncate" :title="user.nickname || user.username">
                          {{ user.nickname || user.username }}
                        </span>
                        <span class="text-[10px] text-gray-400 truncate">
                          {{ user.bio || '暂无简介' }}
                        </span>
                      </div>
                    </div>
                    
                    <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-all duration-200 translate-x-2 group-hover:translate-x-0">
                      <button 
                        v-if="user.id !== userStore.user?.id"
                        @click.stop.prevent="toggleFollowUser(user)"
                        :class="[
                          'p-2 rounded-lg transition-all active:scale-90',
                          user.isFollow 
                            ? 'text-gray-400 bg-gray-100 hover:text-red-500 hover:bg-red-50' 
                            : 'text-white bg-gradient-to-r from-orange-500 to-red-500 shadow-md shadow-orange-200 hover:shadow-orange-300 hover:-translate-y-0.5'
                        ]"
                        :title="user.isFollow ? '取消关注' : '关注'"
                      >
                        <UserMinus v-if="user.isFollow" class="w-3.5 h-3.5" />
                        <UserPlus v-else class="w-3.5 h-3.5" />
                      </button>
                      <button 
                        v-if="user.id !== userStore.user?.id"
                        @click.stop.prevent="sendMessageToUser(user)"
                        class="p-2 text-gray-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition-all active:scale-90"
                        title="发私信"
                      >
                        <MessageCircle class="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                </div>

                <!-- Empty States -->
                <div v-else-if="userSearchKeyword && !loadingUserSearch" class="flex flex-col items-center justify-center py-8 text-center">
                  <div class="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center mb-3">
                    <UserMinus class="w-6 h-6 text-gray-300" />
                  </div>
                  <p class="text-sm text-gray-500 font-medium">未找到相关用户</p>
                  <p class="text-xs text-gray-400 mt-1">换个关键词试试看?</p>
                </div>
                
                <div v-else class="flex flex-col items-center justify-center py-8 text-center">
                   <div class="w-12 h-12 bg-orange-50 rounded-full flex items-center justify-center mb-3 group-hover:scale-110 transition-transform">
                    <Search class="w-6 h-6 text-orange-300" />
                  </div>
                  <p class="text-sm text-gray-500 font-medium">搜索好友</p>
                  <p class="text-xs text-gray-400 mt-1">输入昵称或账号ID进行查找</p>
                </div>
              </div>
            </Transition>
          </div>

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
