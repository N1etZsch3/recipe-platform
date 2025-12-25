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
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { showToast } = useToast()

// ============= 状态定义 =============
const loadingList = ref(false)
const selectedItem = ref(null)
const searchKeyword = ref('')
const mobileView = ref('list')

// 私信相关
const conversations = ref([])
const messages = ref([])
const loadingMessages = ref(false)
const messagesContainer = ref(null)
const newMessage = ref('')
const onlineStatus = ref({})

// 回复我的
const replies = ref([])

// 收到的赞
const likes = ref([])
const likeDetail = ref([])
const loadingLikeDetail = ref(false)

// 系统通知
const systemNotifications = ref([])

// ============= 类型配置 =============
const typeConfig = {
  message: { label: '私信', color: 'bg-blue-500', textColor: 'text-blue-600', icon: Mail },
  reply: { label: '回复', color: 'bg-green-500', textColor: 'text-green-600', icon: MessageCircle },
  like: { label: '点赞', color: 'bg-pink-500', textColor: 'text-pink-600', icon: Heart },
  system: { label: '系统', color: 'bg-purple-500', textColor: 'text-purple-600', icon: Bell }
}

// ============= 统一消息列表（核心）=============
const unifiedList = computed(() => {
  const list = []
  
  // 1. 私信消息
  conversations.value.forEach(c => {
    list.push({
      id: `message-${c.userId}`,
      type: 'message',
      userId: c.userId,
      avatar: c.avatar,
      title: c.nickname,
      subtitle: c.lastMessage || '暂无消息',
      time: c.time,
      timestamp: c.rawTime ? new Date(c.rawTime).getTime() : 0,
      unread: c.unread || 0,
      raw: c
    })
  })
  
  // 2. 回复我的
  replies.value.forEach(r => {
    list.push({
      id: `reply-${r.id}`,
      type: 'reply',
      avatar: r.replyUserAvatar,
      title: r.replyUserName,
      subtitle: r.content,
      time: formatTime(r.createTime),
      timestamp: r.createTime ? new Date(r.createTime).getTime() : 0,
      unread: 0,
      raw: r
    })
  })
  
  // 3. 收到的赞
  likes.value.forEach(l => {
    list.push({
      id: `like-${l.commentId}`,
      type: 'like',
      avatar: l.likers?.[0]?.avatar,
      title: l.likers?.[0]?.nickname + (l.likeCount > 1 ? ` 等${l.likeCount}人` : ''),
      subtitle: `赞了你的评论: ${l.commentContent}`,
      time: formatTime(l.latestLikeTime),
      timestamp: l.latestLikeTime ? new Date(l.latestLikeTime).getTime() : 0,
      unread: 0,
      raw: l
    })
  })
  
  // 4. 系统通知
  systemNotifications.value.forEach(n => {
    list.push({
      id: `system-${n.id}`,
      type: 'system',
      avatar: null,
      title: n.title,
      subtitle: n.content,
      time: n.time,
      timestamp: n.rawTime ? new Date(n.rawTime).getTime() : 0,
      unread: n.read ? 0 : 1,
      raw: n
    })
  })
  
  // 按时间倒序排列
  return list.sort((a, b) => b.timestamp - a.timestamp)
})

// 搜索过滤
const filteredList = computed(() => {
  if (!searchKeyword.value) return unifiedList.value
  const kw = searchKeyword.value.toLowerCase()
  return unifiedList.value.filter(item => 
    item.title?.toLowerCase().includes(kw) || 
    item.subtitle?.toLowerCase().includes(kw)
  )
})

// ============= 工具方法 =============
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



// ============= 数据加载 =============
const loadAllData = async () => {
  loadingList.value = true
  try {
    await Promise.all([
      loadConversations(),
      loadReplies(),
      loadLikes(),
      loadSystemNotifications()
    ])
  } finally {
    loadingList.value = false
  }
}

const loadConversations = async () => {
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
    if (conversations.value.length > 0) {
      const userIds = conversations.value.map(c => c.userId)
      await fetchOnlineStatus(userIds)
    }
  } catch (error) {
    console.error('加载会话失败', error)
  }
}

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

const loadReplies = async () => {
  try {
    const res = await getRepliesForMe({ page: 1, size: 50 })
    replies.value = res.records || []
  } catch (error) {
    console.error('加载回复失败', error)
  }
}

