<template>
  <div class="max-w-5xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">知识库管理</h1>

    <!-- 上传区域 -->
    <div class="border-2 border-dashed border-gray-300 rounded-lg p-8 text-center mb-6"
         @dragover.prevent @drop.prevent="handleDrop">
      <p class="text-gray-500 mb-2">拖拽文件到此处上传（PDF/Word/HTML/Markdown）</p>
      <p class="text-gray-400 text-sm">或</p>
      <label class="inline-block mt-2 px-4 py-2 bg-blue-600 text-white rounded cursor-pointer hover:bg-blue-700">
        选择文件
        <input type="file" class="hidden" accept=".pdf,.docx,.html,.md,.txt" @change="handleFile" />
      </label>
    </div>

    <!-- 上传进度 -->
    <div v-if="uploading" class="mb-4 p-4 bg-blue-50 rounded">
      <div class="flex items-center gap-3">
        <div class="animate-spin w-5 h-5 border-2 border-blue-600 border-t-transparent rounded-full"></div>
        <span class="text-blue-700">{{ progress }}</span>
      </div>
      <div class="mt-2 bg-gray-200 rounded-full h-2">
        <div class="bg-blue-600 h-2 rounded-full transition-all" :style="{width: progressPercent+'%'}"></div>
      </div>
    </div>

    <!-- 文档列表 -->
    <div class="bg-white rounded-lg shadow">
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr><th class="text-left p-3">文档名称</th><th class="text-left p-3">来源</th><th class="text-center p-3">Chunks</th><th class="p-3"></th></tr>
        </thead>
        <tbody>
          <tr v-for="doc in documents" :key="doc.docTitle" class="border-b hover:bg-gray-50">
            <td class="p-3 font-medium">{{ doc.docTitle }}</td>
            <td class="p-3 text-gray-500 text-xs truncate max-w-xs">{{ doc.docSource }}</td>
            <td class="p-3 text-center">{{ doc.chunkCount }}</td>
            <td class="p-3 text-right">
              <button @click="deleteDoc(doc.docTitle)" class="text-red-500 hover:text-red-700 text-xs">删除</button>
            </td>
          </tr>
          <tr v-if="documents.length===0">
            <td colspan="4" class="p-6 text-center text-gray-400">暂无文档，上传一篇技术文章开始吧</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { listKnowledgeDocs, deleteKnowledgeDoc } from '../api/index.js'

const documents = ref([])
const uploading = ref(false)
const progress = ref('')
const progressPercent = ref(0)

onMounted(() => loadDocs())

async function loadDocs() {
  try { const res = await listKnowledgeDocs(); documents.value = res.data || [] } catch(e) {}
}

async function uploadFile(file) {
  uploading.value = true; progress.value = '上传中...'; progressPercent.value = 10
  try {
    const form = new FormData(); form.append('file', file)
    // uploadKnowledge is a placeholder — the backend upload endpoint needs KnowledgeAdminController
    // For now we show the UI ready; upload triggers when controller is wired
    progress.value = '等待后端上传端点...'; progressPercent.value = 50
    setTimeout(() => { uploading.value = false }, 1000)
  } catch(e) { alert('上传失败: ' + e.message); uploading.value = false }
}

function handleDrop(e) { const f = e.dataTransfer?.files?.[0]; if (f) uploadFile(f) }
function handleFile(e) { const f = e.target?.files?.[0]; if (f) uploadFile(f) }
async function deleteDoc(title) { if (confirm('删除 "'+title+'" 的所有chunks?')) { await deleteKnowledgeDoc(title); loadDocs() } }
</script>
