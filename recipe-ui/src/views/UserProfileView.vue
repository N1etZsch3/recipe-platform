<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useToast } from '../components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { getUserProfile, followUser, unfollowUser } from '@/api/social'
import { listRecipies } from '@/api/recipe'
import { ArrowLeft, ChefHat, UserPlus, UserMinus, UserCheck, MessageCircle } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()
const { confirm } = useModal()

const targetUserId = route.params.id
const userProfile = ref(null)
const recipes = ref([])
const loading = ref(false)
const recipesLoading = ref(false)

// 加载用户信息
const loadUserProfile = async () => {
  loading.value = true
  try {
    const res = await getUserProfile(targetUserId)
    userProfile.value = res
  } catch (error) {
    console.error(error)
    showToast(error.message || '加载用户失败', 'error')
    router.replace('/') // 加载失败回首页
  } finally {
    loading.value = false
  }
}

// 解析 JSON 格式的描述，提取 intro
const parseDescription = (description) => {
    if (!description) return ''
    try {
        const data = JSON.parse(description)
        return data.intro || ''
    } catch {
        // 如果不是 JSON，直接返回原文
        return description
    }
}

// 加载用户的菜谱
const loadUserRecipes = async () => {
  recipesLoading.value = true
  try {
    // 假设 listRecipies 支持 authorId 参数
    const res = await listRecipies({
      page: 1,
      size: 20,
      authorId: targetUserId,
      status: 1 // 只能看已发布的
    })
    recipes.value = res.records.map(r => ({
      id: r.id,
      title: r.title,
      image: r.coverImage,
      description: parseDescription(r.description),
      authorId: r.userId,
      authorName: r.nickname,
      authorAvatar: r.avatar
    }))
  } catch (error) {
    console.error('Failed to load recipes', error)
  } finally {
    recipesLoading.value = false
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
    // 跳转到私信页面，带上用户信息以直接打开与该用户的对话
    router.push({
        path: '/messages',
        query: {
            chatWith: targetUserId,
            chatName: userProfile.value?.nickname || '用户'
        }
    })
}


onMounted(() => {
  // 如果不是预览模式，且看的是自己，跳转到个人中心
  const isPreviewMode = route.query.preview === 'true'
  if (targetUserId == userStore.user?.id && !isPreviewMode) {
      // 如果看的是自己，跳转到个人中心
      router.replace('/profile')
      return
  }
  loadUserProfile()
  loadUserRecipes()
})
</script>

<template>
  <div class="min-h-screen bg-orange-50/30 pt-20 pb-10">
    <div class="max-w-5xl mx-auto px-4">
      <!-- 顶部返回 -->
      <button @click="router.back()" class="flex items-center gap-1 text-gray-500 hover:text-gray-800 mb-6 transition">
        <ArrowLeft class="w-5 h-5" /> 返回
      </button>

      <div v-if="loading" class="text-center py-20 text-gray-400">
        加载中...
      </div>

      <div v-else-if="userProfile" class="flex flex-col gap-8">
        <!-- 用户信息卡片 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden relative">
            <!-- 顶部背景图 -->
            <div class="h-32 bg-gradient-to-r from-orange-100 to-red-50"></div>
            
            <div class="px-8 pb-8 flex flex-col md:flex-row items-end -mt-12 gap-6">
                <!-- 头像 -->
                <div class="w-32 h-32 rounded-full border-4 border-white shadow-lg overflow-hidden bg-white flex-shrink-0">
                    <img v-if="userProfile.avatar" :src="userProfile.avatar" class="w-full h-full object-cover">
                    <div v-else class="w-full h-full bg-orange-100 flex items-center justify-center text-4xl text-orange-500 font-bold">
                        {{ userProfile.nickname?.charAt(0).toUpperCase() }}
                    </div>
                </div>

                <!-- 信息与操作 -->
                <div class="flex-1 min-w-0 pb-2 w-full text-center md:text-left">
                    <h1 class="text-2xl font-bold text-gray-800 mb-1">{{ userProfile.nickname }}</h1>
                    <p class="text-gray-500 text-sm mb-4 line-clamp-2 md:line-clamp-1">{{ userProfile.intro || '这个人很懒，什么也没写' }}</p>
                    
                    <div class="flex items-center justify-center md:justify-start gap-3">
                         <button 
                            @click="handleFollow"
                            :class="['px-6 py-2 rounded-full font-bold shadow-sm transition flex items-center gap-2', 
                                userProfile.isFollow 
                                ? 'bg-gray-100 text-gray-600 hover:bg-gray-200' 
                                : 'bg-orange-500 text-white hover:bg-orange-600 shadow-orange-200']"
                         >
                            <UserCheck v-if="userProfile.isFollow" class="w-4 h-4" />
                            <UserPlus v-else class="w-4 h-4" />
                            {{ userProfile.isFollow ? '已关注' : '关注' }}
                         </button>
                         <!-- 私信入口，关注后显示 -->
                         <button 
                            v-if="userProfile.isFollow"
                            @click="handleChat" 
                            class="px-5 py-2 rounded-full bg-white border border-gray-200 text-gray-600 font-medium hover:bg-gray-50 hover:border-orange-300 hover:text-orange-500 transition flex items-center gap-2 shadow-sm"
                         >
                             <MessageCircle class="w-4 h-4" />
                             私信
                         </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 用户的菜谱列表 -->
        <div>
            <div class="flex items-center gap-2 mb-6">
                <ChefHat class="w-6 h-6 text-orange-500" />
                <h2 class="text-xl font-bold text-gray-800">发布的菜谱</h2>
                <span class="text-sm font-normal text-gray-400 bg-gray-100 px-2 py-0.5 rounded-full">{{ recipes.length }}</span>
            </div>

            <div v-if="recipesLoading" class="text-center py-10 text-gray-400">
                加载菜谱中...
            </div>
            
            <div v-else-if="recipes.length > 0" class="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-6">
                 <div 
                    v-for="recipe in recipes" 
                    :key="recipe.id"
                    @click="router.push(`/recipe/${recipe.id}`)"
                    class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden hover:shadow-md hover:translate-y-[-2px] transition cursor-pointer group"
                 >
                    <div class="h-48 overflow-hidden bg-gray-100 relative">
                        <img :src="recipe.image" class="w-full h-full object-cover group-hover:scale-105 transition duration-500">
                    </div>
                    <div class="p-4">
                        <h3 class="font-bold text-gray-800 mb-2 truncate group-hover:text-orange-600 transition">{{ recipe.title }}</h3>
                        <p class="text-xs text-gray-400 line-clamp-2 md:h-8">{{ recipe.description || '暂无描述' }}</p>
                        
                        <div class="mt-3 flex items-center gap-2 text-xs text-gray-500">
                             <img v-if="userProfile.avatar" :src="userProfile.avatar" class="w-5 h-5 rounded-full object-cover">
                             <span class="truncate max-w-[100px]">{{ userProfile.nickname }}</span>
                        </div>
                    </div>
                 </div>
            </div>

            <div v-else class="text-center py-16 bg-white rounded-xl border border-dashed border-gray-200 text-gray-400">
                <ChefHat class="w-12 h-12 mx-auto mb-3 opacity-20" />
                <p>TA 还没有发布任何菜谱</p>
            </div>
        </div>
      </div>
    </div>
  </div>
</template>
