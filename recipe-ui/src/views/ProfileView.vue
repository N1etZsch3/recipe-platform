<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useToast } from '../components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { listRecipies, deleteRecipe as deleteRecipeApi, unpublishRecipe as unpublishRecipeApi, withdrawRecipe as withdrawRecipeApi } from '@/api/recipe'
import { updateProfile, getProfile, changePassword } from '@/api/auth'
import { uploadFile } from '@/api/common'
import { getMyFavorites, getMyFollows, getMyFans, unfollowUser, followUser } from '@/api/social' 
import { ChefHat, Heart, User, MessageCircle, Settings, Edit2, Trash2, X, Send, ArrowDown, UserMinus, UserPlus, Lock, Users } from 'lucide-vue-next'
import UserAvatar from '@/components/UserAvatar.vue'



const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const { showToast } = useToast()
const { confirm } = useModal()


const activeProfileTab = ref('recipes')
const chatTarget = ref(null)

const profileTabs = [
    { id: 'recipes', icon: ChefHat, label: '我的菜谱' },
    { id: 'favorites', icon: Heart, label: '我的收藏' },
    { id: 'following', icon: User, label: '我的关注' },
    { id: 'fans', icon: Users, label: '我的粉丝' },
    { id: 'settings', icon: Settings, label: '个人信息' },
]

// Real data refs
const myRecipes = ref([])
const myFavorites = ref([]) // Store favorites
const myFollows = ref([]) // Empty for now, API missing
const myFans = ref([])
const loading = ref(false)

// 个人资料表单
const avatarInput = ref(null)
const uploadingAvatar = ref(false)
const savingProfile = ref(false)
const profileForm = ref({
    nickname: '',
    avatar: '',
    intro: ''
})
// 原始资料副本（用于取消修改）
const originalProfile = ref({
    nickname: '',
    avatar: '',
    intro: ''
})

// 修改密码相关
const showPasswordModal = ref(false)
const changingPassword = ref(false)
const passwordForm = ref({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
})

const openPasswordModal = () => {
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    showPasswordModal.value = true
}

const handlePasswordChange = async () => {
    if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
        showToast('请填写完整信息', 'error')
        return
    }
    if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
        showToast('两次输入的密码不一致', 'error')
        return
    }
    if (passwordForm.value.newPassword.length < 6) {
        showToast('新密码长度不能少于6位', 'error')
        return
    }
    
    changingPassword.value = true
    try {
        await changePassword({
            oldPassword: passwordForm.value.oldPassword,
            newPassword: passwordForm.value.newPassword
        })
        showToast('密码修改成功，请重新登录', 'success')
        showPasswordModal.value = false
        // 登出逻辑
        userStore.logout()
        router.push('/login')
    } catch (error) {
        console.error(error)
        showToast(error.message || '密码修改失败', 'error')
    } finally {
        changingPassword.value = false
    }
}

// 初始化加载用户资料
const loadProfile = async () => {
    try {
        const userData = await getProfile()
        const profileData = {
            nickname: userData.nickname || '',
            avatar: userData.avatar || '',
            intro: userData.intro || ''
        }
        profileForm.value = { ...profileData }
        originalProfile.value = { ...profileData } // 保存原始副本
        // 同步更新 userStore 中的头像和昵称
        if (userStore.user) {
            userStore.setUser({
                ...userStore.user,
                nickname: userData.nickname || userStore.user.nickname,
                avatar: userData.avatar || userStore.user.avatar
            })
        }
    } catch (error) {
        console.error('Failed to load profile', error)
    }
}

// 取消修改，恢复原始数据
const cancelEdit = () => {
    profileForm.value = { ...originalProfile.value }
}

// 触发头像上传
const triggerAvatarUpload = () => {
    avatarInput.value?.click()
}

