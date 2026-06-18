<template>
  <div class="max-w-3xl mx-auto">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-xl font-semibold text-text-primary">模块管理</h1>
      <button @click="showCreate = true"
        class="px-4 py-2 text-sm rounded-lg bg-coral text-white hover:bg-orange-600 transition-colors">
        + 新建模块
      </button>
    </div>

    <!-- 模块表格 -->
    <div class="bg-white rounded-xl border border-border overflow-hidden">
      <table class="w-full text-sm">
        <thead>
          <tr class="border-b border-border bg-warm">
            <th class="text-left px-4 py-3 text-text-secondary font-medium">模块名</th>
            <th class="text-left px-4 py-3 text-text-secondary font-medium">标识</th>
            <th class="text-center px-4 py-3 text-text-secondary font-medium">状态</th>
            <th class="text-center px-4 py-3 text-text-secondary font-medium">题目数</th>
            <th class="text-right px-4 py-3 text-text-secondary font-medium">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-if="loading" class="border-b border-border">
            <td colspan="5" class="text-center py-8 text-text-muted">加载中...</td>
          </tr>
          <tr v-else-if="!modules.length" class="border-b border-border">
            <td colspan="5" class="text-center py-8 text-text-muted">暂无模块</td>
          </tr>
          <tr v-for="m in modules" :key="m.moduleKey" class="border-b border-border last:border-0 hover:bg-warm/50 transition-colors">
            <td class="px-4 py-3 font-medium text-text-primary">{{ m.moduleName }}</td>
            <td class="px-4 py-3 text-text-muted font-mono text-xs">{{ m.moduleKey }}</td>
            <td class="px-4 py-3 text-center">
              <button @click="toggleStatus(m)"
                class="text-xs px-2.5 py-1 rounded-full font-medium transition-colors"
                :class="m.status === 'ONLINE'
                  ? 'bg-success-bg text-success-text border border-green-200'
                  : 'bg-gray-100 text-text-muted border border-border'">
                {{ m.status === 'ONLINE' ? '线上' : '下线' }}
              </button>
            </td>
            <td class="px-4 py-3 text-center text-text-muted">{{ m.questionCount }}</td>
            <td class="px-4 py-3 text-right">
              <button @click="onDelete(m)"
                class="text-xs px-2 py-1 text-red-500 hover:bg-red-50 rounded transition-colors">
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 新建模块弹窗 -->
    <Teleport to="body">
      <div v-if="showCreate" class="fixed inset-0 z-50 flex items-center justify-center px-4"
        @click.self="showCreate = false">
        <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>
        <div class="relative bg-white rounded-2xl shadow-xl p-6 max-w-sm w-full">
          <h2 class="text-lg font-semibold mb-4">新建模块</h2>
          <form @submit.prevent="handleCreate" class="space-y-4">
            <div>
              <label class="block text-sm text-text-secondary mb-1">模块标识 (key)</label>
              <input v-model="form.moduleKey" type="text" placeholder="如 jvm"
                class="input" required />
            </div>
            <div>
              <label class="block text-sm text-text-secondary mb-1">模块名称</label>
              <input v-model="form.moduleName" type="text" placeholder="如 JVM 虚拟机"
                class="input" required />
            </div>
            <div>
              <label class="block text-sm text-text-secondary mb-1">描述</label>
              <input v-model="form.description" type="text" placeholder="简要描述"
                class="input" />
            </div>
            <p v-if="error" class="text-sm text-red-500">{{ error }}</p>
            <div class="flex gap-2">
              <button type="button" @click="showCreate = false"
                class="flex-1 py-2 text-sm rounded-lg border border-border text-text-secondary hover:bg-gray-50 transition-colors">
                取消
              </button>
              <button type="submit" :disabled="creating"
                class="flex-1 py-2 text-sm rounded-lg bg-coral text-white hover:bg-orange-600 disabled:opacity-50 transition-colors">
                {{ creating ? '创建中...' : '创建' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { listModules, createModule, updateModuleStatus, deleteModule } from '../api'

const modules = ref([])
const loading = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const error = ref('')
const form = reactive({ moduleKey: '', moduleName: '', description: '' })

async function fetchModules() {
  loading.value = true
  try {
    const res = await listModules()
    modules.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function toggleStatus(m) {
  const newStatus = m.status === 'ONLINE' ? 'OFFLINE' : 'ONLINE'
  try {
    await updateModuleStatus(m.moduleKey, newStatus)
    m.status = newStatus
  } catch (e) {
    alert('操作失败: ' + (e.message || ''))
  }
}

async function onDelete(m) {
  if (!confirm(`确定删除模块"${m.moduleName}"吗？`)) return
  try {
    await deleteModule(m.moduleKey)
    modules.value = modules.value.filter(x => x.moduleKey !== m.moduleKey)
  } catch (e) {
    alert('删除失败: ' + (e.message || ''))
  }
}

async function handleCreate() {
  error.value = ''
  creating.value = true
  try {
    await createModule({
      moduleKey: form.moduleKey.trim(),
      moduleName: form.moduleName.trim(),
      description: form.description.trim()
    })
    showCreate.value = false
    form.moduleKey = ''
    form.moduleName = ''
    form.description = ''
    await fetchModules()
  } catch (e) {
    error.value = e.message || '创建失败'
  } finally {
    creating.value = false
  }
}

onMounted(fetchModules)
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
