import { createRouter, createWebHistory } from 'vue-router'
import LoginView from '../views/LoginView.vue'
import LandingPage from '../views/LandingPage.vue'
import HomeView from '../views/HomeView.vue'
import DetailView from '../views/DetailView.vue'
import ProfileView from '../views/ProfileView.vue'
import CreateRecipeView from '../views/CreateRecipeView.vue'
import { useUserStore } from '../stores/user'

// Admin views - lazy loaded
const AdminLoginView = () => import('../views/admin/AdminLoginView.vue')
const AdminLayout = () => import('../views/admin/AdminLayout.vue')
const DashboardView = () => import('../views/admin/DashboardView.vue')
const UserManageView = () => import('../views/admin/UserManageView.vue')
const RecipeManageView = () => import('../views/admin/RecipeManageView.vue')
const CategoryManageView = () => import('../views/admin/CategoryManageView.vue')
const CommentManageView = () => import('../views/admin/CommentManageView.vue')
const LogManageView = () => import('../views/admin/LogManageView.vue')

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
        // 隐藏的管理后台路径
        {
            path: '/backstage-m9x2k7/login',
            name: 'admin-login',
            component: AdminLoginView,
            meta: { guest: true }
        },
        {
            path: '/backstage-m9x2k7',
            component: AdminLayout,
            meta: { requiresAuth: true, requiresAdmin: true },
            children: [
                {
                    path: '',
                    name: 'admin-dashboard',
                    component: DashboardView
                },
                {
                    path: 'users',
                    name: 'admin-users',
                    component: UserManageView
                },
                {
                    path: 'recipes',
                    name: 'admin-recipes',
                    component: RecipeManageView
                },
                {
                    path: 'categories',
                    name: 'admin-categories',
                    component: CategoryManageView
                },
                {
                    path: 'comments',
                    name: 'admin-comments',
                    component: CommentManageView
                },
                {
                    path: 'logs',
                    name: 'admin-logs',
                    component: LogManageView
                }
            ]
        },
        // 404 页面 - 必须放在最后
        {
            path: '/:pathMatch(.*)*',
            name: 'not-found',
            component: () => import('../views/NotFoundView.vue'),
            meta: { title: '页面未找到' }
        }
    ]
})

router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    const token = userStore.token

    // 1. Check Auth (跳过管理员登录页)
    if (to.meta.requiresAuth && !token) {
        // 如果是访问管理后台，跳转到管理员登录页
        if (to.path.startsWith('/backstage-m9x2k7')) {
            return next({ name: 'admin-login' })
        }
        return next({ name: 'login', query: { redirect: to.fullPath } })
    }

    // 2. Check Admin
    if (to.meta.requiresAdmin) {
        if (!userStore.user || userStore.user.role !== 'admin') {
            console.warn('Access denied: Admin only')
            return next({ name: 'admin-login' })
        }
    }

    next()
})

export default router
