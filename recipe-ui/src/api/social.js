import request from '@/utils/request'

// Interaction
export function likeRecipe(id) {
    return request({
        url: `/api/v1/interactions/favorite/${id}`,
        method: 'post'
    })
}

export function commentRecipe(data) {
    return request({
        url: '/api/v1/interactions/comments',
        method: 'post',
        data
    })
}

export function getComments(recipeId, params) {
    return request({
        url: `/api/v1/interactions/comments/${recipeId}`,
        method: 'get',
        params
    })
}

// 点赞/取消点赞评论
export function likeComment(commentId) {
    return request({
        url: `/api/v1/interactions/comments/${commentId}/like`,
        method: 'post'
    })
}

// 获取评论的回复
export function getReplies(parentId, params) {
    return request({
        url: `/api/v1/interactions/comments/${parentId}/replies`,
        method: 'get',
        params
    })
}

// 删除评论
export function deleteComment(commentId) {
    return request({
        url: `/api/v1/interactions/comments/${commentId}`,
        method: 'delete'
    })
}

// 获取我的收藏
export function getMyFavorites(params) {
    return request({
        url: '/api/v1/interactions/favorites',
        method: 'get',
        params
    })
}



// Social
export function followUser(userId) {
    return request({
        url: `/api/v1/social/follow/${userId}`,
        method: 'post'
    })
}

export function unfollowUser(userId) {
    return request({
        url: `/api/v1/social/follow/${userId}`,
        method: 'post'  // 后端是 toggle，用 POST
    })
}

export function getMessages(userId, params) {
    return request({
        url: `/api/v1/social/messages/${userId}`,
        method: 'get',
        params
    })
}

export function sendMessage(data) {
    return request({
        url: '/api/v1/social/messages',
        method: 'post',
        data
    })
}

// 获取会话列表
export function getConversations() {
    return request({
        url: '/api/v1/social/conversations',
        method: 'get'
    })
}

// 获取我的关注列表
export function getMyFollows(params) {
    return request({
        url: '/api/v1/social/follows',
        method: 'get',
        params
    })
}



export function getUserProfile(userId) {
    return request({
        url: `/api/v1/social/user/${userId}`,
        method: 'get'
    })
}

export function markRead(senderId) {
    return request({
        url: `/api/v1/social/messages/read/${senderId}`,
        method: 'put'
    })
}
