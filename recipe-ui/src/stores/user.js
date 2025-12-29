import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getProfile } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
    // Initialize from LocalStorage to persist across reloads
    const user = ref(JSON.parse(localStorage.getItem('user')) || null)
    const token = ref(localStorage.getItem('token') || '')
    // 上次验证时间戳，用于避免频繁验证
    const lastValidated = ref(0)

    const setUser = (userData) => {
        user.value = userData
        localStorage.setItem('user', JSON.stringify(userData))
    }

    const setToken = (tokenStr) => {
        token.value = tokenStr
        localStorage.setItem('token', tokenStr)
        lastValidated.value = 0 // 新 token 需要重新验证
    }

    const logout = () => {
        user.value = null
        token.value = ''
        lastValidated.value = 0
        localStorage.removeItem('user')
        localStorage.removeItem('token')
    }

    // Role helper
    const isAdmin = () => {
        return user.value && user.value.role === 'admin'
    }

    /**
     * 验证 token 有效性
     * @param {boolean} force - 是否强制验证（忽略缓存时间）
     * @returns {Promise<boolean>} - token 是否有效
     */
    const validateToken = async (force = false) => {
        if (!token.value) {
            return false
        }

        // 5分钟内不重复验证（避免频繁请求）
        const now = Date.now()
        if (!force && lastValidated.value && (now - lastValidated.value) < 5 * 60 * 1000) {
            return true
        }

        try {
            const userData = await getProfile()
            if (userData) {
                setUser(userData)
                lastValidated.value = now
                return true
            }
            return false
        } catch (error) {
            // Token 无效或过期
            console.warn('Token validation failed:', error.message)
            logout()
            return false
        }
    }

    return { user, token, setUser, setToken, logout, isAdmin, validateToken }
})
