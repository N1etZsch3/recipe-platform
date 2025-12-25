<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, ArrowRight, ArrowLeft } from 'lucide-vue-next'
import { getLikesForMe, getLikeDetail } from '@/api/social'
import { formatTime } from './useMessageUtils'
import UserAvatar from '@/components/UserAvatar.vue'

const router = useRouter()

const likes = ref([])
const loading = ref(false)
const selectedLike = ref(null)
const likeDetail = ref([])
const loadingDetail = ref(false)
const mobileView = ref('list')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getLikesForMe({ page: 1, size: 50 })
    likes.value = res.records || []
  } catch (error) {
    console.error('加载点赞失败', error)
  } finally {
    loading.value = false
  }
}

const openDetail = async (like) => {
  selectedLike.value = like
  mobileView.value = 'detail'
  loadingDetail.value = true
  try {
    const res = await getLikeDetail(like.commentId, { page: 1, size: 50 })
    likeDetail.value = res.records || []
  } catch (error) {
    console.error('加载点赞详情失败', error)
  } finally {
    loadingDetail.value = false
  }
}

const closeDetail = () => {
  selectedLike.value = null
  likeDetail.value = []
  mobileView.value = 'list'
}

onMounted(() => loadData())
</script>

<template>
  <div class="flex h-full">
    <!-- 点赞列表 -->
    <div :class="['flex-1 flex flex-col', selectedLike ? 'hidden md:flex md:w-1/2 md:border-r md:border-gray-100' : 'flex']">
      <div class="p-4 border-b border-gray-100 bg-white">
        <h3 class="font-medium text-gray-800">收到的赞</h3>
      </div>
      <div class="flex-1 overflow-y-auto">
        <div v-if="loading" class="flex justify-center py-10">
          <div class="animate-spin rounded-full h-6 w-6 border-b-2 border-blue-500"></div>
        </div>
        <div v-else-if="likes.length === 0" class="text-center text-gray-400 py-20">
          <Heart class="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p>暂无点赞</p>
        </div>
        <div v-else class="divide-y divide-gray-100">
          <div 
            v-for="like in likes" 
            :key="like.commentId" 
            @click="openDetail(like)"
            :class="['p-4 bg-white hover:bg-gray-50 transition cursor-pointer', selectedLike?.commentId === like.commentId ? 'bg-blue-50' : '']"
          >
            <div class="flex items-start gap-3">
              <div class="flex -space-x-2 flex-shrink-0">
                <UserAvatar 
                  v-for="(liker, idx) in (like.likers || []).slice(0, 3)" 
                  :key="liker.userId"
                  :src="liker.avatar" 
                  :name="liker.nickname"
                  :class="['w-8 h-8 ring-2 ring-white', idx > 0 ? '-ml-2' : '']"
                />
              </div>
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-1 flex-wrap mb-1">
                  <span class="font-medium text-gray-800 text-sm">{{ like.likers?.[0]?.nickname }}</span>
                  <span v-if="like.likeCount > 1" class="text-xs text-gray-500">等{{ like.likeCount }}人</span>
                  <span class="text-xs text-gray-400">赞了我的评论</span>
                </div>
                <p class="text-sm text-gray-600 truncate">{{ like.commentContent }}</p>
                <div class="flex items-center gap-2 mt-1">
                  <span v-if="like.recipeTitle" class="text-xs text-orange-500 bg-orange-50 px-1.5 py-0.5 rounded">{{ like.recipeTitle }}</span>
                  <span class="text-xs text-gray-400">{{ formatTime(like.latestLikeTime) }}</span>
                </div>
              </div>
              <ArrowRight class="w-4 h-4 text-gray-300 flex-shrink-0 mt-1" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 点赞详情 -->
    <div v-if="selectedLike" :class="['flex-1 flex flex-col', mobileView === 'detail' ? 'flex' : 'hidden md:flex']">
      <div class="p-4 border-b border-gray-100 bg-white">
        <div class="flex items-center gap-2">
          <button @click="closeDetail" class="md:hidden p-1 hover:bg-gray-100 rounded">
            <ArrowLeft class="w-4 h-4 text-gray-600" />
          </button>
          <span class="text-sm text-gray-500">收到的赞</span>
          <span class="text-gray-300">></span>
          <span class="text-sm font-medium text-gray-800">点赞详情</span>
        </div>
      </div>
      <div class="p-4 bg-white border-b border-gray-100">
        <div class="text-sm text-gray-500 mb-1">评论：</div>
        <p class="text-gray-800">{{ selectedLike.commentContent }}</p>
        <div v-if="selectedLike.recipeTitle" class="mt-2">
          <span class="text-xs text-gray-400">来自菜谱：</span>
          <span class="text-xs text-orange-500 cursor-pointer hover:underline" @click="router.push(`/recipe/${selectedLike.recipeId}`)">{{ selectedLike.recipeTitle }}</span>
        </div>
      </div>
      <div class="flex-1 overflow-y-auto p-4 space-y-3">
        <div v-if="loadingDetail" class="flex justify-center py-4">
          <div class="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-400"></div>
        </div>
        <div v-else v-for="item in likeDetail" :key="item.likers?.[0]?.userId" class="flex items-center gap-3 p-3 bg-white rounded-xl">
          <UserAvatar 
            :src="item.likers?.[0]?.avatar" 
            :name="item.likers?.[0]?.nickname"
            class="w-10 h-10 flex-shrink-0"
          />
          <div class="flex-1">
            <div class="font-medium text-gray-800 text-sm">{{ item.likers?.[0]?.nickname }}</div>
            <div class="text-xs text-gray-400">{{ formatTime(item.likers?.[0]?.likeTime) }}</div>
          </div>
          <span class="text-xs text-pink-500">赞了我</span>
        </div>
      </div>
    </div>
  </div>
</template>
