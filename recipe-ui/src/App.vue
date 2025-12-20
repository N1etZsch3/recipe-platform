<script setup>
import { RouterView } from 'vue-router'
import { ref, provide, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { wsManager } from '@/utils/websocket'
import Navbar from './components/Navbar.vue'
import Toast from './components/Toast.vue'
import Modal from './components/Modal.vue'
import NotificationToast from './components/NotificationToast.vue'

const userStore = useUserStore()
const modalRef = ref(null)

const confirm = (message) => {
  if (modalRef.value) {
    return modalRef.value.show(message)
  }
  return Promise.resolve(false)
}

provide('confirm', confirm)

// 监听登录状态变化，自动管理 WebSocket 连接
watch(() => userStore.token, (newToken, oldToken) => {
  if (newToken && !oldToken) {
    // 用户登录，建立 WebSocket 连接
    console.log('App: 用户登录，建立 WebSocket 连接')
    wsManager.connect()
  } else if (!newToken && oldToken) {
    // 用户登出，关闭 WebSocket 连接
    console.log('App: 用户登出，关闭 WebSocket 连接')
    wsManager.close()
  }
})

// 组件挂载时，如果已登录则建立连接
onMounted(() => {
  if (userStore.token) {
    console.log('App: 已登录，建立 WebSocket 连接')
    wsManager.connect()
  }
})

// 组件卸载时关闭连接
onUnmounted(() => {
  wsManager.close()
})
</script>

<template>
  <div class="min-h-screen bg-[#FFFBF5] text-gray-800 font-sans flex flex-col">
    <Navbar />
    <div class="flex-grow">
      <RouterView />
    </div>
    <Toast />
    <Modal ref="modalRef" />
    <!-- 实时通知 Toast -->
    <NotificationToast />
  </div>
</template>
