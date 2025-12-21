import request from '@/utils/request'

// ================== Admin Login ==================

export const adminLogin = (data) => {
    return request({
        url: '/api/v1/admin/login',
        method: 'post',
        data
    })
}

// ================== Dashboard ==================

export const getDashboard = () => {
    return request({
        url: '/api/v1/admin/dashboard',
        method: 'get'
    })
}

// ================== Recipe Management ==================

export const listAuditRecipes = (params) => {
    return request({
        url: '/api/v1/admin/recipes/audit',
        method: 'get',
        params
    })
}

export const auditRecipe = (data) => {
    return request({
        url: '/api/v1/admin/recipes/audit',
        method: 'post',
        data
    })
}

export const listAllRecipes = (params) => {
    return request({
        url: '/api/v1/admin/recipes',
        method: 'get',
        params
    })
}

export const deleteRecipe = (id) => {
    return request({
        url: `/api/v1/admin/recipes/${id}`,
        method: 'delete'
    })
}

// ================== Category Management ==================

export const listCategories = () => {
    return request({
        url: '/api/v1/admin/categories',
        method: 'get'
    })
}

export const addCategory = (data) => {
    return request({
        url: '/api/v1/admin/categories',
        method: 'post',
        data
    })
}

export const updateCategory = (id, data) => {
    return request({
        url: `/api/v1/admin/categories/${id}`,
        method: 'put',
        data
    })
}

export const deleteCategory = (id) => {
    return request({
        url: `/api/v1/admin/categories/${id}`,
        method: 'delete'
    })
}

// ================== User Management ==================

// ================== User Management ==================

export const listUsers = (params) => {
    return request({
        url: '/api/v1/admin/users',
        method: 'get',
        params
    })
}

export const addUser = (data) => {
    return request({
        url: '/api/v1/admin/users',
        method: 'post',
        data
    })
}

export const updateUser = (id, data) => {
    return request({
        url: `/api/v1/admin/users/${id}`,
        method: 'put',
        data
    })
}

export const updateUserStatus = (userId, data) => {
    return request({
        url: `/api/v1/admin/users/${userId}/status`,
        method: 'put',
        data
    })
}

export const batchUpdateUserStatus = (data) => {
    return request({
        url: '/api/v1/admin/users/batch/status',
        method: 'put',
        data
    })
}

// ================== Comment Management ==================

export const listComments = (params) => {
    return request({
        url: '/api/v1/admin/comments',
        method: 'get',
        params
    })
}

export const deleteComment = (id) => {
    return request({
        url: `/api/v1/admin/comments/${id}`,
        method: 'delete'
    })
}

// ================== Operation Logs ==================

export const listLogs = (params) => {
    return request({
        url: '/api/v1/admin/logs',
        method: 'get',
        params
    })
}
