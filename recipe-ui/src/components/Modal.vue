<script setup>
import { ref } from 'vue'
import { X } from 'lucide-vue-next'

const props = defineProps({
  title: {
    type: String,
    default: '提示'
  }
})

const visible = ref(false)
const message = ref('')
const onConfirm = ref(null)
const onCancel = ref(null)

const show = (msg) => {
  message.value = msg
  visible.value = true
  return new Promise((resolve, reject) => {
    onConfirm.value = () => {
      visible.value = false
      resolve(true)
    }
    onCancel.value = () => {
      visible.value = false
      resolve(false) // or reject(false)
    }
  })
}

// Expose open method
defineExpose({ show })
</script>

<template>
  <Transition name="fade">
    <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
      <div class="bg-white rounded-2xl shadow-2xl w-full max-w-sm overflow-hidden transform transition-all scale-100">
        <div class="flex justify-between items-center p-4 border-b bg-gray-50">
          <h3 class="font-bold text-gray-800">{{ title }}</h3>
          <button @click="onCancel" class="text-gray-400 hover:text-gray-600 transition">
            <X class="w-5 h-5" />
          </button>
        </div>
        
        <div class="p-6 text-gray-600">
          {{ message }}
        </div>
        
        <div class="p-4 bg-gray-50 flex gap-3 justify-end">
          <button 
            @click="onCancel"
            class="px-4 py-2 rounded-lg border border-gray-300 text-gray-600 hover:bg-gray-100 transition text-sm font-medium"
          >
            取消
          </button>
          <button 
            @click="onConfirm"
            class="px-4 py-2 rounded-lg bg-orange-500 text-white hover:bg-orange-600 transition text-sm font-medium shadow-sm"
          >
            确定
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
