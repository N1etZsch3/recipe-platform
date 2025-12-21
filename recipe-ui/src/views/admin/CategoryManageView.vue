<script setup>
import { ref, onMounted, computed } from 'vue'
import { listCategories, addCategory, updateCategory, deleteCategory } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'
import { 
    Plus, Edit2, Trash2, Save, X, GripVertical, FolderOpen, 
    Search, RefreshCw, ChevronLeft, ChevronRight 
} from 'lucide-vue-next'

const { showToast } = useToast()
const { confirm } = useModal()

const loading = ref(false)
const allCategories = ref([]) // Store all categories
const showAddForm = ref(false)
const editingId = ref(null)

const newCategory = ref({ name: '', sortOrder: 0 })
const editForm = ref({ name: '', sortOrder: 0 })

// Search & Pagination
const searchKeyword = ref('')
const pagination = ref({
    current: 1,
    size: 20
})

const fetchCategories = async () => {
    loading.value = true
    try {
        const res = await listCategories()
        if (res) {
            allCategories.value = res
        }
    } catch (error) {
        console.error('Failed to fetch categories', error)
        showToast('获取分类列表失败')
    } finally {
        loading.value = false
    }
}

const filteredCategories = computed(() => {
    if (!searchKeyword.value) return allCategories.value
    const lower = searchKeyword.value.toLowerCase()
    return allCategories.value.filter(c => c.name.toLowerCase().includes(lower))
})

const total = computed(() => filteredCategories.value.length)
const totalPages = computed(() => Math.ceil(total.value / pagination.value.size))

const visibleCategories = computed(() => {
    const start = (pagination.value.current - 1) * pagination.value.size
    const end = start + pagination.value.size
    return filteredCategories.value.slice(start, end)
})

const visiblePages = computed(() => {
    const pages = []
    const t = totalPages.value
    const c = pagination.value.current
    if (t <= 7) {
        for (let i = 1; i <= t; i++) pages.push(i)
    } else {
        if (c <= 4) {
            pages.push(1, 2, 3, 4, 5, '...', t)
        } else if (c >= t - 3) {
            pages.push(1, '...', t - 4, t - 3, t - 2, t - 1, t)
        } else {
            pages.push(1, '...', c - 1, c, c + 1, '...', t)
        }
    }
    return pages
})

const handlePageChange = (page) => {
    pagination.value.current = page
}

const handleSearch = () => {
    pagination.value.current = 1
}

