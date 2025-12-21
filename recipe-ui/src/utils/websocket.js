/**
 * WebSocket 连接管理器
 * 负责建立、维护 WebSocket 连接，处理心跳和断线重连
 */
import { useNotificationStore } from '@/stores/notification'
import { useUserStore } from '@/stores/user'

class WebSocketManager {
    constructor() {
        this.ws = null
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 5
        this.reconnectInterval = 3000 // 3秒
        this.heartbeatInterval = null
        this.heartbeatTimeout = 30000 // 30秒发送一次心跳
        this.isManualClose = false
    }

    /**
     * 建立 WebSocket 连接
     */
    connect() {
        const userStore = useUserStore()
        const token = userStore.token

        if (!token) {
            console.warn('WebSocket: 未登录，跳过连接')
            return
        }

        if (this.ws && (this.ws.readyState === WebSocket.OPEN || this.ws.readyState === WebSocket.CONNECTING)) {
            console.warn('WebSocket: 已连接或正在连接，跳过重复连接')
            return
        }

        // 构建 WebSocket URL
        // 开发环境使用 VITE_API_BASE_URL，生产环境使用相对路径
        const apiUrl = import.meta.env.VITE_API_BASE_URL || ''
        let wsUrl

        if (apiUrl) {
            // 将 http:// 替换为 ws://，https:// 替换为 wss://
            wsUrl = apiUrl.replace(/^http/, 'ws') + '/ws?token=' + token
        } else {
            // 使用当前页面的 host
            const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
            wsUrl = `${wsProtocol}//${window.location.host}/ws?token=${token}`
        }

        console.log('WebSocket: 正在连接...', wsUrl)

        try {
            this.ws = new WebSocket(wsUrl)
            this.isManualClose = false

            this.ws.onopen = () => {
                console.log('WebSocket: 连接成功')
                this.reconnectAttempts = 0
                this.startHeartbeat()
            }

            this.ws.onmessage = (event) => {
                this.handleMessage(event.data)
            }

            this.ws.onclose = (event) => {
                console.log('WebSocket: 连接关闭', event.code, event.reason)
                this.stopHeartbeat()

                if (!this.isManualClose) {
                    this.tryReconnect()
                }
            }

            this.ws.onerror = (error) => {
                console.error('WebSocket: 连接错误', error)
            }
        } catch (error) {
            console.error('WebSocket: 创建连接失败', error)
        }
    }

    /**
     * 处理收到的消息
     */
    handleMessage(data) {
        try {
            // 心跳响应
            if (data === 'pong') {
                console.debug('WebSocket: 收到心跳响应')
                return
            }

            const message = JSON.parse(data)
            console.log('WebSocket: 收到消息', message)

            // 将消息传递给通知 store 处理
            const notificationStore = useNotificationStore()
            notificationStore.handleMessage(message)
        } catch (e) {
            console.error('WebSocket: 解析消息失败', e)
        }
    }

    /**
     * 开始心跳检测
     */
    startHeartbeat() {
        this.stopHeartbeat() // 先清除可能存在的定时器

        this.heartbeatInterval = setInterval(() => {
            if (this.ws && this.ws.readyState === WebSocket.OPEN) {
                this.ws.send('ping')
                console.debug('WebSocket: 发送心跳')
            }
        }, this.heartbeatTimeout)
    }

    /**
     * 停止心跳
     */
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval)
            this.heartbeatInterval = null
        }
    }

    /**
     * 尝试重连
     */
    tryReconnect() {
        if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.warn('WebSocket: 重连次数已达上限，停止重连')
            return
        }

        this.reconnectAttempts++
        const delay = this.reconnectInterval * this.reconnectAttempts // 递增延迟

        console.log(`WebSocket: ${delay / 1000}秒后尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})`)

        setTimeout(() => {
            // 检查是否需要重连（用户可能已登出）
            const userStore = useUserStore()
            if (userStore.token && !this.isManualClose) {
                this.connect()
            }
        }, delay)
    }

    /**
     * 关闭连接
     */
    close() {
        console.log('WebSocket: 主动关闭连接')
        this.isManualClose = true
        this.stopHeartbeat()
        this.reconnectAttempts = 0

        if (this.ws) {
            this.ws.close()
            this.ws = null
        }
    }

    /**
     * 发送消息
     */
    send(message) {
        if (this.ws && this.ws.readyState === WebSocket.OPEN) {
            const data = typeof message === 'string' ? message : JSON.stringify(message)
            this.ws.send(data)
            return true
        }
        console.warn('WebSocket: 连接未就绪，无法发送消息')
        return false
    }

    /**
     * 获取连接状态
     */
    getState() {
        if (!this.ws) return 'CLOSED'
        const states = ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED']
        return states[this.ws.readyState] || 'UNKNOWN'
    }

    /**
     * 检查是否已连接
     */
    isConnected() {
        return this.ws && this.ws.readyState === WebSocket.OPEN
    }
}

// 导出单例实例
export const wsManager = new WebSocketManager()
