<script setup>
import { X, Info, CheckCircle, AlertCircle } from 'lucide-vue-next'
</script>

<script>
// Simple shared state for Toast
import { ref as globalRef } from 'vue'

const globalToasts = globalRef([])
let toastId = 0
let lastToastTime = 0
const DEBOUNCE_TIME = 300 // 防抖时间 300ms

export const useToast = () => {
  const showToast = (message, type = 'info') => {
    const now = Date.now()
    // 防抖：相同消息在短时间内不重复显示
    if (now - lastToastTime < DEBOUNCE_TIME) {
      const lastToast = globalToasts.value[globalToasts.value.length - 1]
      if (lastToast && lastToast.message === message) {
        return // 忽略重复消息
      }
    }
    lastToastTime = now

    const id = ++toastId
    globalToasts.value.push({ id, message, type })

    // 限制最多显示 5 条
    if (globalToasts.value.length > 5) {
      globalToasts.value.shift()
    }

    // 3秒后自动移除
    setTimeout(() => {
      closeToast(id)
    }, 3000)
  }

  const closeToast = (id) => {
    const index = globalToasts.value.findIndex(t => t.id === id)
    if (index > -1) {
      globalToasts.value.splice(index, 1)
    }
  }

  return { showToast, closeToast, globalToasts }
}
</script>

<template>
  <!-- Toast 容器 - 右侧弹出 -->
  <div class="fixed top-4 right-4 z-[100] flex flex-col gap-2 max-w-sm">
    <TransitionGroup name="toast">
      <div 
        v-for="toast in globalToasts" 
        :key="toast.id"
        :class="[
          'flex items-center gap-3 px-4 py-3 rounded-lg shadow-lg backdrop-blur-md border transition-all duration-300',
          toast.type === 'success' ? 'bg-green-50/95 border-green-200 text-green-800' :
          toast.type === 'error' ? 'bg-red-50/95 border-red-200 text-red-800' :
          'bg-white/95 border-gray-200 text-gray-800'
        ]"
      >
        <!-- 图标 -->
        <div :class="[
          'flex-shrink-0',
          toast.type === 'success' ? 'text-green-500' :
          toast.type === 'error' ? 'text-red-500' :
          'text-blue-500'
        ]">
          <CheckCircle v-if="toast.type === 'success'" class="w-5 h-5" />
          <AlertCircle v-else-if="toast.type === 'error'" class="w-5 h-5" />
          <Info v-else class="w-5 h-5" />
        </div>
        
        <!-- 消息内容 -->
        <span class="flex-1 text-sm font-medium">{{ toast.message }}</span>
        
        <!-- 关闭按钮 -->
        <button 
          @click="useToast().closeToast(toast.id)"
          class="flex-shrink-0 p-1 rounded-full hover:bg-gray-200/50 transition text-gray-400 hover:text-gray-600"
        >
          <X class="w-4 h-4" />
        </button>
      </div>
    </TransitionGroup>
  </div>
</template>

<style scoped>
/* Toast 进入/离开动画 */
.toast-enter-active {
  animation: slideIn 0.3s ease-out;
}

.toast-leave-active {
  animation: slideOut 0.3s ease-in;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(100%);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes slideOut {
  from {
    opacity: 1;
    transform: translateX(0);
  }
  to {
    opacity: 0;
    transform: translateX(100%);
  }
}
</style>
