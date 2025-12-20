<script setup>
import { ref, onMounted, watch } from 'vue' // Added watch
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { useToast } from '../components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { listRecipies, deleteRecipe as deleteRecipeApi, unpublishRecipe as unpublishRecipeApi } from '@/api/recipe'
import { updateProfile, getProfile } from '@/api/auth'
import { uploadFile } from '@/api/common'
import { getMyFavorites, getMyFollows, unfollowUser } from '@/api/social' 
import { ChefHat, Heart, User, MessageCircle, Settings, Edit2, Trash2, X, Send, ArrowDown, UserMinus } from 'lucide-vue-next'



const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()
const { confirm } = useModal()

const activeProfileTab = ref('recipes')
const chatTarget = ref(null)

const profileTabs = [
    { id: 'recipes', icon: ChefHat, label: '我的菜谱' },
    { id: 'favorites', icon: Heart, label: '我的收藏' },
    { id: 'following', icon: User, label: '我的关注' },

    { id: 'settings', icon: Settings, label: '个人信息' },
]

// Real data refs
const myRecipes = ref([])
const myFavorites = ref([]) // Store favorites
const myFollows = ref([]) // Empty for now, API missing
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

// 初始化加载用户资料
const loadProfile = async () => {
    try {
        const userData = await getProfile()
        profileForm.value = {
            nickname: userData.nickname || '',
            avatar: userData.avatar || '',
            intro: userData.intro || ''
        }
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

// 取消关注
const handleUnfollow = async (userId) => {
    const confirmed = await confirm('确定取消关注吗？')
    if (confirmed) {
        try {
            await unfollowUser(userId)
            myFollows.value = myFollows.value.filter(u => u.id !== userId)
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
</script>


<template>
  <div class="max-w-6xl mx-auto p-4 flex flex-col md:flex-row gap-6 pt-24">
    <!-- 侧边栏 -->
    <div class="w-full md:w-64 bg-white rounded-xl shadow-sm p-6 h-fit border border-gray-100">
        <div class="text-center mb-6">
            <div class="w-20 h-20 rounded-full mx-auto mb-3 border-4 border-white shadow-lg overflow-hidden">
                <img v-if="userStore.user?.avatar" :src="userStore.user.avatar" class="w-full h-full object-cover">
                <div v-else class="w-full h-full bg-orange-100 flex items-center justify-center text-4xl text-orange-600 font-bold">
                    {{ userStore.user?.username?.charAt(0).toUpperCase() || 'U' }}
                </div>
            </div>
            <h2 class="font-bold text-lg text-gray-800">{{ userStore.user?.nickname || userStore.user?.username }}</h2>
            <p class="text-xs text-gray-500 bg-gray-100 px-2 py-1 rounded-full inline-block mt-1">ID: {{ userStore.user?.id }}</p>
        </div>
        <div class="space-y-1">
            <button 
                v-for="item in profileTabs" 
                :key="item.id"
                @click="activeProfileTab = item.id"
                :class="['w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm transition font-medium', activeProfileTab === item.id ? 'bg-orange-50 text-orange-600 shadow-sm' : 'text-gray-600 hover:bg-gray-50']"
            >
                <component :is="item.icon" class="w-4 h-4" />
                {{ item.label }}
            </button>
        </div>
    </div>

    <!-- 个人中心内容区 -->
    <div class="flex-1 bg-white rounded-xl shadow-sm p-6 min-h-[500px] border border-gray-100">
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
                            <div v-else class="text-right">
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
                                <img v-if="r.authorAvatar" :src="r.authorAvatar" class="w-5 h-5 rounded-full object-cover">
                                <span class="truncate max-w-[100px]">{{ r.authorName }}</span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>


        
        <!-- 我的关注 -->
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
                    <div class="w-14 h-14 rounded-full overflow-hidden bg-gray-100 flex-shrink-0 cursor-pointer" @click="router.push(`/user/${user.id}`)">
                        <img v-if="user.avatar" :src="user.avatar" class="w-full h-full object-cover">
                        <div v-else class="w-full h-full bg-gradient-to-br from-orange-400 to-red-400 flex items-center justify-center text-white font-bold text-xl">
                            {{ user.nickname?.charAt(0) }}
                        </div>
                    </div>
                    <div class="flex-1 min-w-0">
                        <div class="flex items-center gap-2 mb-1">
                            <h4 class="font-bold text-gray-800 truncate text-lg cursor-pointer hover:text-orange-500 transition" @click="router.push(`/user/${user.id}`)">{{ user.nickname }}</h4>
                        </div>
                        <p class="text-sm text-gray-500 truncate">{{ user.intro || '这个人很懒，什么也没写' }}</p>
                    </div>
                    <button 
                        @click="handleUnfollow(user.id)"
                        class="px-4 py-2 text-gray-500 bg-gray-50 hover:bg-red-50 hover:text-red-600 rounded-lg transition text-sm font-medium flex items-center gap-2 opacity-0 group-hover:opacity-100"
                        title="取消关注"
                    >
                        <UserMinus class="w-4 h-4" /> <span class="hidden md:inline">取消关注</span>
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
                             class="w-32 h-32 rounded-full overflow-hidden cursor-pointer border-4 border-white shadow-xl hover:shadow-2xl transition relative ring-4 ring-orange-50/50"
                         >
                             <img v-if="profileForm.avatar" :src="profileForm.avatar" class="w-full h-full object-cover">
                             <div v-else class="w-full h-full bg-orange-100 flex items-center justify-center text-4xl font-bold text-orange-600">
                                 {{ userStore.user?.username?.charAt(0).toUpperCase() || 'U' }}
                             </div>
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
                             maxlength="50"
                             class="w-full border border-gray-200 rounded-xl px-4 py-3 focus:ring-4 focus:ring-orange-100 focus:border-orange-500 outline-none transition bg-white"
                             placeholder="给自己取一个好听的昵称"
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
                     
                     <!-- 保存按钮 -->
                     <div class="pt-6">
                         <button 
                             @click="saveProfile" 
                             :disabled="savingProfile"
                             class="w-full md:w-auto bg-gradient-to-r from-orange-500 to-red-500 text-white px-10 py-3 rounded-xl hover:from-orange-600 hover:to-red-600 font-bold shadow-lg shadow-orange-200/50 transition disabled:opacity-70 disabled:grayscale flex items-center justify-center gap-2 transform active:scale-[0.98]"
                         >
                             <div v-if="savingProfile" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                             {{ savingProfile ? '正在保存...' : '保存修改' }}
                         </button>
                     </div>
                 </div>
             </div>
        </div>
    </div>
  </div>
</template>
