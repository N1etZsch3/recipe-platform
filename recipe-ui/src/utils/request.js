import axios from 'axios'
import { useUserStore } from '@/stores/user'

const request = axios.create({
    baseURL: '', // Proxy handles this
    timeout: 5000
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
            // Handle business errors
            // You might want to use the toast here directly or throw error
            return Promise.reject(new Error(res.msg || 'Error'))
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
