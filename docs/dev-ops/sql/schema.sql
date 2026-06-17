-- ============================================
-- 八股文背诵助手 V2 — PostgreSQL 数据库设计
-- 执行: psql -U recite -d recite -f schema.sql
-- 日期: 2026-06-12
-- ============================================

-- pgvector 扩展（替代 Milvus）
CREATE EXTENSION IF NOT EXISTS vector;

-- ============================================
-- 1. 用户表（保留）
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    phone         VARCHAR(20) UNIQUE NOT NULL,
    password_hash VARCHAR(128),
    nickname      VARCHAR(50),
    avatar        VARCHAR(500),
    role          VARCHAR(20) DEFAULT 'USER' CHECK(role IN ('USER','ADMIN')),
    status        VARCHAR(20) DEFAULT 'ACTIVE' CHECK(status IN ('ACTIVE','DISABLED')),
    last_login_at TIMESTAMP,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- 2. 管理员表（保留，独立于用户系统）
-- ============================================
CREATE TABLE IF NOT EXISTS admin_users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(128) NOT NULL,
    nickname      VARCHAR(50),
    status        VARCHAR(20) DEFAULT 'ACTIVE',
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- 3. 知识模块表（保留）
-- ============================================
CREATE TABLE IF NOT EXISTS knowledge_modules (
    id             BIGSERIAL PRIMARY KEY,
    module_key     VARCHAR(50) UNIQUE NOT NULL,
    module_name    VARCHAR(100) NOT NULL,
    description    VARCHAR(500),
    status         VARCHAR(20) DEFAULT 'ONLINE' CHECK(status IN ('ONLINE','OFFLINE')),
    sort_order     INTEGER DEFAULT 0,
    question_count INTEGER DEFAULT 0,
    created_at     TIMESTAMP DEFAULT NOW(),
    updated_at     TIMESTAMP DEFAULT NOW()
);

-- ============================================
-- 4. 题目向量表（新 — 替代 Milvus collection）
-- ============================================
CREATE TABLE IF NOT EXISTS question_vectors (
    id         VARCHAR(36) PRIMARY KEY,
    content    TEXT,
    question   VARCHAR(2000),
    module_key VARCHAR(50),
    category   VARCHAR(200),
    tags       VARCHAR(500),
    difficulty INT DEFAULT 1,
    embedding  vector(1024)
);

CREATE INDEX IF NOT EXISTS idx_qv_embedding ON question_vectors
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 48, ef_construction = 200);

CREATE INDEX IF NOT EXISTS idx_qv_module ON question_vectors(module_key);
CREATE INDEX IF NOT EXISTS idx_qv_tags ON question_vectors USING gin (tags);

-- ============================================
-- 5. 背诵记录表（保留，新增字段）
-- ============================================
CREATE TABLE IF NOT EXISTS recite_records (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT NOT NULL REFERENCES users(id),
    session_id           VARCHAR(64) NOT NULL,
    mode                 VARCHAR(20) NOT NULL CHECK(mode IN ('CATEGORY','RANDOM','REVIEW')),
    module_key           VARCHAR(50),
    question_id          VARCHAR(36),
    user_answer          TEXT,
    score                INTEGER CHECK(score BETWEEN 1 AND 10),
    feedback             TEXT,
    follow_up_question   TEXT,
    follow_up_answer     TEXT,
    follow_up_feedback   TEXT,
    follow_up_depth      INT DEFAULT 0,         -- 追问层数（新增）
    parent_record_id     BIGINT,                -- 追问链父记录（新增）
    response_time_seconds INT,                  -- 单题答题耗时（新增）
    created_at           TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rc_user ON recite_records(user_id);
CREATE INDEX IF NOT EXISTS idx_rc_session ON recite_records(session_id);
CREATE INDEX IF NOT EXISTS idx_rc_created ON recite_records(created_at);
CREATE INDEX IF NOT EXISTS idx_rc_parent ON recite_records(parent_record_id);

-- ============================================
-- 6. 掌握度表（保留，新增字段）
-- ============================================
CREATE TABLE IF NOT EXISTS user_progress (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id),
    question_id     VARCHAR(36) NOT NULL,
    module_key      VARCHAR(50),
    mastery_score   INT DEFAULT 0 CHECK(mastery_score BETWEEN 0 AND 100),  -- 替代原 level 枚举
    recite_count    INT DEFAULT 0,
    average_score   REAL,
    last_recited_at TIMESTAMP,
    next_review_at  TIMESTAMP,          -- 下次复习时间（新增）
    review_interval INT DEFAULT 1,      -- 当前间隔天数（新增）
    ease_factor     REAL DEFAULT 2.5,   -- 难度因子 1.3~5.0（新增）
    UNIQUE(user_id, question_id)
);

