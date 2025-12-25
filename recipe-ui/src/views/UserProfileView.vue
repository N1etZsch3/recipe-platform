<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useToast } from '../components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { getUserProfile, followUser, unfollowUser, getMyFavorites, getMyComments } from '@/api/social'
import { listRecipies } from '@/api/recipe'
import { ArrowLeft, ChefHat, UserPlus, UserCheck, MessageCircle, Heart, Star, Award, Grid, Bookmark, ThumbsUp, User, MessageSquare } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()
const { confirm } = useModal()

const targetUserId = route.params.id
const userProfile = ref(null)
const recipes = ref([])
const favorites = ref([])
const comments = ref([])
const loading = ref(false)
const contentLoading = ref(false)

// Tab切换
const activeTab = ref('home')
const tabs = [
  { key: 'home', label: '首页', icon: Grid },
  { key: 'favorites', label: '收藏', icon: Bookmark },
  { key: 'info', label: '资料', icon: User },
  { key: 'comments', label: '评论', icon: MessageSquare }
]

// 统计数据
const stats = computed(() => [
  { label: '粉丝', value: userProfile.value?.followerCount || 0 },
  { label: '关注', value: userProfile.value?.followingCount || 0 },
  { label: '作品', value: recipes.value?.length || 0 },
  { label: '获赞', value: userProfile.value?.likeCount || 0 }
])

// 加载用户信息
const loadUserProfile = async () => {
  loading.value = true
  try {
    const res = await getUserProfile(targetUserId)
    userProfile.value = res
  } catch (error) {
    console.error(error)
    showToast(error.message || '加载用户失败', 'error')
    router.replace('/')
  } finally {
    loading.value = false
  }
}

// 解析 JSON 格式的描述
const parseDescription = (description) => {
    if (!description) return ''
    try {
        const data = JSON.parse(description)
        return data.intro || ''
    } catch {
        return description
    }
}

// 加载用户的菜谱
const loadUserRecipes = async () => {
  contentLoading.value = true
  try {
    const res = await listRecipies({
      page: 1,
      size: 20,
      authorId: targetUserId,
      status: 1
    })
    recipes.value = res.records.map(r => ({
      id: r.id,
      title: r.title,
      image: r.coverImage,
      description: parseDescription(r.description),
      likeCount: r.likeCount || 0,
      commentCount: r.commentCount || 0,
      viewCount: r.viewCount || 0
    }))
  } catch (error) {
    console.error('Failed to load recipes', error)
  } finally {
    contentLoading.value = false
  }
}

// 加载收藏 (只有查看自己的收藏时才可用)
const loadFavorites = async () => {
  contentLoading.value = true
  try {
    // 只能查看自己的收藏
    if (targetUserId == userStore.user?.id) {
      const res = await getMyFavorites({ page: 1, size: 20 })
      favorites.value = res.records || []
    } else {
      favorites.value = []
    }
  } catch (error) {
    console.error('Failed to load favorites', error)
    favorites.value = []
  } finally {
    contentLoading.value = false
  }
}

// 加载评论
const loadComments = async () => {
  contentLoading.value = true
  try {
    // 如果是自己的页面，可以加载评论
    if (targetUserId == userStore.user?.id) {
      const res = await getMyComments({ page: 1, size: 20 })
      comments.value = res.records || []
    } else {
      comments.value = []
    }
  } catch (error) {
    console.error('Failed to load comments', error)
    comments.value = []
  } finally {
    contentLoading.value = false
  }
}

// Tab切换处理
const switchTab = (tabKey) => {
  activeTab.value = tabKey
  if (tabKey === 'home') {
    loadUserRecipes()
  } else if (tabKey === 'favorites') {
    loadFavorites()
  } else if (tabKey === 'comments') {
    loadComments()
  }
}

// 关注/取关
const handleFollow = async () => {
  if (!userStore.user) {
    router.push('/login')
    return
  }
  if (!userProfile.value) return

  const isFollowing = userProfile.value.isFollow
  try {
    if (isFollowing) {
        const confirmed = await confirm('确定要取消关注吗？')
        if (confirmed) {
            await unfollowUser(targetUserId)
            userProfile.value.isFollow = false
            showToast('已取消关注')
        }
    } else {
      await followUser(targetUserId)
      userProfile.value.isFollow = true
      showToast('关注成功', 'success')
    }
  } catch (error) {
    showToast(error.message || '操作失败', 'error')
  }
}

