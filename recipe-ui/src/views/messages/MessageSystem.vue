<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useNotificationStore } from '@/stores/notification'
import { Bell, CheckCircle, XCircle, Trash2 } from 'lucide-vue-next'
import { formatTime } from './useMessageUtils'

const router = useRouter()
const notificationStore = useNotificationStore()

const notifications = ref([])
const loading = ref(false)

const loadData = () => {
  loading.value = true
  const sysTypes = ['RECIPE_APPROVED', 'RECIPE_REJECTED', 'COMMENT_DELETED']
  notifications.value = notificationStore.notifications
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
  loading.value = false
}

const getIcon = (type) => {
  const icons = {
    'RECIPE_APPROVED': CheckCircle,
    'RECIPE_REJECTED': XCircle,
    'COMMENT_DELETED': Trash2
  }
  return icons[type] || Bell
}

const getColor = (type) => {
  const colors = {
    'RECIPE_APPROVED': 'text-green-500 bg-green-50',
    'RECIPE_REJECTED': 'text-red-500 bg-red-50',
    'COMMENT_DELETED': 'text-orange-500 bg-orange-50'
  }
  return colors[type] || 'text-gray-500 bg-gray-50'
}

onMounted(() => loadData())
</script>

<template>
  <div class="flex-1 overflow-y-auto h-full">
    <div class="p-4 border-b border-gray-100 bg-white">
      <h3 class="font-medium text-gray-800">系统通知</h3>
    </div>
    <div v-if="loading" class="flex justify-center py-10">
      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
    </div>
    <div v-else-if="notifications.length === 0" class="text-center text-gray-400 py-20">
      <Bell class="w-12 h-12 mx-auto mb-3 opacity-30" />
      <p>暂无系统通知</p>
    </div>
    <div v-else class="divide-y divide-gray-100">
      <div v-for="notif in notifications" :key="notif.id" 
           :class="['p-4 bg-white hover:bg-gray-50 transition', !notif.read ? 'bg-blue-50/30' : '']">
        <div class="flex items-start gap-3">
          <div :class="['p-2 rounded-lg flex-shrink-0', getColor(notif.type)]">
            <component :is="getIcon(notif.type)" class="w-5 h-5" />
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
