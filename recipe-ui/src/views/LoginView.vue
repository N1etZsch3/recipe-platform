<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { login, register, getCaptcha, forceLogin } from '@/api/auth'
import { User, Lock, Smile, ArrowRight, Loader2, RefreshCw, ShieldCheck } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const { showToast } = useToast()
const { confirm } = useModal()

const isLogin = ref(true)
const loading = ref(false)
const captchaLoading = ref(false)

const form = ref({
  username: '',
  password: '',
  nickname: '',
  captchaId: '',
  captchaCode: ''
})

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

// 处理登录成功
const handleLoginSuccess = (res) => {
  userStore.setToken(res.token)
  userStore.setUser({
      id: res.userId,
      username: form.value.username,
      nickname: res.nickname,
      avatar: res.avatar,
      role: res.role 
  })
  showToast('登录成功')
  router.push('/')
}

// 执行强制登录
const doForceLogin = async () => {
  loading.value = true
  try {
    // 需要重新获取验证码
    await fetchCaptcha()
    showToast('请重新输入验证码后确认登录')
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.value.username || !form.value.password) {
    showToast('请输入用户名和密码')
    return
  }
  
  // 注册时的字段验证
  if (!isLogin.value) {
    // 用户名验证：6-12位，数字字母下划线
    const usernameRegex = /^[a-zA-Z0-9_]{6,12}$/
    if (!usernameRegex.test(form.value.username)) {
      showToast('用户名必须为6-12位，只能包含字母、数字和下划线')
      return
    }
    
    // 昵称验证：1-20字符必填
    if (!form.value.nickname || form.value.nickname.length < 1 || form.value.nickname.length > 20) {
      showToast('昵称长度必须为1-20个字符')
      return
    }
    
    // 密码验证：数字字母下划线
    const passwordRegex = /^[a-zA-Z0-9_]+$/
    if (!passwordRegex.test(form.value.password)) {
      showToast('密码只能包含字母、数字和下划线')
      return
    }
  }
  
  if (!form.value.captchaCode) {
    showToast('请输入验证码')
    return
  }

  loading.value = true
  try {
    if (isLogin.value) {
      const res = await login({ 
        username: form.value.username, 
        password: form.value.password,
        captchaId: form.value.captchaId,
        captchaCode: form.value.captchaCode
      })
      handleLoginSuccess(res)
    } else {
      await register({ 
        username: form.value.username, 
        password: form.value.password, 
        nickname: form.value.nickname,
        captchaId: form.value.captchaId,
        captchaCode: form.value.captchaCode
      })
      showToast('注册成功，请登录')
      isLogin.value = true
      fetchCaptcha()
    }
  } catch (error) {
    // 检查是否是 409 冲突（账号已在其他设备登录）
    if (error.code === 409) {
      // 获取后端返回的 forceLoginToken
      const forceLoginToken = error.data?.forceLoginToken
      const confirmResult = await confirm(
        error.data?.message || error.message || '该账号已在其他设备登录，是否强制登录？',
        { danger: true, confirmText: '强制登录', cancelText: '取消' }
      )
      if (confirmResult && forceLoginToken) {
        // 用户确认强制登录 - 使用 forceLoginToken
        try {
          const res = await forceLogin({ 
            username: form.value.username, 
            password: form.value.password,
            forceLoginToken: forceLoginToken
          })
          handleLoginSuccess(res)
          return  // 成功后直接返回
        } catch (e) {
          showToast(e.message || '强制登录失败')
          fetchCaptcha()
        }
      } else {
        fetchCaptcha()
      }
    } else {
      showToast(error.message || '操作失败')
      fetchCaptcha() // 刷新验证码
    }
  } finally {
    loading.value = false
    form.value.captchaCode = '' // 清空验证码
  }
}

const toggleMode = () => {
  isLogin.value = !isLogin.value
  form.value = { username: '', password: '', nickname: '', captchaId: form.value.captchaId, captchaCode: '' }
}

const handleVisitor = () => {
    userStore.logout()
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
                :placeholder="isLogin ? '请输入用户名' : '6-12位字母、数字或下划线'"
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
                placeholder="昵称，1-20个字符"
                maxlength="20"
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
                :placeholder="isLogin ? '请输入密码' : '字母、数字或下划线'"
              >
            </div>
          </div>

          <!-- 验证码 -->
          <div>
            <label class="block text-sm font-bold text-gray-700 mb-2">验证码</label>
            <div class="flex gap-3">
              <div class="relative flex-1">
                <ShieldCheck class="absolute left-3 top-3 text-gray-400 w-5 h-5" />
                <input 
                  v-model="form.captchaCode" 
                  type="text" 
                  class="w-full pl-10 pr-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-orange-200 transition bg-gray-50 focus:bg-white"
                  placeholder="请输入验证码"
                  @keyup.enter="handleSubmit"
                >
              </div>
              <div 
                @click="fetchCaptcha"
                class="w-32 h-12 rounded-lg border bg-gray-50 cursor-pointer overflow-hidden flex items-center justify-center hover:border-orange-300 transition"
                title="点击刷新验证码"
              >
                <RefreshCw v-if="captchaLoading" class="w-5 h-5 text-gray-400 animate-spin" />
                <img v-else-if="captchaImage" :src="captchaImage" alt="验证码" class="h-full w-full object-cover" />
                <span v-else class="text-gray-400 text-sm">加载中...</span>
              </div>
            </div>
          </div>

          <button 
            @click="handleSubmit()" 
            :disabled="loading"
            class="w-full py-3 rounded-lg transition flex items-center justify-center gap-2 font-bold shadow-md disabled:opacity-70 disabled:cursor-not-allowed bg-orange-500 hover:bg-orange-600 text-white"
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
