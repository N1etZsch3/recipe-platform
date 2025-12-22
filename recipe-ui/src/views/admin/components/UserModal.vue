<script setup>
import { ref, watch } from 'vue'
import { uploadFile } from '@/api/common'
import { addUser, updateUser } from '@/api/admin'
import { useToast } from '@/components/Toast.vue'
import { X, Upload, Trash2 } from 'lucide-vue-next'

const props = defineProps({
    modelValue: Boolean,
    editData: Object
})

const emit = defineEmits(['update:modelValue', 'success'])
const { showToast } = useToast()

const form = ref({
    username: '',
    nickname: '',
    password: '',
    avatar: '',
    role: 'member',
    intro: '',
    status: 1
})

const loading = ref(false)
const uploading = ref(false)

watch(() => props.modelValue, (val) => {
    if (val) {
        if (props.editData) {
            form.value = { ...props.editData, password: '' }
        } else {
            form.value = {
                username: '',
                nickname: '',
                password: '',
                avatar: '',
                role: 'member',
                intro: '',
                status: 1
            }
        }
    }
})

const handleUpload = async (event) => {
    const file = event.target.files[0]
    if (!file) return
    
    uploading.value = true
    try {
        const res = await uploadFile(file)
        if (res) {
            // Check if res is string (url) or object
            form.value.avatar = typeof res === 'string' ? res : (res.url || res.data)
            showToast('头像上传成功')
        }
    } catch (error) {
        console.error(error)
        showToast('上传失败')
    } finally {
        uploading.value = false
    }
}

const clearAvatar = () => {
    form.value.avatar = ''
}

const handleSubmit = async () => {
    if (!form.value.username) {
        showToast('请输入用户名')
        return
    }
    
    // 用户名格式校验（仅新建时）
    if (!props.editData) {
        const usernameRegex = /^[a-zA-Z0-9_]{6,12}$/
        if (!usernameRegex.test(form.value.username)) {
            showToast('用户名必须为6-12位，只能包含字母、数字和下划线')
            return
        }
    }
    
    if (!props.editData && !form.value.password) {
        showToast('请输入密码')
        return
    }
    
    // 密码格式校验（有输入时）
    if (form.value.password) {
        const passwordRegex = /^[a-zA-Z0-9_]+$/
        if (!passwordRegex.test(form.value.password)) {
            showToast('密码只能包含字母、数字和下划线')
            return
        }
    }
    
    // 昵称验证：1-20字符必填
    if (!form.value.nickname || form.value.nickname.length < 1 || form.value.nickname.length > 20) {
        showToast('昵称长度必须为1-20个字符')
        return
    }

    loading.value = true
    try {
        if (props.editData) {
            await updateUser(props.editData.id, form.value)
            showToast('修改成功')
        } else {
            await addUser(form.value)
            showToast('添加成功')
        }
        emit('success')
        emit('update:modelValue', false)
    } catch (error) {
        console.error(error)
        showToast(props.editData ? '修改失败' : '添加失败')
    } finally {
        loading.value = false
    }
}
</script>

