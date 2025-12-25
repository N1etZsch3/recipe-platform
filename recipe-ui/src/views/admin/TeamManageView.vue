<script setup>
import { ref, onMounted } from 'vue'
import { Plus, Pencil, Trash2, Save, X, Users, Upload, Image } from 'lucide-vue-next'
import { adminListTeamMembers, adminAddTeamMember, adminUpdateTeamMember, adminDeleteTeamMember } from '@/api/team'
import { uploadFile } from '@/api/common'
import { useToast } from '@/components/Toast.vue'
import { useModal } from '@/composables/useModal'

const { showToast } = useToast()
const { confirm } = useModal()

const members = ref([])
const loading = ref(true)
const uploading = ref(false)

// 编辑状态
const editingId = ref(null)
const showAddModal = ref(false)

// 表单数据
const formData = ref({
  name: '',
  role: '',
  avatar: '',
  emoji: '👤',
  color: 'from-orange-500 to-red-500',
  bgColor: 'bg-orange-50',
  description: '',
  skills: '',
  gitType: 'github',
  github: '',
  email: '',
  sortOrder: 0
})

// Git 平台选项
const gitPlatforms = [
  { label: 'GitHub', value: 'github', baseUrl: 'https://github.com/' },
  { label: 'Gitee', value: 'gitee', baseUrl: 'https://gitee.com/' }
]

// 可选的渐变色
const colorOptions = [
  { label: '橙色', value: 'from-orange-500 to-red-500', bg: 'bg-orange-50' },
  { label: '紫色', value: 'from-purple-500 to-indigo-600', bg: 'bg-purple-50' },
  { label: '粉色', value: 'from-pink-500 to-rose-500', bg: 'bg-pink-50' },
  { label: '蓝色', value: 'from-blue-500 to-cyan-500', bg: 'bg-blue-50' },
  { label: '绿色', value: 'from-green-500 to-emerald-500', bg: 'bg-green-50' }
]

