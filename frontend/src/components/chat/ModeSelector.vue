<template>
  <div class="min-h-[60vh] flex items-center justify-center">
    <div class="w-full max-w-md bg-white rounded-2xl shadow-sm border border-border p-6">
      <h2 class="text-lg font-semibold text-text-primary mb-6 text-center">开始背诵</h2>

      <!-- 模式选择 tabs -->
      <div class="flex mb-6 bg-warm rounded-lg p-1">
        <button v-for="tab in modeTabs" :key="tab.key"
          @click="selectedMode = tab.key"
          class="flex-1 py-2 text-sm rounded-md transition-colors"
          :class="selectedMode === tab.key
            ? 'bg-white text-coral font-medium shadow-sm'
            : 'text-text-secondary hover:text-text-primary'"
        >{{ tab.label }}</button>
      </div>

      <!-- 复习模式：无需配置 -->
      <div v-if="selectedMode === 'REVIEW'" class="text-center text-sm text-text-secondary mb-6">
        自动取到期 10 题，无需额外配置
      </div>

      <!-- 模块/随机模式：选模块 + 题数 -->
      <template v-else>
        <!-- 模块选择 -->
        <div class="mb-5">
          <label class="block text-sm text-text-secondary mb-2">
            {{ selectedMode === 'RANDOM' ? '选择模块范围（可多选）' : '选择模块' }}
          </label>
          <div class="max-h-48 overflow-y-auto space-y-1">
            <label v-for="m in modules" :key="m.moduleKey"
              class="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-warm cursor-pointer transition-colors">
              <input
                :type="selectedMode === 'RANDOM' ? 'checkbox' : 'radio'"
                :name="'module-' + (selectedMode === 'RANDOM' ? 'multi' : 'single')"
                :value="m.moduleKey"
                v-model="selectedModuleKeys"
                :checked="selectedModuleKeys.includes(m.moduleKey)"
                @change="onModuleChange(m.moduleKey, $event)"
                class="accent-coral"
              />
              <span class="text-sm text-text-primary">{{ m.moduleName }}</span>
              <span class="text-xs text-text-muted ml-auto">{{ m.questionCount }} 题</span>
            </label>
          </div>
        </div>

        <!-- 题数选择 -->
        <div class="mb-6">
          <label class="block text-sm text-text-secondary mb-2">题目数量</label>
          <div class="flex gap-2">
            <button v-for="n in [5, 10, 15, 20]" :key="n"
              @click="questionCount = n"
              class="flex-1 py-2 text-sm rounded-lg border transition-colors"
              :class="questionCount === n
                ? 'border-coral bg-coral-light text-coral font-medium'
                : 'border-border text-text-secondary hover:border-coral-border'"
            >{{ n }}</button>
          </div>
        </div>
      </template>

      <!-- 错误提示 -->
      <p v-if="error" class="text-sm text-red-500 mb-4 text-center">{{ error }}</p>

      <!-- 开始按钮 -->
      <button @click="start"
        :disabled="!canStart || loading"
        class="w-full py-2.5 bg-coral text-white rounded-lg font-medium
               hover:bg-orange-600 disabled:opacity-50 transition-colors">
        {{ loading ? '加载中...' : '开始背诵' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { listModules } from '../../api'

const emit = defineEmits(['start'])

const selectedMode = ref('CATEGORY')
const selectedModuleKeys = ref([])
const questionCount = ref(10)
const modules = ref([])
const loading = ref(false)
const error = ref('')

const modeTabs = [
  { key: 'CATEGORY', label: '模块背诵' },
  { key: 'RANDOM', label: '随机背诵' },
  { key: 'REVIEW', label: '今日复习' }
]

const canStart = computed(() => {
  if (selectedMode.value === 'REVIEW') return true
  return selectedModuleKeys.value.length > 0
})

onMounted(async () => {
  try {
    const res = await listModules()
    modules.value = (res.data || []).filter(m => m.status === 'ONLINE')
  } catch (e) {
    error.value = '获取模块列表失败'
  }
})

function onModuleChange(key, event) {
  if (selectedMode.value === 'RANDOM') {
    // checkbox: vue v-model handles array
  } else {
    // radio: set single value
    selectedModuleKeys.value = [key]
  }
}

async function start() {
  error.value = ''
  if (!canStart.value) {
    error.value = '请选择模块'
    return
  }
  loading.value = true
  try {
    emit('start', {
      mode: selectedMode.value,
      moduleKeys: selectedMode.value === 'REVIEW' ? [] : [...selectedModuleKeys.value],
      count: selectedMode.value === 'REVIEW' ? 10 : questionCount.value
    })
  } catch (e) {
    error.value = e.message || '启动失败'
  } finally {
    loading.value = false
  }
}
</script>
