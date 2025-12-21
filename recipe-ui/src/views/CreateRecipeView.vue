<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useToast } from '../components/Toast.vue'
import { PenTool, Image, X, Loader2, Plus, Trash2 } from 'lucide-vue-next'
import { uploadFile } from '@/api/common'
import { createRecipe, updateRecipe, getRecipeDetail, getCategories } from '@/api/recipe'
import { useUserStore } from '../stores/user'
import { RECIPE_CATEGORIES } from '@/utils/constants'

const router = useRouter()
const route = useRoute()
const { showToast } = useToast()
const userStore = useUserStore()

// 分类列表（从后端获取）
const categories = ref([])
const loadingCategories = ref(false)

// 获取分类列表
const fetchCategories = async () => {
    loadingCategories.value = true
    try {
        const res = await getCategories()
        if (res && res.length > 0) {
            categories.value = res.map(c => c.name)
        } else {
            // 后端无数据时使用前端常量作为 fallback
            categories.value = RECIPE_CATEGORIES
        }
    } catch (error) {
        console.error('获取分类失败', error)
        // 使用 fallback
        categories.value = RECIPE_CATEGORIES
    } finally {
        loadingCategories.value = false
    }
}

// 编辑模式
const editId = ref(null)
const isEditMode = computed(() => !!editId.value)
const loadingData = ref(false)

// 表单数据
const form = ref({
    title: '',
    category: '家常菜',
    coverImage: '',
    // 结构化描述
    intro: '', // 简介/心得
    ingredients: [{ name: '', amount: '' }], // 用料清单
    steps: [{ content: '' }] // 烹饪步骤
})

const uploading = ref(false)
const submitting = ref(false)
const fileInput = ref(null)

// 添加用料
const addIngredient = () => {
    form.value.ingredients.push({ name: '', amount: '' })
}

// 删除用料
const removeIngredient = (index) => {
    if (form.value.ingredients.length > 1) {
        form.value.ingredients.splice(index, 1)
    }
}

// 添加步骤
const addStep = () => {
    form.value.steps.push({ content: '' })
}

// 删除步骤
const removeStep = (index) => {
    if (form.value.steps.length > 1) {
        form.value.steps.splice(index, 1)
    }
}

// 解析 JSON 描述
const parseDescription = (description) => {
    try {
        const data = JSON.parse(description)
        return {
            intro: data.intro || '',
            ingredients: data.ingredients?.length > 0 ? data.ingredients : [{ name: '', amount: '' }],
            steps: data.steps?.length > 0 ? data.steps : [{ content: '' }]
        }
    } catch {
        // 如果不是 JSON，当作纯文本简介
        return {
            intro: description || '',
            ingredients: [{ name: '', amount: '' }],
            steps: [{ content: '' }]
        }
    }
}

// 序列化为 JSON 描述
const serializeDescription = () => {
    // 过滤空的用料和步骤
    const ingredients = form.value.ingredients.filter(i => i.name.trim())
    const steps = form.value.steps.filter(s => s.content.trim())
    
    return JSON.stringify({
        intro: form.value.intro,
        ingredients: ingredients,
        steps: steps
    })
}

// 加载现有菜谱数据（编辑模式）
const loadRecipeData = async (id) => {
    loadingData.value = true
    try {
        const res = await getRecipeDetail(id)
        const parsed = parseDescription(res.description)
        form.value = {
            title: res.title || '',
            category: res.categoryName || '家常菜',
            coverImage: res.coverImage || '',
            intro: parsed.intro,
            ingredients: parsed.ingredients,
            steps: parsed.steps
        }
    } catch (error) {
        console.error(error)
        showToast('加载菜谱数据失败')
    } finally {
        loadingData.value = false
    }
}

onMounted(() => {
    // 获取分类列表
    fetchCategories()
    
    const id = route.query.id
    if (id) {
        editId.value = id
        loadRecipeData(id)
    }
})

const triggerUpload = () => {
    fileInput.value.click()
}

