<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/components/Toast.vue'
import { login, register } from '@/api/auth'
import { User, Lock, Smile, ArrowRight, Loader2 } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()

const isLogin = ref(true)
const loading = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: ''
})

const handleSubmit = async () => {
  if (!form.value.username || !form.value.password) {
    showToast('请输入用户名和密码')
    return
  }
  if (!isLogin.value && !form.value.nickname) {
    showToast('请输入昵称')
    return
  }

  loading.value = true
  try {
    if (isLogin.value) {
      const res = await login({ username: form.value.username, password: form.value.password })
      // res is the data map from backend
      userStore.setToken(res.token)
      userStore.setUser({
          id: res.userId,
          username: form.value.username, // username not in return map but we know it
          nickname: res.nickname,
          avatar: res.avatar,
          role: res.role 
      })
      showToast('登录成功')
      router.push('/')
    } else {
      await register({ username: form.value.username, password: form.value.password, nickname: form.value.nickname })
      showToast('注册成功，请登录')
      isLogin.value = true
    }
  } catch (error) {
    showToast(error.message || '操作失败')
  } finally {
    loading.value = false
  }
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  form.value = { username: '', password: '', nickname: '' }
}

const handleVisitor = () => {
    userStore.logout() // Clear any stale data
    showToast('欢迎游客访问')
    router.push('/')
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center p-4 pt-24">
    <div class="bg-white p-8 rounded-2xl shadow-xl w-full max-w-md border-t-4 border-orange-500 overflow-hidden relative">
      
      <div class="text-center mb-8">
        <h2 class="text-3xl font-bold text-orange-600 mb-2">三食六记</h2>
        <p class="text-gray-400 text-sm">{{ isLogin ? '记录美食，分享生活' : '加入我们，开启美食之旅' }}</p>
      </div>

      <Transition name="slide-fade" mode="out-in">
        <div :key="isLogin ? 'login' : 'register'" class="space-y-5">
          <div>
            <label class="block text-sm font-bold text-gray-700 mb-2">用户名</label>
            <div class="relative">
              <User class="absolute left-3 top-3 text-gray-400 w-5 h-5" />
              <input 
                v-model="form.username" 
                type="text" 
                class="w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-200 transition bg-gray-50 focus:bg-white"
                placeholder="请输入用户名"
              >
            </div>
          </div>

          <div v-if="!isLogin">
            <label class="block text-sm font-bold text-gray-700 mb-2">昵称</label>
            <div class="relative">
              <Smile class="absolute left-3 top-3 text-gray-400 w-5 h-5" />
              <input 
                v-model="form.nickname" 
                type="text" 
                class="w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-200 transition bg-gray-50 focus:bg-white"
                placeholder="大家怎么称呼你？"
              >
            </div>
          </div>

          <div>
            <label class="block text-sm font-bold text-gray-700 mb-2">密码</label>
            <div class="relative">
              <Lock class="absolute left-3 top-3 text-gray-400 w-5 h-5" />
              <input 
                v-model="form.password" 
                type="password" 
                class="w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-200 transition bg-gray-50 focus:bg-white"
                placeholder="请输入密码"
                @keyup.enter="handleSubmit"
              >
            </div>
          </div>

          <button 
            @click="handleSubmit" 
            :disabled="loading"
            class="w-full bg-orange-500 text-white py-3 rounded-lg hover:bg-orange-600 transition flex items-center justify-center gap-2 font-bold shadow-md disabled:opacity-70 disabled:cursor-not-allowed"
          >
            <Loader2 v-if="loading" class="w-5 h-5 animate-spin" />
            <span v-else>{{ isLogin ? '登录' : '立即注册' }}</span>
            <ArrowRight v-if="!loading" class="w-4 h-4" />
          </button>
        </div>
      </Transition>

      <div class="mt-6 text-center text-sm text-gray-500">
        {{ isLogin ? '还没有账号？' : '已有账号？' }}
        <button @click="toggleMode" class="text-orange-600 font-bold hover:underline transition">
          {{ isLogin ? '去注册' : '去登录' }}
        </button>
      </div>
      
      <div class="mt-8 pt-6 border-t border-gray-100">
        <button @click="handleVisitor" class="w-full py-2 text-gray-400 hover:text-gray-600 font-medium text-sm flex items-center justify-center gap-1 transition">
             只是随便看看？ <span class="underline">游客访问</span>
        </button>
      </div>

    </div>
  </div>
</template>

<style scoped>
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease-out;
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
