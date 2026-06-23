import axios from 'axios'

// ================================================================
// axios 实例
// ================================================================

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截：自动带 token
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = token
  return config
})

// 响应拦截：解包 Response.data，统一错误处理
api.interceptors.response.use(
  res => {
    const body = res.data
    // 如果后端返回了 code 且不为 '0'/'200' 则视为业务错误
    if (body.code && body.code !== '0' && body.code !== '200') {
      if (body.code === '401' || body.code === '403') {
        localStorage.removeItem('token')
        localStorage.removeItem('role')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return body  // 直接返回 Response 对象 { code, data, message }
  },
  err => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

// ================================================================
// Auth
// ================================================================

export function login(account, password) {
  return api.post('/auth/login', { account, password })
}

export function register(phone, password, nickname) {
  return api.post('/auth/register', { phone, password, nickname })
}

export function adminLogin(username, password) {
  return api.post('/admin/auth/login', { username, password })
}

// ================================================================
// Recite
// ================================================================

export function startRecite(mode, moduleKeys, count) {
  return api.post('/recite/start', { mode, moduleKeys, count })
}

/** SSE 流式评分 — 用 fetch + ReadableStream，不走 axios */
export async function submitAnswerStream(sid, questionId, answer) {
  const token = localStorage.getItem('token')
  const res = await fetch(`/api/recite/${sid}/answer`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'Authorization': token || '' },
    body: JSON.stringify({ questionId, answer })
  })
  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem('token')
      window.location.href = '/login'
    }
    throw new Error(`HTTP ${res.status}`)
  }
  return res.body.getReader()
}

export function submitFollowUp(sid, recordId, followUpAnswer) {
  return api.post(`/recite/${sid}/followup`, { recordId, followUpAnswer })
}

export function finishRecite(sid) {
  return api.post(`/recite/${sid}/finish`)
}

export function getSession(sid) {
  return api.get(`/recite/${sid}`)
}

/** 获取会话当前题目（下一题用） */
export function getCurrentQuestion(sid) {
  return api.get(`/recite/${sid}/current-question`)
}

export function getHistory(limit = 20) {
  return api.get('/recite/history', { params: { limit } })
}

// ================================================================
// Knowledge (Admin)
// ================================================================

export function listModules() {
  return api.get('/admin/knowledge/modules')
}

export function createModule(data) {
  return api.post('/admin/knowledge/modules', data)
}

export function updateModuleStatus(moduleKey, status) {
  return api.put(`/admin/knowledge/modules/${moduleKey}/status`, { status })
}

export function deleteModule(moduleKey) {
  return api.delete(`/admin/knowledge/modules/${moduleKey}`)
}

export function listQuestions(moduleKey) {
  return api.get(`/admin/knowledge/modules/${moduleKey}/questions`)
}

export function updateQuestion(questionId, data) {
  return api.put(`/admin/knowledge/questions/${questionId}`, data)
}

export function triggerImport() {
  return api.post('/admin/knowledge/import')
}

// ================================================================
// Report
// ================================================================

export function getDashboard() {
  return api.get('/report/dashboard')
}

export function getReport(sessionId) {
  return api.get(`/report/${sessionId}`)
}

export function getJournal(page = 1, size = 10) {
  return api.get('/report/journal', { params: { page, size } })
}

export function getJournalDetail(id) {
  return api.get(`/report/journal/${id}`)
}

// ================================================================
// Achievement
// ================================================================

export function getAllBadges() {
  return api.get('/achievement/')
}

export function getBadgeDetail(badgeKey) {
  return api.get(`/achievement/${badgeKey}`)
}

export function getNewBadges() {
  return api.get('/achievement/new')
}

export function ackNewBadges() {
  return api.post('/achievement/new/ack')
}

// ================================================================
// Monitor (Admin)
// ================================================================

export function listTraces(page = 1, size = 20) {
  return api.get('/admin/monitor/traces', { params: { page, size } })
}

export function getTraceDetail(traceId) {
  return api.get(`/admin/monitor/traces/${traceId}`)
}

export function getMonitorStats() {
  return api.get('/admin/monitor/stats')
}

export function cleanTraces(before = 30) {
  return api.delete('/admin/monitor/traces', { params: { before } })
}

// ================================================================
// 个人主页
// ================================================================

export function getHomeDashboard() {
  return api.get('/home/dashboard')
}

// ================================================================
// 翻卡学习
// ================================================================

export function getLearnQuestions(moduleKey, order, filter) {
  return api.get('/learn/questions', { params: { moduleKey, order, filter } })
}

export function markMastery(questionId, mastered) {
  return api.post('/learn/mark', { questionId, mastered })
}

export default api
