<script setup>
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useNotificationStore } from '../stores/notification'
import { useToast } from '../components/Toast.vue'
import { MessageSquare, Send, ArrowLeft, User, Search, MoreVertical, AlertCircle } from 'lucide-vue-next'
import { getConversations, getMessages, sendMessage as apiSendMessage, markRead } from '@/api/social'

const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { showToast } = useToast()

// 会话列表
const conversations = ref([])
const loadingConversations = ref(false)

// 当前选中的会话
const selectedConversation = ref(null)

// 消息列表
const messages = ref([])
const loadingMessages = ref(false)
const messagesContainer = ref(null)

// 新消息输入
const newMessage = ref('')

// 搜索关键词
const searchKeyword = ref('')

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
  const isToday = date.toDateString() === now.toDateString()
  if (isToday) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
}

// 加载会话列表
const loadConversations = async () => {
  loadingConversations.value = true
  try {
    const res = await getConversations()
    conversations.value = res.map(c => ({
      id: c.userId, // 用对方ID作为唯一标识
      userId: c.userId,
      nickname: c.nickname,
      avatar: c.avatar,
      lastMessage: c.lastMessage,
      time: formatTime(c.lastTime),
      rawTime: c.lastTime, // 用于排序
      unread: c.unreadCount || 0
    }))
  } catch (error) {
    console.error('加载会话失败', error)
    showToast('加载会话失败', 'error')
  } finally {
    loadingConversations.value = false
  }
}

// 选择会话
const selectConversation = async (conv) => {
  selectedConversation.value = conv
  
  // 设置当前聊天用户 ID，用于判断是否需要显示 Toast
  notificationStore.setCurrentChatUser(conv.userId)
  
  await loadMessages(conv.userId)
  // 标记已读 (前端逻辑，后端可能需要单独的已读接口，这里暂时只清除前端未读数)
  // 实际上点击进入会话，后端应该将该用户的消息标记为已读。
  if (conv.unread > 0) {
      markRead(conv.userId).then(() => {
          conv.unread = 0
      }).catch(e => console.error('标记已读失败', e))
  } else {
      conv.unread = 0
  }
}

// 加载消息记录
const loadMessages = async (userId) => {
  loadingMessages.value = true
  messages.value = []
  try {
    const res = await getMessages(userId, { page: 1, size: 50 })
    // 后端返回的是分页对象，records 是列表
    // 且后端默认是按时间倒序（最新的在前面），前端聊天通常需要正序（旧的在上面）
    const list = res.records || []
    messages.value = list.map(m => ({
      id: m.id,
      senderId: m.senderId,
      content: m.content,
      time: formatTime(m.createTime),
      isMine: m.isMe, // 后端 VO 里的字段
      senderAvatar: m.senderAvatar
    })).reverse()
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败', error)
    showToast('加载消息失败', 'error')
  } finally {
    loadingMessages.value = false
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!newMessage.value.trim() || !selectedConversation.value) return
  
  const content = newMessage.value
  const receiverId = selectedConversation.value.userId
  
  // 临时消息对象，默认状态 sending
  const tempMsg = {
    id: Date.now(),
    senderId: userStore.user?.id,
    content: content,
    time: formatTime(new Date()),
    isMine: true,
    status: 'sending' 
  }

  // 先添加到列表
  messages.value.push(tempMsg)
  newMessage.value = ''
  
  // 获取响应式对象引用，确保后续修改能触发视图更新
  const reactiveMsg = messages.value[messages.value.length - 1]
  
  // 滚动到底部
  await nextTick()
  scrollToBottom()

  try {
    await apiSendMessage({ receiverId, content })
    
    // 发送成功，更新状态
    reactiveMsg.status = 'success'
    
    // 更新会话列表的最后消息
    const convIndex = conversations.value.findIndex(c => c.userId === receiverId)
    if (convIndex > -1) {
      const conv = conversations.value[convIndex]
      conv.lastMessage = content
      conv.time = '刚刚'
      // 把该会话移到最前
      conversations.value.splice(convIndex, 1)
      conversations.value.unshift(conv)
    }
    
  } catch (error) {
    console.error('发送消息失败', error)
    reactiveMsg.status = 'fail'
    // Error object from axios interceptor should have the message
    const errorMsg = error.message || '发送失败'
    showToast(errorMsg, 'error')
  }
}

// 返回会话列表（移动端）
const backToList = () => {
  selectedConversation.value = null
  // 清除当前聊天用户 ID
  notificationStore.clearCurrentChatUser()
}

