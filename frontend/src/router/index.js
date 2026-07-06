import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/car/:id',
    name: 'CarDetail',
    component: () => import('../views/CarDetail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/orders',
    name: 'OrderCenter',
    component: () => import('../views/OrderCenter.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'UserProfile',
    component: () => import('../views/UserProfile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/ai-assistant',
    name: 'AIAssistant',
    component: () => import('../views/AIAssistant.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/drivers',
    name: 'DriverManage',
    component: () => import('../views/DriverManage.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/maintenance',
    name: 'MaintenanceDashboard',
    component: () => import('../views/MaintenanceDashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/coupon',
    name: 'CouponPage',
    component: () => import('../views/CouponPage.vue'),
    meta: { requiresAuth: true }
  },
  // 管理员路由
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: () => import('../views/AdminDashboard.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('../views/AdminOrders.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/users',
    name: 'AdminUsers',
    component: () => import('../views/AdminUsers.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/messages',
    name: 'AdminMessages',
    component: () => import('../views/AdminMessages.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/coupons',
    name: 'AdminCoupons',
    component: () => import('../views/AdminCoupons.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/admin/maintenance',
    name: 'AdminMaintenance',
    component: () => import('../views/MaintenanceDashboard.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
