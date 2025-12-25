<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useToast } from '@/components/Toast.vue'
import { 
  MessageSquare, Search, ArrowLeft, AlertCircle, ChevronLeft, Mail
} from 'lucide-vue-next'
import { 
  getConversations, getMessages, sendMessage as apiSendMessage, markRead, checkOnlineStatus
} from '@/api/social'
import { formatTime } from './useMessageUtils'
import UserAvatar from '@/components/UserAvatar.vue'

const props = defineProps({
  initialChatWith: { type: [String, Number], default: null },
  initialChatName: { type: String, default: '' }
})

const emit = defineEmits(['back'])

const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { showToast } = useToast()

// 状态
const conversations = ref([])
const loadingConversations = ref(false)
const selectedConversation = ref(null)
const messages = ref([])
const loadingMessages = ref(false)
const messagesContainer = ref(null)
const newMessage = ref('')
const searchKeyword = ref('')
const onlineStatus = ref({})
const conversationListCollapsed = ref(false)
const mobileView = ref('list')

// 过滤后的会话列表
const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  return conversations.value.filter(c => 
    c.nickname?.toLowerCase().includes(searchKeyword.value.toLowerCase())
  )
})

// 加载会话列表
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

// 获取在线状态
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

// 选择会话
const selectConversation = async (conv) => {
  selectedConversation.value = conv
  mobileView.value = 'detail'
  notificationStore.setCurrentChatUser(conv.userId)
  fetchOnlineStatus([conv.userId])
  await loadMessages(conv.userId)
  if (conv.unread > 0) {
    markRead(conv.userId).then(() => conv.unread = 0).catch(e => console.error(e))
  }
}

// 加载消息
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

