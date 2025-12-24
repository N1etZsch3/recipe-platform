<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { MessageCircle } from 'lucide-vue-next'
import { getRepliesForMe } from '@/api/social'
import { formatTime, getAvatarUrl } from './useMessageUtils'

const router = useRouter()

const replies = ref([])
const loading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getRepliesForMe({ page: 1, size: 50 })
    replies.value = res.records || []
  } catch (error) {
    console.error('加载回复失败', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="flex-1 overflow-y-auto h-full">
    <div class="p-4 border-b border-gray-100 bg-white">
      <h3 class="font-medium text-gray-800">回复我的</h3>
    </div>
    <div v-if="loading" class="flex justify-center py-10">
      <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
    </div>
    <div v-else-if="replies.length === 0" class="text-center text-gray-400 py-20">
      <MessageCircle class="w-12 h-12 mx-auto mb-3 opacity-30" />
      <p>暂无回复</p>
    </div>
    <div v-else class="divide-y divide-gray-100">
      <div v-for="reply in replies" :key="reply.id" class="p-4 bg-white hover:bg-gray-50 transition">
        <div class="flex items-start gap-3">
          <img 
            :src="getAvatarUrl(reply.replyUserAvatar, reply.replyUserName)" 
            class="w-10 h-10 rounded-full object-cover flex-shrink-0"
          >
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-2 mb-1">
              <span class="font-medium text-gray-800">{{ reply.replyUserName }}</span>
              <span class="text-xs text-gray-400">回复了我的评论</span>
            </div>
            <p class="text-sm text-gray-700 mb-2">{{ reply.content }}</p>
            <div class="text-xs text-gray-400 bg-gray-50 px-3 py-2 rounded-lg">
              <span class="text-gray-500">我的评论：</span>{{ reply.myCommentContent }}
            </div>
            <div class="flex items-center justify-between mt-2">
              <span class="text-xs text-gray-400">{{ formatTime(reply.createTime) }}</span>
              <button @click="router.push(`/recipe/${reply.recipeId}`)" class="text-xs text-blue-500 hover:text-blue-600">查看原文 →</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
