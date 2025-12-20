<script setup>
import { ref, onMounted } from 'vue'
import { listCategories, addCategory, updateCategory, deleteCategory } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { Plus, Edit2, Trash2, Save, X, GripVertical } from 'lucide-vue-next'

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
    <div class="flex flex-col h-full">
        <!-- 添加按钮 (固定) -->
        <div class="flex justify-between items-center flex-shrink-0">
            <h2 class="text-lg font-semibold text-gray-800">分类列表</h2>
            <button 
                @click="showAddForm = true"
                class="flex items-center gap-2 px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition font-medium"
            >
                <Plus class="w-4 h-4" />
                添加分类
            </button>
        </div>

        <!-- 添加表单 -->
        <div v-if="showAddForm" class="bg-white rounded-xl shadow-sm p-6 mt-4 flex-shrink-0">
            <h3 class="font-medium text-gray-800 mb-4">新增分类</h3>
            <div class="flex gap-4 items-end">
                <div class="flex-1">
                    <label class="block text-sm text-gray-600 mb-1">分类名称</label>
                    <input
                        v-model="newCategory.name"
                        type="text"
                        placeholder="输入分类名称"
                        class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none"
                    />
                </div>
                <div class="w-32">
                    <label class="block text-sm text-gray-600 mb-1">排序</label>
                    <input
                        v-model.number="newCategory.sortOrder"
                        type="number"
                        placeholder="0"
                        class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 focus:border-transparent outline-none"
                    />
                </div>
                <button 
                    @click="handleAdd"
                    class="px-6 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 transition"
                >
                    保存
                </button>
                <button 
                    @click="showAddForm = false"
                    class="px-6 py-2 bg-gray-100 text-gray-600 rounded-lg hover:bg-gray-200 transition"
                >
                    取消
                </button>
            </div>
        </div>

        <!-- 分类列表 (可滚动) -->
        <div class="flex-1 mt-4 bg-white rounded-xl shadow-sm overflow-hidden flex flex-col min-h-0">
            <div class="flex-1 overflow-auto">
                <table class="w-full">
                    <thead class="bg-gray-50 border-b border-gray-200 sticky top-0">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-16">排序</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">ID</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">分类名称</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">创建时间</th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider w-40">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200">
                        <tr v-if="loading" class="text-center">
                            <td colspan="5" class="px-6 py-12 text-gray-400">加载中...</td>
                        </tr>
                        <tr v-else-if="categories.length === 0" class="text-center">
                            <td colspan="5" class="px-6 py-12 text-gray-400">暂无分类</td>
                        </tr>
                        <tr v-else v-for="category in categories" :key="category.id" class="hover:bg-gray-50">
                            <td class="px-6 py-4 whitespace-nowrap text-gray-500">
                                <div class="flex items-center gap-2">
                                    <GripVertical class="w-4 h-4 text-gray-300" />
                                    <template v-if="editingId === category.id">
                                        <input
                                            v-model.number="editForm.sortOrder"
                                            type="number"
                                            class="w-16 px-2 py-1 border border-gray-200 rounded focus:ring-2 focus:ring-orange-500 outline-none"
                                        />
                                    </template>
                                    <template v-else>
                                        {{ category.sortOrder || 0 }}
                                    </template>
                                </div>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-600">{{ category.id }}</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <template v-if="editingId === category.id">
                                    <input
                                        v-model="editForm.name"
                                        type="text"
                                        class="px-3 py-1 border border-gray-200 rounded-lg focus:ring-2 focus:ring-orange-500 outline-none"
                                    />
                                </template>
                                <template v-else>
                                    <span class="font-medium text-gray-800">{{ category.name }}</span>
                                </template>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-gray-500">
                                {{ category.createTime ? new Date(category.createTime).toLocaleDateString('zh-CN') : '-' }}
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center gap-1">
                                    <template v-if="editingId === category.id">
                                        <button 
                                            @click="handleUpdate(category.id)"
                                            class="p-2 text-green-500 hover:text-green-700 hover:bg-green-50 rounded-lg transition"
                                            title="保存"
                                        >
                                            <Save class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="cancelEdit"
                                            class="p-2 text-gray-500 hover:text-gray-700 hover:bg-gray-100 rounded-lg transition"
                                            title="取消"
                                        >
                                            <X class="w-4 h-4" />
                                        </button>
                                    </template>
                                    <template v-else>
                                        <button 
                                            @click="startEdit(category)"
                                            class="p-2 text-blue-500 hover:text-blue-700 hover:bg-blue-50 rounded-lg transition"
                                            title="编辑"
                                        >
                                            <Edit2 class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="handleDelete(category)"
                                            class="p-2 text-red-500 hover:text-red-700 hover:bg-red-50 rounded-lg transition"
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
