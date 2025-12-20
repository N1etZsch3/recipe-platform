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
