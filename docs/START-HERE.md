# START HERE — 新会话恢复指南

> 打开此文件即可恢复全部开发上下文，约 5 分钟读完。

---

## 30 秒速览

- **这是什么**：八股文背诵助手 V2，完全重写
- **代码在哪**：`E:\xiangmu\beisong\recite-v2\`（旧代码 `recite\` 不动）
- **Git**：`https://github.com/zwztms/recite`，分支 `main`
- **当前进度**：Phase 0-2 完成（脚手架），**Phase 3 待开始**
- **规则**：不写代码直到用户审批计划

---

## 必读文档

| 顺序 | 文档 | 用途 |
|:--:|------|------|
| 1 | `ARCHITECTURE.md` | 架构决策、模块结构、子域划分 |
| 2 | `DATABASE.md` | 11 张表、字段、索引 |
| 3 | `CONVENTIONS.md` | 编码规范、命名约定 |
| 4 | `PHASE-3-KNOWLEDGE.md` | Phase 3 详细实施计划 |
| 5 | `QA-archive.md` | 历史讨论原文（按需查阅） |

---

## 技术栈

Java 17 · Spring Boot 3.4.3 · MyBatis Plus · Sa-Token · PostgreSQL + pgvector · Redis (Redisson) · RocketMQ · Vue 3 + Pinia + Tailwind

---

## 项目结构

```
recite-v2/
├── recite-types/          基础定义
├── recite-api/            DTO + REST 接口
├── recite-domain/         领域层（五子域 + port/out/）
│   ├── domain/recite/
│   ├── domain/progress/
│   ├── domain/report/
│   ├── domain/achievement/
│   └── domain/knowledge/
├── recite-infrastructure/ 基础设施层（adapter/ 实现）
├── recite-trigger/        控制器 + 拦截器
├── recite-app/            启动入口 + 配置
├── docs/                  文档
└── frontend/              Vue 前端
```

---

## 实施 Phase

| # | 内容 | 状态 |
|:--:|------|:--:|
| 0 | 脚手架 + POM | ✅ |
| 1 | types + api 基类 | ✅ |
| 2 | port/out 目录 | ✅ |
| **3** | **knowledge 子域** | **← 当前** |
| 4 | auth 认证 | — |
| 5 | recite 背诵核心 | — |
| 6 | progress 进度 | — |
| 7 | report 报告 | — |
| 8 | achievement 成就 | — |
| 9 | 可观测性 | — |
| 10 | 前端 | — |

---

## 工作流

1. 读 Phase 计划文档
2. 用户审批
3. 逐文件编码
4. `mvn compile` 验证
5. `git commit` + `git push`
