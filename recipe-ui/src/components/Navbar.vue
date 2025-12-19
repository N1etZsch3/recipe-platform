<script setup>
import { useUserStore } from '../stores/user'
import { useRouter, useRoute } from 'vue-router'
import { computed, inject, ref } from 'vue'
import { LogOut, User, ChefHat, MessageSquare, Menu, X } from 'lucide-vue-next'
import { useToast } from './Toast.vue'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()
const confirm = inject('confirm')
const { showToast } = useToast()

const isLoginPage = computed(() => route.name === 'login')

const isOpen = ref(false)

const handleLogout = async () => {
  if (await confirm('确定要退出登录吗？')) {
    userStore.logout()
    router.push('/login')
  }
}

const toggleMenu = () => {
  isOpen.value = !isOpen.value
}
</script>

<template>
  <nav v-if="!isLoginPage" class="bg-white/90 backdrop-blur-md text-gray-800 p-4 border-b border-gray-100 sticky top-0 z-50 transition-all duration-300">
    <div class="max-w-6xl mx-auto flex justify-between items-center">
      <div class="font-bold text-xl flex items-center gap-2 cursor-pointer hover:opacity-80 transition" @click="router.push('/')">
        <ChefHat class="w-7 h-7 text-orange-500" />
        <span class="text-gray-800 bg-clip-text text-transparent bg-gradient-to-r from-orange-500 to-red-500 hidden md:inline">三食六记</span>
      </div>

      <!-- Center Navigation - 游客 -->
      <div v-if="!userStore.user" class="hidden md:flex items-center gap-8 text-sm font-medium text-gray-600">
        <router-link to="/" class="hover:text-orange-500 transition relative group">
          首页
          <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-orange-500 transition-all group-hover:w-full"></span>
        </router-link>
        <router-link to="/home" class="hover:text-orange-500 transition relative group">
          精选菜谱
          <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-orange-500 transition-all group-hover:w-full"></span>
        </router-link>
        <span @click="showToast('尚未开发')" class="hover:text-orange-500 transition cursor-pointer relative group">
          关于我们
          <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-orange-500 transition-all group-hover:w-full"></span>
        </span>
      </div>

      <!-- Center Navigation - 登录用户 -->
      <div v-else class="hidden md:flex items-center gap-8 text-sm font-medium text-gray-600">
        <router-link to="/home" class="hover:text-orange-500 transition relative group">
          精选菜谱
          <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-orange-500 transition-all group-hover:w-full"></span>
        </router-link>
        <router-link to="/messages" class="hover:text-orange-500 transition relative group flex items-center gap-1">
          <MessageSquare class="w-4 h-4" />
          私信
          <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-orange-500 transition-all group-hover:w-full"></span>
        </router-link>
        <router-link to="/profile" class="hover:text-orange-500 transition relative group">
          个人中心
          <span class="absolute -bottom-1 left-0 w-0 h-0.5 bg-orange-500 transition-all group-hover:w-full"></span>
        </router-link>
      </div>
      
      <div class="flex items-center gap-4 text-sm">
        <template v-if="userStore.user">
          <span class="hidden md:inline text-gray-500">欢迎, {{ userStore.user.username }}</span>
          <button v-if="userStore.user.role === 'admin'" @click="router.push('/admin')" class="bg-orange-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-orange-700 transition">
            后台管理
          </button>
          <button @click="handleLogout" class="hidden md:block hover:text-red-500 transition p-2 rounded-full hover:bg-red-50" title="退出登录">
            <LogOut class="w-5 h-5" />
          </button>
        </template>
        <template v-else>
          <button @click="router.push('/login')" class="bg-orange-500 text-white px-5 py-2 rounded-full font-bold hover:bg-orange-600 transition shadow-lg shadow-orange-200">
            登录 / 注册
          </button>
        </template>
        
        <!-- Hamburger Button (Mobile) -->
        <button @click="toggleMenu" class="md:hidden p-2 text-gray-600 hover:text-orange-500 transition ml-2">
          <Menu v-if="!isOpen" class="w-6 h-6" />
          <X v-else class="w-6 h-6" />
        </button>
      </div>
    </div>

    <!-- Mobile Menu -->
    <div v-show="isOpen" class="md:hidden absolute top-full left-0 w-full bg-white shadow-lg border-t border-gray-100 flex flex-col p-4 gap-4 animate-in slide-in-from-top-2 duration-200">
      <div v-if="!userStore.user" class="flex flex-col gap-4 text-sm font-medium text-gray-600">
        <router-link to="/" class="hover:text-orange-500 transition p-2" @click="isOpen = false">首页</router-link>
        <router-link to="/home" class="hover:text-orange-500 transition p-2" @click="isOpen = false">精选菜谱</router-link>
        <span @click="showToast('尚未开发'); isOpen = false" class="hover:text-orange-500 transition cursor-pointer p-2">关于我们</span>
        <button @click="router.push('/login'); isOpen = false" class="bg-orange-500 text-white px-5 py-2 rounded-full font-bold hover:bg-orange-600 transition shadow-lg shadow-orange-200 w-full mt-2">
            登录 / 注册
        </button>
      </div>

      <div v-else class="flex flex-col gap-4 text-sm font-medium text-gray-600">
        <router-link to="/home" class="hover:text-orange-500 transition p-2" @click="isOpen = false">精选菜谱</router-link>
        <router-link to="/messages" class="hover:text-orange-500 transition p-2 flex items-center gap-2" @click="isOpen = false">
            私信
            <span v-if="(userStore.unreadCount || 0) > 0" class="bg-red-500 text-white text-xs px-1.5 rounded-full">{{ userStore.unreadCount }}</span>
        </router-link>
        <router-link to="/profile" class="hover:text-orange-500 transition p-2" @click="isOpen = false">个人中心</router-link>
        
        <div class="h-px bg-gray-100 my-2"></div>
        
        <div class="flex items-center justify-between p-2">
            <span class="text-gray-500">欢迎, {{ userStore.user.username }}</span>
            <div class="flex items-center gap-4">
                <button v-if="userStore.user.role === 'admin'" @click="router.push('/admin'); isOpen = false" class="bg-orange-600 text-white px-3 py-1.5 rounded-lg text-xs font-medium hover:bg-orange-700 transition">
                    后台管理
                </button>
                <button @click="handleLogout" class="text-gray-400 hover:text-red-500 transition">
                    <LogOut class="w-5 h-5" />
                </button>
            </div>
        </div>
      </div>
    </div>
  </nav>
</template>
