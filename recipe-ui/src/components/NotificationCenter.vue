<script setup>
import { ref, computed, watch } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import { useRouter } from 'vue-router'
import { 
    Bell, MessageSquare, Heart, UserPlus, Check, 
    AlertCircle, X, Mail, ChevronRight, Inbox
} from 'lucide-vue-next'

const props = defineProps({
    /** 是否显示代办 Tab（仅管理员使用） */
    showTodo: {
        type: Boolean,
        default: false
    },
    /** 待审核数量（用于代办 Tab） */
    todoCount: {
        type: Number,
        default: 0
    }
})

const emit = defineEmits(['close', 'view-all', 'mark-read'])

const router = useRouter()
const notificationStore = useNotificationStore()

// 当前选中的 Tab
const activeTab = ref('notification')

// Tab 配置
const tabs = computed(() => {
    const baseTabs = [
        { key: 'notification', label: '通知', count: notificationCount.value },
        { key: 'message', label: '消息', count: messageCount.value }
    ]
    if (props.showTodo) {
        baseTabs.push({ key: 'todo', label: '代办', count: props.todoCount })
    }
    return baseTabs
})

// 通知类型图标映射
const notificationIcons = {
    'LIKE': { icon: Heart, bg: 'bg-pink-100', color: 'text-pink-500' },
    'COMMENT': { icon: MessageSquare, bg: 'bg-green-100', color: 'text-green-500' },
    'FOLLOW': { icon: UserPlus, bg: 'bg-orange-100', color: 'text-orange-500' },
    'SYSTEM': { icon: Bell, bg: 'bg-blue-100', color: 'text-blue-500' },
    'AUDIT_PASS': { icon: Check, bg: 'bg-emerald-100', color: 'text-emerald-500' },
    'AUDIT_REJECT': { icon: AlertCircle, bg: 'bg-red-100', color: 'text-red-500' },
    'NEW_MESSAGE': { icon: Mail, bg: 'bg-amber-100', color: 'text-amber-500' }
}

// 获取通知图标配置
const getIconConfig = (type) => {
    return notificationIcons[type] || notificationIcons['SYSTEM']
}

// 分类通知
const notifications = computed(() => {
    return notificationStore.notifications.filter(n => 
        n.type !== 'NEW_MESSAGE'
    ).slice(0, 5)
})

const messages = computed(() => {
    return notificationStore.notifications.filter(n => 
        n.type === 'NEW_MESSAGE'
    ).slice(0, 5)
})

// 计数
const notificationCount = computed(() => {
    return notificationStore.notifications.filter(n => 
        n.type !== 'NEW_MESSAGE' && !n.read
    ).length
})

const messageCount = computed(() => {
    return notificationStore.notifications.filter(n => 
        n.type === 'NEW_MESSAGE' && !n.read
    ).length
})

// 格式化时间
const formatTime = (time) => {
    if (!time) return ''
    const date = new Date(time)
    const now = new Date()
    const diff = now - date
    
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
    
    return date.toLocaleDateString('zh-CN', { 
        month: '2-digit', 
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    })
}

// 点击通知项
const handleItemClick = (item) => {
    notificationStore.markAsRead(item.id)
    
    // 根据类型跳转到消息中心对应分类
    if (item.type === 'NEW_MESSAGE' && item.senderId) {
        // 私信 - 跳转到消息中心私信分类
        router.push(`/messages?type=message&chatWith=${item.senderId}`)
    } else if (item.type === 'LIKE') {
        // 点赞通知 - 跳转到消息中心点赞分类
        router.push('/messages?type=like')
    } else if (item.type === 'COMMENT') {
        // 评论通知 - 跳转到消息中心评论分类
        router.push('/messages?type=comment')
    } else if (item.type === 'FOLLOW') {
        // 关注通知 - 跳转到用户主页
        if (item.relatedId) {
            router.push(`/user/${item.relatedId}`)
        } else {
            router.push('/messages?type=system')
        }
    } else if (item.type === 'AUDIT_PASS' || item.type === 'AUDIT_REJECT' || item.type === 'RECIPE_APPROVED' || item.type === 'RECIPE_REJECTED' || item.type === 'COMMENT_DELETED') {
        // 审核/系统通知 - 跳转到消息中心系统通知分类
        router.push('/messages?type=system')
    } else if (item.recipeId) {
        // 其他带菜谱ID的 - 跳转到菜谱详情
        router.push(`/recipe/${item.recipeId}`)
    } else {
        // 默认跳转到消息中心
        router.push('/messages')
    }
    
    emit('close')
}

// 标记全部已读
const handleMarkAllRead = () => {
    notificationStore.markAllAsRead()
    emit('mark-read')
}

// 查看全部
const handleViewAll = () => {
    emit('view-all', activeTab.value)
    emit('close')
}
</script>

