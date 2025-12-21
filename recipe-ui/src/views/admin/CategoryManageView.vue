<script setup>
import { ref, onMounted } from 'vue'
import { listCategories, addCategory, updateCategory, deleteCategory } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { Plus, Edit2, Trash2, Save, X, GripVertical, FolderOpen } from 'lucide-vue-next'

const { showToast } = useToast()
const { confirm } = useModal()

const loading = ref(false)
const categories = ref([])
const showAddForm = ref(false)
const editingId = ref(null)

const newCategory = ref({ name: '', sortOrder: 0 })
const editForm = ref({ name: '', sortOrder: 0 })

const fetchCategories = async () => {
    loading.value = true
    try {
        const res = await listCategories()
        if (res) {
            categories.value = res
        }
    } catch (error) {
        console.error('Failed to fetch categories', error)
        showToast('获取分类列表失败')
    } finally {
        loading.value = false
    }
}

const handleAdd = async () => {
    if (!newCategory.value.name.trim()) {
        showToast('请输入分类名称')
        return
    }

    try {
        await addCategory(newCategory.value)
        showToast('添加成功')
        showAddForm.value = false
        newCategory.value = { name: '', sortOrder: 0 }
        fetchCategories()
    } catch (error) {
        console.error(error)
        showToast('添加失败')
    }
}

const startEdit = (category) => {
    editingId.value = category.id
    editForm.value = { name: category.name, sortOrder: category.sortOrder || 0 }
}

const cancelEdit = () => {
    editingId.value = null
    editForm.value = { name: '', sortOrder: 0 }
}

const handleUpdate = async (id) => {
    if (!editForm.value.name.trim()) {
        showToast('请输入分类名称')
        return
    }

    try {
        await updateCategory(id, editForm.value)
        showToast('修改成功')
        cancelEdit()
        fetchCategories()
    } catch (error) {
        console.error(error)
        showToast('修改失败')
    }
}

const handleDelete = async (category) => {
    const confirmed = await confirm(`确定要删除分类 "${category.name}" 吗？`, { danger: true })
    if (!confirmed) return

    try {
        await deleteCategory(category.id)
        categories.value = categories.value.filter(c => c.id !== category.id)
        showToast('删除成功')
    } catch (error) {
        console.error(error)
        showToast(error.message || '删除失败，该分类下可能存在菜谱')
    }
}

onMounted(() => {
    fetchCategories()
})
</script>

