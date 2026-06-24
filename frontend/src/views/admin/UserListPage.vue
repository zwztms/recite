<template>
  <div class="max-w-5xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">用户管理</h1>

    <!-- 搜索栏 -->
    <div class="mb-4">
      <input v-model="keyword" @keyup.enter="loadUsers" placeholder="搜索手机号..."
             class="border rounded px-3 py-1.5 text-sm w-48" />
      <button @click="loadUsers" class="ml-2 bg-blue-600 text-white px-3 py-1.5 rounded text-sm hover:bg-blue-700">
        搜索
      </button>
    </div>

    <div class="bg-white rounded-lg shadow">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr>
            <th class="p-3 text-left">ID</th>
            <th class="p-3 text-left">手机号</th>
            <th class="p-3 text-left">角色</th>
            <th class="p-3 text-center">背诵次数</th>
            <th class="p-3 text-left">注册时间</th>
            <th class="p-3">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id" class="border-b hover:bg-gray-50">
            <td class="p-3">{{ u.id }}</td>
            <td class="p-3">{{ u.phone }}</td>
            <td class="p-3">
              <select v-model="u.role" @change="changeRole(u)"
                      class="border rounded px-2 py-1 text-xs">
                <option value="user">用户</option>
                <option value="admin">管理员</option>
              </select>
            </td>
            <td class="p-3 text-center">{{ u.reciteCount }}</td>
            <td class="p-3 text-gray-500 text-xs">{{ u.createdAt?.slice(0,10) }}</td>
            <td class="p-3">
              <button @click="deleteUser(u)"
                      class="text-red-500 hover:text-red-700 text-xs">删除</button>
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
