<script setup>
import { computed } from 'vue'

const props = defineProps({
  src: {
    type: String,
    default: ''
  },
  name: {
    type: String,
    default: 'U'
  },
  class: {
    type: String,
    default: ''
  }
})

const initial = computed(() => {
  return (props.name || 'U').charAt(0).toUpperCase()
})

const bgColorClass = computed(() => {
    // Generate a consistent color based on name length or char code if desired, 
    // but for now, let's stick to the requested "clean" look (e.g., standard gray/orange).
    // Reference image showed a light gray background.
    return 'bg-gray-100 border border-gray-200 text-gray-500' 
})
</script>

<template>
  <div 
    :class="[
      'rounded-full overflow-hidden flex items-center justify-center font-bold relative shrink-0',
      !src ? bgColorClass : '',
      props.class
    ]"
  >
    <img v-if="src" :src="src" class="w-full h-full object-cover" alt="Avatar">
    <span v-else class="select-none leading-none flex items-center justify-center h-full w-full pb-[10%]">
        {{ initial }}
    </span>
  </div>
</template>
