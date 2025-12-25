<script setup>
import { useUserStore } from '../stores/user'
import { useRouter, useRoute } from 'vue-router'
import { computed, inject, ref, onMounted, onUnmounted } from 'vue'
import { LogOut, ChefHat, MessageSquare, Menu, X, Home, Compass, User, Bell, Search, UserPlus, UserMinus, MessageCircle, Loader2 } from 'lucide-vue-next'
import { useToast } from './Toast.vue'
import { useNotificationStore } from '@/stores/notification'
import NotificationCenter from '@/components/NotificationCenter.vue'
import UserAvatar from '@/components/UserAvatar.vue'
import { searchUsers, followUser, unfollowUser } from '@/api/social'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const confirm = inject('confirm')
const { showToast } = useToast()
const notificationStore = useNotificationStore()

const isLoginPage = computed(() => route.name === 'login')
const isOpen = ref(false)
const showNotifications = ref(false)
const showSearch = ref(false)
const searchKeyword = ref('')
const searchResults = ref([])
const loadingSearch = ref(false)
let searchTimeout = null

// 未读通知数
const unreadCount = computed(() => notificationStore.unreadCount)

const handleLogout = async () => {
  if (await confirm('确定要退出登录吗？')) {
    userStore.logout()
    router.push('/login')
  }
}

const toggleMenu = () => {
  isOpen.value = !isOpen.value
}

const toggleNotifications = () => {
  showNotifications.value = !showNotifications.value
  if (showNotifications.value) showSearch.value = false
}

const closeNotifications = () => {
  showNotifications.value = false
}

const toggleSearch = () => {
  showSearch.value = !showSearch.value
  if (showSearch.value) {
    showNotifications.value = false
    // Reset search when opening
    if (!searchKeyword.value) searchResults.value = []
    
    // Auto focus input (next tick)
    setTimeout(() => {
        const input = document.querySelector('#search-panel input')
        if (input) input.focus()
    }, 100)
  }
}

const handleSearch = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  
  if (!searchKeyword.value.trim()) {
    searchResults.value = []
    return
  }

  loadingSearch.value = true
  searchTimeout = setTimeout(async () => {
    try {
      const res = await searchUsers(searchKeyword.value)
      searchResults.value = res.data || []
    } catch (error) {
      console.error(error)
    } finally {
      loadingSearch.value = false
    }
  }, 300)
}

const goToProfile = (userId) => {
    showSearch.value = false
    router.push(`/user/${userId}`)
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
        // Error handled by request interceptor usually
    }
}

const sendMessageToUser = (user) => {
    showSearch.value = false
    router.push({
        path: '/messages',
        query: {
            chatWith: user.id,
            chatName: user.nickname || user.username
        }
    })
}

// 点击通知
const handleNotificationClick = (notification) => {
  notificationStore.markAsRead(notification.id)
  
  // 根据通知类型跳转
  if (notification.type === 'NEW_MESSAGE' && notification.senderId) {
    // 跳转到消息页面并指定和该用户的对话
    router.push({ 
      path: '/messages', 
      query: { 
        chatWith: notification.senderId,
        chatName: notification.senderName || notification.title?.replace('新私信', '').trim() || '用户'
      }
    })
  } else if (notification.type === 'RECIPE_APPROVED' || notification.type === 'RECIPE_REJECTED') {
    router.push('/profile')
  } else if (notification.type === 'NEW_FOLLOWER') {
    router.push('/profile')
  } else if (notification.type === 'NEW_COMMENT' && notification.relatedId) {
    router.push(`/recipe/${notification.relatedId}`)
  } else if ((notification.type === 'COMMENT_REPLY' || notification.type === 'COMMENT_LIKED') && notification.relatedId) {
    // 点击评论回复或点赞通知，跳转到对应菜谱页面
    router.push(`/recipe/${notification.relatedId}`)
  } else {
    // 默认跳转到消息中心
    router.push('/messages')
  }
  
  closeNotifications()
}

// 全部标记已读
const markAllRead = () => {
  notificationStore.markAllAsRead()
}


// 格式化时间
const formatTime = (date) => {
  if (!date) return ''
  const now = new Date()
  const d = new Date(date)
  const diff = now - d
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return Math.floor(diff / 86400000) + '天前'
}

