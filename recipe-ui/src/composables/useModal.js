/**
 * 全局模态框 composable
 * 提供 confirm, prompt, alert 方法替代原生浏览器弹窗
 */
import { ref, shallowRef } from 'vue'

// 全局模态框实例引用
const modalRef = shallowRef(null)

/**
 * 设置模态框组件引用（在 App.vue 中调用）
 */
export function setModalRef(ref) {
    modalRef.value = ref
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
        if (!modalRef.value) {
            console.warn('Modal component not initialized, falling back to native confirm')
            return window.confirm(message)
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
        if (!modalRef.value) {
            console.warn('Modal component not initialized, falling back to native prompt')
            return window.prompt(message, options.defaultValue || '')
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
        if (!modalRef.value) {
            console.warn('Modal component not initialized, falling back to native alert')
            window.alert(message)
            return true
        }
        return modalRef.value.alert(message, options)
    }

    return { confirm, prompt, alert }
}

export default useModal
