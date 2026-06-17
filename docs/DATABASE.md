# 数据库设计

> Schema: `docs/dev-ops/sql/schema.sql`
> ER 图: `E:\xiangmu\beisong\项目架构.drawio`

---

## 表清单

| # | 表 | 类型 | 用途 |
|:--:|------|:--:|------|
| 1 | `users` | 保留 | 用户 |
| 2 | `admin_users` | 保留 | 管理员 |
| 3 | `knowledge_modules` | 保留 | 模块管理 |
| 4 | `question_vectors` | **新** | pgvector 向量搜索 |
| 5 | `recite_records` | 修改 | +3 列 |
| 6 | `user_progress` | 修改 | +4 列 |
| 7 | `learning_journal` | **新** | LLM 报告归档 |
| 8 | `user_streak` | **新** | 连续天数 |
| 9 | `achievement_log` | **新** | 徽章记录 |
| 10 | `trace_runs` | **新** | 链路追踪根 |
| 11 | `trace_nodes` | **新** | 链路追踪节点 |

---

## 新增字段明细

### recite_records（+3）
- `follow_up_depth INT DEFAULT 0` — 追问层数
- `parent_record_id BIGINT` — 追问链父记录
- `response_time_seconds INT` — 单题答题耗时

### user_progress（~1 +3）
- `mastery_score INT 0-100` — 替代原 level 枚举
- `next_review_at TIMESTAMP` — 下次复习时间
- `review_interval INT DEFAULT 1` — 当前间隔(天)
- `ease_factor REAL DEFAULT 2.5` — 难度因子 1.3~5.0

---

## 关键索引

```sql
-- pgvector HNSW 搜索
CREATE INDEX ON question_vectors USING hnsw (embedding vector_cosine_ops)
    WITH (m = 48, ef_construction = 200);

-- 今日复习出题
CREATE INDEX ON user_progress(user_id, next_review_at);

-- 学习档案查询
CREATE INDEX ON learning_journal(user_id, created_at DESC);

-- 链路追踪
CREATE INDEX ON trace_nodes(node_type, created_at);
```

---

## 外键关系

```
recite_records.user_id     → users.id
recite_records.parent_record_id → recite_records.id (自引用)
user_progress.user_id      → users.id
learning_journal.user_id   → users.id
user_streak.user_id        → users.id
achievement_log.user_id    → users.id
```