// 判断当前路由是否激活
const isActive = (path) => {
  if (path === '/') return route.path === '/'
  return route.path.startsWith(path)
}

// 点击外部关闭通知面板
const handleClickOutside = (e) => {
  const notifPanel = document.getElementById('notification-panel')
  const notifBtn = document.getElementById('notification-btn')
  if (notifPanel && !notifPanel.contains(e.target) && notifBtn && !notifBtn.contains(e.target)) {
    showNotifications.value = false
  }

  const searchPanel = document.getElementById('search-panel')
  const searchBtn = document.getElementById('search-btn')
  if (searchPanel && !searchPanel.contains(e.target) && searchBtn && !searchBtn.contains(e.target)) {
    showSearch.value = false
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
  <nav v-if="!isLoginPage" class="bg-white/95 backdrop-blur-xl text-gray-800 border-b border-gray-100/80 sticky top-0 z-50 shadow-sm">
    <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
      <div class="flex justify-between items-center h-16">
        
        <!-- 左侧: Logo + 导航 -->
        <div class="flex items-center gap-10">
          <!-- Logo -->
          <div 
            class="flex items-center gap-2.5 cursor-pointer group" 
            @click="router.push('/')"
          >
            <div class="relative">
              <div class="absolute inset-0 bg-gradient-to-br from-orange-400 to-red-500 rounded-xl blur opacity-40 group-hover:opacity-60 transition"></div>
              <div class="relative bg-gradient-to-br from-orange-500 to-red-500 p-2 rounded-xl">
                <ChefHat class="w-6 h-6 text-white" />
              </div>
            </div>
            <span class="font-bold text-xl bg-gradient-to-r from-orange-500 via-red-500 to-pink-500 bg-clip-text text-transparent hidden sm:block">
              三食六记
            </span>
          </div>

          <!-- 导航链接 - Desktop -->
          <div class="hidden md:flex items-center gap-1">
            <!-- 游客导航 -->
            <template v-if="!userStore.user">
              <router-link 
                to="/" 
                :class="[
                  'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
                  isActive('/') && route.path === '/' 
                    ? 'bg-orange-50 text-orange-600' 
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                ]"
              >
                <Home class="w-4 h-4" />
                首页
              </router-link>
              <router-link 
                to="/home" 
                :class="[
                  'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
                  isActive('/home') 
                    ? 'bg-orange-50 text-orange-600' 
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                ]"
              >
                <Compass class="w-4 h-4" />
                精选菜谱
              </router-link>
            </template>

            <!-- 登录用户导航 -->
            <template v-else>
              <router-link 
                to="/home" 
                :class="[
                  'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
                  isActive('/home') 
                    ? 'bg-orange-50 text-orange-600' 
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                ]"
              >
                <Compass class="w-4 h-4" />
                精选菜谱
              </router-link>
              <router-link 
                to="/messages" 
                :class="[
                  'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all relative',
                  isActive('/messages') 
                    ? 'bg-orange-50 text-orange-600' 
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                ]"
              >
                <MessageSquare class="w-4 h-4" />
              消息
              </router-link>
              <router-link 
                to="/profile" 
                :class="[
                  'flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-all',
                  isActive('/profile') 
                    ? 'bg-orange-50 text-orange-600' 
                    : 'text-gray-600 hover:bg-gray-50 hover:text-gray-900'
                ]"
              >
                <User class="w-4 h-4" />
                个人中心
              </router-link>
            </template>
          </div>
        </div>

        <!-- 右侧: 用户区域 -->
        <div class="flex items-center gap-3">
          <template v-if="userStore.user">
             <!-- 搜索用户图标 + 下拉面板 -->
            <div class="relative">
              <button 
                id="search-btn"
                @click="toggleSearch"
                class="relative p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition"
                title="搜索用户"
              >
                <Search class="w-5 h-5" />
              </button>

              <!-- 搜索下拉面板 -->
              <Transition
                enter-active-class="transition duration-200 ease-out"
                enter-from-class="opacity-0 scale-95 -translate-y-2"
                enter-to-class="opacity-100 scale-100 translate-y-0"
                leave-active-class="transition duration-150 ease-in"
                leave-from-class="opacity-100 scale-100 translate-y-0"
                leave-to-class="opacity-0 scale-95 -translate-y-2"
              >
                <div 
                  v-if="showSearch"
                  id="search-panel"
                  class="absolute right-0 top-full mt-2 z-50 w-80 bg-white rounded-xl shadow-xl border border-gray-100 p-4"
                >
                   <input 
                    v-model="searchKeyword"
                    @input="handleSearch"
                    type="text" 
                    placeholder="搜索用户名或昵称..." 
                    class="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500 transition-all mb-3"
                    autofocus
                  >
                  
                  <div v-if="loadingSearch" class="flex justify-center py-4 text-gray-400">
                    <Loader2 class="w-5 h-5 animate-spin" />
                  </div>

                  <div v-else-if="searchResults.length > 0" class="space-y-3 max-h-64 overflow-y-auto custom-scrollbar">
                    <div v-for="user in searchResults" :key="user.id" class="flex items-center justify-between group">
                      <div 
                        class="flex items-center gap-2 cursor-pointer"
                        @click="goToProfile(user.id)"
                      >
                         <UserAvatar 
                          :src="user.avatar" 
                          :name="user.nickname || user.username"
                          class="w-8 h-8 flex-shrink-0"
                        />
                        <div class="overflow-hidden">
                          <p class="text-sm font-medium text-gray-800 truncate w-24" :title="user.nickname || user.username">{{ user.nickname || user.username }}</p>
                        </div>
                      </div>
                      
                      <div class="flex items-center gap-1 opacity-100 transition-opacity">
                         <button 
                          v-if="user.id !== userStore.user?.id"
                          @click.stop.prevent="toggleFollowUser(user)"
                          :class="[
                            'p-1.5 rounded-md transition-colors',
                            user.isFollow 
                              ? 'text-gray-400 hover:text-red-500 hover:bg-red-50' 
                              : 'text-orange-500 hover:bg-orange-50'
                          ]"
                          :title="user.isFollow ? '取消关注' : '关注'"
                        >
                          <UserMinus v-if="user.isFollow" class="w-4 h-4" />
                          <UserPlus v-else class="w-4 h-4" />
                        </button>
                        <button 
                          v-if="user.id !== userStore.user?.id"
                          @click.stop.prevent="sendMessageToUser(user)"
                          class="p-1.5 text-gray-400 hover:text-blue-500 hover:bg-blue-50 rounded-md transition-colors"
                          title="发私信"
                        >
                          <MessageCircle class="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  </div>

                  <div v-else-if="searchKeyword && !loadingSearch" class="text-center py-6 text-gray-400 text-sm">
                    未找到相关用户
                  </div>
                   <div v-else class="text-center py-6 text-gray-400 text-sm">
                    输入关键词搜索用户
                  </div>
                </div>
              </Transition>
            </div>

            <!-- 通知图标 + 下拉面板 -->
            <div class="relative">
              <button 
                id="notification-btn"
                @click="toggleNotifications"
                class="relative p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition"
                title="通知"
              >
                <Bell class="w-5 h-5" />
                <span 
                  v-if="unreadCount > 0" 
                  class="absolute -top-0.5 -right-0.5 bg-red-500 text-white text-xs w-5 h-5 flex items-center justify-center rounded-full font-medium animate-pulse"
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
                  id="notification-panel"
                  class="absolute right-0 top-full mt-2 z-50 w-80"
                >
                  <NotificationCenter 
                    :show-todo="false"
                    @close="closeNotifications"
                    @mark-read="markAllRead"
                    @view-all="router.push('/messages'); closeNotifications()"
                  />
                </div>
              </Transition>
            </div>
            
            <!-- 用户信息 -->
            <div class="hidden md:flex items-center gap-3 pl-3 border-l border-gray-200">
              <UserAvatar 
                :src="userStore.user.avatar" 
                :name="userStore.user.nickname || userStore.user.username"
                class="w-9 h-9 ring-2 ring-gray-100"
              />
              <div class="flex flex-col">
                <span class="text-sm font-medium text-gray-800 leading-tight">
                  {{ userStore.user.nickname || userStore.user.username }}
                </span>
                <span class="text-xs text-gray-400">欢迎回来</span>
              </div>
            </div>

            <!-- 退出按钮 -->
            <button 
              @click="handleLogout" 
              class="hidden md:flex items-center gap-1.5 px-3 py-2 text-sm text-gray-500 hover:text-red-500 hover:bg-red-50 rounded-lg transition"
              title="退出登录"
            >
              <LogOut class="w-4 h-4" />
            </button>
          </template>

          <template v-else>
            <button 
              @click="router.push('/login')" 
              class="bg-gradient-to-r from-orange-500 to-red-500 text-white px-6 py-2.5 rounded-xl font-semibold text-sm hover:shadow-lg hover:shadow-orange-200 hover:-translate-y-0.5 transition-all"
            >
              登录 / 注册
            </button>
          </template>
          
          <!-- Mobile Menu Button -->
          <button 
            @click="toggleMenu" 
            class="md:hidden p-2 text-gray-600 hover:text-orange-500 hover:bg-orange-50 rounded-lg transition"
          >
            <Menu v-if="!isOpen" class="w-6 h-6" />
            <X v-else class="w-6 h-6" />
          </button>
        </div>
      </div>
    </div>

    <!-- Mobile Menu -->
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 -translate-y-2"
      enter-to-class="opacity-100 translate-y-0"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 translate-y-0"
      leave-to-class="opacity-0 -translate-y-2"
    >
      <div v-if="isOpen" class="md:hidden bg-white border-t border-gray-100 shadow-lg">
        <div class="px-4 py-3 space-y-1">
          <template v-if="!userStore.user">
            <router-link 
              to="/" 
              class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 hover:bg-orange-50 hover:text-orange-600 transition"
              @click="isOpen = false"
            >
              <Home class="w-5 h-5" />
              首页
            </router-link>
            <router-link 
              to="/home" 
              class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 hover:bg-orange-50 hover:text-orange-600 transition"
              @click="isOpen = false"
            >
              <Compass class="w-5 h-5" />
              精选菜谱
            </router-link>
            <div class="pt-3">
              <button 
                @click="router.push('/login'); isOpen = false" 
                class="w-full bg-gradient-to-r from-orange-500 to-red-500 text-white px-6 py-3 rounded-xl font-semibold hover:shadow-lg transition"
              >
                登录 / 注册
              </button>
            </div>
          </template>

          <template v-else>
            <!-- 用户信息卡片 -->
            <div class="flex items-center gap-3 p-4 bg-gradient-to-r from-orange-50 to-red-50 rounded-xl mb-3">
              <UserAvatar 
                :src="userStore.user.avatar" 
                :name="userStore.user.nickname || userStore.user.username"
                class="w-12 h-12 ring-2 ring-white shadow"
              />
              <div>
                <p class="font-semibold text-gray-800">{{ userStore.user.nickname || userStore.user.username }}</p>
                <p class="text-sm text-gray-500">欢迎回来</p>
              </div>
            </div>

            <router-link 
              to="/home" 
              class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 hover:bg-orange-50 hover:text-orange-600 transition"
              @click="isOpen = false"
            >
              <Compass class="w-5 h-5" />
              精选菜谱
            </router-link>
            <router-link 
              to="/messages" 
              class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 hover:bg-orange-50 hover:text-orange-600 transition"
              @click="isOpen = false"
            >
              <MessageSquare class="w-5 h-5" />
              消息
              <span 
                v-if="(userStore.unreadCount || 0) > 0" 
                class="bg-red-500 text-white text-xs px-2 py-0.5 rounded-full ml-auto"
              >
                {{ userStore.unreadCount }}
              </span>
            </router-link>
            <router-link 
              to="/profile" 
              class="flex items-center gap-3 px-4 py-3 rounded-xl text-gray-600 hover:bg-orange-50 hover:text-orange-600 transition"
              @click="isOpen = false"
            >
              <User class="w-5 h-5" />
              个人中心
            </router-link>
            
            <div class="pt-3 border-t border-gray-100 mt-2">
              <button 
                @click="handleLogout" 
                class="flex items-center gap-3 w-full px-4 py-3 rounded-xl text-red-500 hover:bg-red-50 transition"
              >
                <LogOut class="w-5 h-5" />
                退出登录
              </button>
            </div>
          </template>
        </div>
      </div>
    </Transition>
  </nav>
</template>
