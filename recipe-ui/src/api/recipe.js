import request from '@/utils/request'

// 获取分类列表（公开接口）
export function getCategories() {
    return request({
        url: '/api/v1/recipes/categories',
        method: 'get'
    })
}

export function listRecipies(params) {
    return request({
        url: '/api/v1/recipes/list',
        method: 'get',
        params
    })
}

export function getRecipeDetail(id) {
    return request({
        url: `/api/v1/recipes/${id}`,
        method: 'get'
    })
}

export function createRecipe(data) {
    return request({
        url: '/api/v1/recipes',
        method: 'post',
        data
    })
}

export function updateRecipe(data) {
    return request({
        url: '/api/v1/recipes',
        method: 'put',
        data
    })
}

export function deleteRecipe(id) {
    return request({
        url: `/api/v1/recipes/${id}`,
        method: 'delete'
    })
}

// 管理员审核
export function auditRecipe(data) {
    return request({
        url: '/api/v1/admin/recipes/audit',
        method: 'post',
        data
    })
}

// 下架菜谱（用户）
export function unpublishRecipe(id) {
    return request({
        url: `/api/v1/recipes/${id}/unpublish`,
        method: 'post'
    })
}

