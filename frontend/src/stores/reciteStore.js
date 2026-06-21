import { defineStore } from 'pinia'
import { ref, reactive } from 'vue'
import {
  startRecite as apiStartRecite,
  submitAnswerStream,
  submitFollowUp,
  finishRecite as apiFinishRecite,
  getSession,
  getCurrentQuestion,
  getHistory
} from '../api'

/**
 * 背诵会话 Store — 管理整个背诵生命周期.
 *
 * 消息类型:
 *   system    分隔线/状态提示  { text }
 *   ai        AI 纯文本消息   { text }
 *   user      用户回答 pill   { text }
 *   scoreCard SSE 评分卡片    { score, corrects[], missed[], suggestion, followUpQuestion, recordId, done }
 *   followUp  追问交互        { recordId, question, answered }
 *   review    复习自评        { questionId, answer }
 *   report    报告           { totalScore, averageScore, totalQuestions, strengths, weaknesses, advice }
 */

let nextId = 1
function msg(type, data = {}) {
  return reactive({ id: nextId++, type, data, ts: Date.now() })
}

export const useReciteStore = defineStore('recite', () => {
  const stage = ref('setup')        // 'setup' | 'chatting' | 'finished'
  const mode = ref(null)            // 'CATEGORY' | 'RANDOM' | 'REVIEW'
  const sessionId = ref(null)
  const messages = ref([])
  const streaming = ref(false)
  const currentIndex = ref(0)
  const totalQuestions = ref(0)
  const currentQuestionId = ref(null) // 当前题目的 ID，提交答案用
  const currentRecordId = ref(null)   // 当前题的 recordId，追问用

  // ================================================================
  // 开始背诵
  // ================================================================

  async function startRecite(reciteMode, moduleKeys, count) {
    resetState()
    mode.value = reciteMode
    stage.value = 'chatting'

    const res = await apiStartRecite(reciteMode, moduleKeys, count)
    const data = res.data
    sessionId.value = data.sessionId
    currentIndex.value = data.questionIndex
    totalQuestions.value = data.totalQuestions

    // 系统消息：会话开始
    messages.value.push(msg('system', {
      text: `背诵开始 · ${reciteMode === 'CATEGORY' ? '模块背诵' : reciteMode === 'RANDOM' ? '随机背诵' : '今日复习'} · ${data.totalQuestions} 题`
    }))

    // AI 消息：第一题
    if (data.question) {
      currentQuestionId.value = data.question.id
      messages.value.push(msg('ai', {
        text: data.question.question,
        moduleKey: data.question.moduleKey,
        difficulty: data.question.difficulty,
        questionIndex: 1,
        totalQuestions: data.totalQuestions,
        answer: reciteMode === 'REVIEW' ? data.question.content : null
      }))
    }
  }

  // ================================================================
  // 提交答案 → SSE 流式评分
  // ================================================================

  async function sendAnswer(text) {
        if (!sessionId.value || streaming.value) return

    // 用户消息
    messages.value.push(msg('user', { text }))

    // 评分卡片占位
    const card = msg('scoreCard', {
      score: null,
      corrects: [],
      missed: [],
      suggestion: '',
      followUpQuestion: '',
      recordId: null,
      done: false,
      error: false
    })
    messages.value.push(card)
    streaming.value = true

    try {
      const reader = await submitAnswerStream(sessionId.value, currentQuestionId.value, text)
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })

        // 解析 SSE 事件（\n\n 或 \r\n\r\n 分隔）
        const parts = buffer.split(/\r?\n\r?\n/)
        buffer = parts.pop()

        for (const part of parts) {
          const event = parseSseEvent(part)
          if (!event) continue

          switch (event.name) {
            case 'score':
              card.data.score = event.data.score
              break
            case 'correct':
              card.data.corrects = event.data.points || []
              break
            case 'missed':
              card.data.missed = event.data.points || []
              break
            case 'suggestion':
              card.data.suggestion = event.data.text || ''
              break
            case 'followUp':
              card.data.followUpQuestion = event.data.question || ''
              break
            case 'done':
              card.data.recordId = event.data.recordId || null
              card.data.done = true
              currentRecordId.value = card.data.recordId
              streaming.value = false
              return  // 跳出 sendAnswer，不再阻塞 reader
            case 'error':
              card.data.suggestion = event.data.message || '评分出错，请稍后重试'
              card.data.error = true
              streaming.value = false
              return  // 退出循环，不再阻塞 reader
          }
        }
      }
    } catch (e) {
      card.data.suggestion = '连接中断，请重试'
      card.data.error = true
    } finally {
      streaming.value = false
    }
  }

  // ================================================================
  // 重试
  // ================================================================

  /** 评分出错后重试：移除最后一对 Q&A 消息，重置输入 */
  function retryAnswer() {
    // pop 掉评分卡片 (scoreCard) + 用户消息 (user)
    while (messages.value.length > 0) {
      const last = messages.value[messages.value.length - 1]
      if (last.type === 'scoreCard' || last.type === 'user') {
        messages.value.pop()
      } else {
        break
      }
    }
    streaming.value = false
  }

  // ================================================================
  // 追问
  // ================================================================

  async function sendFollowUp(recordId, answer) {
    if (!sessionId.value) return
    messages.value.push(msg('user', { text: answer }))
    messages.value.push(msg('followUp', { recordId, question: '', answered: true }))

    try {
      const res = await submitFollowUp(sessionId.value, recordId, answer)
      // AI 反馈
      messages.value.push(msg('ai', { text: res.data || '收到，继续加油！' }))
    } catch (e) {
      messages.value.push(msg('ai', { text: '追问提交失败' }))
    }
  }

  // ================================================================
  // 下一题
  // ================================================================

  async function nextQuestion() {
    if (!sessionId.value) return
    try {
      // 1. 查会话状态
      const sessionRes = await getSession(sessionId.value)
      const session = sessionRes.data
      currentIndex.value = session.currentIndex
      totalQuestions.value = session.totalQuestions

      if (session.status === 'FINISHED') {
        return finishRecite()
      }

      // 2. 查当前题目内容
      const qRes = await getCurrentQuestion(sessionId.value)
      const qData = qRes.data

      if (qData) {
        currentQuestionId.value = qData.id

        // 系统消息
        messages.value.push(msg('system', {
          text: `第 ${session.currentIndex} / ${session.totalQuestions} 题`
        }))

        // AI 消息：下一题
        messages.value.push(msg('ai', {
          text: qData.question,
          moduleKey: qData.moduleKey,
          difficulty: qData.difficulty,
          questionIndex: session.currentIndex,
          totalQuestions: session.totalQuestions,
          answer: mode.value === 'REVIEW' ? qData.content : null
        }))
      }
    } catch (e) {
      messages.value.push(msg('system', { text: '获取下一题失败，请结束背诵' }))
    }
  }

  // ================================================================
  // 结束背诵
  // ================================================================

  async function finishRecite() {
    if (!sessionId.value) return
    try {
      const res = await apiFinishRecite(sessionId.value)
      const data = res.data
      stage.value = 'finished'

      messages.value.push(msg('report', {
        totalScore: data.totalScore,
        averageScore: data.averageScore,
        totalQuestions: data.totalQuestions,
        strengths: data.strengths || [],
        weaknesses: data.weaknesses || [],
        advice: data.advice || ''
      }))
    } catch (e) {
      messages.value.push(msg('system', { text: '结束背诵失败' }))
    }
  }

  // ================================================================
  // 复习自评 (REVIEW 模式)
  // ================================================================

  function sendReview(questionId, rating) {
    // rating: 'remembered' | 'uncertain' | 'forgot'
    messages.value.push(msg('review', { questionId, rating }))
    // 自动进入下一题
    nextQuestion()
  }

  // ================================================================
  // 重置
  // ================================================================

  function resetState() {
    stage.value = 'setup'
    mode.value = null
    sessionId.value = null
    messages.value = []
    streaming.value = false
    currentIndex.value = 0
    totalQuestions.value = 0
    currentQuestionId.value = null
    currentRecordId.value = null
    nextId = 1
  }

  // ================================================================
  // history
  // ================================================================

  async function fetchHistory(limit = 20) {
    const res = await getHistory(limit)
    return res.data || []
  }

  return {
    stage, mode, sessionId, messages, streaming,
    currentIndex, totalQuestions, currentQuestionId, currentRecordId,
    startRecite, sendAnswer, retryAnswer, nextQuestion, sendFollowUp, finishRecite,
    sendReview, resetState, fetchHistory
  }
})

// ---- SSE 解析 ----

function parseSseEvent(chunk) {
  const lines = chunk.split('\n')
  let name = ''
  let dataStr = ''

  for (const line of lines) {
    if (line.startsWith('event:')) {
      name = line.slice(6).trim()   // 'event:score' → 'score'
    } else if (line.startsWith('data:')) {
      dataStr = line.slice(5).trim() // 'data:{...}' → '{...}'
    }
  }

  if (!name && !dataStr) return null

  let data = {}
  try {
    data = dataStr ? JSON.parse(dataStr) : {}
  } catch (e) {
    data = {}
  }

  return { name, data }
}
