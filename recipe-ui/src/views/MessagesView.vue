<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useNotificationStore } from '../stores/notification'
import { useToast } from '../components/Toast.vue'
import { 
  MessageSquare, Send, ArrowLeft, User, Search, MoreVertical, AlertCircle,
  Mail, Heart, Bell, Settings, Smile, Image as ImageIcon, ChefHat, 
  CheckCircle, XCircle, Trash2, ArrowRight, MessageCircle, Edit3, Check
} from 'lucide-vue-next'
import { 
  getConversations, getMessages, sendMessage as apiSendMessage, markRead,
  getMyComments, deleteMyComments, getRepliesForMe, getLikesForMe, getLikeDetail,
  checkOnlineStatus
} from '@/api/social'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { showToast } = useToast()

// 左侧导航项
const navItems = [
  { id: 'messages', name: '我的消息', icon: Mail },
  { id: 'myComments', name: '我的评论', icon: Edit3 },
  { id: 'replies', name: '回复我的', icon: MessageCircle },
  { id: 'likes', name: '收到的赞', icon: Heart },
  { id: 'system', name: '系统通知', icon: Bell }
]

const activeNav = ref('messages')

// ============= 我的消息相关 =============
const conversations = ref([])
const loadingConversations = ref(false)
const selectedConversation = ref(null)
const messages = ref([])
const loadingMessages = ref(false)
const messagesContainer = ref(null)
const newMessage = ref('')
const searchKeyword = ref('')
const onlineStatus = ref({})

// ============= 我的评论相关 =============
const myComments = ref([])
const loadingMyComments = ref(false)
const selectedCommentIds = ref([])
const isSelectMode = ref(false)

// ============= 回复我的相关 =============
const replies = ref([])
const loadingReplies = ref(false)

// ============= 收到的赞相关 =============
const likes = ref([])
const loadingLikes = ref(false)
const selectedLike = ref(null)
const likeDetail = ref([])
const loadingLikeDetail = ref(false)

// ============= 系统通知相关 =============
const systemNotifications = ref([])
const loadingSystem = ref(false)

// 移动端显示控制
const mobileView = ref('nav')

