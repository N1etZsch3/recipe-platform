<script setup>
/**
 * 骨架屏加载组件
 * 用于在内容加载时显示占位效果
 */
defineProps({
  // 类型: 'card' | 'list' | 'text' | 'avatar' | 'image'
  type: {
    type: String,
    default: 'text'
  },
  // 行数 (仅 text 类型)
  lines: {
    type: Number,
    default: 3
  },
  // 是否显示头像
  avatar: {
    type: Boolean,
    default: false
  }
})
</script>

<template>
  <!-- 卡片骨架 -->
  <div v-if="type === 'card'" class="bg-white rounded-xl overflow-hidden border border-gray-100">
    <div class="skeleton h-48"></div>
    <div class="p-4 space-y-3">
      <div class="skeleton h-5 w-3/4"></div>
      <div class="skeleton h-4 w-full"></div>
      <div class="flex items-center gap-2 mt-4">
        <div class="skeleton w-8 h-8 rounded-full"></div>
        <div class="skeleton h-4 w-20"></div>
      </div>
    </div>
  </div>

  <!-- 列表项骨架 -->
  <div v-else-if="type === 'list'" class="flex items-center gap-4 p-4 bg-white rounded-lg border border-gray-100">
    <div v-if="avatar" class="skeleton w-12 h-12 rounded-full flex-shrink-0"></div>
    <div class="flex-1 space-y-2">
      <div class="skeleton h-4 w-1/3"></div>
      <div class="skeleton h-3 w-full"></div>
      <div class="skeleton h-3 w-2/3"></div>
    </div>
  </div>

  <!-- 图片骨架 -->
  <div v-else-if="type === 'image'" class="skeleton aspect-video rounded-lg"></div>

  <!-- 头像骨架 -->
  <div v-else-if="type === 'avatar'" class="skeleton w-10 h-10 rounded-full"></div>

  <!-- 文本骨架 (默认) -->
  <div v-else class="space-y-2">
    <div 
      v-for="n in lines" 
      :key="n" 
      :class="['skeleton h-4', n === lines ? 'w-2/3' : 'w-full']"
    ></div>
  </div>
</template>
