<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Check, X, Users, Tag, AlertCircle } from 'lucide-vue-next'
import { useToast } from '../components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { listRecipies, auditRecipe } from '@/api/recipe'

const router = useRouter()
const { showToast } = useToast()
const { confirm, prompt } = useModal()

const pendingRecipes = ref([])
const loading = ref(false)

const loadPendingRecipes = async () => {
    loading.value = true
    try {
        const res = await listRecipies({
            page: 1, 
            size: 50,
            status: 0 // Waiting for audit
        })
        pendingRecipes.value = res.records.map(r => ({
            id: r.id,
            title: r.title,
            image: r.coverImage,
            authorId: r.authorId,
            authorName: r.authorName || 'Unknown',
            category: r.categoryName || 'General',
            status: 0,
            publishTime: r.createTime
        }))
    } catch (error) {
        console.error('Failed to load pending recipes', error)
        showToast('加载失败')
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    loadPendingRecipes()
})

const handleAudit = async (id, status, reason = '') => {
    try {
        // 转换为后端期望的字段名: recipeId, action ("pass"/"reject"), reason
        const action = status === 1 ? 'pass' : 'reject'
        await auditRecipe({ recipeId: id, action, reason })
        pendingRecipes.value = pendingRecipes.value.filter(r => r.id !== id)
        showToast(status === 1 ? '已通过审核' : '已驳回')
    } catch (error) {
        console.error(error)
        showToast('操作失败')
    }
}

const rejectRecipe = async (id) => {
    const reason = await prompt('请输入驳回原因:', { placeholder: '请输入驳回原因...' })
    if (reason) handleAudit(id, 2, reason)
}

const passRecipe = async (id) => {
    const confirmed = await confirm('确定通过该菜谱审核吗？')
    if (confirmed) {
        handleAudit(id, 1)
    }
}

const viewRecipeDetail = (id) => {
    router.push(`/recipe/${id}`)
}
</script>

<template>
  <div class="max-w-6xl mx-auto p-4 pt-24">
    <h2 class="text-2xl font-bold mb-6 text-gray-800 pl-2 border-l-4 border-gray-800">管理员工作台</h2>
    
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- 审核列表 -->
        <div class="lg:col-span-2 space-y-6">
            <div class="bg-white p-6 rounded-xl shadow-sm border-t-4 border-orange-500">
                <h3 class="font-bold text-lg mb-4 flex items-center gap-2">
                    <span class="relative flex h-3 w-3">
                      <span v-if="pendingRecipes.length > 0" class="animate-ping absolute inline-flex h-full w-full rounded-full bg-orange-400 opacity-75"></span>
                      <span :class="['relative inline-flex rounded-full h-3 w-3', pendingRecipes.length > 0 ? 'bg-orange-500' : 'bg-gray-300']"></span>
                    </span>
                    待审核菜谱 ({{ pendingRecipes.length }})
                </h3>
                
                <div v-if="loading" class="text-center py-8 text-gray-400">
                    加载中...
                </div>

                <div v-else class="space-y-4">
                    <div v-if="pendingRecipes.length === 0" class="text-gray-400 text-sm py-8 text-center bg-gray-50 rounded-lg border border-dashed border-gray-200">
                        暂无待审核内容，去喝杯茶吧 🍵
                    </div>
                    <div v-for="r in pendingRecipes" :key="r.id" class="border p-4 rounded-xl flex gap-4 hover:shadow-md transition bg-white items-start">
                        <img :src="r.image" class="w-24 h-24 object-cover rounded-lg bg-gray-100 flex-shrink-0">
                        <div class="flex-1">
                            <div class="flex justify-between items-start">
                                <h4 class="font-bold text-lg text-gray-800">{{ r.title }}</h4>
                                <span class="text-xs text-gray-400 mt-1">{{ r.publishTime }}</span>
                            </div>
                            <p class="text-sm text-gray-600 mb-3 mt-1">
                                <span class="bg-gray-100 px-2 py-0.5 rounded text-xs mr-2">{{ r.category }}</span>
                                发布者: {{ r.authorName }}
                            </p>
                            
                            <div class="flex gap-2 mt-auto">
                                <button @click="viewRecipeDetail(r.id)" class="text-sm bg-gray-100 px-3 py-1.5 rounded hover:bg-gray-200 transition">详情</button>
                                <button @click="passRecipe(r.id)" class="text-sm bg-green-500 text-white px-3 py-1.5 rounded hover:bg-green-600 flex items-center gap-1 transition shadow-sm font-medium">
                                    <Check class="w-3 h-3" /> 通过
                                </button>
                                <button @click="rejectRecipe(r.id)" class="text-sm bg-red-50 text-red-600 border border-red-200 px-3 py-1.5 rounded hover:bg-red-100 flex items-center gap-1 transition font-medium">
                                    <X class="w-3 h-3" /> 驳回
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- 快捷菜单 -->
        <div class="space-y-6">
            <div class="bg-white p-6 rounded-xl shadow-sm">
                <h3 class="font-bold text-lg mb-4 text-gray-800">快捷管理</h3>
                <div class="space-y-2">
                    <button class="w-full text-left px-4 py-3 hover:bg-gray-50 rounded-lg text-sm text-gray-600 transition flex items-center gap-2">
                        <Users class="w-4 h-4" /> 用户管理
                    </button>
                    <button class="w-full text-left px-4 py-3 hover:bg-gray-50 rounded-lg text-sm text-gray-600 transition flex items-center gap-2">
                        <Tag class="w-4 h-4" /> 分类管理
                    </button>
                    <button class="w-full text-left px-4 py-3 hover:bg-gray-50 rounded-lg text-sm text-gray-600 transition flex items-center gap-2">
                        <AlertCircle class="w-4 h-4" /> 举报处理
                    </button>
                </div>
            </div>
        </div>
    </div>
  </div>
</template>