// 监听 WebSocket 新消息通知（使用 latestNotification 而非 latestToast，确保即使不弹 Toast 也能收到）
watch(() => notificationStore.latestNotification, async (notification) => {
  if (!notification || notification.type !== 'NEW_MESSAGE') return
  
  const senderId = notification.senderId
  console.log('MessagesView: 收到新消息通知, senderId =', senderId)
  
  // 更新会话列表
  const convIndex = conversations.value.findIndex(c => c.userId === senderId)
  if (convIndex > -1) {
    // 会话已存在，更新最后消息
    const conv = conversations.value[convIndex]
    conv.lastMessage = notification.content
    conv.time = '刚刚'
    
    // 如果不是当前选中的会话，增加未读数
    if (!selectedConversation.value || selectedConversation.value.userId !== senderId) {
      conv.unread = (conv.unread || 0) + 1
    }
    
    // 把该会话移到最前
    conversations.value.splice(convIndex, 1)
    conversations.value.unshift(conv)
  } else {
    // 新会话，重新加载会话列表
    await loadConversations()
  }
  
  // 如果正在查看的就是这个用户的会话，自动加载新消息
  if (selectedConversation.value && selectedConversation.value.userId === senderId) {
    // 直接添加新消息到列表（避免完全刷新）
    const newMsg = {
      id: Date.now(),
      senderId: senderId,
      content: notification.content,
      time: formatTime(new Date()),
      isMine: false,
      senderAvatar: notification.senderAvatar
    }
    messages.value.push(newMsg)
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
    
    // 标记已读
    markRead(senderId).catch(e => console.error('标记已读失败', e))
  }
})

onMounted(() => {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  loadConversations()
})

