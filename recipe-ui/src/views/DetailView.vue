<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useToast } from '../components/Toast.vue'
import { getRecipeDetail } from '@/api/recipe'
import { getComments, commentRecipe, likeRecipe, followUser, unfollowUser, likeComment, getReplies, deleteComment } from '@/api/social'
import { ArrowLeft, Clock, Heart, MessageCircle, Send, ThumbsUp, Reply, ChevronDown, Trash2 } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()

const selectedRecipe = ref(null)
const comments = ref([])
const commentText = ref('')
const loading = ref(false)

const isFavorite = ref(false)
const isFollowing = ref(false)

// 回复相关状态
const replyingTo = ref(null) // 当前回复的评论对象
const replyText = ref('')
const expandedReplies = ref({}) // { commentId: 显示的回复数量 }
const loadingReplies = ref({}) // { commentId: boolean }

// 解析 JSON 格式的描述
const parseDescription = (description) => {
    try {
        const data = JSON.parse(description)
        return {
            intro: data.intro || '',
            materials: data.ingredients || [],
            steps: data.steps || []
        }
    } catch {
        // 如果不是 JSON，当作纯文本
        return {
            intro: description || '',
            materials: [],
            steps: []
        }
    }
}

const loadData = async () => {
    loading.value = true
    try {
        const id = route.params.id
        const res = await getRecipeDetail(id)
        
        // 解析结构化描述
        const parsed = parseDescription(res.description)
        
        selectedRecipe.value = {
            ...res,
            image: res.coverImage,
            description: parsed.intro, // 简介/心得
            materials: parsed.materials.length > 0 ? parsed.materials : res.ingredients,
            steps: parsed.steps.length > 0 ? parsed.steps : res.steps,
            publishTime: res.createTime
        }
        isFavorite.value = res.isFavorite
        isFollowing.value = res.isFollow
        
        // Fetch comments
        const commentRes = await getComments(id, { page: 1, size: 50 })
        comments.value = commentRes.records.map(c => ({
            ...c,
            authorName: c.nickname,
            authorAvatar: c.avatar,
            time: c.createTime,
            text: c.content
        }))
    } catch (error) {
        console.error(error)
        showToast('加载失败')
    } finally {
        loading.value = false
    }
}


onMounted(() => {
    loadData()
})

const toggleFavorite = async () => {
    try {
        await likeRecipe(selectedRecipe.value.id)
        isFavorite.value = !isFavorite.value
        showToast(isFavorite.value ? '收藏成功' : '取消收藏')
    } catch (e) { showToast('操作失败') }
}

const toggleFollow = async () => {
   // 未登录用户跳转登录页
   if (!userStore.user) {
       router.push('/login')
       return
   }
   try {
       if (isFollowing.value) {
           await unfollowUser(selectedRecipe.value.userId)
           isFollowing.value = false
           showToast('已取消关注')
       } else {
           await followUser(selectedRecipe.value.userId)
           isFollowing.value = true
           showToast('关注成功')
       }
   } catch (e) { showToast('操作失败') }
}

const submitComment = async () => {
    if (!commentText.value) return showToast('请输入评论内容')
    try {
        await commentRecipe({ recipeId: selectedRecipe.value.id, content: commentText.value })
        showToast('评论成功')
        commentText.value = ''
        loadData()
    } catch (e) { showToast('评论失败') }
}

// 回复评论
const submitReply = async (parentComment) => {
    if (!replyText.value) return showToast('请输入回复内容')
    try {
        await commentRecipe({ 
            recipeId: selectedRecipe.value.id, 
            parentId: parentComment.id,
            content: replyText.value 
        })
        showToast('回复成功')
        
        // 局部刷新回复列表，而不是重新加载整个页面（避免折叠）
        const replyRes = await getReplies(parentComment.id, { page: 1, size: (parentComment.replies?.length || 0) + 1 })
        parentComment.replies = replyRes.records
        parentComment.replyCount = (parentComment.replyCount || 0) + 1
        expandedReplies.value[parentComment.id] = parentComment.replies.length
        
        replyText.value = ''
        replyingTo.value = null
    } catch (e) { showToast('回复失败') }
}

