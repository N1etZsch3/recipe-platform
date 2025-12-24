<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Mail, Edit3, MessageCircle, Heart, Bell } from 'lucide-vue-next'

// 子组件
import MessageConversations from './MessageConversations.vue'
import MessageComments from './MessageComments.vue'
import MessageReplies from './MessageReplies.vue'
import MessageLikes from './MessageLikes.vue'
import MessageSystem from './MessageSystem.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 导航项
const navItems = [
  { id: 'messages', name: '我的消息', icon: Mail },
  { id: 'myComments', name: '我的评论', icon: Edit3 },
  { id: 'replies', name: '回复我的', icon: MessageCircle },
  { id: 'likes', name: '收到的赞', icon: Heart },
  { id: 'system', name: '系统通知', icon: Bell }
]

const activeNav = ref('messages')
const navRefs = ref([])

// 初始聊天对象（从 URL 参数获取）
const initialChatWith = ref(null)
const initialChatName = ref('')

// 滑动指示器样式
const getIndicatorStyle = () => {
  const activeIndex = navItems.findIndex(item => item.id === activeNav.value)
  if (activeIndex === -1 || !navRefs.value[activeIndex]) {
    return { left: '6px', width: '100px' }
  }
  const el = navRefs.value[activeIndex]
  return {
    left: `${el.offsetLeft}px`,
    width: `${el.offsetWidth}px`
  }
}

// 切换导航
const selectNav = (item) => {
  activeNav.value = item.id
}

onMounted(() => {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  
  // 处理 URL 参数
  if (route.query.chatWith) {
    initialChatWith.value = route.query.chatWith
    initialChatName.value = route.query.chatName || ''
    router.replace({ path: '/messages' })
  }
})
</script>

<template>
  <div class="messages-view p-6 h-[calc(100vh-64px)] flex flex-col">
    <!-- 顶部导航栏 -->
    <div class="flex justify-center mb-4">
      <div class="bg-gray-100/80 p-1 rounded-2xl inline-flex gap-0.5 relative">
        <!-- 滑动指示器 -->
        <div 
          class="absolute top-1 h-[calc(100%-8px)] bg-white rounded-xl shadow-sm transition-all duration-500 ease-out"
          :style="getIndicatorStyle()"
        ></div>
        
        <!-- 导航按钮 -->
        <button
          v-for="(item, index) in navItems"
          :key="item.id"
          :ref="el => navRefs[index] = el"
          @click="selectNav(item)"
          :class="[
            'relative z-10 flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-medium transition-all duration-300',
            activeNav === item.id
              ? 'text-orange-600'
              : 'text-gray-500 hover:text-gray-700'
          ]"
        >
          <component :is="item.icon" :class="['w-4 h-4 transition-transform duration-300', activeNav === item.id ? 'scale-110' : '']" />
          <span>{{ item.name }}</span>
        </button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="bg-white rounded-3xl shadow-sm border border-gray-100/50 overflow-hidden flex-1">
      <MessageConversations 
        v-if="activeNav === 'messages'" 
        :initial-chat-with="initialChatWith"
        :initial-chat-name="initialChatName"
      />
      <MessageComments v-else-if="activeNav === 'myComments'" />
      <MessageReplies v-else-if="activeNav === 'replies'" />
      <MessageLikes v-else-if="activeNav === 'likes'" />
      <MessageSystem v-else-if="activeNav === 'system'" />
    </div>
  </div>
</template>