// 返回列表
const backToList = () => {
  if (mobileView.value === 'detail') {
    mobileView.value = 'list'
    selectedConversation.value = null
    notificationStore.clearCurrentChatUser()
  } else {
    emit('back')
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

// 在线状态监听
let statusDebounceTimer = null
const handleUserStatusChange = (event) => {
  const { type, relatedId } = event.detail
  if (!relatedId) return
  if (statusDebounceTimer) clearTimeout(statusDebounceTimer)
  statusDebounceTimer = setTimeout(() => {
    onlineStatus.value[relatedId] = type === 'USER_ONLINE'
  }, 300)
}

onMounted(async () => {
  await loadConversations()
  window.addEventListener('admin-user-status', handleUserStatusChange)
  
  // 处理初始聊天对象
  if (props.initialChatWith) {
    const existingConv = conversations.value.find(c => c.userId == props.initialChatWith)
    if (existingConv) {
      selectConversation(existingConv)
    } else {
      const newConv = {
        id: parseInt(props.initialChatWith),
        userId: parseInt(props.initialChatWith),
        nickname: props.initialChatName || '用户',
        avatar: null,
        lastMessage: '',
        time: '',
        unread: 0
      }
      conversations.value.unshift(newConv)
      selectConversation(newConv)
    }
  }
})

onUnmounted(() => {
  notificationStore.clearCurrentChatUser()
  window.removeEventListener('admin-user-status', handleUserStatusChange)
})
</script>

<template>
  <div class="flex h-full">
    <!-- 会话列表面板 -->
    <div 
      :class="[
        'relative flex flex-col bg-gradient-to-b from-gray-50/50 to-white transition-all duration-500 ease-out border-r border-gray-100',
        conversationListCollapsed ? 'w-16' : 'w-80'
      ]"
    >
      <!-- 折叠时显示迷你模式 -->
      <template v-if="conversationListCollapsed">
        <div class="flex flex-col items-center py-4 gap-3">
          <button 
            @click="conversationListCollapsed = false"
            class="p-3 bg-orange-50 text-orange-500 rounded-xl hover:bg-orange-100 transition-all hover:scale-105"
            title="展开消息列表"
          >
            <MessageSquare class="w-5 h-5" />
          </button>
          <div class="flex flex-col gap-2 px-2">
            <div 
              v-for="conv in filteredConversations.slice(0, 5)" 
              :key="conv.id"
              @click="selectConversation(conv); conversationListCollapsed = false"
              class="relative cursor-pointer group"
            >
              <UserAvatar 
                :src="conv.avatar" 
                :name="conv.nickname"
                class="w-10 h-10 ring-2 ring-white shadow-sm group-hover:ring-orange-200"
              />
              <div v-if="conv.unread > 0" class="absolute -top-1 -right-1 w-4 h-4 bg-red-500 text-white text-[9px] rounded-full flex items-center justify-center font-bold">
                {{ conv.unread > 9 ? '•' : conv.unread }}
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 展开时显示完整列表 -->
      <template v-else>
        <div class="p-5 border-b border-gray-100/80">
          <div class="flex items-center justify-between mb-4">
            <h3 class="text-lg font-semibold text-gray-800">消息</h3>
            <button 
              @click="conversationListCollapsed = true"
              class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-xl transition-all"
              title="收起列表"
            >
              <ChevronLeft class="w-4 h-4" />
            </button>
          </div>
          <div class="relative">
            <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
            <input 
              v-model="searchKeyword"
              type="text" 
              placeholder="搜索联系人..." 
              class="w-full pl-10 pr-4 py-2.5 bg-white border border-gray-200 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-orange-100 focus:border-orange-300 transition-all"
            >
          </div>
        </div>
        
        <div class="flex-1 overflow-y-auto px-3 py-2">
          <div v-if="loadingConversations" class="flex justify-center py-12">
            <div class="w-8 h-8 border-2 border-orange-200 border-t-orange-500 rounded-full animate-spin"></div>
          </div>
          <div v-else-if="filteredConversations.length === 0" class="text-center text-gray-400 py-12">
            <Mail class="w-12 h-12 mx-auto mb-3 opacity-30" />
            <p class="text-sm">暂无消息</p>
          </div>
          <div v-else class="space-y-1">
            <div 
              v-for="conv in filteredConversations" 
              :key="conv.id"
              @click="selectConversation(conv)"
              :class="[
                'flex items-center gap-3 px-3 py-3 cursor-pointer rounded-xl transition-all duration-200',
                selectedConversation?.userId === conv.userId 
                  ? 'bg-orange-50 shadow-sm' 
                  : 'hover:bg-gray-50'
              ]"
            >
              <div class="relative flex-shrink-0">
                <UserAvatar 
                  :src="conv.avatar" 
                  :name="conv.nickname"
                  :class="[
                    'w-12 h-12 transition-all',
                    selectedConversation?.userId === conv.userId ? 'ring-2 ring-orange-200' : ''
                  ]"
                  @click.stop="router.push(`/user/${conv.userId}`)"
                />
                <div v-if="conv.unread > 0" class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center font-bold px-1">
                  {{ conv.unread > 99 ? '99+' : conv.unread }}
                </div>
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center justify-between mb-0.5">
                  <span class="font-medium text-gray-800 text-sm truncate">{{ conv.nickname }}</span>
                  <span class="text-[11px] text-gray-400">{{ conv.time }}</span>
                </div>
                <p class="text-xs text-gray-500 truncate">{{ conv.lastMessage }}</p>
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>

    <!-- 聊天区域 -->
    <div :class="[
      'flex-1 flex flex-col min-w-0 bg-gradient-to-br from-orange-50/20 via-white to-rose-50/20',
      !selectedConversation && mobileView !== 'detail' ? 'hidden md:flex' : 'flex'
    ]">
      <div v-if="!selectedConversation" class="flex-1 flex flex-col items-center justify-center text-gray-400">
        <div class="w-24 h-24 mb-6 bg-gradient-to-br from-orange-100 to-rose-100 rounded-3xl flex items-center justify-center">
          <MessageSquare class="w-10 h-10 text-orange-400" />
        </div>
        <p class="text-lg font-medium text-gray-600 mb-2">选择一个对话</p>
        <p class="text-sm text-gray-400">开始与好友聊天吧 ˙ᵕ˙</p>
      </div>
      <template v-else>
        <!-- 聊天头部 -->
        <div class="flex items-center gap-2.5 px-4 py-3 bg-white border-b border-gray-100">
          <button @click="backToList" class="md:hidden p-1.5 hover:bg-gray-100 rounded">
            <ArrowLeft class="w-4 h-4 text-gray-600" />
          </button>
          <UserAvatar 
            :src="selectedConversation.avatar" 
            :name="selectedConversation.nickname"
            class="w-9 h-9 bg-gray-200 cursor-pointer hover:ring-2 hover:ring-orange-300 transition"
            @click="router.push(`/user/${selectedConversation.userId}`)"
          />
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
            <UserAvatar 
              v-if="!msg.isMine" 
              :src="selectedConversation.avatar" 
              :name="selectedConversation.nickname"
              class="w-8 h-8 bg-gray-200 mr-2 flex-shrink-0 self-end mb-4 cursor-pointer hover:ring-2 hover:ring-orange-300 transition"
              @click="router.push(`/user/${selectedConversation.userId}`)"
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