// 处理头像上传
const handleAvatarChange = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    if (!file.type.startsWith('image/')) {
        showToast('请上传图片文件')
        return
    }
    if (file.size > 5 * 1024 * 1024) {
        showToast('图片大小不能超过 5MB')
        return
    }
    
    uploadingAvatar.value = true
    try {
        const url = await uploadFile(file)
        profileForm.value.avatar = url
        showToast('头像上传成功')
    } catch (error) {
        console.error(error)
        showToast('头像上传失败')
    } finally {
        uploadingAvatar.value = false
    }
}

// 保存个人资料
const saveProfile = async () => {
    // 昵称验证
    if (!profileForm.value.nickname || profileForm.value.nickname.length < 1 || profileForm.value.nickname.length > 20) {
        showToast('昵称长度必须为1-20个字符')
        return
    }
    
    savingProfile.value = true
    try {
        await updateProfile({
            nickname: profileForm.value.nickname,
            avatar: profileForm.value.avatar,
            intro: profileForm.value.intro
        })
        // 更新本地存储的用户信息
        if (userStore.user) {
            userStore.setUser({
                ...userStore.user,
                nickname: profileForm.value.nickname,
                avatar: profileForm.value.avatar
            })
        }
        // 更新原始数据副本
        originalProfile.value = { ...profileForm.value }
        showToast('保存成功')
    } catch (error) {
        console.error(error)
        showToast('保存失败')
    } finally {
        savingProfile.value = false
    }
}

const loadMyRecipes = async () => {
    if (!userStore.user) return
    loading.value = true
    try {
        // Assume backend supports authorId filter
        const res = await listRecipies({ 
            page: 1, 
            size: 50,
            authorId: userStore.user.id
        })
        myRecipes.value = res.records.map(r => ({
            id: r.id,
            title: r.title,
            image: r.coverImage,
            status: r.status, // 0: Auditing, 1: Published, 2: Rejected
            publishTime: r.createTime,
            rejectReason: r.rejectReason
        }))
    } catch (error) {
        console.error('Failed to load recipes', error)
        showToast('加载失败')
    } finally {
        loading.value = false
    }
}

// 加载我的收藏
const loadMyFavorites = async () => {
    loading.value = true
    try {
        const res = await getMyFavorites({ page: 1, size: 50 })
        myFavorites.value = res.records
    } catch (error) {
        console.error('Failed to load favorites', error)
        showToast('加载收藏失败')
    } finally {
        loading.value = false
    }
}

// 加载我的关注
const loadMyFollows = async () => {
    loading.value = true
    try {
        const res = await getMyFollows({ page: 1, size: 50 })
        myFollows.value = res.records
    } catch (error) {
        console.error('Failed to load follows', error)
        showToast('加载关注失败')
    } finally {
        loading.value = false
    }
}

// 加载我的粉丝
const loadMyFans = async () => {
    loading.value = true
    try {
        const res = await getMyFans({ page: 1, size: 50 })
        // 后端返回的 UserVO 包含 isFollow 状态
        myFans.value = res.records
    } catch (error) {
        console.error('Failed to load fans', error)
        showToast('加载粉丝失败')
    } finally {
        loading.value = false
    }
}

// 关注用户 (回关)
const handleFollow = async (user) => {
    try {
        await followUser(user.id)
        user.isFollow = true // 更新本地状态
        showToast('关注成功')
    } catch (error) {
        showToast('操作失败')
    }
}

// 取消关注
const handleUnfollow = async (user) => {
    const confirmed = await confirm('确定取消关注吗？')
    if (confirmed) {
        try {
            await unfollowUser(user.id)
            // 如果在关注列表中，移除
            myFollows.value = myFollows.value.filter(u => u.id !== user.id)
            // 如果在粉丝列表中，更新状态
            const fan = myFans.value.find(u => u.id === user.id)
            if (fan) {
                fan.isFollow = false
            }
            // 更新当前对象状态
            user.isFollow = false
            
            showToast('已取消关注')
        } catch (error) {
            showToast('操作失败')
        }
    }
}

