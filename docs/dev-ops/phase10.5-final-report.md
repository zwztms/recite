# Phase 10.5 开发者测试 — 最终编码报告

> 日期：2026-06-20 ~ 06-22 | 提交：13 个 | 缺陷修复：13 | 新增功能：2

---

## 一、总览

| 维度 | 数量 |
|------|:--:|
| 发现缺陷 | 13 |
| 修复提交 | 13 |
| 新增功能 | 2（兜底重试、会话恢复） |
| 后端文件变更 | 6 |
| 前端文件变更 | 7 |
| 新增代码行 | ~200 |

---

## 二、缺陷清单

### 前端渲染层（3 个）

| # | 缺陷 | 提交 | 根因 |
|:--:|------|------|------|
| 1 | 模块列表选中无视觉反馈 | `4308f32` | label 缺 `:class` 选中态 |
| 2 | RANDOM 多选失效 | `4658c01` | `v-model` 与 `:checked` 冲突 |
| 3 | RANDOM 多选仍未实现 | `ad1598a` | 浏览器不支持动态改 input type，Vue 复用 DOM |

### 前端数据流层（5 个）

| # | 缺陷 | 提交 | 根因 |
|:--:|------|------|------|
| 4 | 提交答案传 null questionId | `a1dc0ee` | store 缺 `currentQuestionId` 状态字段 |
| 5 | SSE 事件全部被跳过 | `e875aaf` | `parseSseEvent` 匹配 `'event: '` 但后端发 `'event:name'` |
| 6 | 评分后 streaming 永久卡死 | `e463f36` | SSE `done` 后 `reader.read()` 永不返回 `{done:true}` |
| 7 | 追问自动跳过 | `16dce76` | LLM prompt "可空字符串" + ScoreCard 追问区随 done 消失 |
| 8 | 结束背诵无出口 | `674ece6` | `stage='finished'` 后无"返回主页"按钮 |

### 后端（2 个）

| # | 缺陷 | 提交 | 根因 |
|:--:|------|------|------|
| 9 | finish 耗时 6 秒 | `c050302` | RocketMQ syncSend 超时阻塞 HTTP 响应 |
| 10 | RANDOM 不随机 | `8d312be` | 空字符串 embedding → pgvector 余弦排序 |

### 新功能（2 个）

| # | 功能 | 提交 | 说明 |
|:--:|------|------|------|
| 11 | 评分出错兜底 | `6b8a204` | 错误态显示重试/跳过按钮 |
| 12 | 刷新恢复会话 | `7d6e8bb` + `4c62aca` | sessionStorage 持久化 sessionId + 消息列表 |

---

## 三、缺陷关系图

```
Bug 1  (模块选中态)
Bug 2→3 (RANDOM 多选, 两层掩盖)
Bug 4  (null questionId)
  ↓ 修好后暴露 →
Bug 5  (SSE 格式不匹配)
  ↓ 修好后暴露 →
Bug 6  (streaming 卡死)
  ↓ 修好后暴露 →
Bug 7  (追问跳过)
  ↓
Bug 8  (结束无出口)
Bug 9  (finish 耗时)
Bug 10 (RANDOM 不随机)
Bug 11 (错误兜底, 独立功能)
Bug 12 (会话恢复, 独立功能)
```

Bug 4→5→6 是最典型的"掩盖链"——修好一个才暴露下一个。

---

## 四、SSE 四连缺陷详解

这是本次测试中发现的核心链路问题——SSE 评分流程连续 4 个缺陷层层掩盖：