// 离开页面时清除当前聊天用户 ID
onUnmounted(() => {
  notificationStore.clearCurrentChatUser()
})
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-5xl mx-auto py-6 px-4">
      <!-- 页面标题 -->
      <div class="flex items-center gap-3 mb-6">
        <MessageSquare class="w-7 h-7 text-orange-500" />
        <h1 class="text-2xl font-bold text-gray-800">私信</h1>
      </div>

      <!-- 主体容器 -->
      <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden flex h-[600px]">
        
        <!-- 左侧：会话列表 -->
        <div :class="['w-full md:w-80 border-r border-gray-100 flex flex-col', selectedConversation ? 'hidden md:flex' : 'flex']">
          <!-- 搜索框 -->
          <div class="p-4 border-b border-gray-100">
            <div class="relative">
              <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input 
                v-model="searchKeyword"
                type="text" 
                placeholder="搜索联系人..." 
                class="w-full pl-10 pr-4 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-200 focus:border-orange-300 transition"
              >
            </div>
          </div>

          <!-- 会话列表 -->
          <div class="flex-1 overflow-y-auto">
            <div v-if="loadingConversations" class="flex justify-center py-10">
               <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
            </div>
            <div v-else-if="filteredConversations.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400">
              <MessageSquare class="w-12 h-12 mb-3 opacity-30" />
              <p class="text-sm">暂无私信</p>
            </div>
            <div 
              v-else
              v-for="conv in filteredConversations" 
              :key="conv.id"
              @click="selectConversation(conv)"
              :class="['flex items-center gap-3 p-4 cursor-pointer transition border-b border-gray-50 hover:bg-orange-50', selectedConversation?.userId === conv.userId ? 'bg-orange-50' : '']"
            >
              <!-- 头像 -->
              <div class="relative flex-shrink-0">
                <div class="w-12 h-12 rounded-full overflow-hidden bg-gray-200">
                  <img v-if="conv.avatar" :src="conv.avatar" class="w-full h-full object-cover">
                  <div v-else class="w-full h-full bg-gradient-to-br from-orange-400 to-red-400 flex items-center justify-center text-white font-bold text-lg shadow-sm">
                    {{ conv.nickname?.charAt(0) }}
                  </div>
                </div>
                <div v-if="conv.unread > 0" class="absolute -top-1 -right-1 w-5 h-5 bg-red-500 text-white text-xs rounded-full flex items-center justify-center font-bold border-2 border-white">
                  {{ conv.unread }}
                </div>
              </div>
              <!-- 信息 -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between mb-1">
                  <span class="font-medium text-gray-800 truncate">{{ conv.nickname }}</span>
                  <span class="text-xs text-gray-400 flex-shrink-0">{{ conv.time }}</span>
                </div>
                <p class="text-sm text-gray-500 truncate">{{ conv.lastMessage }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：聊天区域 -->
        <div :class="['flex-1 flex flex-col', selectedConversation ? 'flex' : 'hidden md:flex']">
          <!-- 未选择会话 -->
          <div v-if="!selectedConversation" class="flex-1 flex flex-col items-center justify-center text-gray-400">
            <MessageSquare class="w-16 h-16 mb-4 opacity-20" />
            <p class="text-lg">选择一个对话开始聊天</p>
          </div>

          <!-- 已选择会话 -->
          <template v-else>
            <!-- 聊天头部 -->
            <div class="flex items-center gap-3 p-4 border-b border-gray-100 bg-white">
              <button @click="backToList" class="md:hidden p-2 hover:bg-gray-100 rounded-full transition">
                <ArrowLeft class="w-5 h-5 text-gray-600" />
              </button>
              <div class="w-10 h-10 rounded-full overflow-hidden bg-gray-200">
                  <img v-if="selectedConversation.avatar" :src="selectedConversation.avatar" class="w-full h-full object-cover">
                  <div v-else class="w-full h-full bg-gradient-to-br from-orange-400 to-red-400 flex items-center justify-center text-white font-bold shadow-sm">
                    {{ selectedConversation.nickname?.charAt(0) }}
                  </div>
              </div>
              <div class="flex-1">
                <div class="font-medium text-gray-800">{{ selectedConversation.nickname }}</div>
                <div class="text-xs text-green-500 flex items-center gap-1">
                   <div class="w-1.5 h-1.5 rounded-full bg-green-500"></div> 在线
                </div>
              </div>
              <button class="p-2 hover:bg-gray-100 rounded-full transition">
                <MoreVertical class="w-5 h-5 text-gray-400" />
              </button>
            </div>

            <!-- 消息列表 -->
            <div ref="messagesContainer" class="flex-1 overflow-y-auto p-4 space-y-4 bg-gray-50/50 scroll-smooth">
              <div v-if="loadingMessages" class="flex justify-center py-4">
                 <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-orange-400"></div>
              </div>
              <div v-else-if="messages.length === 0" class="text-center text-gray-400 py-10 text-sm">
                 暂无消息，打个招呼吧！
              </div>
              <div 
                v-for="msg in messages" 
                :key="msg.id"
                :class="['flex', msg.isMine ? 'justify-end' : 'justify-start']"
              >
                <!-- 对方头像 (仅在左侧显示) -->
                <div v-if="!msg.isMine" class="w-8 h-8 rounded-full overflow-hidden bg-gray-200 mr-2 flex-shrink-0 self-end mb-1">
                    <img v-if="selectedConversation.avatar" :src="selectedConversation.avatar" class="w-full h-full object-cover">
                    <div v-else class="w-full h-full bg-gradient-to-br from-orange-400 to-red-400 flex items-center justify-center text-white text-xs font-bold">
                        {{ selectedConversation.nickname?.charAt(0) }}
                    </div>
                </div>

                <div :class="['max-w-[70%]', msg.isMine ? 'items-end' : 'items-start', 'flex flex-col']">
                  <!-- 发送失败图标 (仅我的消息，显示在左侧) -->
                  <div class="relative w-full flex flex-col" :class="msg.isMine ? 'items-end' : 'items-start'">
                      
                      <div class="flex items-center gap-2">
                           <!-- 失败图标放左边 -->
                           <div v-if="msg.isMine && msg.status === 'fail'" class="text-red-500" title="发送失败">
                                <AlertCircle class="w-4 h-4" />
                           </div>

                           <div :class="['px-4 py-2.5 rounded-2xl text-sm shadow-sm break-words', msg.isMine ? 'bg-orange-500 text-white rounded-br-none' : 'bg-white text-gray-800 rounded-bl-none border border-gray-100']">
                                {{ msg.content }}
                           </div>
                      </div>

                      <div :class="['text-[10px] mt-1 px-1', msg.isMine ? 'text-gray-400' : 'text-gray-400']">
                          {{ msg.time }}
                      </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 输入框 -->
            <div class="p-4 border-t border-gray-100 bg-white">
              <div class="flex items-center gap-3">
                <input 
                  v-model="newMessage"
                  type="text" 
                  placeholder="输入消息..." 
                  class="flex-1 px-4 py-3 bg-gray-50 border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-200 focus:border-orange-300 transition"
                  @keyup.enter="handleSendMessage"
                >
                <button 
                  @click="handleSendMessage"
                  :disabled="!newMessage.trim()"
                  :class="['p-3 rounded-xl transition', newMessage.trim() ? 'bg-orange-500 text-white hover:bg-orange-600 shadow-lg shadow-orange-200 transform active:scale-95' : 'bg-gray-100 text-gray-400 cursor-not-allowed']"
                >
                  <Send class="w-5 h-5" />
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