// 监听 Tab 切换
watch(activeProfileTab, (newTab) => {
    if (newTab === 'recipes') {
        loadMyRecipes()
    } else if (newTab === 'favorites') {
        loadMyFavorites()
    } else if (newTab === 'following') {
        loadMyFollows()
    } else if (newTab === 'fans') {
        loadMyFans()
    }
})

// 监听菜谱审核通知，实时更新状态
watch(() => notificationStore.latestNotification, (notification) => {
    if (!notification) return
    
    if (notification.type === 'RECIPE_APPROVED' || notification.type === 'RECIPE_REJECTED') {
        // 本地增量更新：找到对应菜谱并更新状态，无需重新请求API
        const recipeId = notification.relatedId
        const recipe = myRecipes.value.find(r => r.id === recipeId)
        if (recipe) {
            if (notification.type === 'RECIPE_APPROVED') {
                recipe.status = 1 // 已发布
                recipe.rejectReason = null
            } else {
                recipe.status = 2 // 已驳回
                // 从通知内容中提取驳回原因
                const match = notification.content?.match(/原因[：:]\s*(.+)$/)
                recipe.rejectReason = match ? match[1] : null
            }
            console.log('ProfileView: 已更新菜谱状态', recipeId, notification.type)
        }
    }
})


onMounted(() => {
    loadMyRecipes()
    loadProfile()
})

const deleteRecipe = async (id) => {
    const confirmed = await confirm('确定删除该菜谱吗？', { danger: true })
    if (confirmed) {
        try {
            await deleteRecipeApi(id)
            myRecipes.value = myRecipes.value.filter(r => r.id !== id)
            showToast('删除成功')
        } catch (error) {
             showToast('删除失败')
        }
    }
}

// 下架菜谱
const unpublishRecipe = async (id) => {
    const confirmed = await confirm('下架后菜谱将变为待审核状态，您可以进行编辑。确定下架吗？')
    if (confirmed) {
        try {
            await unpublishRecipeApi(id)
            // 更新本地状态
            const recipe = myRecipes.value.find(r => r.id === id)
            if (recipe) {
                recipe.status = 0 // 改为待审核
            }
            showToast('下架成功，您现在可以编辑菜谱了')
        } catch (error) {
            const msg = error.message || '下架失败'
            showToast(msg)
        }
    }
}

// 撤销发布（待审核 -> 草稿）
const withdrawRecipe = async (id) => {
    const confirmed = await confirm('撤销后菜谱将保存为草稿，您可以继续编辑后重新提交。确定撤销吗？')
    if (confirmed) {
        try {
            await withdrawRecipeApi(id)
            // 更新本地状态
            const recipe = myRecipes.value.find(r => r.id === id)
            if (recipe) {
                recipe.status = 3 // 改为草稿
            }
            showToast('已撤销发布，菜谱已保存为草稿')
        } catch (error) {
            const msg = error.message || '撤销失败'
            showToast(msg)
        }
    }
}
</script>


