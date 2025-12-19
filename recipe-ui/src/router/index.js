import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import LandingPage from '../views/LandingPage.vue'
import HomeView from '../views/HomeView.vue'
import DetailView from '../views/DetailView.vue'
import ProfileView from '../views/ProfileView.vue'
import AdminView from '../views/AdminView.vue'
import CreateRecipeView from '../views/CreateRecipeView.vue'
import { useUserStore } from '../stores/user'

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/login',
            name: 'login',
            component: LoginView,
            meta: { guest: true }
        },
        {
            path: '/',
            name: 'landing',
            component: LandingPage
        },
        {
            path: '/home',
            name: 'home',
            component: HomeView
        },
        {
            path: '/recipe/:id',
            name: 'detail',
            component: DetailView
        },
        {
            path: '/profile',
            name: 'profile',
            component: ProfileView,
            meta: { requiresAuth: true }
        },
        {
            path: '/admin',
            name: 'admin',
            component: AdminView,
            meta: { requiresAuth: true, requiresAdmin: true }
        },
        {
            path: '/user/:id',
            name: 'user-profile',
            component: () => import('../views/UserProfileView.vue'),
            meta: { title: '用户主页' }
        },
        {
            path: '/create',
            name: 'create',
            component: CreateRecipeView,
            meta: { requiresAuth: true }
        },
        {
            path: '/messages',
            name: 'messages',
            component: () => import('../views/MessagesView.vue'),
            meta: { requiresAuth: true }
        },
    ]
})

router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    const token = userStore.token

    // 1. Check Auth
    if (to.meta.requiresAuth && !token) {
        return next({ name: 'login', query: { redirect: to.fullPath } })
    }

    // 2. Check Admin
    if (to.meta.requiresAdmin) {
        // userStore.user might be null if token is invalid but present? (Edge case)
        // Assuming if token is present, user is set.
        if (!userStore.user || userStore.user.role !== 'admin') {
            // Not admin. Redirect to home or show toast (can't show toast easily here without importing component instace)
            // Simple redirect to home
            console.warn('Access denied: Admin only')
            return next({ name: 'home' })
        }
    }

    next()
})

export default router