// 开始回复
const startReply = (comment) => {
    replyingTo.value = comment
    replyText.value = ''
}

// 取消回复
const cancelReply = () => {
    replyingTo.value = null
    replyText.value = ''
}

// 点赞评论
const handleLikeComment = async (comment) => {
    if (!userStore.user) return showToast('请先登录')
    try {
        await likeComment(comment.id)
        // 切换点赞状态
        comment.isLiked = !comment.isLiked
        comment.likeCount = (comment.likeCount || 0) + (comment.isLiked ? 1 : -1)
    } catch (e) { showToast('操作失败') }
}

// 展开更多回复（每次加载 3 条）
const loadMoreReplies = async (comment) => {
    if (loadingReplies.value[comment.id]) return
    
    loadingReplies.value[comment.id] = true
    try {
        const currentCount = comment.replies?.length || 0
        // 计算需要加载到多少条（当前 + 3）
        const targetCount = currentCount + 3
        
        // 直接获取前 targetCount 条回复
        const res = await getReplies(comment.id, { page: 1, size: targetCount })
        comment.replies = res.records
        expandedReplies.value[comment.id] = comment.replies.length
    } catch (e) { 
        showToast('加载失败') 
    } finally {
        loadingReplies.value[comment.id] = false
    }
}

// 折叠回复（只保留 1 条预览）
const collapseReplies = (comment) => {
    if (comment.replies?.length > 1) {
        comment.replies = comment.replies.slice(0, 1)
        expandedReplies.value[comment.id] = 1
    }
}

// 删除评论
const handleDeleteComment = async (comment, parentComment = null) => {
    if (!confirm('确定要删除这条评论吗？')) return
    try {
        await deleteComment(comment.id)
        showToast('删除成功')
        
        if (parentComment) {
            // 删除的是回复，从父评论的回复列表中移除
            parentComment.replies = parentComment.replies.filter(r => r.id !== comment.id)
            parentComment.replyCount = Math.max(0, (parentComment.replyCount || 0) - 1)
        } else {
            // 删除的是主评论，从列表中移除
            comments.value = comments.value.filter(c => c.id !== comment.id)
        }
    } catch (e) {
        showToast('删除失败')
    }
}

// 判断是否可以删除评论
const canDeleteComment = (comment) => {
    if (!userStore.user) return false
    return userStore.user.id === comment.userId || userStore.user.role === 'admin'
}
</script>




