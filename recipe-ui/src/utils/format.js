/**
 * Format a date string or object into "YYYY-MM-DD HH:mm"
 * @param {string|Date} date 
 * @returns {string}
 */
export const formatDate = (date) => {
    if (!date) return ''
    const d = new Date(date)
    if (isNaN(d.getTime())) return date // Return original if invalid

    const year = d.getFullYear()
    const month = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    const hour = String(d.getHours()).padStart(2, '0')
    const minute = String(d.getMinutes()).padStart(2, '0')

    return `${year}-${month}-${day} ${hour}:${minute}`
}
