<script setup>
import { RouterView, useRoute } from 'vue-router'
import { ref, provide, watch, onMounted, onUnmounted, computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { wsManager } from '@/utils/websocket'
import { setModalRef } from '@/composables/useModal'
import Navbar from './components/Navbar.vue'
import Toast from './components/Toast.vue'
import Modal from './components/Modal.vue'
import NotificationToast from './components/NotificationToast.vue'

const route = useRoute()
const userStore = useUserStore()
const modalRef = ref(null)

// 判断是否为管理后台路由（包括登录页和后台所有页面）
const isAdminRoute = computed(() => {
  return route.path.startsWith('/backstage-m9x2k7')
})

// 注册全局模态框引用
onMounted(() => {
  if (modalRef.value) {
    setModalRef(modalRef.value)
  }
})

// 兼容旧的 provide 方式
const confirm = (message) => {
  if (modalRef.value) {
    return modalRef.value.confirm(message)
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
  <!-- 管理后台使用独立布局，不显示普通导航栏 -->
  <div v-if="isAdminRoute" class="min-h-screen">
    <RouterView v-slot="{ Component }">
      <Transition name="page-fade" mode="out-in">
        <component :is="Component" />
      </Transition>
    </RouterView>
    <Toast />
    <Modal ref="modalRef" />
  </div>
  
  <!-- 普通用户页面 -->
  <div v-else class="min-h-screen bg-[#FFFBF5] text-gray-800 font-sans flex flex-col">
    <Navbar />
    <div class="flex-grow">
      <RouterView v-slot="{ Component }">
        <Transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </div>
    <Toast />
    <Modal ref="modalRef" />
    <!-- 实时通知 Toast -->
    <NotificationToast />
  </div>
</template>