<template>
  <div v-if="selectedRecipe" class="min-h-screen bg-gray-50 pb-10">

    <!-- 主内容容器 -->
    <div class="max-w-3xl mx-auto px-4 pt-4">
      
      <!-- 顶部标题栏 -->
      <div class="bg-white rounded-xl shadow-sm p-4 mb-4 flex items-center">
        <button @click="router.back()" class="flex items-center text-gray-500 hover:text-orange-500 transition text-sm mr-4">
          <ArrowLeft class="w-5 h-5" />
        </button>
        <div class="flex-1 flex items-center justify-center gap-2">
          <span class="text-orange-500 text-lg">🍳</span>
          <h1 class="text-xl font-bold bg-gradient-to-r from-orange-600 to-red-500 bg-clip-text text-transparent">{{ selectedRecipe.title }}</h1>
        </div>
        <div class="w-9"></div> <!-- 占位保持标题居中 -->
      </div>


      <!-- 主卡片 -->
      <div class="bg-white rounded-lg shadow-sm overflow-hidden">
        
        <!-- 封面图 -->
        <div class="relative aspect-video">
          <img :src="selectedRecipe.image" :alt="selectedRecipe.title" class="w-full h-full object-cover">
          <!-- 收藏按钮在图片左下角 -->
          <button 
            v-if="userStore.user"
            @click="toggleFavorite"
            :class="['absolute bottom-3 left-3 flex items-center gap-1 px-3 py-1.5 rounded-full transition text-sm backdrop-blur-sm', isFavorite ? 'bg-red-500 text-white' : 'bg-white/90 text-gray-700 hover:bg-white']"
          >
            <Heart :class="['w-4 h-4', isFavorite ? 'fill-current' : '']" />
            {{ isFavorite ? '已收藏' : '收藏' }}
          </button>
        </div>

        <!-- 菜谱信息 -->
        <div class="p-5">
          <!-- 分类标签 -->
          <div class="mb-4">
            <span class="inline-block bg-orange-100 text-orange-600 text-xs px-2 py-1 rounded">{{ selectedRecipe.category || '美食' }}</span>
          </div>


          <!-- 作者信息 -->
          <div class="flex items-center justify-between py-4 border-t border-b border-gray-100">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full overflow-hidden bg-gray-200">
                <img v-if="selectedRecipe.authorAvatar" :src="selectedRecipe.authorAvatar" class="w-full h-full object-cover">
                <div v-else class="w-full h-full flex items-center justify-center text-gray-500 font-medium">{{ selectedRecipe.authorName?.charAt(0) }}</div>
              </div>
              <div>
                <div class="font-medium text-gray-800 text-sm">{{ selectedRecipe.authorName }}</div>
                <div class="text-xs text-gray-400">{{ selectedRecipe.publishTime }}</div>
              </div>
            </div>
            <button 
              v-if="!userStore.user || userStore.user.id !== selectedRecipe.userId"
              @click="toggleFollow"
              :class="['px-4 py-1.5 rounded-full text-sm transition', isFollowing ? 'bg-gray-100 text-gray-600' : 'bg-orange-500 text-white hover:bg-orange-600']"
            >
              {{ isFollowing ? '已关注' : '+ 关注' }}
            </button>
          </div>

          <!-- 简介 -->
          <div v-if="selectedRecipe.description" class="py-4 border-b border-gray-100">
            <p class="text-gray-600 text-sm leading-relaxed">{{ selectedRecipe.description }}</p>
          </div>
        </div>

        <!-- 用料清单 -->
        <div class="px-5 py-4 bg-orange-50/50">
          <h2 class="font-bold text-gray-800 mb-4 flex items-center gap-2">
            <span class="w-1 h-5 bg-orange-500 rounded-full"></span>
            用料清单
          </h2>
          <div v-if="selectedRecipe.materials && selectedRecipe.materials.length > 0" class="grid grid-cols-2 gap-2">
            <div v-for="(m, idx) in selectedRecipe.materials" :key="idx" class="flex justify-between bg-white px-3 py-2 rounded text-sm">
              <span class="text-gray-700">{{ m.name }}</span>
              <span class="text-gray-400">{{ m.amount }}</span>
            </div>
          </div>
          <p v-else class="text-gray-400 text-sm text-center py-4">暂无用料信息</p>
        </div>

        <!-- 烹饪步骤 -->
        <div class="p-5">
          <h2 class="font-bold text-gray-800 mb-4 flex items-center gap-2">
            <span class="w-1 h-5 bg-orange-500 rounded-full"></span>
            烹饪步骤
          </h2>
          <div v-if="selectedRecipe.steps && selectedRecipe.steps.length > 0" class="space-y-4">
            <div v-for="(step, idx) in selectedRecipe.steps" :key="idx" class="flex gap-3">
              <div class="w-6 h-6 bg-orange-500 text-white rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-0.5">
                {{ idx + 1 }}
              </div>
              <div class="flex-1">
                <p class="text-gray-700 text-sm leading-relaxed">{{ step.description || step.content }}</p>
                <img v-if="step.imageUrl" :src="step.imageUrl" class="mt-2 rounded-lg w-full max-h-48 object-cover">
              </div>
            </div>
          </div>
          <p v-else class="text-gray-400 text-sm text-center py-4">暂无烹饪步骤</p>
        </div>
      </div>

      <!-- 评论区卡片 -->
      <div class="bg-white rounded-xl shadow-sm mt-4 p-5">
        <h2 class="font-bold text-gray-800 mb-4 flex items-center gap-2">
          <MessageCircle class="w-5 h-5 text-orange-500" />
          评论 <span class="text-gray-400 font-normal text-sm">({{ comments.length }})</span>
        </h2>
        
        <!-- 评论输入 -->
        <div v-if="userStore.user" class="flex gap-3 mb-5 pb-4 border-b border-gray-100">
          <div class="w-10 h-10 rounded-full overflow-hidden bg-gradient-to-br from-orange-100 to-orange-200 flex-shrink-0 shadow-sm">
            <img v-if="userStore.user.avatar" :src="userStore.user.avatar" class="w-full h-full object-cover">
            <div v-else class="w-full h-full flex items-center justify-center text-orange-600 font-bold">{{ userStore.user.username?.charAt(0) }}</div>
          </div>
          <div class="flex-1 flex gap-2">
            <input 
              v-model="commentText" 
              type="text" 
              placeholder="写下你的评论..." 
              class="flex-1 px-4 py-2.5 bg-gray-50 border border-gray-200 rounded-full text-sm focus:outline-none focus:ring-2 focus:ring-orange-300 focus:border-transparent transition"
              @keyup.enter="submitComment"
            >
            <button @click="submitComment" class="bg-gradient-to-r from-orange-500 to-red-500 text-white px-5 py-2.5 rounded-full text-sm hover:from-orange-600 hover:to-red-600 transition shadow-md flex items-center gap-1">
              <Send class="w-4 h-4" /> 发送
            </button>
          </div>
        </div>
        <div v-else class="text-center py-4 mb-4 bg-gradient-to-r from-orange-50 to-red-50 rounded-xl text-sm text-gray-600">
          <button @click="router.push('/login')" class="text-orange-500 hover:underline font-medium">登录</button> 后参与评论
        </div>

        <!-- 评论列表 -->
        <div class="space-y-5">
          <div v-for="comment in comments" :key="comment.id" class="group">
            <!-- 主评论 -->
            <div class="flex gap-3">
              <div class="w-10 h-10 rounded-full overflow-hidden bg-gradient-to-br from-gray-100 to-gray-200 flex-shrink-0 shadow-sm">
                <img v-if="comment.avatar" :src="comment.avatar" class="w-full h-full object-cover">
                <div v-else class="w-full h-full flex items-center justify-center text-gray-500 font-bold text-sm">{{ comment.nickname?.charAt(0) }}</div>
              </div>
              <div class="flex-1">
                <div class="bg-gray-50 rounded-xl p-3 hover:bg-gray-100 transition">
                  <div class="flex items-center justify-between mb-1">
                    <div class="flex items-center gap-2">
                      <span class="font-medium text-sm text-gray-800">{{ comment.nickname }}</span>
                      <span class="text-xs text-gray-400">{{ comment.createTime }}</span>
                    </div>
                  </div>
                  <p class="text-gray-700 text-sm leading-relaxed">{{ comment.content }}</p>
                </div>
                
                <!-- 评论操作按钮 -->
                <div class="flex items-center gap-4 mt-2 ml-1">
                  <button 
                    @click="handleLikeComment(comment)"
                    :class="['flex items-center gap-1 text-xs transition', comment.isLiked ? 'text-red-500' : 'text-gray-400 hover:text-red-500']"
                  >
                    <ThumbsUp :class="['w-3.5 h-3.5', comment.isLiked ? 'fill-current' : '']" />
                    {{ comment.likeCount || 0 }}
                  </button>
                  <button 
                    v-if="userStore.user"
                    @click="startReply(comment)"
                    class="flex items-center gap-1 text-xs text-gray-400 hover:text-orange-500 transition"
                  >
                    <Reply class="w-3.5 h-3.5" /> 回复
                  </button>
                  <button 
                    v-if="canDeleteComment(comment)"
                    @click="handleDeleteComment(comment)"
                    class="flex items-center gap-1 text-xs text-gray-400 hover:text-red-500 transition"
                  >
                    <Trash2 class="w-3.5 h-3.5" /> 删除
                  </button>
                </div>

                <!-- 回复输入框 -->
                <div v-if="replyingTo?.id === comment.id" class="mt-3 flex gap-2 items-center">
                  <input 
                    v-model="replyText"
                    type="text"
                    :placeholder="`回复 @${comment.nickname}...`"
                    class="flex-1 px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-1 focus:ring-orange-300"
                    @keyup.enter="submitReply(comment)"
                  >
                  <button @click="submitReply(comment)" class="bg-orange-500 text-white px-3 py-2 rounded-lg text-xs hover:bg-orange-600 transition">发送</button>
                  <button @click="cancelReply" class="text-gray-400 text-xs hover:text-gray-600">取消</button>
                </div>

                <!-- 回复列表 -->
                <div v-if="comment.replies?.length > 0" class="mt-3 ml-2 pl-3 border-l-2 border-orange-100 space-y-3">
                  <div v-for="reply in comment.replies" :key="reply.id" class="flex gap-2">
                    <div class="w-7 h-7 rounded-full overflow-hidden bg-gray-100 flex-shrink-0">
                      <img v-if="reply.avatar" :src="reply.avatar" class="w-full h-full object-cover">
                      <div v-else class="w-full h-full flex items-center justify-center text-gray-400 text-xs font-bold">{{ reply.nickname?.charAt(0) }}</div>
                    </div>
                    <div class="flex-1">
                      <div class="bg-orange-50 rounded-lg p-2">
                        <div class="flex items-center gap-2 mb-0.5">
                          <span class="font-medium text-xs text-gray-800">{{ reply.nickname }}</span>
                          <span v-if="reply.replyToNickname" class="text-xs text-gray-400">回复 <span class="text-orange-500">@{{ reply.replyToNickname }}</span></span>
                          <span class="text-xs text-gray-400">{{ reply.createTime }}</span>
                        </div>
                        <p class="text-gray-600 text-xs leading-relaxed">{{ reply.content }}</p>
                      </div>
                      <!-- 回复操作 -->
                      <div class="flex items-center gap-3 mt-1 ml-1">
                        <button 
                          @click="handleLikeComment(reply)"
                          :class="['flex items-center gap-1 text-xs transition', reply.isLiked ? 'text-red-500' : 'text-gray-400 hover:text-red-500']"
                        >
                          <ThumbsUp :class="['w-3 h-3', reply.isLiked ? 'fill-current' : '']" />
                          {{ reply.likeCount || 0 }}
                        </button>
                        <button 
                          v-if="userStore.user"
                          @click="startReply(comment)"
                          class="flex items-center gap-1 text-xs text-gray-400 hover:text-orange-500 transition"
                        >
                          <Reply class="w-3 h-3" /> 回复
                        </button>
                        <button 
                          v-if="canDeleteComment(reply)"
                          @click="handleDeleteComment(reply, comment)"
                          class="flex items-center gap-1 text-xs text-gray-400 hover:text-red-500 transition"
                        >
                          <Trash2 class="w-3 h-3" /> 删除
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 查看更多回复 / 折叠回复 -->
                <div class="mt-2 ml-2 flex items-center gap-3">
                  <button 
                    v-if="comment.replyCount > (comment.replies?.length || 0)"
                    @click="loadMoreReplies(comment)"
                    :disabled="loadingReplies[comment.id]"
                    class="flex items-center gap-1 text-xs text-orange-500 hover:text-orange-600 transition"
                  >
                    <ChevronDown class="w-3.5 h-3.5" />
                    {{ loadingReplies[comment.id] ? '加载中...' : `查看更多 (${comment.replyCount - (comment.replies?.length || 0)}条)` }}
                  </button>
                  <button 
                    v-if="(comment.replies?.length || 0) > 1"
                    @click="collapseReplies(comment)"
                    class="flex items-center gap-1 text-xs text-gray-400 hover:text-gray-600 transition"
                  >
                    收起回复
                  </button>
                </div>
              </div>
            </div>
          </div>
          
          <div v-if="comments.length === 0" class="text-center py-8 text-gray-400 text-sm">
            <MessageCircle class="w-10 h-10 mx-auto mb-2 opacity-30" />
            还没有评论，快来抢沙发吧～
          </div>
        </div>
      </div>

    </div>
  </div>
</template>
