<template>
  <Transition name="slide-fade">
    <div v-if="toast" class="notification-toast" :class="toastClass" @click="handleClick">
      <div class="toast-icon">
        <component :is="iconComponent" :size="24" />
      </div>
      <div class="toast-content">
        <div class="toast-title">{{ toast.title }}</div>
        <div class="toast-message">{{ toast.content }}</div>
        <div class="toast-time">{{ formatTime(toast.timestamp) }}</div>
      </div>
      <button class="toast-close" @click.stop="close" aria-label="关闭">
        <X :size="18" />
      </button>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'
import { useNotificationStore } from '@/stores/notification'
import { useRouter } from 'vue-router'
import { 
  Bell, 
  Mail, 
  UserPlus, 
  MessageSquare, 
  CheckCircle, 
  XCircle,
  FileEdit,
  X 
} from 'lucide-vue-next'

const notificationStore = useNotificationStore()
const router = useRouter()

// 获取当前的 toast 通知
const toast = computed(() => notificationStore.latestToast)

// 根据消息类型返回样式类
const toastClass = computed(() => {
  if (!toast.value) return ''
  
  const typeMap = {
    'RECIPE_APPROVED': 'success',
    'RECIPE_REJECTED': 'warning',
    'NEW_MESSAGE': 'info',
    'NEW_FOLLOWER': 'info',
    'NEW_COMMENT': 'info',
    'NEW_RECIPE_PENDING': 'pending'
  }
  return typeMap[toast.value.type] || 'info'
})

// 根据消息类型返回图标组件
const iconComponent = computed(() => {
  if (!toast.value) return Bell
  
  const iconMap = {
    'RECIPE_APPROVED': CheckCircle,
    'RECIPE_REJECTED': XCircle,
    'NEW_MESSAGE': Mail,
    'NEW_FOLLOWER': UserPlus,
    'NEW_COMMENT': MessageSquare,
    'NEW_RECIPE_PENDING': FileEdit
  }
  return iconMap[toast.value.type] || Bell
})

// 格式化时间
function formatTime(timestamp) {
  if (!timestamp) return ''
  
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  
  // 1分钟内
  if (diff < 60000) {
    return '刚刚'
  }
  // 1小时内
  if (diff < 3600000) {
    return Math.floor(diff / 60000) + '分钟前'
  }
  // 今天
  if (date.toDateString() === now.toDateString()) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }
  // 其他
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 点击通知跳转到相关页面
function handleClick() {
  if (!toast.value) return
  
  // 标记为已读
  notificationStore.markAsRead(toast.value.id)
  
  // 根据消息类型跳转
  switch (toast.value.type) {
    case 'RECIPE_APPROVED':
    case 'RECIPE_REJECTED':
      // 跳转到我的菜谱页面
      router.push('/my/recipes')
      break
    case 'NEW_MESSAGE':
      // 跳转到私信页面
      if (toast.value.senderId) {
        router.push(`/messages/${toast.value.senderId}`)
      } else {
        router.push('/messages')
      }
      break
    case 'NEW_FOLLOWER':
      // 跳转到粉丝列表
      router.push('/my/fans')
      break
    case 'NEW_COMMENT':
      // 跳转到菜谱详情
      if (toast.value.relatedId) {
        router.push(`/recipe/${toast.value.relatedId}`)
      }
      break
    case 'NEW_RECIPE_PENDING':
      // 跳转到管理后台审核页面
      router.push('/admin')
      break
    default:
      break
  }
  
  close()
}

// 关闭 toast
function close() {
  notificationStore.clearToast()
}
</script>

<style scoped>
.notification-toast {
  position: fixed;
  top: 80px; /* 位于导航栏(h-16=64px)下方 */
  right: 20px;
  min-width: 320px;
  max-width: 420px;
  padding: 16px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.12);
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  z-index: 9999;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
}

.notification-toast:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.16);
}

/* 成功状态（审核通过） */
.notification-toast.success {
  border-left: 4px solid #10b981;
  background: linear-gradient(135deg, #ecfdf5 0%, #ffffff 50%);
}

.notification-toast.success .toast-icon {
  color: #10b981;
}

/* 警告状态（审核驳回） */
.notification-toast.warning {
  border-left: 4px solid #f59e0b;
  background: linear-gradient(135deg, #fffbeb 0%, #ffffff 50%);
}

.notification-toast.warning .toast-icon {
  color: #f59e0b;
}

/* 信息状态（私信、关注、评论） */
.notification-toast.info {
  border-left: 4px solid #3b82f6;
  background: linear-gradient(135deg, #eff6ff 0%, #ffffff 50%);
}

.notification-toast.info .toast-icon {
  color: #3b82f6;
}

/* 待审核状态（新菜谱待审核） */
.notification-toast.pending {
  border-left: 4px solid #8b5cf6;
  background: linear-gradient(135deg, #f5f3ff 0%, #ffffff 50%);
}

.notification-toast.pending .toast-icon {
  color: #8b5cf6;
}

.toast-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.8);
}

.toast-content {
  flex: 1;
  min-width: 0;
}

.toast-title {
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
  margin-bottom: 4px;
}

.toast-message {
  font-size: 14px;
  color: #4b5563;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 4px;
}

.toast-time {
  font-size: 12px;
  color: #9ca3af;
}

.toast-close {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #9ca3af;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s;
}

.toast-close:hover {
  background: #f3f4f6;
  color: #374151;
}

/* 进入/离开动画 */
.slide-fade-enter-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-fade-leave-active {
  transition: all 0.25s ease-in;
}

.slide-fade-enter-from {
  transform: translateX(100%) translateY(-20px);
  opacity: 0;
}

.slide-fade-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

/* 响应式 */
@media (max-width: 480px) {
  .notification-toast {
    left: 12px;
    right: 12px;
    min-width: auto;
    max-width: none;
    top: 72px; /* 移动端导航栏下方 */
  }
  
  .slide-fade-enter-from {
    transform: translateY(-100%);
  }
  
  .slide-fade-leave-to {
    transform: translateY(-100%);
  }
}
</style>
