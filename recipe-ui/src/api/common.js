import request from '@/utils/request'

// 上传文件
export function uploadFile(file) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/api/v1/common/upload', formData, {
        headers: {
            // 设置为 undefined，让浏览器自动设置 multipart/form-data 及正确的 boundary
            'Content-Type': undefined
        },
        // 文件上传需要更长的超时时间（60秒）
        timeout: 60000
    })
}