```
用户点击发送
  │
  ├── Bug 4: submitAnswerStream(sid, null, text)
  │         questionId=null → 后端 500 → axios 拦截器 reject
  │         → catch 块 "连接中断" → ScoreCard 不出现
  │         ✗ 掩盖了 Bug 5+6
  │
  ├── Bug 5: SSE 事件格式 'event:score' ≠ 'event: score'
  │         parseSseEvent startsWith('event: ') → false
  │         6 个事件全部跳过, card.data 不更新
  │         → ScoreCard 停在 "评分中..."
  │         ✗ 掩盖了 Bug 6
  │
  ├── Bug 6: while(true) { reader.read() }
  │         emitter.complete() 后 reader 永不返回 done
  │         → 循环不退 → finally 不执行 → streaming 永远 true
  │         → ChatInput 永久显示 "评分中..."
  │
  └── 修复后: SSE 正常流转 → 评分 → 追问 → 下一题 → 结束
```

---

## 五、文件变更统计

| 文件 | 缺陷 # | 改动类型 |
|------|:--:|------|
| `ModeSelector.vue` | 1, 2, 3 | CSS + input 绑定修复 |
| `reciteStore.js` | 4, 5, 6, 11, 12 | state 扩展 + SSE 解析 + 兜底 + 会话恢复 |
| `ScoreCard.vue` | 7, 11 | ERROR 态 UI + 追问持久化 |
| `ReportCard.vue` | 8 | 返回主页按钮 |
| `ReciteTopBar.vue` | 8 | finished 状态隐藏结束按钮 |
| `ChatRecite.vue` | 8, 11, 12 | goHome + retry + onMounted 恢复 |
| `api/index.js` | — | debug 清理 |
| `DeepSeekLlmAdapter.java` | 7 | LLM prompt 修正 |
| `ReciteOrchestrationService.java` | 9, 10 | MQ 异步 + searchRandom |
| `QuestionPort.java` | 10 | searchRandom SPI |
| `PgVectorAdapter.java` | 10 | searchRandom 实现 |

---

## 六、经验教训

1. **SSE 两处约定需文档化**：`event:name` 格式（有/无空格）和 `reader.read()` done 信号不可靠——这是前后端集成的常见坑，应在接口文档中标注。

2. **状态字段应显式存储**：`questionId` 放在消息 data 里导致 `sendAnswer` 拿不到——流程控制级别的状态（sessionId, questionId, currentIndex）必须作为独立 store ref，不能只藏在消息对象里。

3. **MQ 不应阻塞用户响应**：RocketMQ 超时 3s × 2 = 6s 卡死 finish 接口——异步任务（报告生成、徽章评估）必须真正异步（线程池/MQ async send），不能 block HTTP 响应线程。

4. **掩盖效应**：Bug 4 (500) 返回 JSON 而非 SSE 流，使 Bug 5+6 无法暴露——联调时先用 curl 验证每个子步骤的独立输入输出，而不是端到端跑通就认为没问题。

5. **前端缓存是最常见的假阳性**：Vite HMR 缓存导致两次"模块列表失败"——curl 验证后端正常后，第一件事应该是重启前端 + 硬刷新。

---

## 七、提交记录

```
8d312be fix: RANDOM 模式真正随机抽题 — searchRandom + Collections.shuffle
ad1598a fix: RANDOM 多选真正实现 — :key 强制重建 input + 切换模式清空
4658c01 fix: RANDOM 模式多选失效 — v-model 与 :checked 冲突
16dce76 fix: 追问自动跳过 — LLM prompt引导 + ScoreCard不自动消失
4c62aca fix: 刷新后恢复历史消息——watch 自动保存到 sessionStorage
7d6e8bb feat: 背诵页刷新后自动恢复会话
674ece6 fix: 结束背诵后无出口 — 添加返回主页按钮
c050302 fix: finishRecite 耗时 6s → 0.14s — MQ 发送移到后台线程
6b8a204 feat: 评分出错兜底机制 — 重试/跳过二选一
e463f36 fix: SSE done 事件后 reader.read() 永不返回 → streaming 卡死
e875aaf fix: SSE 事件解析失败 — event:name 格式不匹配
a1dc0ee fix: submitAnswer 传 null questionId → 后端500 → 评分不显示
4308f32 fix: ModeSelector 模块列表选中态缺失——点击后无视觉反馈
```

---

Co-Authored-By: Claude <noreply@anthropic.com>
