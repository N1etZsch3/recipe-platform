<script setup>
import { useUserStore } from '../stores/user'
import { useRouter, useRoute } from 'vue-router'
import { computed, inject, ref, onMounted, onUnmounted } from 'vue'
import { LogOut, ChefHat, MessageSquare, Menu, X, Home, Compass, User, Bell, Check, CheckCheck, FileText, UserPlus, Heart, Reply } from 'lucide-vue-next'
import { useToast } from './Toast.vue'
import { useNotificationStore } from '@/stores/notification'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const confirm = inject('confirm')
const { showToast } = useToast()
const notificationStore = useNotificationStore()

const isLoginPage = computed(() => route.name === 'login')
const isOpen = ref(false)
const showNotifications = ref(false)

// 未读通知数
const unreadCount = computed(() => notificationStore.unreadCount)

// 最近通知列表（最多显示8条）
const recentNotifications = computed(() => {
  return notificationStore.notifications.slice(0, 8)
})

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
}

// 关闭通知面板
const closeNotifications = () => {
  showNotifications.value = false
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

// 获取通知图标
const getNotificationIcon = (type) => {
  const icons = {
    'NEW_MESSAGE': MessageSquare,
    'RECIPE_APPROVED': Check,
    'RECIPE_REJECTED': X,
    'NEW_FOLLOWER': UserPlus,
    'NEW_COMMENT': MessageSquare,
    'NEW_RECIPE_PENDING': FileText,
    'COMMENT_REPLY': Reply,
    'COMMENT_LIKED': Heart
  }
  return icons[type] || Bell
}

// 获取通知颜色
const getNotificationColor = (type) => {
  const colors = {
    'NEW_MESSAGE': 'text-blue-500 bg-blue-50',
    'RECIPE_APPROVED': 'text-green-500 bg-green-50',
    'RECIPE_REJECTED': 'text-red-500 bg-red-50',
    'NEW_FOLLOWER': 'text-purple-500 bg-purple-50',
    'NEW_COMMENT': 'text-orange-500 bg-orange-50',
    'NEW_RECIPE_PENDING': 'text-amber-500 bg-amber-50',
    'COMMENT_REPLY': 'text-cyan-500 bg-cyan-50',
    'COMMENT_LIKED': 'text-pink-500 bg-pink-50'
  }
  return colors[type] || 'text-gray-500 bg-gray-50'
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
  const panel = document.getElementById('notification-panel')
  const btn = document.getElementById('notification-btn')
  if (panel && !panel.contains(e.target) && btn && !btn.contains(e.target)) {
    showNotifications.value = false
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
                  class="absolute left-1/2 -translate-x-1/2 top-full mt-2 w-80 bg-white rounded-2xl shadow-xl border border-gray-100 overflow-hidden z-50"
                >
                  <!-- 头部 -->
                  <div class="px-4 py-3 bg-gradient-to-r from-orange-50 to-red-50 border-b border-gray-100 flex items-center justify-between">
                    <div class="flex items-center gap-2">
                      <Bell class="w-4 h-4 text-orange-500" />
                      <span class="font-semibold text-gray-800">通知中心</span>
                      <span v-if="unreadCount > 0" class="bg-red-500 text-white text-xs px-1.5 py-0.5 rounded-full">
                        {{ unreadCount }}
                      </span>
                    </div>
                    <button 
                      v-if="unreadCount > 0"
                      @click="markAllRead"
                      class="text-xs text-orange-500 hover:text-orange-600 flex items-center gap-1"
                    >
                      <CheckCheck class="w-3.5 h-3.5" />
                      全部已读
                    </button>
                  </div>
                  
                  <!-- 通知列表 -->
                  <div class="max-h-80 overflow-y-auto">
                    <div v-if="recentNotifications.length === 0" class="py-12 text-center text-gray-400">
                      <Bell class="w-10 h-10 mx-auto mb-2 opacity-30" />
                      <p>暂无通知</p>
                    </div>
                    
                    <div 
                      v-else
                      v-for="notification in recentNotifications" 
                      :key="notification.id"
                      @click="handleNotificationClick(notification)"
                      :class="[
                        'flex items-start gap-3 px-4 py-3 cursor-pointer transition border-b border-gray-50 last:border-0',
                        notification.read ? 'bg-white hover:bg-gray-50' : 'bg-orange-50/50 hover:bg-orange-50'
                      ]"
                    >
                      <!-- 图标 -->
                      <div :class="['p-2 rounded-lg flex-shrink-0', getNotificationColor(notification.type)]">
                        <component :is="getNotificationIcon(notification.type)" class="w-4 h-4" />
                      </div>
                      
                      <!-- 内容 -->
                      <div class="flex-1 min-w-0">
                        <p class="text-sm font-medium text-gray-800 truncate">{{ notification.title }}</p>
                        <p class="text-xs text-gray-500 truncate mt-0.5">{{ notification.content }}</p>
                        <p class="text-xs text-gray-400 mt-1">{{ formatTime(notification.receivedAt) }}</p>
                      </div>
                      
                      <!-- 未读标记 -->
                      <div v-if="!notification.read" class="w-2 h-2 bg-orange-500 rounded-full flex-shrink-0 mt-2"></div>
                    </div>
                  </div>
                  
                  <!-- 底部 -->
                  <div v-if="recentNotifications.length > 0" class="px-4 py-3 bg-gray-50 border-t border-gray-100 text-center">
                    <button 
                      @click="router.push('/messages'); closeNotifications()"
                      class="text-sm text-orange-500 hover:text-orange-600 font-medium"
                    >
                      查看全部消息 →
                    </button>
                  </div>
                </div>
              </Transition>
            </div>
            
            <!-- 用户信息 -->
            <div class="hidden md:flex items-center gap-3 pl-3 border-l border-gray-200">
              <img 
                :src="userStore.user.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${userStore.user.id}`" 
                class="w-9 h-9 rounded-full ring-2 ring-gray-100 object-cover"
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
              <img 
                :src="userStore.user.avatar || `https://api.dicebear.com/7.x/avataaars/svg?seed=${userStore.user.id}`" 
                class="w-12 h-12 rounded-full ring-2 ring-white shadow"
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