<template>
    <div class="w-80 bg-white rounded-xl shadow-xl border border-gray-100 overflow-hidden">
        <!-- 标题栏 -->
        <div class="px-4 py-3 border-b border-gray-100 flex items-center justify-between">
            <h3 class="font-bold text-gray-800">通知</h3>
            <button 
                @click="handleMarkAllRead"
                class="text-sm text-blue-500 hover:text-blue-600 transition"
            >
                标为已读
            </button>
        </div>
        
        <!-- Tab 切换 -->
        <div class="flex border-b border-gray-100">
            <button 
                v-for="tab in tabs" 
                :key="tab.key"
                @click="activeTab = tab.key"
                :class="[
                    'flex-1 py-2.5 text-sm font-medium transition relative',
                    activeTab === tab.key 
                        ? 'text-blue-500' 
                        : 'text-gray-500 hover:text-gray-700'
                ]"
            >
                {{ tab.label }}
                <span v-if="tab.count > 0" class="ml-1 text-xs">({{ tab.count }})</span>
                
                <!-- 底部指示条 -->
                <span 
                    v-if="activeTab === tab.key" 
                    class="absolute bottom-0 left-1/2 -translate-x-1/2 w-12 h-0.5 bg-blue-500 rounded-full"
                />
            </button>
        </div>
        
        <!-- 内容区域 -->
        <div class="max-h-80 overflow-y-auto overflow-x-hidden relative"> <!-- Added overflow-x-hidden and relative -->
            <Transition name="fade-slide" mode="out-in">
                <!-- 通知列表 -->
                <div v-if="activeTab === 'notification'" key="notification">
                    <div v-if="notifications.length === 0" class="py-12 text-center text-gray-400">
                        <Inbox class="w-12 h-12 mx-auto mb-2 opacity-50" />
                        <p>暂无通知</p>
                    </div>
                    <div v-else class="divide-y divide-gray-50">
                        <div 
                            v-for="item in notifications" 
                            :key="item.id"
                            @click="handleItemClick(item)"
                            class="flex items-start gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer transition"
                        >
                            <div :class="[
                                'w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0',
                                getIconConfig(item.type).bg
                            ]">
                                <component 
                                    :is="getIconConfig(item.type).icon" 
                                    :class="['w-5 h-5', getIconConfig(item.type).color]"
                                />
                            </div>
                            <div class="flex-1 min-w-0">
                                <p class="text-sm text-gray-800 line-clamp-2">{{ item.content || item.title }}</p>
                                <p class="text-xs text-gray-400 mt-1">{{ formatTime(item.timestamp || item.receivedAt) }}</p>
                            </div>
                            <span 
                                v-if="!item.read" 
                                class="w-2 h-2 bg-blue-500 rounded-full flex-shrink-0 mt-2"
                            />
                        </div>
                    </div>
                </div>
            
                <!-- 消息列表 -->
                <div v-else-if="activeTab === 'message'" key="message">
                    <div v-if="messages.length === 0" class="py-12 text-center text-gray-400">
                        <MessageSquare class="w-12 h-12 mx-auto mb-2 opacity-50" />
                        <p>暂无私信</p>
                    </div>
                    <div v-else class="divide-y divide-gray-50">
                        <div 
                            v-for="item in messages" 
                            :key="item.id"
                            @click="handleItemClick(item)"
                            class="flex items-center gap-3 px-4 py-3 hover:bg-gray-50 cursor-pointer transition"
                        >
                            <img 
                                :src="item.senderAvatar || 'https://via.placeholder.com/40'"
                                class="w-10 h-10 rounded-full object-cover flex-shrink-0"
                            />
                            <div class="flex-1 min-w-0">
                                <p class="text-sm font-medium text-gray-800 truncate">
                                    {{ item.senderName || '用户' }}
                                </p>
                                <p class="text-xs text-gray-500 truncate mt-0.5">{{ item.content }}</p>
                            </div>
                            <div class="text-right flex-shrink-0">
                                <p class="text-xs text-gray-400">{{ formatTime(item.timestamp || item.receivedAt) }}</p>
                                <span 
                                    v-if="!item.read" 
                                    class="inline-block w-2 h-2 bg-blue-500 rounded-full mt-1"
                                />
                            </div>
                        </div>
                    </div>
                </div>
            
                <!-- 代办列表 (仅管理员) -->
                <div v-else-if="activeTab === 'todo'" key="todo">
                    <div v-if="todoCount === 0" class="py-12 text-center text-gray-400">
                        <Inbox class="w-12 h-12 mx-auto mb-2 opacity-50" />
                        <p>暂无代办</p>
                    </div>
                    <slot name="todo" v-else>
                        <!-- 默认代办内容由父组件通过 slot 传入 -->
                    </slot>
                </div>
            </Transition>
        </div>
        
        <!-- 底部按钮 -->
        <div class="border-t border-gray-100 p-3">
            <button 
                @click="handleViewAll"
                class="w-full py-2 text-sm text-blue-500 font-medium border border-blue-200 rounded-lg hover:bg-blue-50 transition flex items-center justify-center gap-1"
            >
                查看全部
                <ChevronRight class="w-4 h-4" />
            </button>
        </div>
    </div>
</template>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.2s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(10px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-10px);
}
</style>
