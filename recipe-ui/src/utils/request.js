import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
    baseURL: '', // Proxy handles this
    timeout: 20000
})

// Request Interceptor
request.interceptors.request.use(
    config => {
        const userStore = useUserStore()
        if (userStore.token) {
            config.headers['Authorization'] = 'Bearer ' + userStore.token
        }
        // 对于FormData，删除Content-Type让axios自动设置multipart/form-data及boundary
        if (config.data instanceof FormData) {
            delete config.headers['Content-Type']
        }
        return config
    },
    error => Promise.reject(error)
)

// Response Interceptor
request.interceptors.response.use(
    response => {
        const res = response.data
        // Assuming backend returns { code: 200, message: "...", data: ... }
        // Adjust based on your Result class.
        if (res.code === 200) {
            return res.data
        } else {
            // 创建自定义错误对象，保留完整的响应信息
            const error = new Error(res.msg || 'Error')
            error.code = res.code  // 保留状态码（如 409）
            error.data = res.data  // 保留数据（如 requireConfirm 等）
            return Promise.reject(error)
        }
    },
    error => {
        // Handle HTTP errors (401, 403, 500)
        if (error.response && error.response.status === 401) {
            const userStore = useUserStore()
            userStore.logout()
            // redirect to login?
        }
        return Promise.reject(error)
    }
)

export default request
