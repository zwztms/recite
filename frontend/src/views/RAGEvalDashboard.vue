<template>
  <div class="max-w-5xl mx-auto p-6">
    <h1 class="text-2xl font-bold mb-6">RAG 评估仪表盘</h1>

    <!-- 四指标卡片 -->
    <div class="grid grid-cols-4 gap-4 mb-8">
      <div v-for="m in metrics" :key="m.label" class="bg-white rounded-lg shadow p-4 text-center">
        <div class="text-3xl font-bold" :class="m.color">{{ m.value }}</div>
        <div class="text-xs text-gray-500 mt-1">{{ m.label }}</div>
        <div class="text-xs text-gray-400">{{ m.desc }}</div>
      </div>
    </div>

    <!-- 最近评估列表 -->
    <div class="bg-white rounded-lg shadow">
      <h2 class="text-lg font-semibold p-4 border-b">最近评估记录</h2>
      <table class="w-full text-sm">
        <thead class="bg-gray-50 border-b">
          <tr><th class="p-2 text-left">Session</th><th class="p-2 text-center">忠实度</th><th class="p-2 text-center">相关度</th><th class="p-2 text-center">召回率</th><th class="p-2 text-center">答案质量</th><th class="p-2 text-left">时间</th></tr>
        </thead>
        <tbody>
          <tr v-for="r in records" :key="r.sessionId" class="border-b hover:bg-gray-50">
            <td class="p-2 font-mono text-xs">{{ r.sessionId?.slice(0,8) }}</td>
            <td class="p-2 text-center" :class="scoreColor(r.faithfulness)">{{ (r.faithfulness*100).toFixed(0) }}%</td>
            <td class="p-2 text-center" :class="scoreColor(r.contextRelevance)">{{ (r.contextRelevance*100).toFixed(0) }}%</td>
            <td class="p-2 text-center" :class="scoreColor(r.contextRecall)">{{ (r.contextRecall*100).toFixed(0) }}%</td>
            <td class="p-2 text-center" :class="scoreColor(r.answerRelevance)">{{ (r.answerRelevance*100).toFixed(0) }}%</td>
            <td class="p-2 text-xs text-gray-400">{{ r.createdAt?.slice(0,16) }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getRAGStats } from '../api/index.js'

const records = ref([])
const metrics = computed(() => {
  if (!records.value.length) return [
    {label:'忠实度',value:'-',desc:'Faithfulness',color:'text-gray-400'},
    {label:'相关度',value:'-',desc:'ContextRelevance',color:'text-gray-400'},
    {label:'召回率',value:'-',desc:'ContextRecall',color:'text-gray-400'},
    {label:'答案质量',value:'-',desc:'AnswerRelevance',color:'text-gray-400'}]
  const avg = (key) => (records.value.reduce((s,r)=>s+(r[key]||0),0)/records.value.length*100).toFixed(0)+'%'
  return [
    {label:'忠实度',value:avg('faithfulness'),desc:'Faithfulness',color:'text-blue-600'},
    {label:'相关度',value:avg('contextRelevance'),desc:'ContextRelevance',color:'text-green-600'},
    {label:'召回率',value:avg('contextRecall'),desc:'ContextRecall',color:'text-orange-600'},
    {label:'答案质量',value:avg('answerRelevance'),desc:'AnswerRelevance',color:'text-purple-600'}]})
function scoreColor(v) { return v >= 0.7 ? 'text-green-600' : v >= 0.4 ? 'text-yellow-600' : 'text-red-500' }
onMounted(async () => { try { const res = await getRAGStats(20); records.value = res.data || [] } catch(e) {} })
</script>
