import request from '@/utils/request'

/**
 * 获取团队成员列表（公开接口）
 */
export function getTeamMembers() {
    return request.get('/api/v1/team/members')
}

// ============ 管理端接口 ============

/**
 * 管理员获取团队成员列表
 */
export function adminListTeamMembers() {
    return request.get('/api/v1/admin/team/members')
}

/**
 * 管理员添加团队成员
 */
export function adminAddTeamMember(data) {
    return request.post('/api/v1/admin/team/members', data)
}

/**
 * 管理员更新团队成员
 */
export function adminUpdateTeamMember(id, data) {
    return request.put(`/api/v1/admin/team/members/${id}`, data)
}

/**
 * 管理员删除团队成员
 */
export function adminDeleteTeamMember(id) {
    return request.delete(`/api/v1/admin/team/members/${id}`)
}
