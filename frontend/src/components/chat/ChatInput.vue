<template>
  <div class="border-t border-border bg-white px-4 py-3">
    <div class="max-w-3xl mx-auto flex items-end gap-3">
      <!-- 输入框 -->
      <div class="flex-1 bg-warm border border-border rounded-xl
                  focus-within:border-coral focus-within:ring-1 focus-within:ring-coral/20
                  transition-colors overflow-hidden">
        <textarea
          ref="textareaRef"
          v-model="text"
          :disabled="disabled"
          :placeholder="disabled ? '评分中...' : '输入你的回答...'"
          rows="1"
          class="w-full resize-none bg-transparent px-4 py-3 text-sm text-text-primary
                 placeholder:text-text-muted outline-none
                 max-h-32"
          @input="autoResize"
          @keydown="onKeydown"
          @compositionstart="onCompStart"
          @compositionend="onCompEnd"
        ></textarea>
      </div>

      <!-- 发送按钮 -->
      <button
        @click="send"
        :disabled="disabled || !text.trim()"
        class="shrink-0 w-10 h-10 rounded-xl bg-coral text-white
               hover:bg-orange-600 disabled:opacity-40 disabled:cursor-not-allowed
               transition-colors flex items-center justify-center"
      >
        <svg class="w-5 h-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13" />
          <path d="M22 2L15 22L11 13L2 9L22 2Z" />
        </svg>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'

const props = defineProps({
  disabled: { type: Boolean, default: false }
})

const emit = defineEmits(['send'])

const text = ref('')
const textareaRef = ref(null)
const isComposing = ref(false)

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 128) + 'px'
}

function onCompStart() { isComposing.value = true }
function onCompEnd() { isComposing.value = false }

function onKeydown(e) {
  // Enter 发送（不含 Shift），IME 组合中不发送
  if (e.key === 'Enter' && !e.shiftKey && !isComposing.value) {
    e.preventDefault()
    send()
  }
}

function send() {
  const val = text.value.trim()
  if (!val || props.disabled) return
  emit('send', val)
  text.value = ''
  nextTick(() => {
    const el = textareaRef.value
    if (el) { el.style.height = 'auto' }
  })
}

function focus() {
  textareaRef.value?.focus()
}

defineExpose({ focus })
</script>
