# 评分出错兜底机制 — 编码报告

> 日期：2026-06-21 | 提交：`6b8a204` | 文件：3 | 新增行：53

---

## 一、需求

评分过程中发生错误（LLM 超时、网络中断、后端异常）时，用户不能卡死，必须有"重试"或"跳过"的兜底选项。

---

## 二、设计

### 2.1 状态机

```
用户提交答案
  │
  ▼
SCORING ──── SSE 事件正常 ────► DONE ──── 下一题
  │                                │
  ├── SSE error / 网络异常         ├── 追问
  │                                │
  ▼                                ▼
ERROR ◄────────────────────────────┘
  │
  ├── [重试] → pop Q&A → 回到输入状态（同一题重答）
  └── [跳过] → nextQuestion()（放弃此题，继续下一题）
```

### 2.2 改动范围

| 文件 | 改动 |
|------|------|
| `reciteStore.js` | scoreCard 新增 `error` 字段；SSE error 事件修复死循环；catch 设 error 而非 done；新增 `retryAnswer()` |
| `ScoreCard.vue` | 新增 ERROR 态 UI（红底警告 + 重试/跳过按钮）；评分中态加 `!data.error` 守卫 |
| `ChatRecite.vue` | 新增 `onRetry` 处理；ScoreCard 事件绑定 |

---

## 三、实现

### 3.1 Store — scoreCard 数据扩展

```js
// 新增 error 字段
const card = msg('scoreCard', {
  score: null, corrects: [], missed: [], suggestion: '',
  followUpQuestion: '', recordId: null,
  done: false,
  error: false   // ← 新增：区分"完成"和"出错"
})
```

### 3.2 Store — SSE error 事件修复

```js
// 修改前（Bug4 同款死循环）
case 'error':
  card.data.suggestion = '评分出错，请稍后重试'
  card.data.done = true   // 设 done → 显示"下一题"（无重试）
  break                   // 回到 while → reader 阻塞 → streaming 卡死

// 修改后
case 'error':
  card.data.suggestion = event.data.message || '评分出错，请稍后重试'
  card.data.error = true       // 设 error → 显示"重试/跳过"
  streaming.value = false
  return                       // 立即退出，不阻塞
```

### 3.3 Store — catch 块（网络异常）

```js
// 修改前
} catch (e) {
  card.data.suggestion = '连接中断，请重试'
  card.data.done = true   // 设 done → 只能跳过，不能重试
}

// 修改后
} catch (e) {
  card.data.suggestion = '连接中断，请重试'
  card.data.error = true  // 设 error → 可重试
}
```

### 3.4 Store — retryAnswer() 重试逻辑

```js
function retryAnswer() {
  // 移除最后一对 Q&A 消息（用户消息 + 评分卡片）
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
```

重试时清除本轮 Q&A 痕迹，用户回到"看到题目、等待输入"的状态。`streaming` 重置后 ChatInput 自动启用。

### 3.5 ScoreCard — ERROR 态 UI

```html
<!-- 新增：评分出错 → 重试/跳过 -->
<div v-if="data.error" class="p-3 bg-red-50 border border-red-200 rounded-lg mb-3">
  <p class="text-sm text-red-700 mb-3">⚠️ {{ data.suggestion || '评分出错' }}</p>
  <div class="flex gap-2">
    <button @click="$emit('retry')" class="...">重试</button>
    <button @click="$emit('skipQuestion')" class="...">跳过此题</button>
  </div>
</div>
```

错误态显示红底提示 + 两个等宽按钮。左"重试"（coral 实色），右"跳过此题"（灰色边框）。

### 3.6 ChatRecite — 事件桥接

```js
// 新增方法
function onRetry() {
  store.retryAnswer()
  nextTick(() => inputRef.value?.focus())
}

// ScoreCard 绑定
<ScoreCard
  @retry="onRetry"
  @skipQuestion="onNextQuestion"   // 跳过 = 直接下一题
  @nextQuestion="onNextQuestion"
/>
```

---

## 四、验证

| 场景 | 预期 | 验证方式 |
|------|------|------|
| LLM 超时 → SSE error 事件 | 显示红底警告 + 重试/跳过按钮 | 手动测试 |
| 网络中断 → fetch 失败 | 显示"连接中断" + 重试/跳过 | 手动测试 |
| 点"重试" | 错误卡片 + 用户消息消失，输入框聚焦 | 手动测试 |
| 点"跳过" | 调用 nextQuestion，进入下一题 | 手动测试 |

---

## 五、扩展预留

当前 MVP 覆盖了 ScoreCard 层的错误展示。后续可在以下方向扩展：

| 扩展 | 说明 |
|------|------|
| 全局错误提示 | store 级别 `lastError` + Toast 通知 |
| 连续失败降级 | 连续 3 次重试失败 → 自动建议跳过 |
| LLM 不可用降级 | 自动切 REVIEW 模式（自评替代 AI 评分） |
| 错误上报 | `navigator.sendBeacon` 上报错误到运维后台 |

---

## 六、提交

```
6b8a204 feat: 评分出错兜底机制 — 重试/跳过二选一
3 files changed, 53 insertions(+), 7 deletions(-)
```

Co-Authored-By: Claude <noreply@anthropic.com>
