/**
 * 全局模态框 composable
 * 提供 confirm, prompt, alert 方法替代原生浏览器弹窗
 */
import { shallowRef } from 'vue'

// 全局模态框实例引用
const modalRef = shallowRef(null)

/**
 * 设置模态框组件引用（在 App.vue 中调用）
 */
export function setModalRef(ref) {
    modalRef.value = ref
}

/**
 * 等待 modalRef 初始化（最多等待 1 秒）
 */
const waitForModal = () => {
    return new Promise((resolve) => {
        if (modalRef.value) {
            resolve(true)
            return
        }
        
        let attempts = 0
        const maxAttempts = 20
        const interval = setInterval(() => {
            attempts++
            if (modalRef.value) {
                clearInterval(interval)
                resolve(true)
            } else if (attempts >= maxAttempts) {
                clearInterval(interval)
                console.warn('Modal component not initialized after waiting')
                resolve(false)
            }
        }, 50)
    })
}

/**
 * 使用模态框
 */
export function useModal() {
    /**
     * 确认框
     * @param {string} message - 提示消息
     * @param {Object} options - 选项
     * @param {string} options.confirmText - 确认按钮文字
     * @param {string} options.cancelText - 取消按钮文字
     * @param {boolean} options.danger - 是否危险操作（红色按钮）
     * @returns {Promise<boolean>}
     */
    const confirm = async (message, options = {}) => {
        await waitForModal()
        if (!modalRef.value) {
            console.error('Modal component not available')
            return false
        }
        return modalRef.value.confirm(message, options)
    }

    /**
     * 输入框
     * @param {string} message - 提示消息
     * @param {Object} options - 选项
     * @param {string} options.defaultValue - 默认值
     * @param {string} options.placeholder - 占位符
     * @param {string} options.confirmText - 确认按钮文字
     * @param {string} options.cancelText - 取消按钮文字
     * @returns {Promise<string|null>} - 返回输入值，取消时返回 null
     */
    const prompt = async (message, options = {}) => {
        await waitForModal()
        if (!modalRef.value) {
            console.error('Modal component not available')
            return null
        }
        return modalRef.value.prompt(message, options)
    }

    /**
     * 提示框（只有确定按钮）
     * @param {string} message - 提示消息
     * @param {Object} options - 选项
     * @param {string} options.confirmText - 确认按钮文字
     * @returns {Promise<boolean>}
     */
    const alert = async (message, options = {}) => {
        await waitForModal()
        if (!modalRef.value) {
            console.error('Modal component not available')
            return true
        }
        return modalRef.value.alert(message, options)
    }

    return { confirm, prompt, alert }
}

export default useModal