const loadLikes = async () => {
  try {
    const res = await getLikesForMe({ page: 1, size: 50 })
    likes.value = res.records || []
  } catch (error) {
    console.error('加载点赞失败', error)
  }
}

const loadSystemNotifications = async () => {
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
      rawTime: n.receivedAt,
      read: n.read
    }))
}

// ============= 选择消息项 =============
const selectItem = async (item) => {
  selectedItem.value = item
  mobileView.value = 'detail'
  
  if (item.type === 'message') {
    notificationStore.setCurrentChatUser(item.userId)
    fetchOnlineStatus([item.userId])
    await loadMessages(item.userId)
    if (item.unread > 0) {
      markRead(item.userId).then(() => {
        const conv = conversations.value.find(c => c.userId === item.userId)
        if (conv) conv.unread = 0
      }).catch(e => console.error(e))
    }
  } else if (item.type === 'like') {
    await loadLikeDetail(item.raw)
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

const loadLikeDetail = async (like) => {
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

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// ============= 发送消息 =============
const handleSendMessage = async () => {
  if (!newMessage.value.trim() || !selectedItem.value || selectedItem.value.type !== 'message') return
  
  const content = newMessage.value
  const receiverId = selectedItem.value.userId
  
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
    const conv = conversations.value.find(c => c.userId === receiverId)
    if (conv) {
      conv.lastMessage = content
      conv.time = '刚刚'
      conv.rawTime = new Date().toISOString()
    }
  } catch (error) {
    reactiveMsg.status = 'fail'
    showToast(error.message || '发送失败', 'error')
  }
}

// ============= 返回列表 =============
const backToList = () => {
  if (mobileView.value === 'detail') {
    mobileView.value = 'list'
    selectedItem.value = null
    notificationStore.clearCurrentChatUser()
    likeDetail.value = []
  }
}

// ============= 系统通知辅助函数 =============
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

// ============= 监听新消息 =============
watch(() => notificationStore.latestNotification, async (notification) => {
  if (!notification || notification.type !== 'NEW_MESSAGE') return
  
  const senderId = notification.senderId
  const conv = conversations.value.find(c => c.userId === senderId)
  
  if (conv) {
    conv.lastMessage = notification.content
    conv.time = '刚刚'
    conv.rawTime = new Date().toISOString()
    if (!selectedItem.value || selectedItem.value.type !== 'message' || selectedItem.value.userId !== senderId) {
      conv.unread = (conv.unread || 0) + 1
    }
  } else {
    await loadConversations()
  }
  
  if (selectedItem.value && selectedItem.value.type === 'message' && selectedItem.value.userId === senderId) {
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

// ============= 生命周期 =============
onMounted(async () => {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  await loadAllData()
  
  window.addEventListener('admin-user-status', handleUserStatusChange)
  
  const chatWith = route.query.chatWith
  const chatName = route.query.chatName
  if (chatWith) {
    const existingConv = conversations.value.find(c => c.userId == chatWith)
    if (existingConv) {
      const item = unifiedList.value.find(i => i.type === 'message' && i.userId == chatWith)
      if (item) selectItem(item)
    } else {
      const newConv = {
        id: parseInt(chatWith),
        userId: parseInt(chatWith),
        nickname: chatName || '用户',
        avatar: null,
        lastMessage: '',
        time: '',
        rawTime: new Date().toISOString(),
        unread: 0
      }
      conversations.value.unshift(newConv)
      await nextTick()
      const item = unifiedList.value.find(i => i.type === 'message' && i.userId == chatWith)
      if (item) selectItem(item)
    }
    router.replace({ path: '/messages' })
  }
})

onUnmounted(() => {
  notificationStore.clearCurrentChatUser()
  window.removeEventListener('admin-user-status', handleUserStatusChange)
})

let statusDebounceTimer = null
const handleUserStatusChange = (event) => {
  const { type, relatedId } = event.detail
  if (!relatedId) return
  
  if (statusDebounceTimer) {
    clearTimeout(statusDebounceTimer)
  }
  
  statusDebounceTimer = setTimeout(() => {
    onlineStatus.value[relatedId] = type === 'USER_ONLINE'
  }, 300)
}
</script>

<template>
  <div class="min-h-[calc(100vh-64px)] py-4">
    <div class="max-w-4xl mx-auto px-4">
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex h-[calc(100vh-64px-32px)]">
        
        <!-- 左侧统一消息列表 -->
        <div :class="[
          'border-r border-gray-200 flex-shrink-0 flex flex-col bg-gray-50',
          'w-full md:w-80',
          selectedItem && mobileView === 'detail' ? 'hidden md:flex' : 'flex'
        ]">
          <!-- 头部 -->
          <div class="p-3 bg-white border-b border-gray-100">
            <div class="flex items-center justify-between mb-3">
              <span class="font-bold text-gray-800 text-lg">消息</span>
              <span class="text-xs text-gray-400">{{ filteredList.length }} 条消息</span>
            </div>
            <div class="relative">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input 
                v-model="searchKeyword"
                type="text" 
                placeholder="搜索消息..." 
                class="w-full pl-9 pr-3 py-2 bg-gray-100 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 focus:bg-white transition"
              >
            </div>
          </div>
          
          <!-- 消息列表 -->
          <div class="flex-1 overflow-y-auto">
            <div v-if="loadingList" class="flex justify-center py-8">
              <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
            </div>
            <div v-else-if="filteredList.length === 0" class="text-center text-gray-400 py-12 text-sm">
              <Mail class="w-12 h-12 mx-auto mb-3 opacity-30" />
              <p>暂无消息</p>
            </div>
            <div 
              v-else
              v-for="item in filteredList" 
              :key="item.id"
              @click="selectItem(item)"
              :class="[
                'flex items-center gap-3 px-3 py-3 cursor-pointer transition border-b border-gray-100 bg-white hover:bg-gray-50',
                selectedItem?.id === item.id ? 'bg-blue-50 hover:bg-blue-50' : ''
              ]"
            >
              <!-- 头像 -->
              <div class="relative flex-shrink-0">
                <template v-if="item.type === 'system'">
                  <div :class="['w-11 h-11 rounded-full flex items-center justify-center', getSystemColor(item.raw?.type)]">
                    <component :is="getSystemIcon(item.raw?.type)" class="w-5 h-5" />
                  </div>
                </template>
                <template v-else>
                  <UserAvatar 
                    :src="item.avatar" 
                    :name="item.title"
                    class="w-11 h-11 bg-gray-200"
                  />
                </template>
                <!-- 未读红点 -->
                <div 
                  v-if="item.unread > 0" 
                  class="absolute -top-1 -right-1 min-w-[18px] h-[18px] bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center font-bold px-1"
                >
                  {{ item.unread > 99 ? '99+' : item.unread }}
                </div>
                <!-- 在线状态（仅私信） -->
                <div 
                  v-if="item.type === 'message' && onlineStatus[item.userId]" 
                  class="absolute bottom-0 right-0 w-3 h-3 bg-green-500 rounded-full border-2 border-white"
                ></div>
              </div>
              
              <!-- 内容 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2 mb-0.5">
                  <!-- 类型标签 -->
                  <span :class="['text-[10px] px-1.5 py-0.5 rounded text-white font-medium', typeConfig[item.type].color]">
                    {{ typeConfig[item.type].label }}
                  </span>
                  <span class="font-medium text-gray-800 text-sm truncate flex-1">{{ item.title }}</span>
                </div>
                <p class="text-xs text-gray-500 truncate">{{ item.subtitle }}</p>
              </div>
              
              <!-- 时间 -->
              <div class="flex-shrink-0 text-right">
                <span class="text-[10px] text-gray-400">{{ item.time }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧内容区 -->
        <div :class="[
          'flex-1 flex flex-col min-w-0 bg-gray-50',
          !selectedItem && mobileView !== 'detail' ? 'hidden md:flex' : 'flex'
        ]">
          <!-- 未选中状态 -->
          <div v-if="!selectedItem" class="flex-1 flex flex-col items-center justify-center text-gray-400">
            <div class="w-24 h-24 mb-4 bg-gray-100 rounded-full flex items-center justify-center">
              <MessageSquare class="w-10 h-10 text-gray-300" />
            </div>
            <p class="text-base font-medium mb-1">选择一个对话</p>
            <p class="text-sm">开始查看消息详情</p>
          </div>
          
          <!-- ===== 私信聊天界面 ===== -->
          <template v-else-if="selectedItem.type === 'message'">
            <!-- 聊天头部 -->
            <div class="flex items-center gap-3 px-4 py-3 bg-white border-b border-gray-100">
              <button @click="backToList" class="md:hidden p-1.5 hover:bg-gray-100 rounded-lg transition">
                <ArrowLeft class="w-5 h-5 text-gray-600" />
              </button>
              <UserAvatar 
                :src="selectedItem.avatar" 
                :name="selectedItem.title"
                class="w-10 h-10 bg-gray-200"
              />
              <div class="flex-1">
                <div class="font-medium text-gray-800">{{ selectedItem.title }}</div>
                <div :class="[
                  'text-xs flex items-center gap-1',
                  onlineStatus[selectedItem.userId] ? 'text-green-500' : 'text-gray-400'
                ]">
                  <div :class="[
                    'w-1.5 h-1.5 rounded-full',
                    onlineStatus[selectedItem.userId] ? 'bg-green-500' : 'bg-gray-300'
                  ]"></div>
                  {{ onlineStatus[selectedItem.userId] ? '在线' : '离线' }}
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
                <UserAvatar 
                  v-if="!msg.isMine" 
                  :src="selectedItem.avatar" 
                  :name="selectedItem.title"
                  class="w-8 h-8 bg-gray-200 mr-2 flex-shrink-0 self-end mb-4"
                />
                <div :class="['max-w-[70%] flex flex-col', msg.isMine ? 'items-end' : 'items-start']">
                  <div class="flex items-center gap-1.5">
                    <div v-if="msg.isMine && msg.status === 'fail'" class="text-red-500"><AlertCircle class="w-3.5 h-3.5" /></div>
                    <div :class="['px-3 py-2 rounded-2xl text-sm', msg.isMine ? 'bg-blue-500 text-white rounded-br-sm' : 'bg-white text-gray-800 rounded-bl-sm border border-gray-100']">
                      {{ msg.content }}
                    </div>
                  </div>
                  <div class="text-[10px] mt-1 text-gray-400">{{ msg.time }}</div>
                </div>
                <UserAvatar 
                  v-if="msg.isMine" 
                  :src="userStore.user?.avatar" 
                  :name="userStore.user?.nickname || userStore.user?.username"
                  class="w-8 h-8 bg-gray-200 ml-2 flex-shrink-0 self-end mb-4"
                />
              </div>
            </div>
            
            <!-- 输入框 -->
            <div class="p-3 bg-white border-t border-gray-100">
              <div class="flex items-center gap-2">
                <input 
                  v-model="newMessage"
                  placeholder="请输入消息内容" 
                  class="flex-1 px-4 py-2.5 bg-gray-100 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-blue-300 focus:bg-white transition"
                  @keydown.enter.prevent="handleSendMessage"
                >
                <button 
                  @click="handleSendMessage"
                  :disabled="!newMessage.trim()"
                  :class="['px-5 py-2.5 rounded-full text-sm font-medium transition', newMessage.trim() ? 'bg-blue-500 text-white hover:bg-blue-600' : 'bg-gray-200 text-gray-400 cursor-not-allowed']"
                >发送</button>
              </div>
            </div>
          </template>
          
          <!-- ===== 回复详情 ===== -->
          <template v-else-if="selectedItem.type === 'reply'">
            <div class="flex items-center gap-3 px-4 py-3 bg-white border-b border-gray-100">
              <button @click="backToList" class="md:hidden p-1.5 hover:bg-gray-100 rounded-lg transition">
                <ArrowLeft class="w-5 h-5 text-gray-600" />
              </button>
              <span class="font-medium text-gray-800">回复详情</span>
            </div>
            <div class="flex-1 overflow-y-auto p-4">
              <div class="bg-white rounded-xl p-4 shadow-sm">
                <div class="flex items-start gap-3 mb-4">
                  <UserAvatar 
                    :src="selectedItem.raw.replyUserAvatar" 
                    :name="selectedItem.raw.replyUserName"
                    class="w-12 h-12"
                  />
                  <div class="flex-1">
                    <div class="font-medium text-gray-800 mb-1">{{ selectedItem.raw.replyUserName }}</div>
                    <div class="text-xs text-gray-400">{{ formatTime(selectedItem.raw.createTime) }}</div>
                  </div>
                </div>
                <p class="text-gray-700 mb-4">{{ selectedItem.raw.content }}</p>
                <div class="bg-gray-50 rounded-lg p-3 text-sm">
                  <span class="text-gray-500">我的评论：</span>
                  <span class="text-gray-700">{{ selectedItem.raw.myCommentContent }}</span>
                </div>
                <button 
                  @click="router.push(`/recipe/${selectedItem.raw.recipeId}`)" 
                  class="mt-4 text-sm text-blue-500 hover:text-blue-600 flex items-center gap-1"
                >
                  查看原文 <ArrowRight class="w-4 h-4" />
                </button>
              </div>
            </div>
          </template>
          
          <!-- ===== 点赞详情 ===== -->
          <template v-else-if="selectedItem.type === 'like'">
            <div class="flex items-center gap-3 px-4 py-3 bg-white border-b border-gray-100">
              <button @click="backToList" class="md:hidden p-1.5 hover:bg-gray-100 rounded-lg transition">
                <ArrowLeft class="w-5 h-5 text-gray-600" />
              </button>
              <span class="font-medium text-gray-800">点赞详情</span>
            </div>
            <div class="flex-1 overflow-y-auto p-4">
              <div class="bg-white rounded-xl p-4 shadow-sm mb-4">
                <div class="text-sm text-gray-500 mb-2">我的评论</div>
                <p class="text-gray-800">{{ selectedItem.raw.commentContent }}</p>
                <div v-if="selectedItem.raw.recipeTitle" class="mt-3">
                  <span class="text-xs text-gray-400">来自菜谱：</span>
                  <span 
                    class="text-xs text-orange-500 cursor-pointer hover:underline" 
                    @click="router.push(`/recipe/${selectedItem.raw.recipeId}`)"
                  >{{ selectedItem.raw.recipeTitle }}</span>
                </div>
              </div>
              <div class="text-sm text-gray-500 mb-3">点赞的人 ({{ selectedItem.raw.likeCount }})</div>
              <div v-if="loadingLikeDetail" class="flex justify-center py-4">
                <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-pink-400"></div>
              </div>
              <div v-else class="space-y-2">
                <div v-for="item in likeDetail" :key="item.likers?.[0]?.userId" class="flex items-center gap-3 p-3 bg-white rounded-xl">
                  <UserAvatar 
                    :src="item.likers?.[0]?.avatar" 
                    :name="item.likers?.[0]?.nickname"
                    class="w-10 h-10"
                  />
                  <div class="flex-1">
                    <div class="font-medium text-gray-800 text-sm">{{ item.likers?.[0]?.nickname }}</div>
                    <div class="text-xs text-gray-400">{{ formatTime(item.likers?.[0]?.likeTime) }}</div>
                  </div>
                  <Heart class="w-4 h-4 text-pink-500 fill-pink-500" />
                </div>
              </div>
            </div>
          </template>
          
          <!-- ===== 系统通知详情 ===== -->
          <template v-else-if="selectedItem.type === 'system'">
            <div class="flex items-center gap-3 px-4 py-3 bg-white border-b border-gray-100">
              <button @click="backToList" class="md:hidden p-1.5 hover:bg-gray-100 rounded-lg transition">
                <ArrowLeft class="w-5 h-5 text-gray-600" />
              </button>
              <span class="font-medium text-gray-800">系统通知</span>
            </div>
            <div class="flex-1 overflow-y-auto p-4">
              <div class="bg-white rounded-xl p-5 shadow-sm">
                <div class="flex items-start gap-4 mb-4">
                  <div :class="['p-3 rounded-xl', getSystemColor(selectedItem.raw.type)]">
                    <component :is="getSystemIcon(selectedItem.raw.type)" class="w-6 h-6" />
                  </div>
                  <div class="flex-1">
                    <h3 class="font-medium text-gray-800 text-lg mb-1">{{ selectedItem.raw.title }}</h3>
                    <div class="text-xs text-gray-400">{{ selectedItem.time }}</div>
                  </div>
                </div>
                <p class="text-gray-600 leading-relaxed">{{ selectedItem.raw.content }}</p>
                <button 
                  v-if="selectedItem.raw.type === 'RECIPE_APPROVED' || selectedItem.raw.type === 'RECIPE_REJECTED'"
                  @click="router.push('/profile')"
                  class="mt-4 text-sm text-blue-500 hover:text-blue-600 flex items-center gap-1"
                >
                  查看详情 <ArrowRight class="w-4 h-4" />
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
