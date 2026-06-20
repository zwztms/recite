# 缺陷修复编码报告 — 成就墙分类名称不匹配

> 日期：2026-06-20 | 提交：`01ad91a` | 文件：1 | 行数：1

---

## 一、问题

成就墙只展示 15 枚徽章，统计却显示 38 枚。缺失的 23 枚 = 模块类 19 枚 + 组合类 4 枚。

---

## 二、根因

`frontend/src/stores/achievementStore.js` L21 的 `order` 数组分类名与后端 API 不一致。

**后端返回的 category**（由 `BadgeRegistry` 硬编码定义）：
```
背诵量 / 质量 / 坚持 / 模块 / 组合
```

**前端 order 数组（旧）**：
```js
['背诵量', '质量', '坚持', '模块通关', '趣味 & 隐藏']
```

`groupedBadges` 通过 `order.filter(k => groups[k])` 过滤——`'模块通关'` 匹配不到 `groups['模块']`，`'趣味 & 隐藏'` 匹配不到 `groups['组合']`，19+4=23 枚被静默丢弃。

---

## 三、修复

```js
// 修改前
const order = ['背诵量', '质量', '坚持', '模块通关', '趣味 & 隐藏']

// 修改后
const order = ['背诵量', '质量', '坚持', '模块', '组合']
```

---

## 四、验证

| 检查项 | 结果 |
|------|:--:|
| `vite build` | ✅ 1.85s |
| 背诵量组 | ✅ 5 枚 |
| 质量组 | ✅ 5 枚 |
| 坚持组 | ✅ 5 枚 |
| 模块组 | ✅ 19 枚 |
| 组合组 | ✅ 4 枚 |
| 合计 | ✅ 38 枚 |

---

## 五、经验教训

### 5.1 技术层面

`order.filter(k => groups[k])` 这种"按白名单过滤"模式在名称不匹配时会静默丢弃数据。应改为：

```js
// 更安全的写法：已排序的在前，未排序的追加到末尾
const known = order.filter(k => groups[k])
const remaining = Object.keys(groups).filter(k => !order.includes(k))
return [...known, ...remaining].map(...)
```

### 5.2 流程层面

前后端共享的枚举值（category 名称）应由单一来源定义。当前 BadgeRegistry (Java) 和 achievementStore (JS) 各自硬编码，容易出现不一致。后续应考虑将 category 定义收敛到 types 模块或共享常量文件。

---

## 六、提交

```
01ad91a fix: 成就墙分类名称不匹配——模块+组合23枚未展示
1 file changed, 1 insertion(+), 1 deletion(-)
```

Co-Authored-By: Claude <noreply@anthropic.com>
