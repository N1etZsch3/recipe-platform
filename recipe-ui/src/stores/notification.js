/**
 * 通知状态管理 (Pinia Store)
 * 管理实时通知的接收、存储和显示状态
 */
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
    // 通知列表
    const notifications = ref([])

    // 未读数量
    const unreadCount = computed(() => {
        return notifications.value.filter(n => !n.read).length
    })

    // 最新的 toast 通知（用于显示弹窗）
    const latestToast = ref(null)

    // 最新的通知（不管是否显示 Toast，供其他组件监听）
    const latestNotification = ref(null)

    // Toast 显示时间（毫秒）
    const toastDuration = 5000

    // 当前正在聊天的用户 ID（用于判断是否需要显示 Toast）
    const currentChatUserId = ref(null)

    /**
     * 处理 WebSocket 消息
     */
    function handleMessage(message) {
        // 忽略连接成功消息（可选择显示）
        if (message.type === 'CONNECTED') {
            console.log('WebSocket: 已连接到服务器')
            return
        }

        // 处理强制下线消息
        if (message.type === 'FORCED_LOGOUT') {
            console.warn('WebSocket: 收到强制下线消息', message)
            handleForcedLogout(message.content || '您已被强制下线')
            return
        }

        // ========== 管理员实时推送处理 ==========
        // 用户上线/离线状态变化 - 分发自定义事件供管理页面监听
        if (message.type === 'USER_ONLINE' || message.type === 'USER_OFFLINE') {
            window.dispatchEvent(new CustomEvent('admin-user-status', { detail: message }))
            // 不添加到通知列表，仅用于实时更新
            return
        }

        // 新菜谱待审核 - 分发事件并显示通知
        if (message.type === 'NEW_RECIPE_PENDING') {
            window.dispatchEvent(new CustomEvent('admin-recipe-pending', { detail: message }))
        }

        // 管理员新评论通知
        if (message.type === 'ADMIN_NEW_COMMENT') {
            window.dispatchEvent(new CustomEvent('admin-new-comment', { detail: message }))
        }

        // 菜谱撤销/删除通知
        if (message.type === 'RECIPE_WITHDRAWN') {
            window.dispatchEvent(new CustomEvent('admin-recipe-withdrawn', { detail: message }))
        }

        // 创建通知对象
        const notification = {
            id: Date.now() + Math.random(), // 确保唯一性
            ...message,
            read: false,
            receivedAt: new Date()
        }

        // 添加到通知列表头部
        notifications.value.unshift(notification)

        // 限制通知列表长度（最多保留100条）
        if (notifications.value.length > 100) {
            notifications.value = notifications.value.slice(0, 100)
        }

        // 更新最新通知（供 MessagesView 等组件监听）
        latestNotification.value = notification

        // 判断是否需要显示 Toast
        // 如果是新消息且正在聊天页面查看该用户，则不弹窗
        const shouldShowToast = !(message.type === 'NEW_MESSAGE' &&
            currentChatUserId.value &&
            message.senderId === currentChatUserId.value)

        if (shouldShowToast) {
            showToast(notification)
        }
    }

    /**
     * 处理强制下线
     */
    function handleForcedLogout(reason) {
        // 延迟导入避免循环依赖
        Promise.all([
            import('@/stores/user'),
            import('@/composables/useModal')
        ]).then(([{ useUserStore }, { useModal }]) => {
            const userStore = useUserStore()
            const { alert: showAlert } = useModal()
            // 清除用户状态
            userStore.logout()
            // 使用模态框显示提示
            showAlert(reason, { title: '您已被强制下线' }).finally(() => {
                // 跳转到登录页
                window.location.href = '/login'
            })
        })
    }

    /**
     * 显示 Toast 通知
     */
    function showToast(notification) {
        latestToast.value = notification

        // 自动隐藏
        setTimeout(() => {
            if (latestToast.value?.id === notification.id) {
                latestToast.value = null
            }
        }, toastDuration)
    }

    /**
     * 标记单个通知为已读
     */
    function markAsRead(notificationId) {
        const notification = notifications.value.find(n => n.id === notificationId)
        if (notification) {
            notification.read = true
        }
    }

    /**
     * 全部标记为已读
     */
    function markAllAsRead() {
        notifications.value.forEach(n => n.read = true)
    }

    /**
     * 删除单个通知
     */
    function removeNotification(notificationId) {
        const index = notifications.value.findIndex(n => n.id === notificationId)
        if (index > -1) {
            notifications.value.splice(index, 1)
        }
    }

    /**
     * 清空所有通知
     */
    function clearAll() {
        notifications.value = []
    }

    /**
     * 清除当前 toast
     */
    function clearToast() {
        latestToast.value = null
    }

    /**
     * 获取特定类型的通知
     */
    function getByType(type) {
        return notifications.value.filter(n => n.type === type)
    }

    /**
     * 设置当前聊天的用户 ID
     */
    function setCurrentChatUser(userId) {
        currentChatUserId.value = userId
    }

    /**
     * 清除当前聊天用户 ID
     */
    function clearCurrentChatUser() {
        currentChatUserId.value = null
    }

    return {
        // State
        notifications,
        unreadCount,
        latestToast,
        latestNotification,
        currentChatUserId,

        // Actions
        handleMessage,
        showToast,
        markAsRead,
        markAllAsRead,
        removeNotification,
        clearAll,
        clearToast,
        getByType,
        setCurrentChatUser,
        clearCurrentChatUser
    }
})
