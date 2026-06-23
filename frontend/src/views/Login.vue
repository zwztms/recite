<template>
  <div class="min-h-screen flex items-center justify-center px-4">
    <div class="w-full max-w-sm">
      <!-- 标题 -->
      <h1 class="text-2xl font-bold text-center text-text-primary mb-8">八股文背诵助手</h1>

      <!-- 模式切换 tabs -->
      <div class="flex mb-6 bg-white rounded-lg p-1 shadow-sm border border-border">
        <button
          v-for="tab in tabs"
          :key="tab.key"
          @click="mode = tab.key"
          class="flex-1 py-2 text-sm rounded-md transition-colors"
          :class="mode === tab.key
            ? 'bg-coral text-white font-medium shadow-sm'
            : 'text-text-secondary hover:text-text-primary'"
        >{{ tab.label }}</button>
      </div>

      <!-- 表单卡片 -->
      <div class="bg-white rounded-xl shadow-sm border border-border p-6">

        <!-- ====== 用户登录 ====== -->
        <form v-if="mode === 'login'" @submit.prevent="handleLogin" class="space-y-4">
          <div>
            <label class="block text-sm text-text-secondary mb-1">手机号 / 用户名</label>
            <input v-model="form.account" type="text" placeholder="输入手机号或用户名"
              class="input" autocomplete="username" />
          </div>
          <div>
            <label class="block text-sm text-text-secondary mb-1">密码</label>
            <input v-model="form.password" type="password" placeholder="输入密码"
              class="input" autocomplete="current-password" />
          </div>
          <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
          <button type="submit" :disabled="loading"
            class="w-full py-2.5 bg-coral text-white rounded-lg font-medium
                   hover:bg-orange-600 disabled:opacity-50 transition-colors">
            {{ loading ? '登录中...' : '登录' }}
          </button>
        </form>

        <!-- ====== 用户注册 ====== -->
        <form v-if="mode === 'register'" @submit.prevent="handleRegister" class="space-y-4">
          <div>
            <label class="block text-sm text-text-secondary mb-1">手机号</label>
            <input v-model="form.phone" type="tel" placeholder="输入手机号"
              class="input" autocomplete="tel" />
          </div>
          <div>
            <label class="block text-sm text-text-secondary mb-1">昵称</label>
            <input v-model="form.nickname" type="text" placeholder="给自己起个名字"
              class="input" />
          </div>
          <div>
            <label class="block text-sm text-text-secondary mb-1">密码</label>
            <input v-model="form.password" type="password" placeholder="至少 6 位"
              class="input" autocomplete="new-password" />
          </div>
          <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
          <button type="submit" :disabled="loading"
            class="w-full py-2.5 bg-coral text-white rounded-lg font-medium
                   hover:bg-orange-600 disabled:opacity-50 transition-colors">
            {{ loading ? '注册中...' : '注册' }}
          </button>
        </form>

        <!-- ====== 管理员登录 ====== -->
        <form v-if="mode === 'admin'" @submit.prevent="handleAdminLogin" class="space-y-4">
          <div>
            <label class="block text-sm text-text-secondary mb-1">管理员用户名</label>
            <input v-model="form.username" type="text" placeholder="输入管理员用户名"
              class="input" autocomplete="username" />
          </div>
          <div>
            <label class="block text-sm text-text-secondary mb-1">密码</label>
            <input v-model="form.password" type="password" placeholder="输入密码"
              class="input" autocomplete="current-password" />
          </div>
          <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
          <button type="submit" :disabled="loading"
            class="w-full py-2.5 bg-stone-700 text-white rounded-lg font-medium
                   hover:bg-stone-800 disabled:opacity-50 transition-colors">
            {{ loading ? '登录中...' : '管理员登录' }}
          </button>
        </form>

      </div>

      <!-- 账号提示 -->
      <p class="text-center text-xs text-text-muted mt-6">
        测试账号：15386747351 / zw123456
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/authStore'

const router = useRouter()
const auth = useAuthStore()

const mode = ref('login')
const loading = ref(false)
const error = ref('')
const form = reactive({
  account: '', phone: '', nickname: '', username: '', password: ''
})

const tabs = [
  { key: 'login', label: '用户登录' },
  { key: 'register', label: '注册' },
  { key: 'admin', label: '管理员' }
]

function clearError() { error.value = '' }

function validate(...fields) {
  for (const f of fields) {
    if (!form[f]?.trim()) {
      error.value = '请填写所有必填项'
      return false
    }
  }
  return true
}

async function handleLogin() {
  clearError()
  if (!validate('account', 'password')) return
  loading.value = true
  try {
    await auth.login(form.account, form.password)
    router.push('/home')
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  clearError()
  if (!validate('phone', 'password')) return
  const phone = form.phone.trim()
  if (!/^1\d{10}$/.test(phone)) {
    error.value = '手机号格式不正确'
    return
  }
  if (form.password.trim().length < 6) {
    error.value = '密码至少 6 位'
    return
  }
  loading.value = true
  try {
    await auth.register(phone, form.password, form.nickname.trim())
    router.push('/home')
  } catch (e) {
    error.value = e.message || '注册失败'
  } finally {
    loading.value = false
  }
}

async function handleAdminLogin() {
  clearError()
  if (!validate('username', 'password')) return
  loading.value = true
  try {
    await auth.adminLogin(form.username, form.password)
    router.push('/home')
  } catch (e) {
    error.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.input {
  width: 100%;
  padding: 0.625rem 0.75rem;
  background: #f5f0eb;
  border: 1px solid #e7e5e4;
  border-radius: 0.5rem;
  color: #1c1917;
  font-size: 0.875rem;
  transition: border-color 0.15s, box-shadow 0.15s;
}
.input::placeholder { color: #a8a29e; }
.input:focus {
  outline: none;
  border-color: #f97316;
  box-shadow: 0 0 0 1px rgba(249, 115, 22, 0.2);
}
</style>