const handleFileChange = async (event) => {
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

    uploading.value = true
    try {
        const res = await uploadFile(file)
        form.value.coverImage = res
    } catch (error) {
        console.error(error)
        showToast('图片上传失败')
    } finally {
        uploading.value = false
    }
}

const removeImage = () => {
    form.value.coverImage = ''
}

const handleSubmit = async () => {
    if (!form.value.title.trim()) {
        showToast('请输入菜谱名称')
        return
    }
    if (!form.value.coverImage) {
        showToast('请上传成品图')
        return
    }

    submitting.value = true
    try {
        const payload = {
            title: form.value.title,
            category: form.value.category,
            coverImage: form.value.coverImage,
            content: serializeDescription(),
            authorId: userStore.user?.id
        }

        if (isEditMode.value) {
            payload.id = editId.value
            await updateRecipe(payload)
            showToast('修改成功，等待管理员审核')
        } else {
            await createRecipe(payload)
            showToast('提交成功，等待管理员审核')
        }
        router.push('/profile')
    } catch (error) {
        console.error(error)
        // 显示后端返回的具体错误信息
        const msg = error.message || (isEditMode.value ? '修改失败，请重试' : '发布失败，请重试')
        showToast(msg)
    } finally {
        submitting.value = false
    }
}

</script>

