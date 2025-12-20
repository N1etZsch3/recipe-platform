<script setup>
import { ref } from 'vue'
import { X, AlertTriangle, Info, HelpCircle } from 'lucide-vue-next'

const props = defineProps({
  title: {
    type: String,
    default: '提示'
  }
})

const visible = ref(false)
const message = ref('')
const inputValue = ref('')
const inputPlaceholder = ref('')
const modalType = ref('confirm') // 'confirm' | 'prompt' | 'alert'
const confirmText = ref('确定')
const cancelText = ref('取消')
const confirmButtonClass = ref('')
const onConfirm = ref(null)
const onCancel = ref(null)

// 确认框
const confirm = (msg, options = {}) => {
  message.value = msg
  modalType.value = 'confirm'
  confirmText.value = options.confirmText || '确定'
  cancelText.value = options.cancelText || '取消'
  confirmButtonClass.value = options.danger 
    ? 'bg-red-500 hover:bg-red-600' 
    : 'bg-orange-500 hover:bg-orange-600'
  visible.value = true
  
  return new Promise((resolve) => {
    onConfirm.value = () => {
      visible.value = false
      resolve(true)
    }
    onCancel.value = () => {
      visible.value = false
      resolve(false)
    }
  })
}

// 输入框
const prompt = (msg, options = {}) => {
  message.value = msg
  modalType.value = 'prompt'
  inputValue.value = options.defaultValue || ''
  inputPlaceholder.value = options.placeholder || '请输入...'
  confirmText.value = options.confirmText || '确定'
  cancelText.value = options.cancelText || '取消'
  confirmButtonClass.value = 'bg-orange-500 hover:bg-orange-600'
  visible.value = true
  
  return new Promise((resolve) => {
    onConfirm.value = () => {
      visible.value = false
      resolve(inputValue.value)
    }
    onCancel.value = () => {
      visible.value = false
      resolve(null)
    }
  })
}

// 提示框（只有确定按钮）
const alert = (msg, options = {}) => {
  message.value = msg
  modalType.value = 'alert'
  confirmText.value = options.confirmText || '知道了'
  confirmButtonClass.value = 'bg-orange-500 hover:bg-orange-600'
  visible.value = true
  
  return new Promise((resolve) => {
    onConfirm.value = () => {
      visible.value = false
      resolve(true)
    }
    onCancel.value = onConfirm.value
  })
}

// 兼容旧的 show 方法
const show = (msg) => confirm(msg)

defineExpose({ show, confirm, prompt, alert })
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="fixed inset-0 z-[100] flex items-center justify-center p-4" @click.self="onCancel">
        <!-- 遮罩层 -->
        <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"></div>
        
        <!-- 模态框内容 -->
        <div class="relative bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden transform transition-all">
          <!-- 头部 -->
          <div class="flex justify-between items-center px-6 py-4 border-b border-gray-100">
            <div class="flex items-center gap-2">
              <div v-if="confirmButtonClass.includes('red')" class="w-8 h-8 rounded-full bg-red-100 flex items-center justify-center">
                <AlertTriangle class="w-4 h-4 text-red-500" />
              </div>
              <div v-else class="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center">
                <HelpCircle class="w-4 h-4 text-orange-500" />
              </div>
              <h3 class="font-semibold text-gray-800">{{ title }}</h3>
            </div>
            <button @click="onCancel" class="text-gray-400 hover:text-gray-600 transition p-1 hover:bg-gray-100 rounded-lg">
              <X class="w-5 h-5" />
            </button>
          </div>
          
          <!-- 内容区域 -->
          <div class="px-6 py-5">
            <p class="text-gray-600 leading-relaxed">{{ message }}</p>
            
            <!-- 输入框（prompt 模式） -->
            <div v-if="modalType === 'prompt'" class="mt-4">
              <input
                v-model="inputValue"
                type="text"
                :placeholder="inputPlaceholder"
                class="w-full px-4 py-3 border border-gray-200 rounded-xl focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none transition"
                @keyup.enter="onConfirm"
                autofocus
              />
            </div>
          </div>
          
          <!-- 底部按钮 -->
          <div class="px-6 py-4 bg-gray-50 flex gap-3 justify-end">
            <button 
              v-if="modalType !== 'alert'"
              @click="onCancel"
              class="px-5 py-2.5 rounded-xl border border-gray-200 text-gray-600 hover:bg-gray-100 transition text-sm font-medium"
            >
              {{ cancelText }}
            </button>
            <button 
              @click="onConfirm"
              :class="['px-5 py-2.5 rounded-xl text-white transition text-sm font-medium shadow-sm', confirmButtonClass]"
            >
              {{ confirmText }}
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-enter-active,
.modal-leave-active {
  transition: all 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95) translateY(-10px);
}

.modal-enter-to .relative,
.modal-leave-from .relative {
  transform: scale(1) translateY(0);
}
</style>