// 发起私信
const handleChat = () => {
    if (!userStore.user) {
        router.push('/login')
        return
    }
    router.push({
        path: '/messages',
        query: {
            chatWith: targetUserId,
            chatName: userProfile.value?.nickname || '用户'
        }
    })
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

onMounted(() => {
  const isPreviewMode = route.query.preview === 'true'
  if (targetUserId == userStore.user?.id && !isPreviewMode) {
      router.replace('/profile')
      return
  }
  loadUserProfile()
  loadUserRecipes()
})
</script>

<template>
  <div class="min-h-full bg-gray-50">
    <!-- Loading -->
    <div v-if="loading" class="flex items-center justify-center min-h-[400px] text-gray-400">
      加载中...
    </div>

    <div v-else-if="userProfile">
      <!-- 橙色 Header 区域 -->
      <div class="bg-gradient-to-b from-orange-500 to-orange-400 text-white relative">
        <!-- 返回按钮 -->
        <button 
          @click="router.back()" 
          class="absolute top-4 left-4 p-2 rounded-full bg-white/20 hover:bg-white/30 transition z-10"
        >
          <ArrowLeft class="w-5 h-5" />
        </button>

        <!-- 统计数据 - 左2+右2分布 -->
        <!-- 统计数据 - 居中对称分布 (中间留空给头像) -->
        <div class="max-w-4xl mx-auto flex justify-center items-end pt-24 pb-12 gap-32">
          <!-- 左侧两个统计 -->
          <div class="flex gap-12">
            <div v-for="stat in stats.slice(0, 2)" :key="stat.label" class="text-center min-w-[3rem]">
              <div class="text-xs text-white/70 mb-1">{{ stat.label }}</div>
              <div class="text-lg font-bold">{{ stat.value }}</div>
            </div>
          </div>
          <!-- 右侧两个统计 -->
          <div class="flex gap-12">
            <div v-for="stat in stats.slice(2, 4)" :key="stat.label" class="text-center min-w-[3rem]">
              <div class="text-xs text-white/70 mb-1">{{ stat.label }}</div>
              <div class="text-lg font-bold">{{ stat.value }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 用户信息卡片（覆盖 header） -->
      <div class="relative -mt-12 pb-6">
        <div class="max-w-2xl mx-auto px-4">
          <!-- 头像 -->
          <div class="flex justify-center mb-4">
            <div class="w-24 h-24 rounded-full border-4 border-white shadow-lg overflow-hidden bg-white">
              <img v-if="userProfile.avatar" :src="userProfile.avatar" class="w-full h-full object-cover">
              <div v-else class="w-full h-full bg-gray-200 flex items-center justify-center text-3xl text-gray-400 font-bold">
                {{ userProfile.nickname?.charAt(0).toUpperCase() }}
              </div>
            </div>
          </div>

          <!-- 用户名和简介 -->
          <div class="text-center mb-6">
            <h1 class="text-xl font-bold text-gray-800 mb-1">{{ userProfile.nickname }}</h1>
            <p class="text-sm text-gray-500">{{ userProfile.intro || '这个人很懒，什么也没写' }}</p>
          </div>

          <!-- 操作按钮 -->
          <div class="flex justify-center gap-3">
            <button 
              @click="handleFollow"
              :class="['px-6 py-2.5 rounded-lg font-medium transition flex items-center gap-2', 
                userProfile.isFollow 
                  ? 'bg-gray-100 text-gray-600 hover:bg-gray-200' 
                  : 'bg-blue-500 text-white hover:bg-blue-600']"
            >
              <UserCheck v-if="userProfile.isFollow" class="w-4 h-4" />
              <UserPlus v-else class="w-4 h-4" />
              {{ userProfile.isFollow ? '已关注' : '关注' }}
            </button>
            <button 
              @click="handleChat" 
              class="px-6 py-2.5 rounded-lg bg-white border border-gray-200 text-gray-600 font-medium hover:bg-gray-50 transition flex items-center gap-2"
            >
              <MessageCircle class="w-4 h-4" />
              私信
            </button>
          </div>
        </div>
      </div>

      <!-- 菜谱作品区域 -->
      <div class="max-w-4xl mx-auto px-4 py-6">
        <div class="flex items-center gap-2 mb-4">
          <ChefHat class="w-5 h-5 text-orange-500" />
          <span class="text-sm text-gray-600">发布的菜谱</span>
          <span class="text-sm text-gray-400">{{ recipes.length }}</span>
        </div>
        
        <!-- Loading -->
        <div v-if="contentLoading" class="text-center py-12 text-gray-400">
          加载中...
        </div>

        <div v-else-if="recipes.length > 0" class="grid grid-cols-2 md:grid-cols-4 gap-4">
          <div 
            v-for="recipe in recipes" 
            :key="recipe.id"
            @click="router.push(`/recipe/${recipe.id}`)"
            class="bg-white rounded-xl shadow-sm overflow-hidden hover:shadow-md transition cursor-pointer group"
          >
            <div class="aspect-square bg-gray-100 overflow-hidden">
              <img :src="recipe.image" class="w-full h-full object-cover group-hover:scale-105 transition duration-300">
            </div>
            <div class="p-3">
              <h3 class="font-medium text-gray-800 text-sm truncate mb-1">{{ recipe.title }}</h3>
              <p class="text-xs text-gray-400 line-clamp-1 mb-2">{{ recipe.description || '暂无描述' }}</p>
              <div class="flex items-center gap-3 text-xs text-gray-400">
                <span class="flex items-center gap-0.5"><Heart class="w-3 h-3" /> {{ recipe.likeCount }}</span>
                <span class="flex items-center gap-0.5"><MessageSquare class="w-3 h-3" /> {{ recipe.commentCount }}</span>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="text-center py-16 text-gray-400">
          <ChefHat class="w-12 h-12 mx-auto mb-3 opacity-30" />
          <p>还没有发布任何菜谱</p>
        </div>
      </div>
    </div>
  </div>
</template>