<template>
    <div v-if="modelValue" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
        <div class="bg-white rounded-2xl w-full max-w-lg shadow-xl animate-in fade-in zoom-in-95 duration-200 flex flex-col max-h-[90vh]">
            <!-- Header -->
            <div class="p-6 border-b border-gray-100 flex items-center justify-between flex-shrink-0">
                <h3 class="text-lg font-semibold text-gray-800">{{ editData ? '编辑用户' : '新增用户' }}</h3>
                <button @click="$emit('update:modelValue', false)" class="text-gray-400 hover:text-gray-600 transition">
                    <X class="w-5 h-5" />
                </button>
            </div>

            <!-- Body -->
            <div class="p-6 space-y-5 overflow-y-auto custom-scrollbar flex-1">
                <!-- Avatar -->
                <div class="flex items-center gap-5">
                    <div class="relative group w-24 h-24 rounded-full bg-gray-50 flex items-center justify-center overflow-hidden border-2 border-dashed border-gray-200 hover:border-blue-400 transition">
                         <img v-if="form.avatar" :src="form.avatar" class="w-full h-full object-cover" />
                         <div v-else class="text-gray-400 flex flex-col items-center gap-1">
                             <Upload class="w-6 h-6" />
                             <span class="text-xs">头像</span>
                         </div>
                         <div v-if="uploading" class="absolute inset-0 bg-white/80 flex items-center justify-center">
                             <div class="w-5 h-5 border-2 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
                         </div>
                    </div>
                    <div class="flex flex-col gap-2">
                        <div class="flex gap-2">
                             <label class="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-lg cursor-pointer hover:bg-blue-700 transition shadow-sm shadow-blue-200">
                                 上传头像
                                 <input type="file" class="hidden" accept="image/*" @change="handleUpload" />
                             </label>
                             <button v-if="form.avatar" @click="clearAvatar" class="px-4 py-2 bg-red-50 text-red-600 text-sm font-medium rounded-lg hover:bg-red-100 transition">
                                 删除
                             </button>
                        </div>
                        <p class="text-xs text-gray-400">支持 JPG, PNG, GIF 格式，建议 200x200 像素</p>
                    </div>
                </div>

                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1.5">用户名 <span class="text-red-500">*</span></label>
                        <input v-model="form.username" :disabled="!!editData" class="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-100 focus:border-blue-500 outline-none disabled:bg-gray-50 disabled:text-gray-500 transition" placeholder="6-12位字母、数字或下划线" />
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1.5">昵称</label>
                        <input v-model="form.nickname" maxlength="20" class="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-100 focus:border-blue-500 outline-none transition" placeholder="昵称，1-20个字符" />
                    </div>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1.5">密码 <span v-if="!editData" class="text-red-500">*</span></label>
                    <input v-model="form.password" type="password" class="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-100 focus:border-blue-500 outline-none transition" :placeholder="editData ? '如果不修改请留空' : '字母、数字或下划线'" />
                </div>

                <div class="grid grid-cols-2 gap-4">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1.5">角色</label>
                        <select v-model="form.role" class="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-100 focus:border-blue-500 outline-none bg-white transition">
                            <option value="member">普通用户</option>
                            <option value="common_admin">普通管理员</option>
                        </select>
                    </div>
                    <div>
                         <label class="block text-sm font-medium text-gray-700 mb-1.5">状态</label>
                         <select v-model="form.status" class="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-100 focus:border-blue-500 outline-none bg-white transition">
                            <option :value="1">正常</option>
                            <option :value="0">封禁</option>
                        </select>
                    </div>
                </div>

                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1.5">个人简介</label>
                    <textarea v-model="form.intro" rows="3" class="w-full px-4 py-2.5 border border-gray-200 rounded-xl focus:ring-2 focus:ring-blue-100 focus:border-blue-500 outline-none transition resize-none" placeholder="写点什么介绍自己..."></textarea>
                </div>
            </div>

            <!-- Footer -->
            <div class="p-6 border-t border-gray-100 flex justify-end gap-3 flex-shrink-0 bg-gray-50/50 rounded-b-2xl">
                <button @click="$emit('update:modelValue', false)" class="px-5 py-2.5 text-gray-600 hover:bg-gray-100 hover:text-gray-900 rounded-xl transition font-medium">取消</button>
                <button @click="handleSubmit" :disabled="loading" class="px-6 py-2.5 bg-blue-600 text-white rounded-xl hover:bg-blue-700 transition shadow-md shadow-blue-200 disabled:opacity-50 disabled:cursor-not-allowed font-medium flex items-center gap-2">
                    <div v-if="loading" class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    {{ loading ? '提交中...' : '确定' }}
                </button>
            </div>
        </div>
    </div>
</template>
