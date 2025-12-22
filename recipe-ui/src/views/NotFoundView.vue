<script setup>
import { useRouter } from 'vue-router'
import { Home, ArrowLeft, Search, ChefHat } from 'lucide-vue-next'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-orange-50 via-white to-red-50 flex items-center justify-center p-4">
    <!-- Main Container -->
    <div class="max-w-2xl w-full text-center">
      <!-- Animated 404 -->
      <div class="relative mb-8">
        <!-- Background Decoration -->
        <div class="absolute inset-0 flex items-center justify-center opacity-10">
          <ChefHat class="w-64 h-64 text-orange-500 animate-pulse" />
        </div>
        
        <!-- 404 Number -->
        <div class="relative">
          <h1 class="text-[180px] md:text-[220px] font-black text-transparent bg-clip-text bg-gradient-to-r from-orange-400 via-red-500 to-orange-600 leading-none select-none">
            404
          </h1>
        </div>
      </div>

      <!-- Message -->
      <div class="space-y-4 mb-12">
        <h2 class="text-3xl md:text-4xl font-bold text-gray-800">
          哎呀，页面走丢了！
        </h2>
        <p class="text-lg text-gray-500 max-w-md mx-auto">
          您访问的页面不存在或已被移除。<br>
          不如回到首页，探索更多美味食谱？
        </p>
      </div>

      <!-- Decorative Illustration -->
      <div class="flex justify-center items-center gap-4 mb-12 text-gray-300">
        <div class="w-16 h-16 rounded-full bg-gradient-to-br from-orange-100 to-orange-200 flex items-center justify-center">
          <span class="text-3xl">🍳</span>
        </div>
        <div class="w-12 h-12 rounded-full bg-gradient-to-br from-red-100 to-red-200 flex items-center justify-center">
          <span class="text-2xl">🥗</span>
        </div>
        <div class="w-20 h-20 rounded-full bg-gradient-to-br from-yellow-100 to-yellow-200 flex items-center justify-center">
          <Search class="w-8 h-8 text-yellow-500" />
        </div>
        <div class="w-12 h-12 rounded-full bg-gradient-to-br from-green-100 to-green-200 flex items-center justify-center">
          <span class="text-2xl">🍜</span>
        </div>
        <div class="w-16 h-16 rounded-full bg-gradient-to-br from-blue-100 to-blue-200 flex items-center justify-center">
          <span class="text-3xl">🍰</span>
        </div>
      </div>

      <!-- Action Buttons -->
      <div class="flex flex-col sm:flex-row gap-4 justify-center">
        <button
          @click="goBack"
          class="group inline-flex items-center justify-center gap-2 px-8 py-4 rounded-2xl bg-white border-2 border-gray-200 text-gray-700 font-semibold shadow-sm hover:shadow-md hover:border-gray-300 transition-all duration-300"
        >
          <ArrowLeft class="w-5 h-5 group-hover:-translate-x-1 transition-transform" />
          返回上一页
        </button>
        
        <button
          @click="goHome"
          class="group inline-flex items-center justify-center gap-2 px-8 py-4 rounded-2xl bg-gradient-to-r from-orange-500 to-red-500 text-white font-semibold shadow-lg hover:shadow-xl hover:scale-[1.02] transition-all duration-300"
        >
          <Home class="w-5 h-5 group-hover:scale-110 transition-transform" />
          回到首页
        </button>
      </div>

      <!-- Footer Hint -->
      <p class="mt-12 text-sm text-gray-400">
        如有问题，请联系网站管理员
      </p>
    </div>
  </div>
</template>

<style scoped>
/* Animation for the 404 text */
@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

h1 {
  animation: float 3s ease-in-out infinite;
}
</style>
