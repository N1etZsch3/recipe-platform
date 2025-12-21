import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 前端本地缓存 Store
 * 用于缓存分类列表等不常变化的数据
 */
export const useCacheStore = defineStore('cache', () => {
    // 分类列表缓存
    const categories = ref(JSON.parse(sessionStorage.getItem('categories')) || null)
    const categoriesExpire = ref(parseInt(sessionStorage.getItem('categoriesExpire')) || 0)

    // 热门菜谱缓存
    const hotRecipes = ref(JSON.parse(sessionStorage.getItem('hotRecipes')) || null)
    const hotRecipesExpire = ref(parseInt(sessionStorage.getItem('hotRecipesExpire')) || 0)

    // 缓存有效期 (毫秒)
    const CATEGORIES_TTL = 30 * 60 * 1000  // 30分钟
    const HOT_RECIPES_TTL = 5 * 60 * 1000  // 5分钟

    /**
     * 获取分类列表 (带缓存)
     */
    const getCategories = async (fetchFn) => {
        const now = Date.now()
        if (categories.value && categoriesExpire.value > now) {
            return categories.value
        }
        // 调用 API 获取
        const data = await fetchFn()
        setCategories(data)
        return data
    }

    const setCategories = (data) => {
        categories.value = data
        categoriesExpire.value = Date.now() + CATEGORIES_TTL
        sessionStorage.setItem('categories', JSON.stringify(data))
        sessionStorage.setItem('categoriesExpire', categoriesExpire.value.toString())
    }

    const clearCategories = () => {
        categories.value = null
        categoriesExpire.value = 0
        sessionStorage.removeItem('categories')
        sessionStorage.removeItem('categoriesExpire')
    }

    /**
     * 获取热门菜谱 (带缓存)
     */
    const getHotRecipes = async (fetchFn) => {
        const now = Date.now()
        if (hotRecipes.value && hotRecipesExpire.value > now) {
            return hotRecipes.value
        }
        const data = await fetchFn()
        setHotRecipes(data)
        return data
    }

    const setHotRecipes = (data) => {
        hotRecipes.value = data
        hotRecipesExpire.value = Date.now() + HOT_RECIPES_TTL
        sessionStorage.setItem('hotRecipes', JSON.stringify(data))
        sessionStorage.setItem('hotRecipesExpire', hotRecipesExpire.value.toString())
    }

    /**
     * 清除所有缓存
     */
    const clearAll = () => {
        clearCategories()
        hotRecipes.value = null
        hotRecipesExpire.value = 0
        sessionStorage.removeItem('hotRecipes')
        sessionStorage.removeItem('hotRecipesExpire')
    }

    return {
        categories,
        hotRecipes,
        getCategories,
        setCategories,
        clearCategories,
        getHotRecipes,
        setHotRecipes,
        clearAll
    }
})
