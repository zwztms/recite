# Phase 10.5 开发者测试报告

> 日期：2026-06-20 | 提交：`e463f36` | 发现缺陷：4 | 修复：4

---

## 一、测试范围

手动端到端测试 recite V2 完整背诵流程：登录 → 模块选择 → 开始背诵 → 提交答案 → SSE 评分 → 下一题 → 结束背诵。

---

## 二、缺陷清单

### Bug 1：模块选择器选中态缺失

**现象**：点击模块后只有原生 radio 变色，整行无高亮，用户无法判断是否选中。

**根因**：`ModeSelector.vue` 第 31 行模块 label 只有 `hover:bg-warm`，无选中态样式。同组件的模式 Tab 和题数按钮均有选中态，唯独模块列表遗漏。

**修复**：label 添加 `:class` 条件样式——`selectedModuleKeys.includes(m.moduleKey)` 时加 `bg-coral-light border border-coral-border text-coral font-medium`。

**提交**：`4308f32`

**文件**：`ModeSelector.vue`（2 行）

---

### Bug 2：submitAnswer 传入 null questionId

**现象**：用户输入答案后，后端返回 500，评分卡片不出现。

**根因**：`reciteStore.sendAnswer(text)` 签名没有 `questionId` 参数，调用 `submitAnswerStream(sid, null, text)` 把 `null` 传给后端。后端 `questionPort.getById(null)` 抛异常。

`startRecite` 和 `nextQuestion` 获取题目后，`question.id` 只放在 AI 消息的 data 里，没有存到 store 的独立状态字段。

**修复**：
1. store 新增 `currentQuestionId` ref
2. `startRecite` 和 `nextQuestion` 收到题目后设置 `currentQuestionId.value`
3. `sendAnswer` 传 `currentQuestionId.value` 给 `submitAnswerStream`

**提交**：`a1dc0ee`

**文件**：`reciteStore.js`（5 处改动）

---

### Bug 3：SSE 事件解析失败——event 格式不匹配

**现象**：fetch 返回 200，chunk 正常接收，但评分卡片数据不更新。Console 无报错。

**根因**：后端 `ReciteController.sendSse()` 发送的 SSE 行格式为 `event:score`（冒号后无空格）。前端 `parseSseEvent()` 匹配 `'event: '`（冒号后有空格），`startsWith('event: ')` 永远为 false。

```
后端发送:  event:score\n         ← 无空格
前端期望:  event: score\n        ← 有空格
parseSseEvent: startsWith('event: ') → false → 事件被跳过
```

6 个 SSE 事件（score/correct/missed/suggestion/followUp/done）全部被静默丢弃，`card.data` 保持初始值 `{score:null, corrects:[], done:false}`，ScoreCard 永远停在"评分中..."。

**修复**：
1. `parseSseEvent`：`startsWith('event:')` → `slice(6).trim()`，兼容有/无空格
2. `buffer.split`：`/\r?\n\r?\n/` 兼容 `\r\n` 行分隔符

**提交**：`e875aaf`

**文件**：`reciteStore.js`（4 行）

---

### Bug 4：SSE done 后 streaming 卡死

**现象**：第一题评分完成，点"下一题"后新题目出现，但输入框一直显示"评分中..."（loading 动画不消失），无法继续答题。

**根因**：Bug 3 修复后，SSE 事件正常解析，`done` 事件设 `card.data.done = true`。但之后的 `while(true)` 循环继续 `await reader.read()`——Spring `SseEmitter.complete()` 关闭连接后，浏览器 `fetch ReadableStream` 的 `reader.read()` **不返回** `{done: true}`，Promise 永久挂起。

```
async function sendAnswer(text) {
    streaming.value = true
    try {
        const reader = await submitAnswerStream(...)
        while (true) {
            const { done, value } = await reader.read()  // ← 永不 resolve
            if (done) break                               // ← 永远到不了
            // ... SSE 解析
        }
    } finally {
        streaming.value = false  // ← 永不执行 → streaming 永远 true → 输入框永远 disabled
    }
}
```

**修复**：收到 `done` 事件后直接 `streaming.value = false` + `return` 退出函数，不再等待 reader 返回 `{done: true}`。`finally` 块仍会执行（JavaScript finally 在 return 后也运行），但 streaming 已在 done 处理中设为 false。

**提交**：`e463f36`

**文件**：`reciteStore.js`（3 行）

---

## 三、缺陷关系图

```
Bug 1 (选中态缺失)
  └─ UX 问题，独立修复

Bug 2 (null questionId)
  └─ 导致 SSE 请求 500 → Bug 3+4 被掩盖

Bug 3 (SSE 格式不匹配)
  └─ Bug 2 修复后才暴露：fetch 成功但事件被丢弃

Bug 4 (streaming 卡死)
  └─ Bug 3 修复后才暴露：事件正确但 reader 阻塞
```

四个缺陷逐层掩盖——修好一个才暴露下一个。

---

## 四、修复文件汇总

| 文件 | Bug | 改动行数 |
|------|:--:|:--:|
| `ModeSelector.vue` | 1 | 2 |
| `reciteStore.js` | 2, 3, 4 | 12 |
| `api/index.js` | — (debug 清理) | ±5 |

---

## 五、提交记录

```
4308f32 fix: ModeSelector 模块列表选中态缺失
a1dc0ee fix: submitAnswer 传 null questionId
e875aaf fix: SSE 事件解析失败 — event:name 格式不匹配
e463f36 fix: SSE done 后 reader.read() 永不返回 → streaming 卡死
```

---

## 六、经验教训

1. **前后端 SSE 格式约定**：SSE 规范推荐 `field: value`（带空格），但 Spring SseEmitter 默认用 `field:value`。应在接口文档中明确约定，或前端同时兼容两种格式。

2. **ReadableStream + SseEmitter 兼容性**：`SseEmitter.complete()` 不一定触发 `reader.read()` 的 `done` 信号。可靠的模式是收到自定义 `done` 事件后主动退出，不依赖底层传输层关闭通知。

3. **状态字段显式存储**：`questionId` 不应该只藏在消息 data 里——它是跨步骤的关键状态，必须作为独立 store 字段。消息对象用于渲染，状态字段用于流程控制。

4. **缺陷掩盖效应**：后端 500 返回的是普通 JSON 而非 SSE 流，使前端 SSE 解析器和 reader 阻塞问题无法暴露。修好一个才看到下一个。

---

Co-Authored-By: Claude <noreply@anthropic.com>
