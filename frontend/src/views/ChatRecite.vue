<template>
  <div class="h-[calc(100vh-3.5rem)] flex flex-col -mx-4 -my-8">
    <!-- 阶段 1: 设置面板 -->
    <ModeSelector v-if="store.stage === 'setup'" @start="onStart" />

    <!-- 阶段 2/3: 聊天界面 -->
    <template v-else>
      <!-- 顶部栏 -->
      <ReciteTopBar
        :mode="store.mode"
        :current="store.currentIndex"
        :total="store.totalQuestions"
        @finish="onFinish"
      />

      <!-- 消息列表 -->
      <MessageList
        :messages="store.messages"
        :streaming="store.streaming"
      >
        <!-- SSE 评分卡片插槽 -->
        <template #scoreCard="{ message }">
          <ScoreCard
            :data="message.data"
            @acceptFollowUp="onAcceptFollowUp(message)"
            @skipFollowUp="onSkipFollowUp(message)"
            @nextQuestion="onNextQuestion"
          />
        </template>

        <!-- 追问插槽 -->
        <template #followUp="{ message }">
          <FollowUpPrompt
            :message="message"
            @accept="onAcceptFollowUpFromPrompt"
            @skip="onSkipFollowUpFromPrompt"
          />
        </template>

        <!-- 复习自评插槽 -->
        <template #review="{ message }">
          <ReviewButtons @rate="(r) => onReviewRate(message, r)" />
        </template>

        <!-- 报告插槽 -->
        <template #report="{ message }">
          <ReportCard :data="message.data" />
        </template>
      </MessageList>

      <!-- 输入区 -->
      <ChatInput
        ref="inputRef"
        :disabled="store.streaming || store.stage === 'finished'"
        @send="onSend"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useReciteStore } from '../stores/reciteStore'
import ModeSelector from '../components/chat/ModeSelector.vue'
import ReciteTopBar from '../components/chat/ReciteTopBar.vue'
import MessageList from '../components/chat/MessageList.vue'
import ChatInput from '../components/chat/ChatInput.vue'
import ScoreCard from '../components/chat/ScoreCard.vue'
import FollowUpPrompt from '../components/chat/FollowUpPrompt.vue'
import ReviewButtons from '../components/chat/ReviewButtons.vue'
import ReportCard from '../components/chat/ReportCard.vue'

const store = useReciteStore()
const inputRef = ref(null)

// ---- 开始背诵 ----

async function onStart({ mode, moduleKeys, count }) {
  await store.startRecite(mode, moduleKeys, count)
  nextTick(() => inputRef.value?.focus())
}

// ---- 发送消息 ----

async function onSend(text) {
  if (store.mode === 'REVIEW') {
    store.messages.push({ id: Date.now(), type: 'user', data: { text }, ts: Date.now() })
    return
  }
  await store.sendAnswer(text)
}

// ---- 追问 ----

function onAcceptFollowUp(message) {
  store.messages.push(msg('followUp', {
    recordId: message.data.recordId,
    question: message.data.followUpQuestion,
    answered: false
  }))
  // 用户输入后会通过 sendAnswer 的 followup 逻辑处理
  inputRef.value?.focus()
}

function onSkipFollowUp(message) {
  message.data.done = true
  message.data.followUpQuestion = message.data.followUpQuestion || '' // 保留问题文本
}

function onAcceptFollowUpFromPrompt(recordId) {
  // ChatRecite 需要跟踪当前追问 recordId
  store.currentRecordId = recordId
  inputRef.value?.focus()
}

function onSkipFollowUpFromPrompt(recordId) {
  // 已在 FollowUpPrompt 中标记 answered
}

// ---- 下一题 ----

async function onNextQuestion() {
  await store.nextQuestion()
}

// ---- 复习自评 ----

function onReviewRate(message, rating) {
  store.sendReview(message.data.questionId, rating)
}

// ---- 结束背诵 ----

async function onFinish() {
  await store.finishRecite()
}

// helper
function msg(type, data = {}) {
  return { id: Date.now(), type, data, ts: Date.now() }
}
</script>
