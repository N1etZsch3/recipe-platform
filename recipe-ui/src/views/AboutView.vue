<script setup>
import { ref, onMounted } from 'vue'
import { ChefHat, Github, Mail, Code, Heart, Users, Sparkles } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { getTeamMembers } from '@/api/team'

const router = useRouter()

// 团队信息
const teamInfo = {
  name: '师徒四人行',
  slogan: '用代码烹饪美味，用热爱点亮生活',
  description: '我们是一支充满激情的开发团队，致力于为美食爱好者打造最温暖的社区平台。从创意到实现，从设计到开发，我们共同协作，将对美食和技术的热爱融入每一行代码。'
}

// 团队成员（从 API 获取）
const members = ref([])
const loading = ref(true)

const fetchMembers = async () => {
  try {
    loading.value = true
    const data = await getTeamMembers()
    members.value = data || []
  } catch (error) {
    console.error('获取团队成员失败', error)
    members.value = []
  } finally {
    loading.value = false
  }
}

// 解析技能标签
const parseSkills = (skills) => {
  if (!skills) return []
  try {
    return JSON.parse(skills)
  } catch {
    return skills.split(',').map(s => s.trim())
  }
}

onMounted(() => {
  fetchMembers()
})

// 项目亮点
const highlights = [
  { icon: Code, title: '现代技术栈', desc: 'Vue 3 + Spring Boot 3 + Redis' },
  { icon: Heart, title: '用心打磨', desc: '每一个细节都经过精心设计' },
  { icon: Users, title: '社区驱动', desc: '倾听用户声音，持续迭代优化' },
  { icon: Sparkles, title: '开源精神', desc: '拥抱开源，分享知识与热爱' }
]
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-[#FFFBF5] via-white to-orange-50">
    <!-- Header -->
    <header class="sticky top-0 z-50 bg-white/80 backdrop-blur-lg border-b border-orange-100">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4 flex items-center justify-between">
        <div class="flex items-center gap-3 cursor-pointer" @click="router.push('/')">
          <div class="w-8 h-8 bg-orange-500 rounded-lg flex items-center justify-center">
            <ChefHat class="w-5 h-5 text-white" />
          </div>
          <span class="font-bold text-gray-800">三食六记</span>
        </div>
        <div class="flex items-center gap-4">
          <button 
            @click="router.push('/')"
            class="text-gray-500 hover:text-orange-500 transition text-sm font-medium"
          >
            返回首页
          </button>
          <button 
            @click="router.push('/login')"
            class="px-4 py-2 bg-orange-500 text-white rounded-lg font-medium text-sm hover:bg-orange-600 transition"
          >
            登录
          </button>
        </div>
      </div>
    </header>

    <!-- Hero Section -->
    <section class="pt-16 pb-20 px-4">
      <div class="max-w-4xl mx-auto text-center">
        <div class="inline-flex items-center gap-2 bg-orange-100 text-orange-600 px-4 py-2 rounded-full text-sm font-bold mb-6">
          <Users class="w-4 h-4" />
          开发团队
        </div>
        <h1 class="text-5xl lg:text-6xl font-bold text-gray-900 mb-4">
          {{ teamInfo.name }}
        </h1>
        <p class="text-xl text-orange-500 font-medium mb-6">
          {{ teamInfo.slogan }}
        </p>
        <p class="text-gray-500 max-w-2xl mx-auto leading-relaxed">
          {{ teamInfo.description }}
        </p>
      </div>
    </section>

    <!-- Team Members -->
    <section class="py-16 px-4">
      <div class="max-w-6xl mx-auto">
        <h2 class="text-3xl font-bold text-gray-900 text-center mb-12">认识我们</h2>
        
        <!-- Loading -->
        <div v-if="loading" class="flex justify-center py-12">
          <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
        </div>
        
        <!-- Empty State -->
        <div v-else-if="members.length === 0" class="text-center py-12 text-gray-400">
          <Users class="w-16 h-16 mx-auto mb-4 opacity-50" />
          <p>暂无团队成员信息</p>
        </div>
        
        <!-- Members Grid -->
        <div v-else class="grid md:grid-cols-2 gap-8">
          <div 
            v-for="member in members" 
            :key="member.id"
            class="group bg-white rounded-3xl p-8 shadow-sm hover:shadow-xl transition duration-500 border border-orange-50 hover:border-orange-200"
          >
            <div class="flex items-start gap-6">
              <!-- Avatar -->
              <div 
                v-if="member.avatar"
                class="w-20 h-20 rounded-2xl overflow-hidden shadow-lg flex-shrink-0"
              >
                <img :src="member.avatar" :alt="member.name" class="w-full h-full object-cover" />
              </div>
              <div 
                v-else
                :class="['w-20 h-20 rounded-2xl bg-gradient-to-br flex items-center justify-center text-4xl shadow-lg flex-shrink-0', member.color || 'from-orange-500 to-red-500']"
              >
                {{ member.emoji || '👤' }}
              </div>
              
              <!-- Info -->
              <div class="flex-1 min-w-0">
                <h3 class="text-xl font-bold text-gray-900 mb-1">{{ member.name }}</h3>
                <p class="text-orange-500 font-medium text-sm mb-3">{{ member.role }}</p>
                <p class="text-gray-500 text-sm leading-relaxed mb-4">{{ member.description }}</p>
                
                <!-- Skills -->
                <div class="flex flex-wrap gap-2 mb-4">
                  <span 
                    v-for="skill in parseSkills(member.skills)" 
                    :key="skill"
                    :class="['text-xs px-2 py-1 rounded-full font-medium', member.bgColor || 'bg-orange-50', 'text-gray-600']"
                  >
                    {{ skill }}
                  </span>
                </div>
                
                <!-- Links -->
                <div class="flex items-center gap-4">
                  <a 
                    v-if="member.github"
                    :href="member.gitType === 'gitee' ? `https://gitee.com/${member.github}` : `https://github.com/${member.github}`" 
                    target="_blank" 
                    class="flex items-center gap-1 text-gray-400 hover:text-gray-700 transition text-sm"
                  >
                    <Github class="w-4 h-4" />
                    <span>{{ member.gitType === 'gitee' ? 'Gitee' : 'GitHub' }}</span>
                  </a>
                  <a 
                    v-if="member.email"
                    :href="`mailto:${member.email}`" 
                    class="flex items-center gap-1 text-gray-400 hover:text-orange-500 transition text-sm"
                  >
                    <Mail class="w-4 h-4" />
                    <span>邮箱</span>
                  </a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Project Highlights -->
    <section class="py-16 px-4 bg-white/50">
      <div class="max-w-6xl mx-auto">
        <h2 class="text-3xl font-bold text-gray-900 text-center mb-12">项目亮点</h2>
        
        <div class="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
          <div 
            v-for="(item, index) in highlights" 
            :key="index"
            class="bg-white rounded-2xl p-6 text-center shadow-sm border border-orange-50 hover:shadow-lg hover:border-orange-200 transition"
          >
            <div class="w-14 h-14 bg-orange-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <component :is="item.icon" class="w-7 h-7 text-orange-500" />
            </div>
            <h3 class="font-bold text-gray-900 mb-2">{{ item.title }}</h3>
            <p class="text-gray-500 text-sm">{{ item.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- CTA Section -->
    <section class="py-20 px-4">
      <div class="max-w-4xl mx-auto text-center">
        <h2 class="text-3xl font-bold text-gray-900 mb-4">加入我们的美食之旅</h2>
        <p class="text-gray-500 mb-8">开始探索、分享、收藏你喜爱的美味</p>
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <button 
            @click="router.push('/login')"
            class="px-8 py-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-full font-bold text-lg hover:shadow-lg hover:shadow-orange-200 transition"
          >
            立即加入
          </button>
          <button 
            @click="router.push('/home')"
            class="px-8 py-4 bg-white border border-gray-200 text-gray-700 rounded-full font-bold text-lg hover:bg-orange-50 hover:border-orange-200 transition"
          >
            浏览菜谱
          </button>
        </div>
      </div>
    </section>

    <!-- Footer -->
    <footer class="py-8 border-t border-orange-100 text-center text-sm text-gray-400">
      <p>&copy; 2025 三食六记 · 师徒四人行团队出品</p>
    </footer>
  </div>
</template>
