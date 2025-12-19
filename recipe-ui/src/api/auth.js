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
