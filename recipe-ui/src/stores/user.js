import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
    // Initialize from LocalStorage to persist across reloads
    const user = ref(JSON.parse(localStorage.getItem('user')) || null)
    const token = ref(localStorage.getItem('token') || '')

    const setUser = (userData) => {
        user.value = userData
        localStorage.setItem('user', JSON.stringify(userData))
    }

    const setToken = (tokenStr) => {
        token.value = tokenStr
        localStorage.setItem('token', tokenStr)
    }

    const logout = () => {
        user.value = null
        token.value = ''
        localStorage.removeItem('user')
        localStorage.removeItem('token')
    }

    // Role helper
    const isAdmin = () => {
        return user.value && user.value.role === 'admin'
    }

    return { user, token, setUser, setToken, logout, isAdmin }
})
