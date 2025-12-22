<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin } from '@/api/admin'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/components/Toast.vue'
import { ChefHat, Lock, User, Eye, EyeOff, Utensils, Coffee, Soup, RefreshCw, ShieldCheck, Loader2 } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()

const form = ref({
    username: '',
    password: '',
    captchaId: '',
    captchaCode: ''
})
const loading = ref(false)
const showPassword = ref(false)
const captchaLoading = ref(false)
const captchaImage = ref('')

// 获取验证码
const fetchCaptcha = async () => {
    captchaLoading.value = true
    try {
        const res = await getCaptcha()
        form.value.captchaId = res.captchaId
        captchaImage.value = res.imageBase64
    } catch (error) {
        showToast('获取验证码失败')
    } finally {
        captchaLoading.value = false
    }
}

onMounted(() => {
    fetchCaptcha()
})

const handleLogin = async () => {
    if (!form.value.username.trim() || !form.value.password.trim()) {
        showToast('请输入用户名和密码')
        return
    }
    
    if (!form.value.captchaCode.trim()) {
        showToast('请输入验证码')
        return
    }

    loading.value = true
    try {
        const res = await adminLogin(form.value)
        if (res) {
            userStore.setToken(res.token)
            userStore.setUser({
                id: res.userId,
                nickname: res.nickname,
                role: res.role,
                avatar: res.avatar
            })
            showToast('登录成功')
            router.push('/backstage-m9x2k7')
        }
    } catch (error) {
        console.error('Login failed:', error)
        showToast(error.message || '登录失败')
        // 刷新验证码
        fetchCaptcha()
        form.value.captchaCode = ''
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="min-h-screen flex">
        <!-- 左侧品牌展示区 -->
        <div class="hidden lg:flex lg:w-3/5 relative overflow-hidden bg-gradient-to-br from-orange-50 via-white to-amber-50">
            <!-- 装饰背景 -->
            <div class="absolute top-0 right-0 w-96 h-96 bg-orange-100 rounded-full blur-3xl opacity-30 -translate-y-1/2 translate-x-1/3"></div>
            <div class="absolute bottom-0 left-0 w-[500px] h-[500px] bg-amber-100 rounded-full blur-3xl opacity-30 translate-y-1/3 -translate-x-1/4"></div>

            <!-- Logo 区域 -->
            <div class="absolute top-10 left-10 flex items-center gap-3 z-10">
                <div class="w-12 h-12 bg-gradient-to-br from-orange-500 to-amber-500 rounded-2xl flex items-center justify-center shadow-lg shadow-orange-200/50">
                    <ChefHat class="w-7 h-7 text-white" />
                </div>
                <span class="text-2xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-gray-900 to-gray-700">三食六记</span>
            </div>

            <!-- 中央内容区域 -->
            <div class="flex-1 flex flex-col items-center justify-center p-12 relative z-10">
                <!-- 装饰插画替代 -->
                <div class="relative w-full max-w-lg aspect-[4/3] flex items-center justify-center mb-12">
                    <!-- 圆环背景 -->
                    <div class="absolute inset-0 border border-orange-100 rounded-full scale-110 animate-[spin_60s_linear_infinite]"></div>
                    <div class="absolute inset-4 border border-dashed border-orange-200 rounded-full animate-[spin_40s_linear_infinite_reverse]"></div>
                    
                    <!-- 中心图标 -->
                    <div class="relative bg-white p-8 rounded-full shadow-2xl shadow-orange-100/50 flex flex-col items-center justify-center z-20 w-48 h-48 animate-float">
                        <div class="w-20 h-20 bg-orange-50 rounded-2xl flex items-center justify-center mb-2">
                             <Utensils class="w-10 h-10 text-orange-500" />
                        </div>
                        <span class="text-sm font-medium text-gray-500 mt-2">汇聚美味</span>
                    </div>

                    <!-- 浮动标签 - 中式 -->
                    <div class="absolute top-[10%] right-[10%] bg-white px-4 py-2 rounded-full shadow-lg shadow-amber-100/50 animate-float-delayed flex items-center gap-2">
                        <div class="p-1.5 bg-amber-50 rounded-full">
                            <Soup class="w-4 h-4 text-amber-500" />
                        </div>
                        <span class="text-xs font-bold text-gray-600">中式佳肴</span>
                    </div>

                    <!-- 浮动标签 - 烘焙 -->
                    <div class="absolute bottom-[20%] left-[5%] bg-white px-4 py-2 rounded-full shadow-lg shadow-orange-100/50 animate-float flex items-center gap-2" style="animation-delay: 1.5s">
                        <div class="p-1.5 bg-orange-50 rounded-full">
                            <Coffee class="w-4 h-4 text-orange-500" />
                        </div>
                        <span class="text-xs font-bold text-gray-600">西点烘焙</span>
                    </div>

                     <!-- 浮动标签 - 灵感 -->
                    <div class="absolute bottom-[20%] right-[5%] bg-white px-4 py-2 rounded-full shadow-lg shadow-red-100/50 animate-float-delayed" style="animation-delay: 4s">
                        <span class="text-xs font-bold text-gray-600">每日灵感 🔥</span>
                    </div>
                </div>

                <!-- 品牌标语 -->
                <div class="text-center space-y-4 max-w-md">
                    <h2 class="text-4xl font-bold text-gray-800 tracking-tight leading-tight">
                        汇聚全球美味<br>连接每一个热爱烹饪的灵魂
                    </h2>
                    <p class="text-lg text-gray-500 font-light">
                        专业、高效、安全的美食社区管理平台
                    </p>
                </div>
            </div>
        </div>

        <!-- 右侧登录表单区 -->
        <div class="w-full lg:w-2/5 flex flex-col bg-white">
            <div class="flex-1 flex items-center justify-center px-8 lg:px-16 sm:px-12">
                <div class="w-full max-w-[400px]">
                    <!-- 标题 -->
                    <div class="mb-10">
                        <h1 class="text-3xl font-bold text-gray-900 mb-2">欢迎回来</h1>
                        <p class="text-gray-500">请登录您的管理员账号</p>
                    </div>

                    <form @submit.prevent="handleLogin" class="space-y-6">
                        <!-- 用户名 -->
                        <div class="space-y-2">
                            <label class="text-sm font-medium text-gray-700">账号</label>
                            <div class="relative group">
                                <input
                                    v-model="form.username"
                                    type="text"
                                    placeholder="请输入管理员账号"
                                    class="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3.5 pl-11 text-gray-900 focus:bg-white focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition-all"
                                />
                                <User class="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 group-focus-within:text-orange-500 transition-colors" />
                            </div>
                        </div>

                        <!-- 密码 -->
                        <div class="space-y-2">
                            <label class="text-sm font-medium text-gray-700">密码</label>
                            <div class="relative group">
                                <input
                                    v-model="form.password"
                                    :type="showPassword ? 'text' : 'password'"
                                    placeholder="请输入密码"
                                    class="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3.5 pl-11 text-gray-900 focus:bg-white focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition-all"
                                />
                                <Lock class="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 group-focus-within:text-orange-500 transition-colors" />
                                <button
                                    type="button"
                                    @click="showPassword = !showPassword"
                                    class="absolute right-4 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 transition"
                                >
                                    <component :is="showPassword ? EyeOff : Eye" class="w-5 h-5" />
                                </button>
                            </div>
                        </div>

                        <!-- 验证码 -->
                        <div class="space-y-2">
                            <label class="text-sm font-medium text-gray-700">验证码</label>
                            <div class="flex gap-3">
                                <div class="relative group flex-1">
                                    <input
                                        v-model="form.captchaCode"
                                        type="text"
                                        placeholder="请输入验证码"
                                        class="w-full bg-gray-50 border border-gray-200 rounded-xl px-4 py-3.5 pl-11 text-gray-900 focus:bg-white focus:border-orange-500 focus:ring-4 focus:ring-orange-500/10 outline-none transition-all"
                                    />
                                    <ShieldCheck class="w-5 h-5 text-gray-400 absolute left-4 top-1/2 -translate-y-1/2 group-focus-within:text-orange-500 transition-colors" />
                                </div>
                                <div 
                                    @click="fetchCaptcha" 
                                    class="w-28 h-[52px] bg-gray-50 border border-gray-200 rounded-xl overflow-hidden cursor-pointer hover:border-orange-400 transition-colors flex items-center justify-center"
                                >
                                    <Loader2 v-if="captchaLoading" class="w-5 h-5 text-gray-400 animate-spin" />
                                    <img v-else-if="captchaImage" :src="captchaImage" alt="验证码" class="h-full w-full object-contain" />
                                    <RefreshCw v-else class="w-5 h-5 text-gray-400" />
                                </div>
                            </div>
                        </div>

                        <!-- 登录按钮 -->
                        <button
                            type="submit"
                            :disabled="loading"
                            class="w-full bg-gradient-to-r from-orange-500 to-amber-500 hover:from-orange-600 hover:to-amber-600 text-white py-3.5 rounded-xl font-medium transition-all shadow-lg shadow-orange-500/30 hover:shadow-orange-500/40 disabled:opacity-50 disabled:cursor-not-allowed disabled:shadow-none translate-y-0 active:translate-y-0.5"
                        >
                            <span v-if="loading" class="flex items-center justify-center gap-2">
                                <div class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                                登录中...
                            </span>
                            <span v-else>立即登录</span>
                        </button>
                    </form>
                </div>
            </div>
            
            <!-- 底部版权 -->
            <div class="p-6 text-center">
                <p class="text-xs text-gray-400">© 2025 Recipe Platform. All rights reserved.</p>
            </div>
        </div>
    </div>
</template>

<style scoped>
.animate-float {
    animation: float 6s ease-in-out infinite;
}
.animate-float-delayed {
    animation: float 6s ease-in-out infinite;
    animation-delay: 3s;
}
@keyframes float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-20px); }
}
</style>