// 过滤后的会话列表
const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  return conversations.value.filter(c => 
    c.nickname?.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 格式化时间
const formatTime = (timeStr) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  
  const isThisYear = date.getFullYear() === now.getFullYear()
  if (isThisYear) {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' + 
           date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}

// ============= 我的消息方法 =============
const loadConversations = async () => {
  loadingConversations.value = true
  try {
    const res = await getConversations()
    conversations.value = res.map(c => ({
      id: c.userId,
      userId: c.userId,
      nickname: c.nickname,
      avatar: c.avatar,
      lastMessage: c.lastMessage,
      time: formatTime(c.lastTime),
      rawTime: c.lastTime,
      unread: c.unreadCount || 0
    }))
    // 加载所有会话用户的在线状态
    if (conversations.value.length > 0) {
      const userIds = conversations.value.map(c => c.userId)
      await fetchOnlineStatus(userIds)
    }
  } catch (error) {
    console.error('加载会话失败', error)
  } finally {
    loadingConversations.value = false
  }
}

// 获取用户在线状态
const fetchOnlineStatus = async (userIds) => {
  if (!userIds || userIds.length === 0) return
  try {
    const res = await checkOnlineStatus(userIds)
    if (res) {
      onlineStatus.value = { ...onlineStatus.value, ...res }
    }
  } catch (error) {
    console.error('获取在线状态失败', error)
  }
}

const selectConversation = async (conv) => {
  selectedConversation.value = conv
  mobileView.value = 'detail'
  notificationStore.setCurrentChatUser(conv.userId)
  // 获取该用户的在线状态
  fetchOnlineStatus([conv.userId])
  await loadMessages(conv.userId)
  if (conv.unread > 0) {
    markRead(conv.userId).then(() => conv.unread = 0).catch(e => console.error(e))
  }
}

const loadMessages = async (userId) => {
  loadingMessages.value = true
  messages.value = []
  try {
    const res = await getMessages(userId, { page: 1, size: 50 })
    const list = res.records || []
    messages.value = list.map(m => ({
      id: m.id,
      senderId: m.senderId,
      content: m.content,
      time: formatTime(m.createTime),
      isMine: m.isMe,
      senderAvatar: m.senderAvatar
    })).reverse()
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败', error)
  } finally {
    loadingMessages.value = false
  }
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const handleSendMessage = async () => {
  if (!newMessage.value.trim() || !selectedConversation.value) return
  
  const content = newMessage.value
  const receiverId = selectedConversation.value.userId
  
  const tempMsg = {
    id: Date.now(),
    senderId: userStore.user?.id,
    content: content,
    time: formatTime(new Date()),
    isMine: true,
    status: 'sending'
  }
  messages.value.push(tempMsg)
  newMessage.value = ''
  const reactiveMsg = messages.value[messages.value.length - 1]
  await nextTick()
  scrollToBottom()

  try {
    await apiSendMessage({ receiverId, content })
    reactiveMsg.status = 'success'
    const convIndex = conversations.value.findIndex(c => c.userId === receiverId)
    if (convIndex > -1) {
      const conv = conversations.value[convIndex]
      conv.lastMessage = content
      conv.time = '刚刚'
      conversations.value.splice(convIndex, 1)
      conversations.value.unshift(conv)
    }
  } catch (error) {
    reactiveMsg.status = 'fail'
    showToast(error.message || '发送失败', 'error')
  }
}

// ============= 我的评论方法 =============
const loadMyComments = async () => {
  loadingMyComments.value = true
  try {
    const res = await getMyComments({ page: 1, size: 50 })
    myComments.value = res.records || []
  } catch (error) {
    console.error('加载我的评论失败', error)
  } finally {
    loadingMyComments.value = false
  }
}

const toggleSelectMode = () => {
  isSelectMode.value = !isSelectMode.value
  if (!isSelectMode.value) {
    selectedCommentIds.value = []
  }
}

const toggleSelectComment = (id) => {
  const index = selectedCommentIds.value.indexOf(id)
  if (index > -1) {
    selectedCommentIds.value.splice(index, 1)
  } else {
    selectedCommentIds.value.push(id)
  }
}

const selectAllComments = () => {
  if (selectedCommentIds.value.length === myComments.value.length) {
    selectedCommentIds.value = []
  } else {
    selectedCommentIds.value = myComments.value.map(c => c.id)
  }
}

const handleDeleteComments = async () => {
  if (selectedCommentIds.value.length === 0) return
  try {
    await deleteMyComments(selectedCommentIds.value)
    showToast('删除成功', 'success')
    selectedCommentIds.value = []
    isSelectMode.value = false
    loadMyComments()
  } catch (error) {
    showToast(error.message || '删除失败', 'error')
  }
}

// ============= 回复我的方法 =============
const loadReplies = async () => {
  loadingReplies.value = true
  try {
    const res = await getRepliesForMe({ page: 1, size: 50 })
    replies.value = res.records || []
  } catch (error) {
    console.error('加载回复失败', error)
  } finally {
    loadingReplies.value = false
  }
}

// ============= 收到的赞方法 =============
const loadLikes = async () => {
  loadingLikes.value = true
  try {
    const res = await getLikesForMe({ page: 1, size: 50 })
    likes.value = res.records || []
  } catch (error) {
    console.error('加载点赞失败', error)
  } finally {
    loadingLikes.value = false
  }
}

const openLikeDetail = async (like) => {
  selectedLike.value = like
  mobileView.value = 'detail'
  loadingLikeDetail.value = true
  try {
    const res = await getLikeDetail(like.commentId, { page: 1, size: 50 })
    likeDetail.value = res.records || []
  } catch (error) {
    console.error('加载点赞详情失败', error)
  } finally {
    loadingLikeDetail.value = false
  }
}

const closeLikeDetail = () => {
  selectedLike.value = null
  likeDetail.value = []
}

// ============= 系统通知方法 =============
const loadSystemNotifications = async () => {
  loadingSystem.value = true
  // 从 notificationStore 获取系统通知
  const sysTypes = ['RECIPE_APPROVED', 'RECIPE_REJECTED', 'COMMENT_DELETED']
  systemNotifications.value = notificationStore.notifications
    .filter(n => sysTypes.includes(n.type))
    .map(n => ({
      id: n.id,
      type: n.type,
      title: n.title,
      content: n.content,
      relatedId: n.relatedId,
      time: formatTime(n.receivedAt),
      read: n.read
    }))
  loadingSystem.value = false
}

const getSystemIcon = (type) => {
  const icons = {
    'RECIPE_APPROVED': CheckCircle,
    'RECIPE_REJECTED': XCircle,
    'COMMENT_DELETED': Trash2
  }
  return icons[type] || Bell
}

const getSystemColor = (type) => {
  const colors = {
    'RECIPE_APPROVED': 'text-green-500 bg-green-50',
    'RECIPE_REJECTED': 'text-red-500 bg-red-50',
    'COMMENT_DELETED': 'text-orange-500 bg-orange-50'
  }
  return colors[type] || 'text-gray-500 bg-gray-50'
}

// ============= 导航切换 =============
const selectNav = (item) => {
  activeNav.value = item.id
  mobileView.value = 'list'
  selectedConversation.value = null
  selectedLike.value = null
  isSelectMode.value = false
  selectedCommentIds.value = []
  
  if (item.id === 'messages') {
    loadConversations()
  } else if (item.id === 'myComments') {
    loadMyComments()
  } else if (item.id === 'replies') {
    loadReplies()
  } else if (item.id === 'likes') {
    loadLikes()
  } else if (item.id === 'system') {
    loadSystemNotifications()
  }
}

const backToList = () => {
  if (selectedLike.value) {
    selectedLike.value = null
    likeDetail.value = []
    return
  }
  if (mobileView.value === 'detail') {
    mobileView.value = 'list'
    selectedConversation.value = null
    notificationStore.clearCurrentChatUser()
  } else if (mobileView.value === 'list') {
    mobileView.value = 'nav'
  }
}

// 监听新消息
watch(() => notificationStore.latestNotification, async (notification) => {
  if (!notification || notification.type !== 'NEW_MESSAGE') return
  
  const senderId = notification.senderId
  const convIndex = conversations.value.findIndex(c => c.userId === senderId)
  if (convIndex > -1) {
    const conv = conversations.value[convIndex]
    conv.lastMessage = notification.content
    conv.time = '刚刚'
    if (!selectedConversation.value || selectedConversation.value.userId !== senderId) {
      conv.unread = (conv.unread || 0) + 1
    }
    conversations.value.splice(convIndex, 1)
    conversations.value.unshift(conv)
  } else {
    await loadConversations()
  }
  
  if (selectedConversation.value && selectedConversation.value.userId === senderId) {
    messages.value.push({
      id: Date.now(),
      senderId: senderId,
      content: notification.content,
      time: formatTime(new Date()),
      isMine: false,
      senderAvatar: notification.senderAvatar
    })
    await nextTick()
    scrollToBottom()
    markRead(senderId).catch(e => console.error(e))
  }
})

onMounted(async () => {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  await loadConversations()
  
  // 监听用户在线状态变化事件（实时更新）
  window.addEventListener('admin-user-status', handleUserStatusChange)
  
  // 处理从通知跳转过来的情况
  const chatWith = route.query.chatWith
  const chatName = route.query.chatName
  if (chatWith) {
    // 查找是否已有该用户的会话
    const existingConv = conversations.value.find(c => c.userId == chatWith)
    if (existingConv) {
      selectConversation(existingConv)
    } else {
      // 创建新的临时会话
      const newConv = {
        id: parseInt(chatWith),
        userId: parseInt(chatWith),
        nickname: chatName || '用户',
        avatar: null,
        lastMessage: '',
        time: '',
        unread: 0
      }
      conversations.value.unshift(newConv)
      selectConversation(newConv)
    }
    // 清理 URL 参数
    router.replace({ path: '/messages' })
  }
})

onUnmounted(() => {
  notificationStore.clearCurrentChatUser()
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

// 获取头像显示
const getAvatarUrl = (avatar, name) => {
  if (avatar) return avatar
  return `https://api.dicebear.com/7.x/avataaars/svg?seed=${encodeURIComponent(name || 'default')}`
}
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] py-6">
    <div class="max-w-6xl mx-auto px-4">
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex h-[calc(100vh-64px-48px)]">
        
        <!-- 左侧导航栏 -->
        <div :class="[
          'border-r border-gray-200 flex-shrink-0 flex flex-col',
          'w-full md:w-48',
          mobileView === 'nav' ? 'flex' : 'hidden md:flex'
        ]">
          <!-- Logo -->
          <div class="p-4 border-b border-gray-100">
            <span class="font-bold text-gray-800">信息中心</span>
          </div>

          <!-- 导航菜单 -->
          <nav class="flex-1 p-2 space-y-0.5 overflow-y-auto">
            <button
              v-for="item in navItems"
              :key="item.id"
              @click="selectNav(item)"
              :class="[
                'w-full flex items-center gap-2.5 px-3 py-2.5 rounded-lg text-sm transition-all',
                activeNav === item.id
                  ? 'bg-orange-500 text-white font-medium'
                  : 'text-gray-600 hover:bg-orange-50 hover:text-orange-600'
              ]"
            >
              <component :is="item.icon" class="w-4 h-4" />
              {{ item.name }}
            </button>
          </nav>
        </div>

        <!-- 中间内容区 -->
        <div :class="[
          'flex-1 flex flex-col min-w-0',
          mobileView === 'list' || mobileView === 'detail' ? 'flex' : 'hidden md:flex'
        ]">
          
          <!-- ========== 我的消息 ========== -->
          <template v-if="activeNav === 'messages'">
            <div class="flex h-full">
              <!-- 会话列表 -->
              <div :class="[
                'border-r border-gray-100 flex flex-col bg-white',
                'w-full md:w-72',
                selectedConversation && mobileView === 'detail' ? 'hidden md:flex' : 'flex'
              ]">
                <div class="p-3 border-b border-gray-100">
                  <div class="flex items-center gap-2 mb-2">
                    <button @click="backToList" class="md:hidden p-1 hover:bg-gray-100 rounded">
                      <ArrowLeft class="w-4 h-4 text-gray-600" />
                    </button>
                    <span class="text-sm font-medium text-gray-700">最近消息</span>
                  </div>
                  <div class="relative">
                    <Search class="absolute left-2.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                    <input 
                      v-model="searchKeyword"
                      type="text" 
                      placeholder="搜索联系人..." 
                      class="w-full pl-8 pr-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-1 focus:ring-blue-300"
                    >
                  </div>
                </div>
                <div class="flex-1 overflow-y-auto">
                  <div v-if="loadingConversations" class="flex justify-center py-8">
                    <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
                  </div>
                  <div v-else-if="filteredConversations.length === 0" class="text-center text-gray-400 py-8 text-sm">
                    暂无消息
                  </div>
                  <div 
                    v-else
                    v-for="conv in filteredConversations" 
                    :key="conv.id"
                    @click="selectConversation(conv)"
                    :class="[
                      'flex items-center gap-2.5 px-3 py-2.5 cursor-pointer transition border-b border-gray-50',
                      selectedConversation?.userId === conv.userId ? 'bg-blue-50' : 'hover:bg-gray-50'
                    ]"
                  >
                    <div class="relative flex-shrink-0">
                      <img 
                        :src="getAvatarUrl(conv.avatar, conv.nickname)" 
                        class="w-10 h-10 rounded-full object-cover bg-gray-200 cursor-pointer hover:ring-2 hover:ring-orange-300 transition"
                        @click.stop="router.push(`/user/${conv.userId}`)"
                        title="查看用户主页"
                      >
                      <div v-if="conv.unread > 0" class="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center font-bold pointer-events-none">
                        {{ conv.unread > 9 ? '9+' : conv.unread }}
                      </div>
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center justify-between">
                        <span class="font-medium text-gray-800 text-sm truncate">{{ conv.nickname }}</span>
                        <span class="text-[10px] text-gray-400">{{ conv.time }}</span>
                      </div>
                      <p class="text-xs text-gray-500 truncate">{{ conv.lastMessage }}</p>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 聊天区域 -->
              <div :class="[
                'flex-1 flex flex-col min-w-0',
                !selectedConversation && mobileView !== 'detail' ? 'hidden md:flex' : 'flex'
              ]">
                <div v-if="!selectedConversation" class="flex-1 flex flex-col items-center justify-center text-gray-400">
                  <div class="w-32 h-32 mb-4">
                    <svg viewBox="0 0 200 200" class="w-full h-full opacity-60">
                      <circle cx="100" cy="80" r="40" fill="#e5e7eb"/>
                      <ellipse cx="100" cy="160" rx="60" ry="30" fill="#e5e7eb"/>
                      <text x="100" y="85" text-anchor="middle" fill="#9ca3af" font-size="20">💬</text>
                    </svg>
                  </div>
                  <p class="text-base font-medium mb-1">选择一个对话</p>
                  <p class="text-sm">开始与好友聊天吧 ˙ᵕ˙</p>
                </div>
                <template v-else>
                  <!-- 聊天头部 -->
                  <div class="flex items-center gap-2.5 px-4 py-3 bg-white border-b border-gray-100">
                    <button @click="backToList" class="md:hidden p-1.5 hover:bg-gray-100 rounded">
                      <ArrowLeft class="w-4 h-4 text-gray-600" />
                    </button>
                    <img 
                      :src="getAvatarUrl(selectedConversation.avatar, selectedConversation.nickname)" 
                      class="w-9 h-9 rounded-full object-cover bg-gray-200 cursor-pointer hover:ring-2 hover:ring-orange-300 transition"
                      @click="router.push(`/user/${selectedConversation.userId}`)"
                      title="查看用户主页"
                    >
                    <div class="flex-1">
                      <div class="font-medium text-gray-800 text-sm">{{ selectedConversation.nickname }}</div>
                      <div :class="[
                        'text-[10px] flex items-center gap-1',
                        onlineStatus[selectedConversation.userId] ? 'text-green-500' : 'text-gray-400'
                      ]">
                        <div :class="[
                          'w-1.5 h-1.5 rounded-full',
                          onlineStatus[selectedConversation.userId] ? 'bg-green-500' : 'bg-gray-300'
                        ]"></div>
                        {{ onlineStatus[selectedConversation.userId] ? '在线' : '离线' }}
                      </div>
                    </div>
                  </div>
                  <!-- 消息列表 -->
                  <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-3">
                    <div v-if="loadingMessages" class="flex justify-center py-4">
                      <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-400"></div>
                    </div>
                    <div v-else-if="messages.length === 0" class="text-center text-gray-400 py-10 text-sm">
                      暂无消息，打个招呼吧！
                    </div>
                    <div v-for="msg in messages" :key="msg.id" :class="['flex', msg.isMine ? 'justify-end' : 'justify-start']">
                      <!-- 对方头像 -->
                      <img 
                        v-if="!msg.isMine" 
                        :src="getAvatarUrl(selectedConversation.avatar, selectedConversation.nickname)" 
                        class="w-8 h-8 rounded-full object-cover bg-gray-200 mr-2 flex-shrink-0 self-end mb-4 cursor-pointer hover:ring-2 hover:ring-orange-300 transition"
                        @click="router.push(`/user/${selectedConversation.userId}`)"
                        title="查看用户主页"
                      >
                      <div :class="['max-w-[70%] flex flex-col', msg.isMine ? 'items-end' : 'items-start']">
                        <div class="flex items-center gap-1.5">
                          <div v-if="msg.isMine && msg.status === 'fail'" class="text-red-500"><AlertCircle class="w-3.5 h-3.5" /></div>
                          <div :class="['px-3 py-2 rounded-2xl text-sm', msg.isMine ? 'bg-blue-500 text-white rounded-br-sm' : 'bg-white text-gray-800 rounded-bl-sm border border-gray-100']">
                            {{ msg.content }}
                          </div>
                        </div>
                        <div class="text-[10px] mt-1 text-gray-400">{{ msg.time }}</div>
                      </div>
                      <!-- 我的头像 -->
                      <img 
                        v-if="msg.isMine" 
                        :src="getAvatarUrl(userStore.user?.avatar, userStore.user?.nickname || userStore.user?.username)" 
                        class="w-8 h-8 rounded-full object-cover bg-gray-200 ml-2 flex-shrink-0 self-end mb-4"
                      >
                    </div>
                  </div>
                  <!-- 输入框 -->
                  <div class="p-3 bg-white border-t border-gray-100">
                    <div class="flex items-center gap-2">
                      <input 
                        v-model="newMessage"
                        placeholder="请输入消息内容" 
                        class="flex-1 px-3 py-2 bg-gray-50 border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-1 focus:ring-blue-300"
                        @keydown.enter.prevent="handleSendMessage"
                      >
                      <button 
                        @click="handleSendMessage"
                        :disabled="!newMessage.trim()"
                        :class="['px-4 py-2 rounded-lg text-sm font-medium transition', newMessage.trim() ? 'bg-blue-500 text-white hover:bg-blue-600' : 'bg-gray-100 text-gray-400 cursor-not-allowed']"
                      >发送</button>
                    </div>
                  </div>
                </template>
              </div>
            </div>
          </template>

          <!-- ========== 我的评论 ========== -->
          <template v-else-if="activeNav === 'myComments'">
            <div class="flex-1 flex flex-col">
              <div class="p-4 border-b border-gray-100 bg-white flex items-center justify-between">
                <h3 class="font-medium text-gray-800">我的评论</h3>
                <div class="flex items-center gap-2">
                  <button 
                    v-if="isSelectMode && selectedCommentIds.length > 0"
                    @click="handleDeleteComments"
                    class="px-3 py-1.5 bg-red-500 text-white text-sm rounded-lg hover:bg-red-600"
                  >删除 ({{ selectedCommentIds.length }})</button>
                  <button 
                    v-if="isSelectMode"
                    @click="selectAllComments"
                    class="px-3 py-1.5 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200"
                  >{{ selectedCommentIds.length === myComments.length ? '取消全选' : '全选' }}</button>
                  <button 
                    @click="toggleSelectMode"
                    :class="['px-3 py-1.5 text-sm rounded-lg', isSelectMode ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
                  >{{ isSelectMode ? '取消' : '管理' }}</button>
                </div>
              </div>
              <div class="flex-1 overflow-y-auto">
                <div v-if="loadingMyComments" class="flex justify-center py-10">
                  <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
                </div>
                <div v-else-if="myComments.length === 0" class="text-center text-gray-400 py-20">
                  <Edit3 class="w-12 h-12 mx-auto mb-3 opacity-30" />
                  <p>暂无评论</p>
                </div>
                <div v-else class="divide-y divide-gray-100">
                  <div v-for="comment in myComments" :key="comment.id" class="p-4 bg-white hover:bg-gray-50 transition">
                    <div class="flex items-start gap-3">
                      <div 
                        v-if="isSelectMode"
                        @click="toggleSelectComment(comment.id)"
                        :class="['w-5 h-5 rounded border-2 flex items-center justify-center cursor-pointer flex-shrink-0 mt-1', selectedCommentIds.includes(comment.id) ? 'bg-blue-500 border-blue-500' : 'border-gray-300']"
                      >
                        <Check v-if="selectedCommentIds.includes(comment.id)" class="w-3 h-3 text-white" />
                      </div>
                      <img 
                        v-if="comment.recipeCoverImage"
                        :src="comment.recipeCoverImage" 
                        class="w-16 h-16 rounded-lg object-cover flex-shrink-0"
                      >
                      <div v-else class="w-16 h-16 rounded-lg bg-gray-100 flex items-center justify-center flex-shrink-0">
                        <ChefHat class="w-6 h-6 text-gray-400" />
                      </div>
                      <div class="flex-1 min-w-0">
                        <div class="text-sm text-gray-500 mb-1">
                          <template v-if="comment.parentId">
                            回复了 <span class="text-orange-500">{{ comment.replyToUserName || '用户' }}</span> 在 
                          </template>
                          <template v-else>
                            评论于 
                          </template>
                          <span class="text-blue-500 cursor-pointer hover:underline" @click="router.push(`/recipe/${comment.recipeId}`)">{{ comment.recipeTitle }}</span>
                        </div>
                        <p v-if="comment.parentId && comment.parentContent" class="text-xs text-gray-400 mb-1 line-clamp-1">
                          原评论：{{ comment.parentContent }}
                        </p>
                        <p class="text-gray-800 text-sm line-clamp-2">{{ comment.content }}</p>
                        <div class="flex items-center gap-4 mt-2 text-xs text-gray-400">
                          <span>{{ formatTime(comment.createTime) }}</span>
                          <span class="flex items-center gap-1"><Heart class="w-3 h-3" /> {{ comment.likeCount }}</span>
                          <span v-if="!comment.parentId" class="flex items-center gap-1"><MessageCircle class="w-3 h-3" /> {{ comment.replyCount }}</span>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- ========== 回复我的 ========== -->
          <template v-else-if="activeNav === 'replies'">
            <div class="flex-1 overflow-y-auto">
              <div class="p-4 border-b border-gray-100 bg-white">
                <h3 class="font-medium text-gray-800">回复我的</h3>
              </div>
              <div v-if="loadingReplies" class="flex justify-center py-10">
                <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
              </div>
              <div v-else-if="replies.length === 0" class="text-center text-gray-400 py-20">
                <MessageCircle class="w-12 h-12 mx-auto mb-3 opacity-30" />
                <p>暂无回复</p>
              </div>
              <div v-else class="divide-y divide-gray-100">
                <div v-for="reply in replies" :key="reply.id" class="p-4 bg-white hover:bg-gray-50 transition">
                  <div class="flex items-start gap-3">
                    <img 
                      :src="getAvatarUrl(reply.replyUserAvatar, reply.replyUserName)" 
                      class="w-10 h-10 rounded-full object-cover flex-shrink-0"
                    >
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <span class="font-medium text-gray-800">{{ reply.replyUserName }}</span>
                        <span class="text-xs text-gray-400">回复了我的评论</span>
                      </div>
                      <p class="text-sm text-gray-700 mb-2">{{ reply.content }}</p>
                      <div class="text-xs text-gray-400 bg-gray-50 px-3 py-2 rounded-lg">
                        <span class="text-gray-500">我的评论：</span>{{ reply.myCommentContent }}
                      </div>
                      <div class="flex items-center justify-between mt-2">
                        <span class="text-xs text-gray-400">{{ formatTime(reply.createTime) }}</span>
                        <button @click="router.push(`/recipe/${reply.recipeId}`)" class="text-xs text-blue-500 hover:text-blue-600">查看原文 →</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- ========== 收到的赞 ========== -->
          <template v-else-if="activeNav === 'likes'">
            <div class="flex h-full">
              <!-- 点赞列表 -->
              <div :class="['flex-1 flex flex-col', selectedLike ? 'hidden md:flex md:w-1/2 md:border-r md:border-gray-100' : 'flex']">
                <div class="p-4 border-b border-gray-100 bg-white">
                  <h3 class="font-medium text-gray-800">收到的赞</h3>
                </div>
                <div class="flex-1 overflow-y-auto">
                  <div v-if="loadingLikes" class="flex justify-center py-10">
                    <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
                  </div>
                  <div v-else-if="likes.length === 0" class="text-center text-gray-400 py-20">
                    <Heart class="w-12 h-12 mx-auto mb-3 opacity-30" />
                    <p>暂无点赞</p>
                  </div>
                  <div v-else class="divide-y divide-gray-100">
                    <div 
                      v-for="like in likes" 
                      :key="like.commentId" 
                      @click="openLikeDetail(like)"
                      :class="['p-4 bg-white hover:bg-gray-50 transition cursor-pointer', selectedLike?.commentId === like.commentId ? 'bg-blue-50' : '']"
                    >
                      <div class="flex items-start gap-3">
                        <!-- 点赞者头像组 -->
                        <div class="flex -space-x-2 flex-shrink-0">
                          <img 
                            v-for="(liker, idx) in (like.likers || []).slice(0, 3)" 
                            :key="liker.userId"
                            :src="getAvatarUrl(liker.avatar, liker.nickname)" 
                            :class="['w-8 h-8 rounded-full border-2 border-white object-cover', idx > 0 ? '-ml-2' : '']"
                          >
                        </div>
                        <div class="flex-1 min-w-0">
                          <div class="flex items-center gap-1 flex-wrap mb-1">
                            <span class="font-medium text-gray-800 text-sm">{{ like.likers?.[0]?.nickname }}</span>
                            <span v-if="like.likeCount > 1" class="text-xs text-gray-500">
                              等{{ like.likeCount }}人
                            </span>
                            <span class="text-xs text-gray-400">赞了我的评论</span>
                          </div>
                          <p class="text-sm text-gray-600 truncate">{{ like.commentContent }}</p>
                          <div class="flex items-center gap-2 mt-1">
                            <span v-if="like.recipeTitle" class="text-xs text-orange-500 bg-orange-50 px-1.5 py-0.5 rounded">{{ like.recipeTitle }}</span>
                            <span class="text-xs text-gray-400">{{ formatTime(like.latestLikeTime) }}</span>
                          </div>
                        </div>
                        <ArrowRight class="w-4 h-4 text-gray-300 flex-shrink-0 mt-1" />
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 点赞详情 -->
              <div v-if="selectedLike" :class="['flex-1 flex flex-col', mobileView === 'detail' ? 'flex' : 'hidden md:flex']">
                <div class="p-4 border-b border-gray-100 bg-white">
                  <div class="flex items-center gap-2">
                    <button @click="closeLikeDetail" class="md:hidden p-1 hover:bg-gray-100 rounded">
                      <ArrowLeft class="w-4 h-4 text-gray-600" />
                    </button>
                    <span class="text-sm text-gray-500">收到的赞</span>
                    <span class="text-gray-300">></span>
                    <span class="text-sm font-medium text-gray-800">点赞详情</span>
                  </div>
                </div>
                <div class="p-4 bg-white border-b border-gray-100">
                  <div class="text-sm text-gray-500 mb-1">评论：</div>
                  <p class="text-gray-800">{{ selectedLike.commentContent }}</p>
                  <div v-if="selectedLike.recipeTitle" class="mt-2">
                    <span class="text-xs text-gray-400">来自菜谱：</span>
                    <span class="text-xs text-orange-500 cursor-pointer hover:underline" @click="$router.push(`/recipe/${selectedLike.recipeId}`)">{{ selectedLike.recipeTitle }}</span>
                  </div>
                </div>
                <div class="flex-1 overflow-y-auto p-4 space-y-3">
                  <div v-if="loadingLikeDetail" class="flex justify-center py-4">
                    <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-400"></div>
                  </div>
                  <div v-else v-for="item in likeDetail" :key="item.likers?.[0]?.userId" class="flex items-center gap-3 p-3 bg-white rounded-xl">
                    <img 
                      :src="getAvatarUrl(item.likers?.[0]?.avatar, item.likers?.[0]?.nickname)" 
                      class="w-10 h-10 rounded-full object-cover flex-shrink-0"
                    >
                    <div class="flex-1">
                      <div class="font-medium text-gray-800 text-sm">{{ item.likers?.[0]?.nickname }}</div>
                      <div class="text-xs text-gray-400">{{ formatTime(item.likers?.[0]?.likeTime) }}</div>
                    </div>
                    <span class="text-xs text-pink-500">赞了我</span>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- ========== 系统通知 ========== -->
          <template v-else-if="activeNav === 'system'">
            <div class="flex-1 overflow-y-auto">
              <div class="p-4 border-b border-gray-100 bg-white">
                <h3 class="font-medium text-gray-800">系统通知</h3>
              </div>
              <div v-if="loadingSystem" class="flex justify-center py-10">
                <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
              </div>
              <div v-else-if="systemNotifications.length === 0" class="text-center text-gray-400 py-20">
                <Bell class="w-12 h-12 mx-auto mb-3 opacity-30" />
                <p>暂无系统通知</p>
              </div>
              <div v-else class="divide-y divide-gray-100">
                <div v-for="notif in systemNotifications" :key="notif.id" 
                     :class="['p-4 bg-white hover:bg-gray-50 transition', !notif.read ? 'bg-blue-50/30' : '']">
                  <div class="flex items-start gap-3">
                    <div :class="['p-2 rounded-lg flex-shrink-0', getSystemColor(notif.type)]">
                      <component :is="getSystemIcon(notif.type)" class="w-5 h-5" />
                    </div>
                    <div class="flex-1 min-w-0">
                      <div class="flex items-center gap-2 mb-1">
                        <span class="font-medium text-gray-800">{{ notif.title }}</span>
                        <div v-if="!notif.read" class="w-1.5 h-1.5 bg-blue-500 rounded-full"></div>
                      </div>
                      <p class="text-sm text-gray-600 mb-2">{{ notif.content }}</p>
                      <div class="flex items-center justify-between">
                        <span class="text-xs text-gray-400">{{ notif.time }}</span>
                        <button 
                          v-if="notif.type === 'RECIPE_APPROVED' || notif.type === 'RECIPE_REJECTED'"
                          @click="router.push('/profile')"
                          class="text-xs text-blue-500 hover:text-blue-600"
                        >查看详情 →</button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

        </div>
      </div>
    </div>
  </div>
</template>