CREATE INDEX IF NOT EXISTS idx_up_user ON user_progress(user_id);
CREATE INDEX IF NOT EXISTS idx_up_review ON user_progress(user_id, next_review_at);

-- ============================================
-- 7. 学习档案表（新）
-- ============================================
CREATE TABLE IF NOT EXISTS learning_journal (
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT NOT NULL REFERENCES users(id),
    session_id   VARCHAR(64),
    summary_json JSONB NOT NULL,         -- LLM 输出的 8 项结构化摘要
    created_at   TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_lj_user_time ON learning_journal(user_id, created_at DESC);

-- ============================================
-- 8. 连续天数表（新）
-- ============================================
CREATE TABLE IF NOT EXISTS user_streak (
    user_id          BIGINT PRIMARY KEY REFERENCES users(id),
    current_streak   INT DEFAULT 0,
    last_active_date DATE,
    longest_streak   INT DEFAULT 0
);

-- ============================================
-- 9. 成就记录表（新）
-- ============================================
CREATE TABLE IF NOT EXISTS achievement_log (
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users(id),
    badge_key VARCHAR(64) NOT NULL,
    earned_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, badge_key)
);

CREATE INDEX IF NOT EXISTS idx_al_user ON achievement_log(user_id);

-- ============================================
-- 10. 链路追踪 — 运行记录（新）
-- ============================================
CREATE TABLE IF NOT EXISTS trace_runs (
    id            BIGSERIAL PRIMARY KEY,
    trace_id      VARCHAR(20),
    user_id       BIGINT,
    entry_method  VARCHAR(100),
    status        VARCHAR(10),              -- RUNNING / SUCCESS / ERROR
    latency_ms    INT,
    error_msg     VARCHAR(500),
    created_at    TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tr_trace ON trace_runs(trace_id);
CREATE INDEX IF NOT EXISTS idx_tr_created ON trace_runs(created_at);

-- ============================================
-- 11. 链路追踪 — 节点明细（新）
-- ============================================
CREATE TABLE IF NOT EXISTS trace_nodes (
    id          BIGSERIAL PRIMARY KEY,
    trace_id    VARCHAR(20),
    node_name   VARCHAR(100),
    node_type   VARCHAR(20),                -- AUTH/CACHE/VALIDATE/LLM/DB/BUSINESS/MQ/SSE
    status      VARCHAR(10),
    latency_ms  INT,
    error_msg   VARCHAR(500),
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tn_trace ON trace_nodes(trace_id);
CREATE INDEX IF NOT EXISTS idx_tn_type ON trace_nodes(node_type, created_at);

-- ============================================
-- 初始模块数据
-- ============================================
INSERT INTO knowledge_modules (module_key, module_name, description, sort_order)
VALUES
    ('java-basics',     'Java基础',     '语言特性、面向对象、异常、反射、泛型、IO', 1),
    ('juc',             'JUC并发',      '多线程、锁、线程池、并发工具类', 2),
    ('jvm',             'JVM',          '内存模型、垃圾回收、类加载、调优', 3),
    ('java-collections','Java集合',     'List、Set、Map 及线程安全集合', 4),
    ('spring',          'Spring',       'IoC、AOP、事务、MVC、Spring Boot', 5),
    ('mysql',           'MySQL',        'SQL基础、索引、事务、锁、优化', 10),
    ('redis',           'Redis',        '数据结构、持久化、集群、缓存问题', 11),
    ('os',              '操作系统',      '进程线程、内存管理、文件系统、IO', 20),
    ('ds-algo',         '数据结构与算法', '数组、链表、树、图、排序、查找', 21),
    ('network',         '计算机网络',    'OSI模型、TCP/UDP、HTTP、网络安全', 22),
    ('ai-rag',          'AI-RAG',       'RAG架构、检索增强生成', 30),
    ('ai-spring',       'AI-SpringAI',  'Spring AI 框架与实战', 31),
    ('ai-finetune',     'AI-微调',      '大模型微调与参数高效微调', 32),
    ('ai-prompt',       'AI-Prompt工程','CoT、Prompt优化、涌现能力', 33),
    ('ai-eval',         'AI-性能评估',   '缓存、并发、RAG评估', 34),
    ('ai-security',     'AI-安全治理',   '护栏、结构化输出', 35),
    ('ai-design',       'AI-场景设计',   '系统设计、部署选型', 36),
    ('ai-openclaw',     'AI-OpenClaw',  'Agent 工程化框架深度', 37),
    ('ai-agent',        'AI-Agent项目', 'Agent 架构全解', 38)
ON CONFLICT (module_key) DO NOTHING;
