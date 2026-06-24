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
  // 管理后台 — 嵌套路由（AdminLayout 为壳）
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { auth: true, role: 'ADMIN' },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', component: () => import('../views/admin/DashboardPage.vue') },
      { path: 'users', component: () => import('../views/admin/UserListPage.vue') },
      { path: 'traces', component: () => import('../views/admin/TraceListPage.vue') },
      { path: 'traces/:traceId', component: () => import('../views/admin/TraceDetailPage.vue') },
      { path: 'settings', component: () => import('../views/admin/SystemSettings.vue') },
      { path: 'modules', component: () => import('../views/AdminModules.vue') },
      { path: 'monitor', component: () => import('../views/AdminMonitor.vue') },
      { path: 'knowledge', component: () => import('../views/KnowledgeAdmin.vue') },
      { path: 'rag-eval', component: () => import('../views/RAGEvalDashboard.vue') }
    ]
  },
  {
    path: '/home',
    component: () => import('../views/HomePage.vue'),
    meta: { auth: true }
  },
  {
    path: '/learn',
    component: () => import('../views/CardLearn.vue'),
    meta: { auth: true }
  },
  { path: '/', redirect: '/home' },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
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
