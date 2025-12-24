<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useToast } from '@/components/Toast.vue'
import { Edit3, Heart, MessageCircle, ChefHat, Check } from 'lucide-vue-next'
import { getMyComments, deleteMyComments } from '@/api/social'
import { formatTime } from './useMessageUtils'

const router = useRouter()
const { showToast } = useToast()

const myComments = ref([])
const loading = ref(false)
const selectedIds = ref([])
const isSelectMode = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyComments({ page: 1, size: 50 })
    myComments.value = res.records || []
  } catch (error) {
    console.error('加载我的评论失败', error)
  } finally {
    loading.value = false
  }
}

const toggleSelectMode = () => {
  isSelectMode.value = !isSelectMode.value
  if (!isSelectMode.value) selectedIds.value = []
}

const toggleSelectComment = (id) => {
  const index = selectedIds.value.indexOf(id)
  if (index > -1) {
    selectedIds.value.splice(index, 1)
  } else {
    selectedIds.value.push(id)
  }
}

const selectAllComments = () => {
  if (selectedIds.value.length === myComments.value.length) {
    selectedIds.value = []
  } else {
    selectedIds.value = myComments.value.map(c => c.id)
  }
}

const handleDeleteComments = async () => {
  if (selectedIds.value.length === 0) return
  try {
    await deleteMyComments(selectedIds.value)
    showToast('删除成功', 'success')
    selectedIds.value = []
    isSelectMode.value = false
    loadData()
  } catch (error) {
    showToast(error.message || '删除失败', 'error')
  }
}

onMounted(() => loadData())
</script>

<template>
  <div class="flex-1 flex flex-col h-full">
    <div class="p-4 border-b border-gray-100 bg-white flex items-center justify-between">
      <h3 class="font-medium text-gray-800">我的评论</h3>
      <div class="flex items-center gap-2">
        <button 
          v-if="isSelectMode && selectedIds.length > 0"
          @click="handleDeleteComments"
          class="px-3 py-1.5 bg-red-500 text-white text-sm rounded-lg hover:bg-red-600"
        >删除 ({{ selectedIds.length }})</button>
        <button 
          v-if="isSelectMode"
          @click="selectAllComments"
          class="px-3 py-1.5 bg-gray-100 text-gray-600 text-sm rounded-lg hover:bg-gray-200"
        >{{ selectedIds.length === myComments.length ? '取消全选' : '全选' }}</button>
        <button 
          @click="toggleSelectMode"
          :class="['px-3 py-1.5 text-sm rounded-lg', isSelectMode ? 'bg-blue-500 text-white' : 'bg-gray-100 text-gray-600 hover:bg-gray-200']"
        >{{ isSelectMode ? '取消' : '管理' }}</button>
      </div>
    </div>
    <div class="flex-1 overflow-y-auto">
      <div v-if="loading" class="flex justify-center py-10">
        <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
      </div>
      <div v-else-if="myComments.length === 0" class="text-center text-gray-400 py-20">
        <Edit3 class="w-12 h-12 mx-auto mb-3 opacity-30" />
        <p>暂无评论</p>
      </div>
      <div v-else class="divide-y divide-gray-100">
        <div v-for="comment in myComments" :key="comment.id" class="p-4 bg-white hover:bg-gray-50 transition">
          <div class="flex items-start gap-3">
            <div 
              v-if="isSelectMode"
              @click="toggleSelectComment(comment.id)"
              :class="['w-5 h-5 rounded border-2 flex items-center justify-center cursor-pointer flex-shrink-0 mt-1', selectedIds.includes(comment.id) ? 'bg-blue-500 border-blue-500' : 'border-gray-300']"
            >
              <Check v-if="selectedIds.includes(comment.id)" class="w-3 h-3 text-white" />
            </div>
            <img 
              v-if="comment.recipeCoverImage"
              :src="comment.recipeCoverImage" 
              class="w-16 h-16 rounded-lg object-cover flex-shrink-0"
            >
            <div v-else class="w-16 h-16 rounded-lg bg-gray-100 flex items-center justify-center flex-shrink-0">
              <ChefHat class="w-6 h-6 text-gray-400" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm text-gray-500 mb-1">
                <template v-if="comment.parentId">
                  回复了 <span class="text-orange-500">{{ comment.replyToUserName || '用户' }}</span> 在 
                </template>
                <template v-else>评论于 </template>
                <span class="text-blue-500 cursor-pointer hover:underline" @click="router.push(`/recipe/${comment.recipeId}`)">{{ comment.recipeTitle }}</span>
              </div>
              <p v-if="comment.parentId && comment.parentContent" class="text-xs text-gray-400 mb-1 line-clamp-1">
                原评论：{{ comment.parentContent }}
              </p>
              <p class="text-gray-800 text-sm line-clamp-2">{{ comment.content }}</p>
              <div class="flex items-center gap-4 mt-2 text-xs text-gray-400">
                <span>{{ formatTime(comment.createTime) }}</span>
                <span class="flex items-center gap-1"><Heart class="w-3 h-3" /> {{ comment.likeCount }}</span>
                <span v-if="!comment.parentId" class="flex items-center gap-1"><MessageCircle class="w-3 h-3" /> {{ comment.replyCount }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
