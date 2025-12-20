<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminLogin } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/components/Toast.vue'
import { ChefHat, Lock, User, Eye, EyeOff } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()

const form = ref({
    username: '',
    password: ''
})
const loading = ref(false)
const showPassword = ref(false)

const handleLogin = async () => {
    if (!form.value.username.trim() || !form.value.password.trim()) {
        showToast('请输入用户名和密码')
        return
    }

    loading.value = true
    try {
        const res = await adminLogin(form.value)
        if (res) {
            // 保存用户信息到 store
            userStore.setToken(res.token)
            userStore.setUser({
                id: res.userId,
                nickname: res.nickname,
                role: res.role,
                avatar: res.avatar
            })
            showToast('登录成功')
            // 管理员登录后直接进入后台
            router.push('/backstage-m9x2k7')
        }
    } catch (error) {
        console.error('Login failed:', error)
        showToast(error.message || '登录失败')
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div class="min-h-screen bg-gray-900 flex items-center justify-center p-4">
        <div class="w-full max-w-md">
            <!-- Logo -->
            <div class="text-center mb-8">
                <div class="inline-flex items-center justify-center w-16 h-16 bg-orange-500 rounded-2xl mb-4">
                    <ChefHat class="w-8 h-8 text-white" />
                </div>
                <h1 class="text-2xl font-bold text-white">管理后台</h1>
                <p class="text-gray-400 mt-2">三食六记 · 管理员专用入口</p>
            </div>

            <!-- Login Form -->
            <div class="bg-gray-800 rounded-2xl p-8 shadow-2xl">
                <form @submit.prevent="handleLogin" class="space-y-6">
                    <!-- Username -->
                    <div>
                        <label class="block text-sm text-gray-400 mb-2">账号</label>
                        <div class="relative">
                            <User class="w-5 h-5 text-gray-500 absolute left-3 top-1/2 -translate-y-1/2" />
                            <input
                                v-model="form.username"
                                type="text"
                                placeholder="请输入管理员账号"
                                class="w-full bg-gray-700 text-white pl-10 pr-4 py-3 rounded-lg border border-gray-600 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 outline-none transition"
                            />
                        </div>
                    </div>

                    <!-- Password -->
                    <div>
                        <label class="block text-sm text-gray-400 mb-2">密码</label>
                        <div class="relative">
                            <Lock class="w-5 h-5 text-gray-500 absolute left-3 top-1/2 -translate-y-1/2" />
                            <input
                                v-model="form.password"
                                :type="showPassword ? 'text' : 'password'"
                                placeholder="请输入密码"
                                class="w-full bg-gray-700 text-white pl-10 pr-12 py-3 rounded-lg border border-gray-600 focus:border-orange-500 focus:ring-2 focus:ring-orange-500/20 outline-none transition"
                            />
                            <button
                                type="button"
                                @click="showPassword = !showPassword"
                                class="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                            >
                                <Eye v-if="!showPassword" class="w-5 h-5" />
                                <EyeOff v-else class="w-5 h-5" />
                            </button>
                        </div>
                    </div>

                    <!-- Submit -->
                    <button
                        type="submit"
                        :disabled="loading"
                        class="w-full bg-orange-500 hover:bg-orange-600 text-white py-3 rounded-lg font-medium transition disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {{ loading ? '登录中...' : '登录' }}
                    </button>
                </form>

                <div class="mt-6 text-center">
                    <p class="text-gray-500 text-sm">仅限授权管理员使用</p>
                </div>
            </div>
        </div>
    </div>
</template>
