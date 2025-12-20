<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ChevronDown, Check } from 'lucide-vue-next'

/**
 * 统一的下拉选择框组件
 * 支持自定义样式、图标、动画
 */
const props = defineProps({
  modelValue: {
    type: [String, Number, null],
    default: null
  },
  options: {
    type: Array,
    required: true
    // 格式: [{ value: any, label: string, icon?: Component }]
  },
  placeholder: {
    type: String,
    default: '请选择'
  },
  disabled: {
    type: Boolean,
    default: false
  },
  // 尺寸: 'sm' | 'md' | 'lg'
  size: {
    type: String,
    default: 'md'
  }
})

const emit = defineEmits(['update:modelValue'])

const isOpen = ref(false)
const selectRef = ref(null)

const selectedOption = computed(() => {
  return props.options.find(opt => opt.value === props.modelValue)
})

const sizeClasses = computed(() => {
  switch (props.size) {
    case 'sm': return 'px-3 py-1.5 text-xs'
    case 'lg': return 'px-5 py-3.5 text-base'
    default: return 'px-4 py-2.5 text-sm'
  }
})

const toggleDropdown = () => {
  if (!props.disabled) {
    isOpen.value = !isOpen.value
  }
}

const selectOption = (option) => {
  emit('update:modelValue', option.value)
  isOpen.value = false
}

// 点击外部关闭
const handleClickOutside = (event) => {
  if (selectRef.value && !selectRef.value.contains(event.target)) {
    isOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <div ref="selectRef" class="relative">
    <!-- 选择框触发器 -->
    <button
      type="button"
      @click="toggleDropdown"
      :disabled="disabled"
      :class="[
        'w-full flex items-center justify-between gap-2 bg-white border rounded-lg transition-all duration-200',
        sizeClasses,
        isOpen ? 'border-orange-500 ring-2 ring-orange-100' : 'border-gray-200 hover:border-orange-300',
        disabled ? 'bg-gray-50 text-gray-400 cursor-not-allowed' : 'cursor-pointer'
      ]"
    >
      <span :class="selectedOption ? 'text-gray-800' : 'text-gray-400'">
        {{ selectedOption?.label || placeholder }}
      </span>
      <ChevronDown 
        :class="[
          'w-4 h-4 text-gray-400 transition-transform duration-200',
          isOpen ? 'rotate-180' : ''
        ]" 
      />
    </button>

    <!-- 下拉选项 -->
    <Transition name="dropdown">
      <div 
        v-if="isOpen"
        class="absolute z-50 w-full mt-1 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden"
      >
        <div class="max-h-60 overflow-y-auto py-1">
          <button
            v-for="option in options"
            :key="option.value"
            type="button"
            @click="selectOption(option)"
            :class="[
              'w-full flex items-center justify-between gap-2 px-4 py-2.5 text-sm text-left transition-colors',
              option.value === modelValue 
                ? 'bg-orange-50 text-orange-600' 
                : 'text-gray-700 hover:bg-gray-50'
            ]"
          >
            <span class="flex items-center gap-2">
              <component v-if="option.icon" :is="option.icon" class="w-4 h-4" />
              {{ option.label }}
            </span>
            <Check 
              v-if="option.value === modelValue" 
              class="w-4 h-4 text-orange-500" 
            />
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>