const handleReset = () => {
    searchKeyword.value = ''
    handleSearch()
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
        allCategories.value = allCategories.value.filter(c => c.id !== category.id) // Local update
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
    <div class="flex flex-col h-full p-5 space-y-4">
        <!-- 顶部搜索栏 -->
        <div class="flex-shrink-0 bg-white rounded-xl p-5 border border-gray-100 shadow-sm">
            <div class="flex flex-wrap items-center gap-6">
                <!-- 搜索关键词 -->
                <div class="flex items-center gap-3">
                    <label class="text-sm font-medium text-gray-600 whitespace-nowrap">搜索分类</label>
                    <div class="relative">
                        <input 
                            v-model="searchKeyword"
                            type="text" 
                            placeholder="搜索分类名称..." 
                            class="w-64 px-3 py-2 pl-9 bg-white border border-gray-200 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-100 focus:border-blue-400 transition"
                            @keyup.enter="handleSearch"
                        >
                        <Search class="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    </div>
                </div>

                <!-- 操作按钮组 -->
                <div class="flex items-center gap-3 ml-auto">
                    <button 
                        @click="handleReset"
                        class="px-4 py-2 text-sm font-medium text-blue-500 bg-white border border-blue-200 rounded-lg hover:bg-blue-50 transition"
                    >
                        重置
                    </button>
                    <!-- 查询按钮其实对于前端搜索来说不是必需的，但为了保持UI一致性保留 -->
                    <button 
                        @click="handleSearch"
                        class="px-5 py-2 text-sm font-medium text-white bg-blue-500 rounded-lg hover:bg-blue-600 shadow-sm shadow-blue-200 transition"
                    >
                        查询
                    </button>
                </div>
            </div>
        </div>

        <!-- 表格主体区域 -->
        <div class="flex-1 min-h-0 bg-white rounded-xl border border-gray-100 shadow-sm flex flex-col relative">
            <!-- 工具栏 -->
            <div class="flex-shrink-0 p-4 border-b border-gray-100 flex items-center justify-between z-20 bg-white rounded-t-xl">
                <div class="flex items-center gap-3">
                    <button 
                        @click="showAddForm = true"
                        class="px-4 py-2 text-sm font-medium text-blue-500 border border-blue-200 rounded-lg hover:bg-blue-50 transition flex items-center gap-2"
                    >
                        <Plus class="w-4 h-4" />
                        新增分类
                    </button>
                    <div class="text-sm font-medium text-gray-500 flex items-center gap-2 ml-2">
                         <FolderOpen class="w-4 h-4" />
                         共 {{ total }} 个分类
                    </div>
                </div>
                
                <div class="flex items-center gap-2">
                    <button class="p-2 text-gray-400 hover:bg-gray-100 rounded-lg transition" title="刷新" @click="fetchCategories">
                        <RefreshCw class="w-4 h-4" />
                    </button>
                </div>
            </div>

            <!-- 添加表单 (嵌入式) -->
            <transition
                enter-active-class="transition ease-out duration-200"
                enter-from-class="opacity-0 -translate-y-2"
                enter-to-class="opacity-100 translate-y-0"
                leave-active-class="transition ease-in duration-150"
                leave-from-class="opacity-100 translate-y-0"
                leave-to-class="opacity-0 -translate-y-2"
            >
                <div v-if="showAddForm" class="p-4 bg-blue-50/50 border-b border-gray-100">
                    <div class="flex gap-4 items-end max-w-3xl">
                        <div class="flex-1">
                            <label class="block text-xs font-medium text-gray-500 mb-1.5">分类名称</label>
                            <input
                                v-model="newCategory.name"
                                type="text"
                                placeholder="输入分类名称"
                                class="w-full px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-100 focus:border-blue-400 outline-none transition"
                            />
                        </div>
                        <div class="w-24">
                            <label class="block text-xs font-medium text-gray-500 mb-1.5">排序</label>
                            <input
                                v-model.number="newCategory.sortOrder"
                                type="number"
                                placeholder="0"
                                class="w-full px-3 py-2 bg-white border border-gray-200 rounded-lg text-sm focus:ring-2 focus:ring-blue-100 focus:border-blue-400 outline-none transition"
                            />
                        </div>
                        <div class="flex gap-2 pb-0.5">
                            <button 
                                @click="handleAdd"
                                class="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition text-sm font-medium shadow-sm shadow-blue-200"
                            >
                                保存
                            </button>
                            <button 
                                @click="showAddForm = false; newCategory = { name: '', sortOrder: 0 }"
                                class="px-4 py-2 bg-white border border-gray-200 text-gray-600 rounded-lg hover:bg-gray-50 transition text-sm font-medium"
                            >
                                取消
                            </button>
                        </div>
                    </div>
                </div>
            </transition>

            <!-- 表格滚动区域 -->
            <div class="flex-1 overflow-auto custom-scrollbar">
                <table class="w-full text-left">
                    <thead class="bg-white sticky top-0 z-10 text-xs font-semibold text-gray-500 uppercase">
                        <tr class="border-b border-gray-100">
                            <th class="p-4 w-24 text-center">排序</th>
                            <th class="p-4 w-20">ID</th>
                            <th class="p-4">分类名称</th>
                            <th class="p-4">创建时间</th>
                            <th class="p-4 text-right pr-8 w-40">操作</th>
                        </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-50 text-sm">
                        <tr v-if="loading">
                             <td colspan="5" class="p-12 text-center text-gray-400">
                                 <div class="flex flex-col items-center">
                                     <div class="w-8 h-8 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                                     <span class="mt-2 text-xs">加载中...</span>
                                 </div>
                             </td>
                        </tr>
                        <tr v-else-if="visibleCategories.length === 0">
                             <td colspan="5" class="p-12 text-center text-gray-400">暂无分类数据</td>
                        </tr>
                        <tr 
                            v-else
                            v-for="category in visibleCategories" 
                            :key="category.id" 
                            class="hover:bg-gray-50/80 transition group"
                        >
                             <td class="p-4 text-center text-gray-500">
                                <div class="flex items-center justify-center gap-2">
                                    <template v-if="editingId === category.id">
                                        <input
                                            v-model.number="editForm.sortOrder"
                                            type="number"
                                            class="w-16 px-2 py-1 bg-white border border-gray-200 rounded focus:ring-2 focus:ring-blue-100 focus:border-blue-400 outline-none transition text-sm text-center"
                                        />
                                    </template>
                                    <template v-else>
                                        <GripVertical class="w-4 h-4 text-gray-300 cursor-grab" />
                                        <span class="inline-block w-6 text-center">{{ category.sortOrder || 0 }}</span>
                                    </template>
                                </div>
                             </td>
                             <td class="p-4 text-gray-500">{{ category.id }}</td>
                             <td class="p-4">
                                <template v-if="editingId === category.id">
                                    <input
                                        v-model="editForm.name"
                                        type="text"
                                        class="px-3 py-1.5 bg-white border border-gray-200 rounded focus:ring-2 focus:ring-blue-100 focus:border-blue-400 outline-none transition text-sm"
                                    />
                                </template>
                                <template v-else>
                                    <span class="font-medium text-gray-800">{{ category.name }}</span>
                                </template>
                             </td>
                             <td class="p-4 text-gray-500">{{ category.createTime ? new Date(category.createTime).toLocaleDateString('zh-CN') : '-' }}</td>
                             <td class="p-4 text-right pr-6">
                                <div class="flex items-center justify-end gap-2">
                                    <template v-if="editingId === category.id">
                                        <button 
                                            @click="handleUpdate(category.id)"
                                            class="p-1.5 text-emerald-500 hover:bg-emerald-50 rounded transition"
                                            title="保存"
                                        >
                                            <Save class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="cancelEdit"
                                            class="p-1.5 text-gray-400 hover:bg-gray-100 rounded transition"
                                            title="取消"
                                        >
                                            <X class="w-4 h-4" />
                                        </button>
                                    </template>
                                    <template v-else>
                                        <button 
                                            @click="startEdit(category)"
                                            class="p-1.5 text-blue-500 hover:bg-blue-50 rounded transition opacity-0 group-hover:opacity-100"
                                            title="编辑"
                                        >
                                            <Edit2 class="w-4 h-4" />
                                        </button>
                                        <button 
                                            @click="handleDelete(category)"
                                            class="p-1.5 text-red-500 hover:bg-red-50 rounded transition opacity-0 group-hover:opacity-100"
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

            <!-- 底部主要分页 -->
            <div class="flex-shrink-0 p-4 border-t border-gray-100 flex items-center justify-between">
                <div class="text-gray-500 text-sm">
                    共 {{ total }} 条
                </div>
                
                <div class="flex items-center gap-2">
                    <button 
                        class="w-8 h-8 flex items-center justify-center rounded border border-gray-200 text-gray-400 hover:border-blue-500 hover:text-blue-500 disabled:opacity-50 transition"
                        :disabled="pagination.current <= 1"
                        @click="handlePageChange(pagination.current - 1)"
                    >
                        <ChevronLeft class="w-4 h-4" />
                    </button>
                    
                    <template v-for="page in visiblePages" :key="page">
                        <span v-if="page === '...'" class="px-1 text-gray-400">...</span>
                        <button 
                            v-else
                            @click="handlePageChange(page)"
                            :class="[
                                'min-w-[32px] h-8 px-2 rounded border text-sm transition',
                                pagination.current === page 
                                    ? 'bg-blue-500 border-blue-500 text-white' 
                                    : 'border-gray-200 text-gray-600 hover:border-blue-500 hover:text-blue-500'
                            ]"
                        >
                            {{ page }}
                        </button>
                    </template>
                    
                    <button 
                        class="w-8 h-8 flex items-center justify-center rounded border border-gray-200 text-gray-400 hover:border-blue-500 hover:text-blue-500 disabled:opacity-50 transition"
                        :disabled="pagination.current >= totalPages"
                        @click="handlePageChange(pagination.current + 1)"
                    >
                        <ChevronRight class="w-4 h-4" />
                    </button>
                    
                    <select 
                        v-model="pagination.size"
                        @change="handleSearch"
                        class="h-8 px-2 border border-gray-200 rounded text-sm text-gray-600 focus:border-blue-500 outline-none ml-2"
                    >
                        <option :value="10">10条/页</option>
                        <option :value="20">20条/页</option>
                        <option :value="50">50条/页</option>
                    </select>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
    height: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
    background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #e2e8f0;
    border-radius: 3px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: #cbd5e1;
}
</style>