<template>
  <div class="profile-view-wrapper">
    <div class="p-6 max-w-5xl mx-auto">
      <!-- 用户信息卡片 -->
      <div class="bg-white rounded-2xl shadow-sm p-6 mb-6 border border-gray-100">
        <div class="flex items-center gap-6">
          <!-- 头像 -->
          <UserAvatar 
            :src="userStore.user?.avatar" 
            :name="userStore.user?.username"
            class="w-20 h-20 border-4 border-white shadow-lg flex-shrink-0"
          />
          
          <!-- 用户信息 -->
          <div class="flex-1 min-w-0">
            <h2 class="font-bold text-xl text-gray-800 mb-1">{{ userStore.user?.nickname || userStore.user?.username }}</h2>
            <p class="text-sm text-gray-500 mb-3">ID: {{ userStore.user?.id }}</p>
            <button 
              @click="router.push(`/user/${userStore.user?.id}?preview=true`)"
              class="px-4 py-2 text-sm text-orange-600 bg-orange-50 hover:bg-orange-100 rounded-lg transition flex items-center gap-1.5 font-medium"
            >
              <User class="w-4 h-4" />
              查看我的主页
            </button>
          </div>
        </div>
      </div>

      <!-- Tab 导航 -->
      <div class="flex items-center gap-2 mb-6 overflow-x-auto pb-2">
        <button 
          v-for="item in profileTabs" 
          :key="item.id"
          @click="activeProfileTab = item.id"
          :class="[
            'flex items-center gap-2 px-5 py-2.5 rounded-full text-sm transition font-medium whitespace-nowrap',
            activeProfileTab === item.id 
              ? 'bg-orange-500 text-white shadow-md shadow-orange-200' 
              : 'bg-white text-gray-600 hover:bg-gray-50 border border-gray-200'
          ]"
        >
          <component :is="item.icon" class="w-4 h-4" />
          {{ item.label }}
        </button>
      </div>

      <!-- 内容区 -->
      <div class="bg-white rounded-2xl shadow-sm p-6 min-h-[400px] border border-gray-100">
          <!-- 我的菜谱 -->
          <div v-if="activeProfileTab === 'recipes'">
              <div class="flex justify-between items-center mb-6">
                  <h3 class="font-bold text-xl text-gray-800">我的发布 ({{ myRecipes.length }})</h3>
                  <button @click="router.push('/create')" class="bg-orange-500 text-white px-4 py-2 rounded-lg text-sm flex items-center gap-2 hover:bg-orange-600 shadow-lg shadow-orange-200 transition font-bold">
                      <component :is="ChefHat" class="w-4 h-4" /> 发布新菜谱
                  </button>
              </div>
              
              <div v-if="loading" class="py-10 text-center text-gray-400">
                  加载中...
              </div>
              <div v-else-if="myRecipes.length === 0" class="py-10 text-center text-gray-400 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                  还没有发布过菜谱，快去分享你的第一道美食吧！
              </div>
              
              <div v-else class="space-y-4">
                  <div v-for="r in myRecipes" :key="r.id" @click="router.push(`/recipe/${r.id}`)" class="flex gap-4 border border-gray-100 p-3 rounded-xl hover:border-orange-200 transition relative bg-white hover:shadow-md group cursor-pointer">
                      <img :src="r.image" class="w-24 h-24 object-cover rounded-lg bg-gray-100">
                      <div class="flex-1 py-1">
                          <div class="flex justify-between items-start">
                              <h4 class="font-bold text-gray-800 text-lg group-hover:text-orange-600 transition">{{ r.title }}</h4>
                              <!-- 状态标签 -->
                              <span v-if="r.status === 1" class="text-xs bg-green-100 text-green-700 px-2 py-1 rounded font-medium border border-green-200">已发布</span>
                              <span v-else-if="r.status === 0" class="text-xs bg-yellow-100 text-yellow-700 px-2 py-1 rounded font-medium border border-yellow-200">审核中</span>
                              <span v-else-if="r.status === 3" class="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded font-medium border border-gray-200">草稿</span>
                              <span v-else-if="r.status === 4" class="text-xs bg-blue-100 text-blue-700 px-2 py-1 rounded font-medium border border-blue-200">处理中</span>
                              <div v-else-if="r.status === 2" class="text-right">
                                  <span class="text-xs bg-red-100 text-red-700 px-2 py-1 rounded font-medium border border-red-200">未通过</span>
                                  <p class="text-[10px] text-red-500 mt-1 max-w-[150px] truncate" :title="r.rejectReason">{{ r.rejectReason }}</p>
                              </div>
                          </div>
                          <p class="text-xs text-gray-400 mt-2">发布于: {{ r.publishTime }}</p>
                          
                          <div class="absolute bottom-3 right-3 flex gap-2">
                               <!-- 已发布状态显示下架按钮 -->
                               <button 
                                  v-if="r.status === 1" 
                                  @click.stop="unpublishRecipe(r.id)" 
                                  class="text-orange-500 hover:bg-orange-50 p-1.5 rounded text-xs flex items-center gap-1 transition"
                               >
                                  <ArrowDown class="w-3 h-3" /> 下架
                               </button>
                               <!-- 待审核或处理中状态显示撤销按钮 -->
                               <button 
                                  v-if="r.status === 0 || r.status === 4" 
                                  @click.stop="withdrawRecipe(r.id)" 
                                  class="text-gray-500 hover:bg-gray-50 p-1.5 rounded text-xs flex items-center gap-1 transition"
                               >
                                  <X class="w-3 h-3" /> 撤销
                               </button>
                               <!-- 非已发布状态可以编辑 -->
                               <button 
                                  v-if="r.status !== 1" 
                                  @click.stop="router.push(`/create?id=${r.id}`)" 
                                  class="text-blue-500 hover:bg-blue-50 p-1.5 rounded text-xs flex items-center gap-1 transition"
                               >
                                  <Edit2 class="w-3 h-3" /> 编辑
                               </button>
                              <button @click.stop="deleteRecipe(r.id)" class="text-gray-400 hover:text-red-500 hover:bg-red-50 p-1.5 rounded transition">
                                  <Trash2 class="w-4 h-4" />
                              </button>
                          </div>
                      </div>
                  </div>
              </div>
          </div>

          <!-- 我的收藏 -->
          <div v-if="activeProfileTab === 'favorites'">
              <div class="flex justify-between items-center mb-6">
                  <h3 class="font-bold text-xl text-gray-800">我的收藏 ({{ myFavorites.length }})</h3>
              </div>
              
              <div v-if="loading" class="py-10 text-center text-gray-400">
                  加载中...
              </div>
              <div v-else-if="myFavorites.length === 0" class="py-10 text-center text-gray-400 bg-gray-50 rounded-lg border border-dashed border-gray-200">
                  还没有收藏任何菜谱，快去发现美味吧！
              </div>
              
              <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                  <div v-for="r in myFavorites" :key="r.id" @click="router.push(`/recipe/${r.id}`)" class="bg-white rounded-xl overflow-hidden border border-gray-100 shadow-sm hover:shadow-md transition cursor-pointer group flex flex-col">
                      <div class="h-40 overflow-hidden relative">
                          <img :src="r.coverImage" class="w-full h-full object-cover group-hover:scale-105 transition duration-500">
                          <div class="absolute top-2 right-2 bg-white/90 backdrop-blur px-2 py-1 rounded text-xs font-bold text-orange-500 flex items-center gap-1">
                               <Heart class="w-3 h-3 fill-current" /> 已收藏
                          </div>
                      </div>
                      <div class="p-4 flex-1 flex flex-col justify-between">
                          <div>
                               <h4 class="font-bold text-gray-800 mb-2 line-clamp-1" :title="r.title">{{ r.title }}</h4>
                               <div class="flex items-center gap-2 text-xs text-gray-500">
                                  <UserAvatar 
                                    :src="r.authorAvatar" 
                                    :name="r.authorName"
                                    class="w-5 h-5 flex-shrink-0"
                                  />
                                  <span class="truncate max-w-[100px]">{{ r.authorName }}</span>
                              </div>
                          </div>
                      </div>
                  </div>
              </div>
          </div>



          <div v-if="activeProfileTab === 'following'" class="h-full flex flex-col">
               <div class="flex justify-between items-center mb-6">
                  <h3 class="font-bold text-xl text-gray-800">我的关注 ({{ myFollows.length }})</h3>
              </div>
              
              <div v-if="loading" class="py-10 text-center text-gray-400">
                  加载中...
              </div>
              <div v-else-if="myFollows.length === 0" class="flex-1 flex flex-col items-center justify-center text-gray-400 bg-gray-50 rounded-xl border border-dashed border-gray-200 py-12">
                  <User class="w-12 h-12 mb-4 opacity-20" />
                  <p>暂无关注</p>
              </div>
              
              <div v-else class="space-y-4">
                  <div v-for="user in myFollows" :key="user.id" class="flex items-center gap-4 p-4 border border-gray-100 rounded-xl bg-white hover:border-orange-200 hover:shadow-sm transition group">
                      <UserAvatar 
                        :src="user.avatar" 
                        :name="user.nickname"
                        class="w-14 h-14 bg-gray-100 flex-shrink-0 cursor-pointer"
                        @click="router.push(`/user/${user.id}`)"
                      />
                      <div class="flex-1 min-w-0">
                          <div class="flex items-center gap-2 mb-1">
                              <h4 class="font-bold text-gray-800 truncate text-lg cursor-pointer hover:text-orange-500 transition" @click="router.push(`/user/${user.id}`)">{{ user.nickname }}</h4>
                          </div>
                          <p class="text-sm text-gray-500 truncate">{{ user.intro || '这个人很懒，什么也没写' }}</p>
                      </div>
                      <button 
                          @click="handleUnfollow(user)"
                          class="px-5 py-2 text-gray-500 bg-gray-50 hover:bg-red-50 hover:text-red-600 rounded-lg transition text-sm font-medium flex items-center gap-1.5 opacity-0 group-hover:opacity-100 whitespace-nowrap"
                          title="取消关注"
                      >
                          <UserMinus class="w-4 h-4" /> <span class="hidden md:inline">取消关注</span>
                      </button>
                  </div>
              </div>
          </div>

          <!-- 我的粉丝 -->
          <div v-if="activeProfileTab === 'fans'" class="h-full flex flex-col">
               <div class="flex justify-between items-center mb-6">
                  <h3 class="font-bold text-xl text-gray-800">我的粉丝 ({{ myFans.length }})</h3>
              </div>
              
              <div v-if="loading" class="py-10 text-center text-gray-400">
                  加载中...
              </div>
              <div v-else-if="myFans.length === 0" class="flex-1 flex flex-col items-center justify-center text-gray-400 bg-gray-50 rounded-xl border border-dashed border-gray-200 py-12">
                  <Users class="w-12 h-12 mb-4 opacity-20" />
                  <p>还没有粉丝，快去发布优质内容吧！</p>
              </div>
              
              <div v-else class="space-y-4">
                  <div v-for="user in myFans" :key="user.id" class="flex items-center gap-4 p-4 border border-gray-100 rounded-xl bg-white hover:border-orange-200 hover:shadow-sm transition group">
                      <UserAvatar 
                        :src="user.avatar" 
                        :name="user.nickname"
                        class="w-14 h-14 bg-gray-100 flex-shrink-0 cursor-pointer"
                        @click="router.push(`/user/${user.id}`)"
                      />
                      <div class="flex-1 min-w-0">
                          <div class="flex items-center gap-2 mb-1">
                              <h4 class="font-bold text-gray-800 truncate text-lg cursor-pointer hover:text-orange-500 transition" @click="router.push(`/user/${user.id}`)">{{ user.nickname }}</h4>
                              <span v-if="user.isFollow" class="text-xs bg-gray-100 text-gray-500 px-2 py-0.5 rounded">互相关注</span>
                          </div>
                          <p class="text-sm text-gray-500 truncate">{{ user.intro || '这个人很懒，什么也没写' }}</p>
                      </div>
                      <button 
                          v-if="user.isFollow"
                          @click="handleUnfollow(user)"
                          class="px-5 py-2 text-gray-400 bg-gray-50 hover:bg-red-50 hover:text-red-500 rounded-lg transition text-sm font-medium whitespace-nowrap"
                      >
                          取消关注
                      </button>
                      <button 
                          v-else
                          @click="handleFollow(user)"
                          class="px-5 py-2 text-white bg-orange-500 hover:bg-orange-600 rounded-lg transition text-sm font-medium flex items-center gap-1.5 whitespace-nowrap shadow-sm shadow-orange-200"
                      >
                          <UserPlus class="w-4 h-4" /> 回关
                      </button>
                  </div>
              </div>
          </div>
          
          <div v-if="activeProfileTab === 'settings'" class="h-full flex flex-col">
               <h3 class="font-bold text-xl mb-8 text-gray-800 border-b border-gray-100 pb-4">个人资料设置</h3>
               
               <div class="grid grid-cols-1 md:grid-cols-12 gap-10">
                   <!-- 左侧头像上传 -->
                   <div class="md:col-span-4 flex flex-col items-center pt-4">
                       <div class="relative group mb-4">
                           <input type="file" ref="avatarInput" class="hidden" accept="image/*" @change="handleAvatarChange">
                           <div 
                               @click="triggerAvatarUpload" 
                               class="w-32 h-32 rounded-full cursor-pointer border-4 border-white shadow-xl hover:shadow-2xl transition relative ring-4 ring-orange-50/50 group"
                           >
                                <UserAvatar 
                                    :src="profileForm.avatar" 
                                    :name="userStore.user?.username"
                                    class="w-full h-full text-4xl"
                                />
                               <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 flex items-center justify-center transition backdrop-blur-[2px]">
                                   <div class="text-center">
                                       <component :is="activeProfileTab === 'settings' ? Edit2 : null" class="w-6 h-6 text-white mx-auto mb-1" />
                                       <span class="text-white text-xs font-medium block">更换头像</span>
                                   </div>
                               </div>
                           </div>
                           <div v-if="uploadingAvatar" class="absolute inset-0 bg-white/80 rounded-full flex items-center justify-center z-10">
                               <div class="w-8 h-8 border-3 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
                           </div>
                       </div>
                       <button @click="triggerAvatarUpload" class="text-sm text-orange-500 font-medium hover:text-orange-600">点击更换头像</button>
                       <p class="text-xs text-gray-400 mt-2 text-center">支持 JPG, PNG 格式<br>最大 5MB</p>
                   </div>
                   
                   <!-- 右侧表单 -->
                   <div class="md:col-span-8 max-w-xl space-y-6">
                       <!-- 用户名 -->
                       <div>
                           <label class="block text-sm font-bold text-gray-700 mb-2">用户名</label>
                           <div class="relative">
                               <input type="text" :value="userStore.user?.username" disabled class="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3 text-gray-500 cursor-not-allowed pl-10">
                               <User class="w-4 h-4 text-gray-400 absolute left-3.5 top-3.5" />
                           </div>
                           <p class="text-[10px] text-gray-400 mt-1.5 ml-1">用户名是您的唯一标识，不支持修改</p>
                       </div>
                       
                       <!-- 昵称 -->
                       <div>
                           <label class="block text-sm font-bold text-gray-700 mb-2">昵称</label>
                           <input 
                               type="text" 
                               v-model="profileForm.nickname" 
                               maxlength="20"
                               class="w-full border border-gray-200 rounded-xl px-4 py-3 focus:ring-4 focus:ring-orange-100 focus:border-orange-500 outline-none transition bg-white"
                               placeholder="昵称，1-20个字符"
                           >
                       </div>
                       
                       <!-- 个人简介 -->
                       <div>
                           <label class="block text-sm font-bold text-gray-700 mb-2 flex justify-between">
                               <span>个人简介</span>
                               <span class="text-xs font-normal text-gray-400">{{ profileForm.intro?.length || 0 }}/200</span>
                           </label>
                           <textarea 
                               v-model="profileForm.intro" 
                               maxlength="200"
                               rows="4"
                               class="w-full border border-gray-200 rounded-xl px-4 py-3 focus:ring-4 focus:ring-orange-100 focus:border-orange-500 outline-none transition resize-none bg-white leading-relaxed"
                               placeholder="介绍一下自己，分享你的美食故事..."
                           ></textarea>
                       </div>
                       
                       <!-- 操作按钮 -->
                       <div class="pt-6 flex gap-3">
                           <button 
                               @click="saveProfile" 
                               :disabled="savingProfile"
                               class="bg-gradient-to-r from-orange-500 to-red-500 text-white px-10 py-3 rounded-xl hover:from-orange-600 hover:to-red-600 font-bold shadow-lg shadow-orange-200/50 transition disabled:opacity-70 disabled:grayscale flex items-center justify-center gap-2 transform active:scale-[0.98]"
                           >
                               <div v-if="savingProfile" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                               {{ savingProfile ? '正在保存...' : '保存修改' }}
                           </button>
                           <button 
                               @click="cancelEdit"
                               class="px-6 py-3 text-gray-600 bg-gray-100 hover:bg-gray-200 font-medium rounded-xl transition"
                           >
                               取消修改
                           </button>
                       </div>
                       
                       <!-- 安全设置分割线 -->
                       <div class="border-t border-gray-100 pt-6 mt-6">
                            <h4 class="font-bold text-gray-800 mb-4 flex items-center gap-2">
                                <Lock class="w-4 h-4 text-orange-500" />
                                安全设置
                            </h4>
                            <button 
                                @click="openPasswordModal"
                                class="text-sm bg-gray-50 text-gray-600 hover:text-orange-600 hover:bg-orange-50 px-4 py-2.5 rounded-xl border border-gray-200 transition flex items-center gap-2 font-medium"
                            >
                                <Lock class="w-4 h-4" />
                                修改登录密码
                            </button>
                       </div>
                   </div>
               </div>
          </div>
      </div>
    </div>
    <!-- 修改密码弹窗 -->
    <div v-if="showPasswordModal" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
        <div class="bg-white rounded-2xl w-full max-w-md p-6 shadow-2xl animate-fade-in relative">
            <button @click="showPasswordModal = false" class="absolute right-4 top-4 p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-full transition">
                <X class="w-5 h-5" />
            </button>
            
            <h3 class="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
                <div class="p-2 bg-orange-100 rounded-lg">
                    <Lock class="w-5 h-5 text-orange-500" />
                </div>
                修改密码
            </h3>
            
            <div class="space-y-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1.5">当前密码</label>
                    <input 
                        v-model="passwordForm.oldPassword" 
                        type="password" 
                        placeholder="请输入当前使用的密码"
                        class="w-full border border-gray-200 rounded-xl px-4 py-3 bg-gray-50 focus:bg-white focus:ring-4 focus:ring-orange-100 focus:border-orange-500 transition outline-none"
                    >
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1.5">新密码</label>
                    <input 
                        v-model="passwordForm.newPassword" 
                        type="password" 
                        placeholder="请输入新密码 (至少6位)"
                        class="w-full border border-gray-200 rounded-xl px-4 py-3 bg-gray-50 focus:bg-white focus:ring-4 focus:ring-orange-100 focus:border-orange-500 transition outline-none"
                    >
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1.5">确认新密码</label>
                    <input 
                        v-model="passwordForm.confirmPassword" 
                        type="password" 
                        placeholder="请再次输入新密码"
                        class="w-full border border-gray-200 rounded-xl px-4 py-3 bg-gray-50 focus:bg-white focus:ring-4 focus:ring-orange-100 focus:border-orange-500 transition outline-none"
                    >
                </div>
            </div>
            
            <div class="flex gap-3 mt-8">
                <button 
                    @click="showPasswordModal = false"
                    class="flex-1 py-3 text-gray-600 bg-gray-100 hover:bg-gray-200 font-bold rounded-xl transition"
                >取消</button>
                <button 
                    @click="handlePasswordChange"
                    :disabled="changingPassword"
                    class="flex-1 py-3 text-white bg-gradient-to-r from-orange-500 to-red-500 hover:from-orange-600 hover:to-red-600 font-bold rounded-xl shadow-lg shadow-orange-200/50 transition flex items-center justify-center gap-2 disabled:opacity-70 disabled:grayscale"
                >
                    <div v-if="changingPassword" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    {{ changingPassword ? '修改中...' : '确认修改' }}
                </button>
            </div>
        </div>
    </div>
  </div>
</template>
