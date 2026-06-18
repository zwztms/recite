import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/recite',
    component: () => import('../views/ChatRecite.vue'),
    meta: { auth: true }
  },
  {
    path: '/achievements',
    component: () => import('../views/AchievementWall.vue'),
    meta: { auth: true }
  },
  {
    path: '/admin/modules',
    component: () => import('../views/AdminModules.vue'),
    meta: { auth: true, role: 'ADMIN' }
  },
  {
    path: '/admin/monitor',
    component: () => import('../views/AdminMonitor.vue'),
    meta: { auth: true, role: 'ADMIN' }
  },
  { path: '/', redirect: '/recite' },
  { path: '/:pathMatch(.*)*', redirect: '/recite' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  // 无需鉴权的页面直接放行
  if (!to.meta.auth) return next()

  // 检查 token
  const token = localStorage.getItem('token')
  if (!token) return next('/login')

  // 检查角色限制
  if (to.meta.role && to.meta.role !== localStorage.getItem('role')) {
    return next('/recite')
  }

  next()
})

export default router