<template>
    <div class="space-y-6 p-5 h-full overflow-y-auto">
        <!-- 页面标题 -->
        <div class="flex items-center justify-between">
            <div>
                <h1 class="text-2xl font-bold text-gray-800">分类管理</h1>
                <p class="text-sm text-gray-500 mt-1">管理菜谱分类标签</p>
            </div>
            <div class="flex items-center gap-3">
                <div class="flex items-center gap-2 px-4 py-2 bg-blue-50 text-blue-600 rounded-xl">
                    <FolderOpen class="w-4 h-4" />
                    <span class="text-sm font-medium">{{ categories.length }} 个分类</span>
                </div>
                <button 
                    @click="showAddForm = true"
                    class="flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-orange-500 to-amber-500 text-white rounded-xl hover:from-orange-600 hover:to-amber-600 transition font-medium shadow-md shadow-orange-200"
                >
                    <Plus class="w-4 h-4" />
                    添加分类
                </button>
            </div>
        </div>

        <!-- 添加表单 -->
        <transition
            enter-active-class="transition ease-out duration-200"
            enter-from-class="opacity-0 -translate-y-2"
            enter-to-class="opacity-100 translate-y-0"
            leave-active-class="transition ease-in duration-150"
            leave-from-class="opacity-100 translate-y-0"
            leave-to-class="opacity-0 -translate-y-2"
        >
            <div v-if="showAddForm" class="bg-white rounded-2xl shadow-sm border border-gray-50 p-6">
                <h3 class="font-semibold text-gray-800 mb-4">新增分类</h3>
                <div class="flex gap-4 items-end">
                    <div class="flex-1">
                        <label class="block text-sm text-gray-500 mb-2">分类名称</label>
                        <input
                            v-model="newCategory.name"
                            type="text"
                            placeholder="输入分类名称"
                            class="w-full px-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition"
                        />
                    </div>
                    <div class="w-32">
                        <label class="block text-sm text-gray-500 mb-2">排序</label>
                        <input
                            v-model.number="newCategory.sortOrder"
                            type="number"
                            placeholder="0"
                            class="w-full px-4 py-2.5 bg-gray-50 border border-gray-100 rounded-xl focus:ring-2 focus:ring-orange-200 focus:border-orange-400 focus:bg-white outline-none transition"
                        />
                    </div>
                    <button 
                        @click="handleAdd"
                        class="px-6 py-2.5 bg-emerald-500 text-white rounded-xl hover:bg-emerald-600 transition font-medium shadow-md shadow-emerald-200"
                    >
                        保存
                    </button>
                    <button 
                        @click="showAddForm = false; newCategory = { name: '', sortOrder: 0 }"
                        class="px-6 py-2.5 bg-gray-100 text-gray-600 rounded-xl hover:bg-gray-200 transition font-medium"
                    >
                        取消
                    </button>
                </div>
            </div>
        </transition>

        <!-- 分类列表 -->
        <div class="bg-white rounded-2xl shadow-sm border border-gray-50 overflow-hidden">
            <div class="overflow-x-auto">
                <table class="w-full">
                    <thead>
                        <tr class="border-b border-gray-100">
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider w-20">排序</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">ID</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">分类名称</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider">创建时间</th>
                            <th class="px-6 py-4 text-left text-xs font-semibold text-gray-400 uppercase tracking-wider w-32">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-50">
                        <tr v-if="loading" class="text-center">
                            <td colspan="5" class="px-6 py-16 text-gray-400">
                                <div class="flex flex-col items-center">
                                    <div class="w-8 h-8 border-2 border-orange-500 border-t-transparent rounded-full animate-spin"></div>
                                    <span class="mt-3">加载中...</span>
                                </div>
                            </td>
                        </tr>
                        <tr v-else-if="categories.length === 0" class="text-center">
                            <td colspan="5" class="px-6 py-16 text-gray-400">
                                <div class="flex flex-col items-center">
                                    <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-3">
                                        <FolderOpen class="w-8 h-8 text-gray-300" />
                                    </div>
                                    <span>暂无分类</span>
                                </div>
                            </td>
                        </tr>
                        <tr v-else v-for="category in categories" :key="category.id" class="hover:bg-gray-50/50 transition group">
                            <td class="px-6 py-4 whitespace-nowrap text-gray-500">
                                <div class="flex items-center gap-2">
                                    <GripVertical class="w-4 h-4 text-gray-300 cursor-grab" />
                                    <template v-if="editingId === category.id">
                                        <input
                                            v-model.number="editForm.sortOrder"
                                            type="number"
                                            class="w-16 px-2 py-1.5 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-200 focus:border-orange-400 outline-none transition text-sm"
                                        />
                                    </template>
                                    <template v-else>
                                        <span class="inline-flex items-center justify-center w-8 h-8 bg-gray-100 rounded-lg text-sm font-medium text-gray-600">
                                            {{ category.sortOrder || 0 }}
                                        </span>
                                    </template>
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-500 text-sm">{{ category.id }}</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <template v-if="editingId === category.id">
                                    <input
                                        v-model="editForm.name"
                                        type="text"
                                        class="px-3 py-1.5 bg-gray-50 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-200 focus:border-orange-400 outline-none transition"
                                    />
                                </template>
                                <template v-else>
                                    <span class="inline-flex items-center px-3 py-1 text-sm font-medium rounded-lg bg-blue-50 text-blue-600">
                                        {{ category.name }}
                                    </span>
                                </template>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-500 text-sm">
                                {{ category.createTime ? new Date(category.createTime).toLocaleDateString('zh-CN') : '-' }}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center gap-1">
                                    <template v-if="editingId === category.id">
                                        <button 
                                            @click="handleUpdate(category.id)"
                                            class="p-2 text-emerald-500 hover:text-emerald-700 hover:bg-emerald-50 rounded-lg transition"
                                            title="保存"
                                        >
                                            <Save class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="cancelEdit"
                                            class="p-2 text-gray-400 hover:text-gray-600 hover:bg-gray-100 rounded-lg transition"
                                            title="取消"
                                        >
                                            <X class="w-4 h-4" />
                                        </button>
                                    </template>
                                    <template v-else>
                                        <button 
                                            @click="startEdit(category)"
                                            class="p-2 text-blue-400 hover:text-blue-600 hover:bg-blue-50 rounded-lg transition"
                                            title="编辑"
                                        >
                                            <Edit2 class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="handleDelete(category)"
                                            class="p-2 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition opacity-0 group-hover:opacity-100"
                                            title="删除"
                                        >
                                            <Trash2 class="w-4 h-4" />
                                        </button>
                                    </template>
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</template>
