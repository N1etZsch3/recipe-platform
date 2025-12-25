// 消息模块共享工具函数
import { useRouter } from 'vue-router'

// 格式化时间
export const formatTime = (timeStr) => {
    if (!timeStr) return ''
    const date = new Date(timeStr)
    const now = new Date()
    const diff = now - date

    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'

    const isThisYear = date.getFullYear() === now.getFullYear()
    if (isThisYear) {
        return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' }) + ' ' +
            date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
    }
    return date.toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' })
}



// 导航到用户主页
export const useUserNavigation = () => {
    const router = useRouter()

    const goToUserProfile = (userId) => {
        if (userId) {
            router.push(`/user/${userId}`)
        }
    }

    const goToRecipe = (recipeId) => {
        if (recipeId) {
            router.push(`/recipe/${recipeId}`)
        }
    }

    return { goToUserProfile, goToRecipe }
}
