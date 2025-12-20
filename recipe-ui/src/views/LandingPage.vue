<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listRecipies } from '@/api/recipe'
import { 
  Search, 
  Eye, 
  Users, 
  Bookmark, 
  ChefHat, 
  ArrowRight, 
  PlayCircle,
  Star,
  Utensils,
  Flame,
  Soup,
  Instagram,
  Twitter,
  Facebook
} from 'lucide-vue-next'

const router = useRouter()
const popularRecipes = ref([])
const loading = ref(true)

// 动画控制
const observerOptions = {
  threshold: 0.1,
  rootMargin: '0px 0px -50px 0px'
}

const fetchPopularRecipes = async () => {
  try {
    const res = await listRecipies({
      page: 1,
      size: 3
    })
    popularRecipes.value = res.records.map(r => ({
      id: r.id,
      title: r.title,
      image: r.coverImage,
      category: r.categoryName || '美味',
      author: r.authorName || r.nickname || '大厨',
      authorAvatar: r.authorAvatar || r.avatar,
      likes: r.viewCount || 0
    }))
  } catch (error) {
    console.error('Failed to fetch recipes', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchPopularRecipes()

  const observer = new IntersectionObserver((entries) => {
    entries.forEach(entry => {
      if (entry.isIntersecting) {
        entry.target.classList.add('animate-active')
        observer.unobserve(entry.target)
      }
    })
  }, observerOptions)

  document.querySelectorAll('.animate-on-scroll').forEach(el => {
    observer.observe(el)
  })
})

const features = [
  {
    icon: Search,
    title: '发现美食',
    desc: '海量菜谱任你搜索，按分类、食材智能筛选，找到你的心头好。',
    bg: 'bg-orange-100',
    color: 'text-orange-600'
  },
  {
    icon: Eye,
    title: '分享创作',
    desc: '记录你的烹饪灵感，图文并茂分享食谱，成为社区里的美食达人。',
    bg: 'bg-green-100',
    color: 'text-green-600'
  },
  {
    icon: Users,
    title: '社交互动',
    desc: '关注心仪大厨，私信交流心得，点赞评论，让美食不再孤单。',
    bg: 'bg-blue-100',
    color: 'text-blue-600'
  },
  {
    icon: Bookmark,
    title: '个人收藏',
    desc: '建立专属美食收藏夹，喜欢的味道永远不会走丢。',
    bg: 'bg-purple-100',
    color: 'text-purple-600'
  }
]
</script>

<template>
  <div class="min-h-screen bg-[#FFFBF5] font-sans overflow-x-hidden flex flex-col">
    <!-- Navbar is handled globally in App.vue -->
    
    <!-- Premium Hero Section (CSS Composition) -->
    <section class="relative pt-12 pb-20 lg:pt-24 lg:pb-32 overflow-hidden flex-shrink-0">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative">
        <div class="grid lg:grid-cols-2 gap-12 lg:gap-16 items-center">
          
          <!-- Left Content -->
          <div class="space-y-8 animate-fadeInDown relative z-10 text-center lg:text-left">
            <div class="inline-flex items-center gap-2 bg-white/60 border border-orange-100 text-orange-600 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide shadow-sm backdrop-blur-sm">
              <Star class="w-3 h-3 fill-current" />
              全新三食六记 Pro 上线
            </div>
            
            <h1 class="text-5xl lg:text-7xl font-bold tracking-tight text-gray-900 leading-tight">
              投资味蕾<br>
              赢得<span class="text-transparent bg-clip-text bg-gradient-to-r from-orange-500 to-red-600">生活乐趣</span>
            </h1>
            
            <p class="text-lg text-gray-600 max-w-2xl mx-auto lg:mx-0 leading-relaxed font-medium">
              汇聚全球美食爱好者，提供最前沿的烹饪灵感。从零基础小白到米其林大厨，我们陪你走过每一餐。
            </p>

            <div class="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
              <button 
                @click="router.push('/home')"
                class="px-8 py-4 bg-gradient-to-r from-orange-500 to-orange-600 text-white rounded-full font-bold text-lg hover:shadow-lg hover:shadow-orange-200 transition transform active:scale-95 flex items-center justify-center gap-2"
              >
                开始探索
              </button>
              <button 
                @click="router.push('/create')"
                class="px-8 py-4 bg-white border border-gray-200 text-gray-700 rounded-full font-bold text-lg hover:bg-orange-50 hover:border-orange-200 transition flex items-center justify-center gap-2 transform active:scale-95 shadow-sm"
              >
                <PlayCircle class="w-5 h-5 text-orange-500 opacity-80" />
                去分享
              </button>
            </div>
          </div>

          <!-- Right CSS Composition (No Images) -->
          <div class="relative animate-fadeInUp delay-300 h-[400px] lg:h-[500px] flex items-center justify-center select-none pointer-events-none">
             <!-- Background Shapes -->
             <div class="absolute inset-0 flex items-center justify-center">
                <div class="w-[300px] h-[300px] lg:w-[450px] lg:h-[450px] bg-gradient-to-br from-orange-100 to-red-50 rounded-full blur-none shadow-inner border-[20px] border-white/40"></div>
             </div>
             
             <!-- Floating Icons Composition -->
             <div class="relative z-10 w-full h-full max-w-lg flex items-center justify-center">
                <!-- Main Icon -->
                <div class="bg-white p-8 rounded-3xl shadow-2xl shadow-orange-100 flex flex-col items-center gap-4 border border-orange-50 transform hover:scale-105 transition duration-700">
                   <div class="w-24 h-24 bg-orange-500 rounded-2xl flex items-center justify-center shadow-lg transform rotate-3">
                      <ChefHat class="w-12 h-12 text-white" />
                   </div>
                   <div class="text-center">
                      <div class="font-bold text-xl text-gray-800">烹饪大师</div>
                      <div class="text-xs text-gray-400 mt-1">你的专属厨房</div>
                   </div>
                   <div class="w-full h-1 bg-gray-100 rounded-full overflow-hidden">
                      <div class="w-2/3 h-full bg-orange-400 rounded-full"></div>
                   </div>
                </div>

                <!-- Floating Element 1 -->
                <div class="absolute top-10 right-10 lg:right-0 bg-white p-4 rounded-2xl shadow-xl flex items-center gap-3 animate-bounce border border-orange-50" style="animation-duration: 3s;">
                   <div class="bg-green-100 p-2.5 rounded-xl">
                      <Utensils class="w-6 h-6 text-green-600" />
                   </div>
                   <div class="hidden sm:block">
                      <div class="text-[10px] text-gray-400 font-bold uppercase">Ready</div>
                      <div class="text-sm font-bold text-gray-800">美味</div>
                   </div>
                </div>

                <!-- Floating Element 2 -->
                <div class="absolute bottom-20 left-4 lg:left-0 bg-white p-4 rounded-2xl shadow-xl flex items-center gap-3 animate-bounce border border-orange-50" style="animation-duration: 4s; animation-delay: 1s;">
                   <div class="bg-red-100 p-2.5 rounded-xl">
                      <Flame class="w-6 h-6 text-red-500" />
                   </div>
                   <div class="hidden sm:block">
                      <div class="text-[10px] text-gray-400 font-bold uppercase">Hot</div>
                      <div class="text-sm font-bold text-gray-800">热门</div>
                   </div>
                </div>

                 <!-- Floating Element 3 -->
                <div class="absolute top-1/2 left-0 -translate-x-4 bg-white/90 backdrop-blur p-3 rounded-xl shadow-lg border border-orange-50 animate-pulse hidden sm:block">
                    <Soup class="w-5 h-5 text-orange-400" />
                </div>
             </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Features Section -->
    <section class="py-24 bg-white/50 flex-shrink-0">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="text-center mb-16 max-w-3xl mx-auto animate-on-scroll">
          <h2 class="text-3xl font-bold text-gray-900 mb-4">为什么选择三食六记</h2>
          <p class="text-gray-500 text-lg">不仅仅是菜谱，更是热爱生活的方式。</p>
        </div>

        <div class="grid md:grid-cols-2 lg:grid-cols-4 gap-8">
          <div 
            v-for="(feature, index) in features" 
            :key="index"
            class="bg-white p-8 rounded-2xl shadow-sm hover:shadow-xl transition duration-300 group animate-on-scroll slide-up border border-orange-50/50 hover:border-orange-200"
            :style="{ transitionDelay: `${index * 100}ms` }"
          >
            <div :class="['w-14 h-14 rounded-2xl flex items-center justify-center mb-6 transition-colors duration-300', feature.bg, 'bg-opacity-50 group-hover:bg-orange-500 group-hover:text-white']">
              <component :is="feature.icon" :class="['w-7 h-7', feature.color, 'group-hover:text-white']" />
            </div>
            <h3 class="text-xl font-bold text-gray-900 mb-3">{{ feature.title }}</h3>
            <p class="text-gray-500 leading-relaxed text-sm">{{ feature.desc }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- Popular Recipes -->
    <section class="py-24 overflow-hidden flex-shrink-0">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="flex flex-col md:flex-row justify-between items-end mb-12 animate-on-scroll">
          <div>
            <h2 class="text-3xl font-bold text-gray-900 mb-2">本周精选美味</h2>
            <p class="text-gray-500">来自社区的最新热门创作</p>
          </div>
          <button @click="router.push('/home')" class="mt-4 md:mt-0 px-6 py-2 bg-white border border-gray-200 text-gray-700 rounded-full font-bold hover:bg-orange-50 hover:text-orange-600 hover:border-orange-200 transition flex items-center gap-2 text-sm shadow-sm hover:shadow-md">
            查看全部 <ArrowRight class="w-4 h-4" />
          </button>
        </div>

        <div v-if="loading" class="flex justify-center py-12">
           <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
        </div>

        <div v-else class="grid md:grid-cols-3 gap-8">
          <div 
            v-for="(recipe, index) in popularRecipes" 
            :key="recipe.id"
            class="group relative bg-white rounded-3xl overflow-hidden shadow-sm border border-orange-50 hover:shadow-xl hover:shadow-orange-100 transition duration-500 cursor-pointer animate-on-scroll slide-up"
            :style="{ transitionDelay: `${index * 100}ms` }"
            @click="router.push(`/recipe/${recipe.id}`)"
          >
            <div class="h-64 overflow-hidden">
              <img :src="recipe.image" :alt="recipe.title" class="w-full h-full object-cover group-hover:scale-105 transition duration-700">
            </div>
            <div class="p-6">
              <div class="flex items-center justify-between mb-3 text-xs font-bold uppercase tracking-wider text-gray-400">
                <span class="bg-orange-50 text-orange-600 px-2 py-1 rounded">{{ recipe.category }}</span>
              </div>
              <h3 class="text-xl font-bold text-gray-900 mb-2 group-hover:text-orange-600 transition truncate">{{ recipe.title }}</h3>
              <div class="flex items-center justify-between border-t border-gray-50 pt-4 mt-4">
                 <div class="flex items-center gap-2">
                   <div class="w-8 h-8 rounded-full overflow-hidden bg-orange-100 text-orange-600 flex items-center justify-center text-xs font-bold border border-white shadow-sm">
                     <img v-if="recipe.authorAvatar" :src="recipe.authorAvatar" class="w-full h-full object-cover">
                     <span v-else>{{ recipe.author.charAt(0) }}</span>
                   </div>
                   <span class="text-sm font-medium text-gray-600 truncate max-w-[100px]">{{ recipe.author }}</span>
                 </div>
                 <div class="flex items-center gap-1 text-sm text-gray-400">
                   <Eye class="w-4 h-4" /> {{ recipe.likes }}
                 </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Footer (Refined & Clean) -->
    <footer class="bg-[#FDF8F5] border-t border-orange-100 pt-20 pb-10 mt-auto">
      <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div class="grid md:grid-cols-12 gap-12 lg:gap-8 mb-16">
          <!-- Brand Column -->
          <div class="md:col-span-4 lg:col-span-5 space-y-6">
            <div class="font-bold text-3xl flex items-center gap-3 text-gray-900">
              <div class="w-10 h-10 bg-orange-500 rounded-xl flex items-center justify-center shadow-lg shadow-orange-200">
                <ChefHat class="w-6 h-6 text-white" />
              </div>
              <span class="tracking-tight">三食六记</span>
            </div>
            <p class="text-gray-500 text-sm leading-7 max-w-sm">
              我们致力于构建最温暖的美食社区。在这里，每一道菜都有故事，每一次烹饪都是对生活的热爱。加入我们，记录你的味蕾记忆。
            </p>
            <div class="flex gap-4">
               <button class="w-10 h-10 rounded-full bg-white border border-gray-200 flex items-center justify-center text-gray-400 hover:text-orange-500 hover:border-orange-200 hover:bg-orange-50 transition shadow-sm">
                 <Instagram class="w-5 h-5" />
               </button>
               <button class="w-10 h-10 rounded-full bg-white border border-gray-200 flex items-center justify-center text-gray-400 hover:text-orange-500 hover:border-orange-200 hover:bg-orange-50 transition shadow-sm">
                 <Twitter class="w-5 h-5" />
               </button>
               <button class="w-10 h-10 rounded-full bg-white border border-gray-200 flex items-center justify-center text-gray-400 hover:text-orange-500 hover:border-orange-200 hover:bg-orange-50 transition shadow-sm">
                 <Facebook class="w-5 h-5" />
               </button>
            </div>
          </div>
          
          <!-- Links Column 1 -->
          <div class="md:col-span-2 lg:col-start-7 lg:col-span-2">
            <h4 class="font-bold text-gray-900 mb-6 text-lg">探索</h4>
            <ul class="space-y-4 text-gray-500 text-sm">
              <li><router-link to="/home" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">今日推荐</router-link></li>
              <li><router-link to="/home" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">热门分类</router-link></li>
              <li><router-link to="/home" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">社区活动</router-link></li>
              <li><router-link to="/home" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">生活周刊</router-link></li>
            </ul>
          </div>
          
          <!-- Links Column 2 -->
          <div class="md:col-span-2 lg:col-span-2">
            <h4 class="font-bold text-gray-900 mb-6 text-lg">平台</h4>
            <ul class="space-y-4 text-gray-500 text-sm">
              <li><router-link to="/login" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">登录/注册</router-link></li>
              <li><a href="#" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">关于我们</a></li>
              <li><a href="#" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">用户协议</a></li>
              <li><a href="#" class="hover:text-orange-600 hover:translate-x-1 transition inline-block">隐私政策</a></li>
            </ul>
          </div>

          <!-- Subscribe Column -->
          <div class="md:col-span-4 lg:col-span-2">
             <h4 class="font-bold text-gray-900 mb-6 text-lg">加入社区</h4>
             <p class="text-sm text-gray-400 mb-4 leading-relaxed">订阅我们的周刊，不错过每一个美味瞬间。</p>
             <div class="relative">
               <input type="email" placeholder="您的邮箱地址" class="w-full bg-white border border-gray-200 text-gray-700 px-4 py-3 rounded-lg text-sm focus:outline-none focus:border-orange-500 focus:ring-1 focus:ring-orange-500 transition shadow-sm placeholder-gray-300">
               <button class="absolute right-1 top-1 bottom-1 bg-orange-500 text-white px-3 rounded-md text-xs font-bold hover:bg-orange-600 transition shadow-md">
                 订阅
               </button>
             </div>
          </div>
        </div>
        
        <!-- Copyright -->
        <div class="border-t border-gray-200/60 pt-8 flex flex-col md:flex-row justify-between items-center gap-4 text-sm text-gray-400">
          <p>&copy; 2025 三食六记 (Three Meals Six Records). All rights reserved.</p>
          <div class="flex gap-6">
            <span>ICP备88888888号</span>
            <span>联系我们</span>
          </div>
        </div>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.animate-on-scroll {
  opacity: 0;
  transform: translateY(30px);
  transition: opacity 0.8s ease-out, transform 0.8s ease-out;
}

.animate-on-scroll.animate-active {
  opacity: 1;
  transform: translateY(0);
}
</style>