const fetchMembers = async () => {
  try {
    loading.value = true
    const data = await adminListTeamMembers()
    members.value = data || []
  } catch (error) {
    console.error('获取成员失败', error)
    showToast('获取成员列表失败')
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  formData.value = {
    name: '',
    role: '',
    avatar: '',
    emoji: '👤',
    color: 'from-orange-500 to-red-500',
    bgColor: 'bg-orange-50',
    description: '',
    skills: '',
    gitType: 'github',
    github: '',
    email: '',
    sortOrder: 0
  }
}

const startAdd = () => {
  resetForm()
  showAddModal.value = true
  editingId.value = null
}

const startEdit = (member) => {
  formData.value = {
    name: member.name || '',
    role: member.role || '',
    avatar: member.avatar || '',
    emoji: member.emoji || '👤',
    color: member.color || 'from-orange-500 to-red-500',
    bgColor: member.bgColor || 'bg-orange-50',
    description: member.description || '',
    skills: member.skills || '',
    gitType: member.gitType || 'github',
    github: member.github || '',
    email: member.email || '',
    sortOrder: member.sortOrder || 0
  }
  editingId.value = member.id
  showAddModal.value = true
}

const cancelEdit = () => {
  editingId.value = null
  showAddModal.value = false
  resetForm()
}

const handleUpload = async (event) => {
  const file = event.target.files?.[0]
  if (!file) return
  
  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    showToast('请选择图片文件')
    return
  }
  
  // 验证文件大小 (最大 2MB)
  if (file.size > 2 * 1024 * 1024) {
    showToast('图片大小不能超过 2MB')
    return
  }
  
  try {
    uploading.value = true
    const url = await uploadFile(file)
    formData.value.avatar = url
    showToast('上传成功')
  } catch (error) {
    console.error('上传失败', error)
    showToast('上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const handleSave = async () => {
  if (!formData.value.name) {
    showToast('请输入姓名')
    return
  }
  try {
    if (editingId.value) {
      await adminUpdateTeamMember(editingId.value, formData.value)
      showToast('修改成功')
    } else {
      await adminAddTeamMember(formData.value)
      showToast('添加成功')
    }
    cancelEdit()
    fetchMembers()
  } catch (error) {
    console.error('保存失败', error)
    showToast('保存失败')
  }
}

const handleDelete = async (member) => {
  const confirmed = await confirm(`确定要删除成员「${member.name}」吗？`)
  if (!confirmed) return
  
  try {
    await adminDeleteTeamMember(member.id)
    showToast('删除成功')
    fetchMembers()
  } catch (error) {
    console.error('删除失败', error)
    showToast('删除失败')
  }
}

onMounted(() => {
  fetchMembers()
})
</script>

<template>
  <div class="p-6 h-full overflow-y-auto">
    <!-- Header -->
    <div class="flex items-center justify-between mb-6">
      <div class="flex items-center gap-3">
        <div class="p-2 bg-orange-500/20 rounded-lg">
          <Users class="w-5 h-5 text-orange-400" />
        </div>
        <h1 class="text-xl font-bold text-gray-200">团队成员管理</h1>
      </div>
      <button
        @click="startAdd"
        class="flex items-center gap-2 px-4 py-2 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition font-medium"
      >
        <Plus class="w-4 h-4" />
        添加成员
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center py-12">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-orange-500"></div>
    </div>

    <!-- Empty State -->
    <div v-else-if="members.length === 0" class="text-center py-20">
      <div class="w-20 h-20 bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-4">
        <Users class="w-10 h-10 text-gray-600" />
      </div>
      <p class="text-gray-500 mb-4">暂无团队成员</p>
      <button @click="startAdd" class="text-orange-400 hover:text-orange-300 font-medium">
        点击添加第一位成员
      </button>
    </div>

    <!-- Member Cards Grid -->
    <div v-else class="grid grid-cols-1 lg:grid-cols-2 gap-4">
      <div
        v-for="member in members"
        :key="member.id"
        class="bg-gray-800/50 rounded-xl p-5 border border-gray-700/50 hover:border-gray-600 transition group"
      >
        <div class="flex items-start gap-4">
          <!-- Avatar -->
          <div 
            v-if="member.avatar"
            class="w-16 h-16 rounded-xl overflow-hidden flex-shrink-0 shadow-lg"
          >
            <img :src="member.avatar" :alt="member.name" class="w-full h-full object-cover" />
          </div>
          <div 
            v-else
            :class="['w-16 h-16 rounded-xl bg-gradient-to-br flex items-center justify-center text-2xl shadow-lg flex-shrink-0', member.color || 'from-orange-500 to-red-500']"
          >
            {{ member.emoji || '👤' }}
          </div>
          
          <!-- Info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-start justify-between">
              <div>
                <h3 class="font-bold text-gray-200">{{ member.name }}</h3>
                <p class="text-orange-400 text-sm">{{ member.role }}</p>
              </div>
              <div class="opacity-0 group-hover:opacity-100 transition flex gap-1">
                <button @click="startEdit(member)" class="p-1.5 text-blue-400 hover:bg-blue-900/30 rounded-lg">
                  <Pencil class="w-4 h-4" />
                </button>
                <button @click="handleDelete(member)" class="p-1.5 text-red-400 hover:bg-red-900/30 rounded-lg">
                  <Trash2 class="w-4 h-4" />
                </button>
              </div>
            </div>
            <p class="text-gray-400 text-sm mt-2 line-clamp-2">{{ member.description }}</p>
            <div class="flex gap-3 mt-2 text-xs text-gray-500">
              <span v-if="member.github">@{{ member.github }}</span>
              <span v-if="member.email">{{ member.email }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Modal -->
    <Teleport to="body">
      <div v-if="showAddModal" class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
        <div class="bg-gray-800 rounded-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden shadow-2xl">
          <!-- Modal Header -->
          <div class="px-6 py-4 border-b border-gray-700 flex items-center justify-between">
            <h3 class="text-lg font-bold text-gray-200">
              {{ editingId ? '编辑成员' : '添加新成员' }}
            </h3>
            <button @click="cancelEdit" class="p-2 text-gray-400 hover:text-gray-200 hover:bg-gray-700 rounded-lg">
              <X class="w-5 h-5" />
            </button>
          </div>
          
          <!-- Modal Body -->
          <div class="p-6 overflow-y-auto max-h-[calc(90vh-140px)]">
            <!-- Avatar Upload -->
            <div class="flex items-center gap-6 mb-6 pb-6 border-b border-gray-700">
              <div class="relative">
                <div 
                  v-if="formData.avatar"
                  class="w-24 h-24 rounded-xl overflow-hidden"
                >
                  <img :src="formData.avatar" class="w-full h-full object-cover" />
                </div>
                <div 
                  v-else
                  :class="['w-24 h-24 rounded-xl bg-gradient-to-br flex items-center justify-center text-4xl', formData.color]"
                >
                  {{ formData.emoji }}
                </div>
              </div>
              <div class="flex-1">
                <p class="text-sm text-gray-400 mb-2">头像</p>
                <div class="flex gap-3">
                  <label class="flex items-center gap-2 px-4 py-2 bg-gray-700 text-gray-200 rounded-lg cursor-pointer hover:bg-gray-600 transition text-sm">
                    <Upload class="w-4 h-4" />
                    {{ uploading ? '上传中...' : '上传图片' }}
                    <input type="file" accept="image/*" class="hidden" @change="handleUpload" :disabled="uploading" />
                  </label>
                  <button 
                    v-if="formData.avatar"
                    @click="formData.avatar = ''"
                    class="px-4 py-2 text-red-400 hover:bg-red-900/30 rounded-lg text-sm"
                  >
                    删除
                  </button>
                </div>
                <p class="text-xs text-gray-500 mt-2">支持 JPG、PNG 格式，最大 2MB。如未上传图片将使用 Emoji 头像。</p>
              </div>
            </div>

            <!-- Form Fields -->
            <div class="grid grid-cols-2 gap-4">
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">姓名 <span class="text-red-400">*</span></label>
                <input v-model="formData.name" type="text" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none" placeholder="请输入姓名" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">角色/职位</label>
                <input v-model="formData.role" type="text" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none" placeholder="如: 全栈开发工程师" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">备选 Emoji</label>
                <input v-model="formData.emoji" type="text" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none" placeholder="👤" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">主题色</label>
                <select v-model="formData.color" @change="formData.bgColor = colorOptions.find(c => c.value === formData.color)?.bg" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none">
                  <option v-for="c in colorOptions" :key="c.value" :value="c.value">{{ c.label }}</option>
                </select>
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">Git 主页</label>
                <div class="flex gap-2">
                  <select v-model="formData.gitType" class="w-28 bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none">
                    <option v-for="p in gitPlatforms" :key="p.value" :value="p.value">{{ p.label }}</option>
                  </select>
                  <input v-model="formData.github" type="text" class="flex-1 bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none" placeholder="用户名" />
                </div>
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">邮箱</label>
                <input v-model="formData.email" type="email" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none" placeholder="email@example.com" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">排序</label>
                <input v-model.number="formData.sortOrder" type="number" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none" placeholder="0" />
              </div>
              <div>
                <label class="block text-sm text-gray-400 mb-1.5">技能标签</label>
                <input v-model="formData.skills" type="text" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none" placeholder='["Vue.js", "Java"]' />
              </div>
              <div class="col-span-2">
                <label class="block text-sm text-gray-400 mb-1.5">个人简介</label>
                <textarea v-model="formData.description" rows="3" class="w-full bg-gray-700 border border-gray-600 rounded-lg px-3 py-2.5 text-gray-200 focus:border-orange-500 focus:outline-none resize-none" placeholder="一句话介绍"></textarea>
              </div>
            </div>
          </div>
          
          <!-- Modal Footer -->
          <div class="px-6 py-4 border-t border-gray-700 flex justify-end gap-3">
            <button @click="cancelEdit" class="px-5 py-2.5 text-gray-300 hover:bg-gray-700 rounded-lg transition">
              取消
            </button>
            <button @click="handleSave" class="flex items-center gap-2 px-5 py-2.5 bg-orange-500 text-white rounded-lg hover:bg-orange-600 transition font-medium">
              <Save class="w-4 h-4" />
              保存
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>
