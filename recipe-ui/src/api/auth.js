import request from '@/utils/request'

export function login(data) {
    return request({
        url: '/api/v1/users/login',
        method: 'post',
        data
    })
}

export function register(data) {
    return request({
        url: '/api/v1/users/register',
        method: 'post',
        data
    })
}

export function getProfile() {
    return request({
        url: '/api/v1/users/me',
        method: 'get'
    })
}

// 更新个人资料 (昵称、头像、简介)
export function updateProfile(data) {
    return request({
        url: '/api/v1/users/me',
        method: 'put',
        data
    })
}

// 获取图形验证码
export function getCaptcha() {
    return request({
        url: '/api/v1/captcha',
        method: 'get'
    })
}

// 强制登录（踢掉已在线的旧会话）
export function forceLogin(data) {
    return request({
        url: '/api/v1/users/force-login',
        method: 'post',
        data
    })
}

// 修改密码
export function changePassword(data) {
    return request({
        url: '/api/v1/auth/password',
        method: 'put',
        data
    })
}