<template>
  <div class="max-w-2xl mx-auto p-4 pt-8 pb-20">
    <div class="bg-white p-6 md:p-8 rounded-2xl shadow-lg border border-gray-100">
        <h2 class="text-2xl font-bold mb-6 flex items-center gap-2 text-gray-800">
            <PenTool class="w-6 h-6 text-orange-500" /> {{ isEditMode ? '编辑菜谱' : '发布新菜谱' }}
        </h2>
        
        <div class="space-y-6">
            <!-- 菜谱名称 -->
            <div>
                <label class="block text-sm font-bold text-gray-700 mb-2">菜谱名称 <span class="text-red-500">*</span></label>
                <input v-model="form.title" type="text" class="w-full border border-gray-200 rounded-lg px-4 py-3 focus:ring-2 focus:ring-orange-200 focus:border-orange-500 outline-none transition" placeholder="例如：宫保鸡丁">
            </div>
            
            <!-- 分类 -->
            <div>
                <label class="block text-sm font-bold text-gray-700 mb-2">所属分类</label>
                <select v-model="form.category" class="custom-select w-full">
                    <option v-for="c in categories" :key="c" :value="c">{{ c }}</option>
                </select>
            </div>
            
            <!-- 成品图 -->
            <div>
                <label class="block text-sm font-bold text-gray-700 mb-2">成品图 <span class="text-red-500">*</span></label>
                <input type="file" ref="fileInput" class="hidden" accept="image/*" @change="handleFileChange">
                
                <div v-if="!form.coverImage" @click="triggerUpload" class="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center text-gray-400 hover:border-orange-400 hover:text-orange-500 hover:bg-orange-50 transition cursor-pointer">
                    <div v-if="uploading" class="flex flex-col items-center">
                         <Loader2 class="w-8 h-8 animate-spin text-orange-500 mb-2" />
                         <span class="text-sm">上传中...</span>
                    </div>
                    <div v-else class="flex flex-col items-center">
                        <Image class="w-10 h-10 mb-3 opacity-80" />
                        <p class="font-medium">点击上传成品图</p>
                        <p class="text-xs mt-1 opacity-60">支持 JPG, PNG (Max 5MB)</p>
                    </div>
                </div>
                
                <div v-else class="relative rounded-xl overflow-hidden border border-gray-200 group">
                    <img :src="form.coverImage" class="w-full h-48 object-cover">
                    <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition flex items-center justify-center gap-4">
                        <button @click="triggerUpload" class="text-white hover:text-orange-300 transition font-medium text-sm flex items-center gap-1">
                            <PenTool class="w-4 h-4" /> 更换
                        </button>
                        <button @click="removeImage" class="text-white hover:text-red-300 transition font-medium text-sm flex items-center gap-1">
                            <X class="w-4 h-4" /> 删除
                        </button>
                    </div>
                </div>
            </div>

            <!-- 分割线 -->
            <div class="border-t border-gray-100 pt-6">
                <h3 class="text-lg font-bold text-gray-800 mb-4">📝 菜谱详情</h3>
            </div>
            
            <!-- 创作心得/简介 -->
            <div>
                <label class="block text-sm font-bold text-gray-700 mb-2">创作心得</label>
                <textarea v-model="form.intro" class="w-full border border-gray-200 rounded-lg px-4 py-3 h-24 focus:ring-2 focus:ring-orange-200 focus:border-orange-500 outline-none transition resize-none" placeholder="分享这道美食背后的故事或小技巧..."></textarea>
            </div>
            
            <!-- 用料清单 -->
            <div>
                <label class="block text-sm font-bold text-gray-700 mb-2">🥬 用料清单</label>
                <div class="space-y-2">
                    <div v-for="(item, index) in form.ingredients" :key="index" class="flex gap-2 items-center">
                        <input 
                            v-model="item.name" 
                            type="text" 
                            class="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-200 focus:border-orange-500 outline-none" 
                            placeholder="食材名称"
                        >
                        <input 
                            v-model="item.amount" 
                            type="text" 
                            class="w-24 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-200 focus:border-orange-500 outline-none" 
                            placeholder="用量"
                        >
                        <button 
                            @click="removeIngredient(index)" 
                            class="p-2 text-gray-400 hover:text-red-500 transition"
                            :class="{ 'opacity-30 cursor-not-allowed': form.ingredients.length <= 1 }"
                        >
                            <Trash2 class="w-4 h-4" />
                        </button>
                    </div>
                </div>
                <button @click="addIngredient" class="mt-2 text-sm text-orange-500 hover:text-orange-600 flex items-center gap-1 font-medium">
                    <Plus class="w-4 h-4" /> 添加食材
                </button>
            </div>
            
            <!-- 烹饪步骤 -->
            <div>
                <label class="block text-sm font-bold text-gray-700 mb-2">👨‍🍳 烹饪步骤</label>
                <div class="space-y-3">
                    <div v-for="(step, index) in form.steps" :key="index" class="flex gap-2 items-start">
                        <div class="w-6 h-6 bg-orange-500 text-white rounded-full flex items-center justify-center text-xs font-bold flex-shrink-0 mt-2">
                            {{ index + 1 }}
                        </div>
                        <textarea 
                            v-model="step.content" 
                            class="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-sm focus:ring-2 focus:ring-orange-200 focus:border-orange-500 outline-none resize-none h-20" 
                            :placeholder="`步骤 ${index + 1} 的详细描述...`"
                        ></textarea>
                        <button 
                            @click="removeStep(index)" 
                            class="p-2 text-gray-400 hover:text-red-500 transition mt-1"
                            :class="{ 'opacity-30 cursor-not-allowed': form.steps.length <= 1 }"
                        >
                            <Trash2 class="w-4 h-4" />
                        </button>
                    </div>
                </div>
                <button @click="addStep" class="mt-2 text-sm text-orange-500 hover:text-orange-600 flex items-center gap-1 font-medium">
                    <Plus class="w-4 h-4" /> 添加步骤
                </button>
            </div>
            
            <!-- 提交按钮 -->
            <div class="flex gap-4 mt-8 pt-4 border-t border-gray-100">
                <button 
                    @click="handleSubmit"
                    :disabled="submitting || uploading"
                    class="flex-1 bg-gradient-to-r from-orange-500 to-red-500 text-white py-3.5 rounded-xl hover:from-orange-600 hover:to-red-600 font-bold shadow-lg shadow-orange-200 transition disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                >
                    <Loader2 v-if="submitting" class="w-5 h-5 animate-spin" />
                    <span>{{ submitting ? '提交中...' : '提交审核' }}</span>
                </button>
                <button 
                    @click="router.back()"
                    class="px-8 border border-gray-200 text-gray-600 rounded-xl hover:bg-gray-50 font-medium transition"
                >
                    取消
                </button>
            </div>
        </div>
    </div>
  </div>
</template>
