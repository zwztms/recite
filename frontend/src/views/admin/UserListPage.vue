<template>
  <div class="max-w-5xl mx-auto p-6">
    <h1 class="text-2xl font-bold text-text-primary mb-6">用户管理</h1>

    <!-- 搜索栏 -->
    <div class="mb-4 flex gap-2">
      <input v-model="keyword" @keyup.enter="loadUsers" placeholder="搜索手机号..."
             class="input w-48" />
      <button @click="loadUsers" class="px-4 py-1.5 text-sm rounded-lg bg-coral text-white hover:bg-orange-600 transition-colors">
        搜索
      </button>
    </div>

    <div class="bg-surface rounded-2xl border border-border overflow-hidden">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-border bg-warm">
            <th class="text-left px-4 py-3 text-text-secondary font-medium">ID</th>
            <th class="text-left px-4 py-3 text-text-secondary font-medium">手机号</th>
            <th class="text-left px-4 py-3 text-text-secondary font-medium">角色</th>
            <th class="text-center px-4 py-3 text-text-secondary font-medium">背诵次数</th>
            <th class="text-left px-4 py-3 text-text-secondary font-medium">注册时间</th>
            <th class="text-right px-4 py-3 text-text-secondary font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id" class="border-b border-border last:border-0 hover:bg-warm/50 transition-colors">
            <td class="px-4 py-3 text-text-primary">{{ u.id }}</td>
            <td class="px-4 py-3 text-text-primary">{{ u.phone }}</td>
            <td class="px-4 py-3">
              <select v-model="u.role" @change="changeRole(u)"
                      class="border border-border rounded-lg px-2.5 py-1 text-xs text-text-primary bg-warm focus:outline-none focus:border-coral">
                <option value="user">用户</option>
                <option value="admin">管理员</option>
              </select>
            </td>
            <td class="px-4 py-3 text-center text-text-muted">{{ u.reciteCount }}</td>
            <td class="px-4 py-3 text-text-muted text-xs">{{ u.createdAt?.slice(0,10) }}</td>
            <td class="px-4 py-3 text-right">
              <button @click="deleteUser(u)"
                      class="text-xs px-2 py-1 text-red-500 hover:bg-red-50 rounded transition-colors">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../../api/index.js'

const users = ref([])
const keyword = ref('')

onMounted(() => loadUsers())

async function loadUsers() {
  try {
    let url = '/admin/users'
    if (keyword.value) url += '?keyword=' + encodeURIComponent(keyword.value)
    const r = await api.get(url)
    users.value = r.data?.records || []
  } catch(e) {}
}

async function changeRole(u) {
  try { await api.put('/admin/users/'+u.id, {role:u.role}) } catch(e) { alert('修改失败') }
}

async function deleteUser(u) {
  if (!confirm('删除用户 '+u.phone+' ?')) return
  try { await api.delete('/admin/users/'+u.id); loadUsers() } catch(e) { alert('删除失败') }
}
</script>
