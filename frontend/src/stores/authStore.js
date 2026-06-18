import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as apiLogin, register as apiRegister, adminLogin as apiAdminLogin } from '../api'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || null)
  const nickname = ref(localStorage.getItem('nickname') || '')
  const role = ref(localStorage.getItem('role') || 'USER')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function persist(tok, nick, r) {
    token.value = tok
    nickname.value = nick
    role.value = r || 'USER'
    localStorage.setItem('token', tok)
    localStorage.setItem('nickname', nick)
    localStorage.setItem('role', r || 'USER')
  }

  async function login(account, password) {
    const res = await apiLogin(account, password)
    const data = res.data
    persist(data.token, data.nickname, data.role)
    return data
  }

  async function register(phone, password, nick) {
    const res = await apiRegister(phone, password, nick)
    const data = res.data
    persist(data.token, data.nickname, data.role)
    return data
  }

  async function adminLogin(username, password) {
    const res = await apiAdminLogin(username, password)
    const data = res.data
    persist(data.token, data.username, 'ADMIN')
    return data
  }

  function logout() {
    token.value = null
    nickname.value = ''
    role.value = 'USER'
    localStorage.removeItem('token')
    localStorage.removeItem('nickname')
    localStorage.removeItem('role')
  }

  return { token, nickname, role, isLoggedIn, isAdmin, login, register, adminLogin, logout }
})
