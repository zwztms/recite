# 翻卡学习 + 个人主页 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增个人主页（学习概览+双入口）和翻卡学习（侧边索引+内容浏览+标记掌握度），重构导航结构

**Architecture:** 遵循现有 DDD 六模块架构。新增 2 个 Controller、2 个 API 接口、4 个 DTO、3 个前端页面、5 个前端组件。全部复用现有 SPI Port，不新增数据库表，不新增 SPI 接口，不修改背诵流程。

**Tech Stack:** Java 17 · Spring Boot 3.4.3 · MyBatis Plus · Vue 3 + Pinia + Tailwind CSS v4

---

## 文件结构总览

```
新增后端文件:
  recite-api/src/main/java/cn/bugstack/recite/api/IHomeService.java         — 个人主页 REST 接口
  recite-api/src/main/java/cn/bugstack/recite/api/ILearnService.java        — 翻卡学习 REST 接口
  recite-api/src/main/java/cn/bugstack/recite/api/dto/HomeDashboardDTO.java  — 主页聚合 DTO
  recite-api/src/main/java/cn/bugstack/recite/api/dto/LearnQuestionDTO.java  — 学习题目 DTO
  recite-api/src/main/java/cn/bugstack/recite/api/dto/MarkRequestDTO.java    — 标记请求 DTO
  recite-domain/src/main/java/cn/bugstack/recite/domain/home/service/HomeService.java  — 主页聚合服务
  recite-domain/src/main/java/cn/bugstack/recite/domain/learn/service/LearnService.java  — 学习编排服务
  recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/HomeController.java  — 主页控制器
  recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/LearnController.java  — 学习控制器

新增前端文件:
  frontend/src/views/HomePage.vue                — 个人主页
  frontend/src/views/CardLearn.vue               — 翻卡学习页
  frontend/src/components/home/ReportModal.vue    — 背诵报告弹窗
  frontend/src/components/learn/SideIndex.vue     — 侧边索引
  frontend/src/components/learn/QuestionCard.vue  — 题目卡片

修改文件:
  frontend/src/App.vue               — 导航链接
  frontend/src/router/index.js       — 路由配置
  frontend/src/stores/authStore.js   — 登录后跳转 /home
  frontend/src/api/index.js          — 新增 API 函数
```

---

### Task 12.1: 主页 DTO — HomeDashboardDTO

**Files:**
- Create: `recite-api/src/main/java/cn/bugstack/recite/api/dto/HomeDashboardDTO.java`

**职责:** 承载个人主页全部聚合数据，一次 API 调用返回所有区域所需数据。

- [ ] **Step 1: 创建 HomeDashboardDTO**

```java
package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 个人主页聚合 DTO — 包含问候统计、模块掌握、趋势、徽章、建议、最近背诵.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HomeDashboardDTO {

    private UserInfo user;
    private Stats stats;
    private List<ModuleMastery> moduleMastery;
    private List<TrendBar> trend;
    private List<BadgeItem> badges;
    private List<String> weakTags;
    private String advice;
    private List<RecentRecite> recentRecites;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private String nickname;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Stats {
        private int streakDays;
        private int totalRecites;
        private int masteredCount;
        private int totalProgress;   // 0-100 百分比
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleMastery {
        private String moduleKey;
        private String moduleName;
        private int mastered;        // 已掌握题数
        private int total;           // 该模块总题数
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendBar {
        private String dayLabel;     // "一","二",...,"日"
        private int count;           // 当天背诵题数
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BadgeItem {
        private String key;
        private String name;
        private String icon;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentRecite {
        private String sessionId;
        private String dateLabel;    // "今天 14:30" / "昨天 21:40" / "6月20日"
        private String moduleKey;
        private String moduleName;
        private int questionCount;
        private double avgScore;
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl recite-api -q
```

---

### Task 12.2: 主页 REST 接口 — IHomeService

**Files:**
- Create: `recite-api/src/main/java/cn/bugstack/recite/api/IHomeService.java`

**职责:** 定义主页聚合数据的 REST 契约。

- [ ] **Step 1: 创建 IHomeService**

```java
package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.HomeDashboardDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 个人主页 REST 接口 — 1 端点.
 */
@RequestMapping("/home")
public interface IHomeService {

    /** 聚合主页数据 */
    @GetMapping("/dashboard")
    Response<HomeDashboardDTO> dashboard();
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl recite-api -q
```

---

### Task 12.3: 主页聚合服务 — HomeService

**Files:**
- Create: `recite-domain/src/main/java/cn/bugstack/recite/domain/home/service/HomeService.java`

**职责:** 跨子域聚合主页所需数据。纯编排服务，只依赖已有 SPI Port，不创建新数据。

**依赖注入 (7 个已有 Port):**
- `ReciteRecordPort` — 累计题数、每日趋势、最近背诵
- `ProgressPort` — 已掌握题数、各模块掌握数
- `StreakPort` — 连续天数
- `AchievementPort` — 最近徽章
- `ReportPort` — 薄弱标签、AI 建议
- `QuestionPort` — 各模块总题数
- `ModulePort` — 模块名称

- [ ] **Step 1: 创建 HomeService**

```java
package cn.bugstack.recite.domain.home.service;

import cn.bugstack.recite.api.dto.HomeDashboardDTO;
import cn.bugstack.recite.domain.achievement.model.entity.AchievementLog;
import cn.bugstack.recite.domain.achievement.port.out.AchievementPort;
import cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.progress.model.entity.UserStreakEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.domain.progress.port.out.StreakPort;
import cn.bugstack.recite.domain.recite.model.entity.ReciteRecordEntity;
import cn.bugstack.recite.domain.recite.port.out.ReciteRecordPort;
import cn.bugstack.recite.domain.report.model.entity.LearningJournal;
import cn.bugstack.recite.domain.report.port.out.ReportPort;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 个人主页聚合服务 — 只读编排，跨 recite/progress/report/achievement/knowledge 子域.
 * 不创建新表，不创建新 SPI，纯聚合现有数据.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private final ReciteRecordPort reciteRecordPort;
    private final ProgressPort progressPort;
    private final StreakPort streakPort;
    private final AchievementPort achievementPort;
    private final ReportPort reportPort;
    private final QuestionPort questionPort;
    private final ModulePort modulePort;

    private static final Gson gson = new Gson();
    private static final List<String> ALL_MODULE_KEYS = List.of(
            "java-basics", "java-collections", "jvm", "juc", "mysql", "redis", "spring",
            "os", "ds-algo", "network", "ai-rag", "ai-prompt", "ai-eval",
            "ai-security", "ai-finetune", "ai-openclaw", "ai-spring", "ai-agent", "ai-design"
    );

    public HomeDashboardDTO build(Long userId) {
        return new HomeDashboardDTO(
                buildUserInfo(userId),
                buildStats(userId),
                buildModuleMastery(userId),
                buildTrend(userId),
                buildBadges(userId),
                buildWeakTags(userId),
                buildAdvice(userId),
                buildRecentRecites(userId)
        );
    }

    // ---- 各区域组装 ----

    private HomeDashboardDTO.UserInfo buildUserInfo(Long userId) {
        // 复用现有 auth 领域? 为了方便，直接从 recite_records 取一条拿到 userId，
        // 实际项目中 UserPort 可提供 getUserName。
        // 这里简洁处理：主页不显示昵称也可，或从 StpUtil 获取 loginId。
        return new HomeDashboardDTO.UserInfo("用户" + userId);
    }

    private HomeDashboardDTO.Stats buildStats(Long userId) {
        int streakDays = Optional.ofNullable(streakPort.findByUserId(userId))
                .map(UserStreakEntity::getCurrentStreak).orElse(0);
        int totalRecites = reciteRecordPort.countByUserId(userId);
        int masteredCount = progressPort.countMastered(userId);
        // 全题库总数，取各模块 question_vectors 计数和
        int totalQuestions = ALL_MODULE_KEYS.stream()
                .mapToInt(questionPort::countByModule).sum();
        int totalProgress = totalQuestions > 0 ? masteredCount * 100 / totalQuestions : 0;
        return new HomeDashboardDTO.Stats(streakDays, totalRecites, masteredCount, totalProgress);
    }

    private List<HomeDashboardDTO.ModuleMastery> buildModuleMastery(Long userId) {
        List<KnowledgeModuleEntity> modules = modulePort.listAll();
        List<HomeDashboardDTO.ModuleMastery> result = new ArrayList<>();
        for (KnowledgeModuleEntity m : modules) {
            int total = questionPort.countByModule(m.getModuleKey());
            // 掌握数 = 该模块 user_progress 中 masteryScore >= 80 的记录数
            int mastered = (int) progressPort.findByUserId(userId).stream()
                    .filter(p -> m.getModuleKey().equals(p.getModuleKey()) && p.getMasteryScore() >= 80)
                    .count();
            result.add(new HomeDashboardDTO.ModuleMastery(
                    m.getModuleKey(), m.getModuleName(), mastered, total));
        }
        return result;
    }

    private List<HomeDashboardDTO.TrendBar> buildTrend(Long userId) {
        // 近 7 天每日背诵题数，用 recite_records group by date
        // ReciteRecordPort 没有日维度查询 → 此处用 findByUserId 内存聚合（性能 OK，每人每天最多几十条）
        List<ReciteRecordEntity> records = reciteRecordPort.findByUserId(userId, 500);
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> dayCount = records.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<HomeDashboardDTO.TrendBar> trend = new ArrayList<>();
        String[] dayLabels = {"一", "二", "三", "四", "五", "六", "日"};
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int count = dayCount.getOrDefault(d, 0L).intValue();
            trend.add(new HomeDashboardDTO.TrendBar(dayLabels[(6 - i)], count));
        }
        return trend;
    }

    private List<HomeDashboardDTO.BadgeItem> buildBadges(Long userId) {
        Map<String, LocalDateTime> earned = achievementPort.findEarnedBadgeMap(userId);
        // 按获得时间倒序取最近 3 枚
        return earned.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(e -> new HomeDashboardDTO.BadgeItem(e.getKey(), e.getKey(), "⭐"))
                .toList();
    }

    private List<String> buildWeakTags(Long userId) {
        // 从最近 learning_journal 的 summaryJson 中提取 weakTags
        List<LearningJournal> journals = reportPort.findRecentJournals(userId, 5);
        Set<String> tags = new LinkedHashSet<>();
        Type listType = new TypeToken<List<String>>() {}.getType();
        for (LearningJournal j : journals) {
            try {
                JsonObject obj = gson.fromJson(j.getSummaryJson(), JsonObject.class);
                if (obj != null && obj.has("weakTags")) {
                    List<String> list = gson.fromJson(obj.get("weakTags"), listType);
                    if (list != null) tags.addAll(list);
                }
            } catch (Exception ignored) {}
        }
        return new ArrayList<>(tags);
    }

    private String buildAdvice(Long userId) {
        List<LearningJournal> journals = reportPort.findRecentJournals(userId, 1);
        if (journals.isEmpty()) return "开始你的第一次背诵吧！";
        try {
            JsonObject obj = gson.fromJson(journals.get(0).getSummaryJson(), JsonObject.class);
            return obj != null && obj.has("advice") ? obj.get("advice").getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    private List<HomeDashboardDTO.RecentRecite> buildRecentRecites(Long userId) {
        // 按 session 分组取最近 5 场
        List<ReciteRecordEntity> records = reciteRecordPort.findByUserId(userId, 200);
        Map<String, List<ReciteRecordEntity>> bySession = records.stream()
                .filter(r -> r.getSessionId() != null)
                .collect(Collectors.groupingBy(ReciteRecordEntity::getSessionId, LinkedHashMap::new, Collectors.toList()));

        List<HomeDashboardDTO.RecentRecite> result = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, List<ReciteRecordEntity>> entry : bySession.entrySet()) {
            if (count >= 5) break;
            List<ReciteRecordEntity> recs = entry.getValue();
            if (recs.isEmpty()) continue;
            ReciteRecordEntity first = recs.get(0);
            double avg = recs.stream().filter(r -> r.getScore() != null)
                    .mapToInt(ReciteRecordEntity::getScore).average().orElse(0);
            result.add(new HomeDashboardDTO.RecentRecite(
                    entry.getKey(),
                    formatDateLabel(first.getCreatedAt()),
                    first.getModuleKey(),
                    first.getModuleKey(), // moduleName 略，前端可映射
                    recs.size(),
                    Math.round(avg * 10.0) / 10.0
            ));
            count++;
        }
        return result;
    }

    /** 时间格式化：今天/昨天/日期 */
    private String formatDateLabel(LocalDateTime dt) {
        if (dt == null) return "";
        LocalDate d = dt.toLocalDate();
        LocalDate today = LocalDate.now();
        if (d.equals(today)) return "今天 " + dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (d.equals(today.minusDays(1))) return "昨天 " + dt.format(DateTimeFormatter.ofPattern("HH:mm"));
        return d.format(DateTimeFormatter.ofPattern("M月d日"));
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl recite-domain -q
```

---

### Task 12.4: 主页控制器 — HomeController

**Files:**
- Create: `recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/HomeController.java`

**职责:** 实现 IHomeService，解析 token 获取 userId，调用 HomeService 聚合数据返回。

- [ ] **Step 1: 创建 HomeController**

```java
package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IHomeService;
import cn.bugstack.recite.api.dto.HomeDashboardDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.home.service.HomeService;
import cn.bugstack.recite.trigger.config.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人主页控制器 — 实现 IHomeService.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class HomeController implements IHomeService {

    private final HomeService homeService;

    @Override
    public Response<HomeDashboardDTO> dashboard() {
        Long userId = UserContext.getUserId();
        HomeDashboardDTO dto = homeService.build(userId);
        return Response.ok(dto);
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl recite-trigger -q
```

- [ ] **Step 3: 全模块编译 + install**

```bash
mvn install -DskipTests -q
```

---

### Task 12.5: 前端 API 函数 + 路由预留

**Files:**
- Modify: `frontend/src/api/index.js`
- Modify: `frontend/src/router/index.js`

**职责:** 新增 getHomeDashboard API 函数；预留 /home 路由（指向占位组件，Phase 13 替换）。

- [ ] **Step 1: api/index.js 追加函数**

在文件末尾 `export default api` 之前追加：

```js
// ======== 个人主页 ========

export function getHomeDashboard() {
  return api.get('/home/dashboard')
}
```

- [ ] **Step 2: router/index.js 追加 /home 路由**

在 routes 数组中追加（import 区加一行懒加载）：

```js
// import 区追加:
const HomePage = () => import('../views/HomePage.vue')

// routes 数组中追加:
{ path: '/home', name: 'Home', component: HomePage, meta: { auth: true } },
```

同时修改根路径重定向：
```js
// Before:
{ path: '/', redirect: '/recite' },
// After:
{ path: '/', redirect: '/home' },
```

- [ ] **Step 3: 前端构建验证**

```bash
cd frontend && npx vite build 2>&1 | tail -3
```

注意：此时 HomePage.vue 还不存在，构建会报错。先创建占位文件：

```vue
<!-- frontend/src/views/HomePage.vue (占位) -->
<template>
  <div class="p-8 text-center text-text-muted">个人主页 — 开发中</div>
</template>
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "phase12: 个人主页后端 — HomeDashboardDTO + IHomeService + HomeService + HomeController"
```

---

### Task 13.1: 报告弹窗组件 — ReportModal.vue

**Files:**
- Create: `frontend/src/components/home/ReportModal.vue`

**职责:** 弹出背诵报告详情。复用 `GET /report/{sessionId}` API，展示总分/均分/题数/优势/薄弱/AI 评语。

- [ ] **Step 1: 创建 ReportModal.vue**

```vue
<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center"
         @click.self="$emit('close')">
      <!-- 遮罩 -->
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>
      <!-- 弹窗 -->
      <div class="relative bg-white rounded-2xl shadow-2xl max-w-lg w-full mx-4 max-h-[80vh] overflow-y-auto">
        <div class="sticky top-0 bg-white border-b border-border px-6 py-4 flex items-center justify-between rounded-t-2xl">
          <h3 class="font-semibold text-text-primary">背诵报告</h3>
          <button @click="$emit('close')" class="text-text-muted hover:text-text-primary text-xl leading-none">&times;</button>
        </div>

        <div v-if="loading" class="p-8 text-center text-text-muted">加载中...</div>

        <div v-else-if="report" class="p-6 space-y-5">
          <!-- 统计 -->
          <div class="grid grid-cols-3 gap-3">
            <div class="text-center p-3 bg-warm rounded-xl">
              <span class="block text-2xl font-bold text-coral">{{ report.totalScore }}</span>
              <span class="text-xs text-text-muted">总分</span>
            </div>
            <div class="text-center p-3 bg-warm rounded-xl">
              <span class="block text-2xl font-bold text-text-primary">{{ report.averageScore }}</span>
              <span class="text-xs text-text-muted">均分</span>
            </div>
            <div class="text-center p-3 bg-warm rounded-xl">
              <span class="block text-2xl font-bold text-text-primary">{{ report.totalQuestions }}</span>
              <span class="text-xs text-text-muted">题数</span>
            </div>
          </div>

          <!-- 优势 -->
          <div v-if="report.strengths?.length">
            <p class="text-sm font-medium text-success-text mb-2">✅ 优势模块</p>
            <div class="flex flex-wrap gap-2">
              <span v-for="s in report.strengths" :key="s"
                class="px-3 py-1 bg-green-50 text-green-700 rounded-full text-xs">{{ s }}</span>
            </div>
          </div>

          <!-- 薄弱 -->
          <div v-if="report.weaknesses?.length">
            <p class="text-sm font-medium text-danger-text mb-2">❗ 薄弱模块</p>
            <div class="flex flex-wrap gap-2">
              <span v-for="w in report.weaknesses" :key="w"
                class="px-3 py-1 bg-red-50 text-red-700 rounded-full text-xs">{{ w }}</span>
            </div>
          </div>

          <!-- AI 评语 -->
          <div v-if="report.advice">
            <p class="text-sm font-medium text-text-primary mb-2">💡 建议</p>
            <p class="text-sm text-text-secondary leading-relaxed">{{ report.advice }}</p>
          </div>
        </div>

        <div v-else class="p-8 text-center text-text-muted">报告生成中，请稍后查看</div>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getReport } from '../../api'

const props = defineProps({
  visible: { type: Boolean, default: false },
  sessionId: { type: String, default: '' }
})

defineEmits(['close'])

const report = ref(null)
const loading = ref(false)

watch(() => props.sessionId, async (sid) => {
  if (!sid) return
  loading.value = true
  report.value = null
  try {
    const res = await getReport(sid)
    if (res.data?.status === 'done' && res.data.journal) {
      report.value = res.data.journal
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
})
</script>
```

---

### Task 13.2: 个人主页 — HomePage.vue

**Files:**
- Create: `frontend/src/views/HomePage.vue` (覆盖占位文件)

**职责:** 完整个人主页，包含问候统计、双 CTA、三列卡片、最近背诵列表。点击背诵记录弹出报告。

- [ ] **Step 1: 创建 HomePage.vue**

```vue
<template>
  <div class="max-w-4xl mx-auto px-4 py-6 space-y-5">
    <!-- 问候 + 统计 -->
    <div class="bg-surface rounded-2xl p-6 border border-border shadow-sm flex items-center gap-5">
      <div class="w-16 h-16 rounded-full bg-gradient-to-br from-coral to-orange-600 flex items-center justify-center text-white text-2xl font-bold shrink-0">
        {{ initial }}
      </div>
      <div class="flex-1">
        <p v-if="data" class="text-lg font-bold text-text-primary mb-2">{{ data.user?.nickname }}，{{ greeting }} ☀️</p>
        <div v-if="data" class="flex gap-5 flex-wrap">
          <div class="text-center"><span class="block text-xl font-bold text-coral">{{ data.stats?.streakDays }}</span><span class="text-xs text-text-muted">连续天数</span></div>
          <div class="w-px bg-border"></div>
          <div class="text-center"><span class="block text-xl font-bold text-text-primary">{{ data.stats?.totalRecites }}</span><span class="text-xs text-text-muted">累计背诵</span></div>
          <div class="w-px bg-border"></div>
          <div class="text-center"><span class="block text-xl font-bold text-green-600">{{ data.stats?.masteredCount }}</span><span class="text-xs text-text-muted">已掌握</span></div>
          <div class="w-px bg-border"></div>
          <div class="text-center"><span class="block text-xl font-bold text-coral">{{ data.stats?.totalProgress }}%</span><span class="text-xs text-text-muted">总进度</span></div>
        </div>
      </div>
    </div>

    <!-- 双 CTA -->
    <div class="grid grid-cols-2 gap-4">
      <button @click="$router.push('/learn')"
        class="p-5 bg-gradient-to-br from-coral to-orange-600 text-white rounded-2xl font-bold text-base shadow-lg shadow-orange-200 hover:shadow-xl transition-shadow">
        📖 翻卡学习
        <span class="block text-xs font-normal opacity-85 mt-1">浏览题目 · 自由选择 · 随手标记</span>
      </button>
      <button @click="$router.push('/recite')"
        class="p-5 bg-white text-coral border-2 border-coral rounded-2xl font-bold text-base hover:bg-orange-50 transition-colors">
        ✍️ 对话背诵
        <span class="block text-xs font-normal text-text-muted mt-1">AI 出题 · 回答评分 · 追问深入</span>
      </button>
    </div>

    <!-- 三列卡片 -->
    <div class="grid grid-cols-3 gap-4" v-if="data">
      <!-- 模块掌握 -->
      <div class="bg-surface rounded-2xl p-5 border border-border">
        <div class="flex justify-between items-center mb-4">
          <span class="font-semibold text-sm text-text-primary">📊 模块掌握</span>
        </div>
        <div class="space-y-3" v-for="m in (data.moduleMastery || []).slice(0,5)" :key="m.moduleKey">
          <div>
            <div class="flex justify-between text-xs mb-1">
              <span class="text-text-primary">{{ m.moduleName }}</span>
              <span :class="m.mastered > m.total/2 ? 'text-coral font-semibold' : 'text-text-muted'">{{ m.mastered }} / {{ m.total }}</span>
            </div>
            <div class="h-2 bg-warm rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all"
                :style="{ width: (m.total > 0 ? m.mastered * 100 / m.total : 0) + '%', background: m.mastered > m.total/2 ? 'linear-gradient(90deg,#f97316,#fb923c)' : '#fed7aa' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 7 天趋势 -->
      <div class="bg-surface rounded-2xl p-5 border border-border">
        <span class="font-semibold text-sm text-text-primary block mb-4">📈 近 7 天背诵</span>
        <div class="flex items-end justify-between h-28 gap-2 px-1">
          <div v-for="t in (data.trend || [])" :key="t.dayLabel" class="flex flex-col items-center flex-1">
            <span class="text-2xs text-text-muted mb-1">{{ t.count }}</span>
            <div class="w-full rounded-t-sm transition-all"
              :style="{ height: Math.max(t.count > 0 ? 4 : 0, Math.min(t.count * 2.5, 100)) + 'px', background: t.count >= 20 ? '#f97316' : '#fed7aa' }"></div>
            <span class="text-2xs text-text-muted mt-2">{{ t.dayLabel }}</span>
          </div>
        </div>
      </div>

      <!-- 徽章 + 标签 + 建议 -->
      <div class="bg-surface rounded-2xl p-5 border border-border flex flex-col gap-4">
        <div>
          <span class="font-semibold text-sm text-text-primary block mb-3">🏆 徽章</span>
          <div class="flex gap-2 items-center">
            <div v-for="b in (data.badges || [])" :key="b.key"
              class="w-10 h-10 rounded-full bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center text-lg"
              :title="b.name">⭐</div>
            <span v-if="(data.badges || []).length === 0" class="text-xs text-text-muted">暂无徽章</span>
          </div>
        </div>
        <div class="border-t border-border pt-4">
          <span class="font-semibold text-sm text-text-primary block mb-3">🏷️ 薄弱标签</span>
          <div class="flex flex-wrap gap-1">
            <span v-for="t in (data.weakTags || [])" :key="t"
              class="px-2 py-0.5 bg-orange-50 text-orange-700 border border-orange-200 rounded-full text-2xs">{{ t }}</span>
            <span v-if="!(data.weakTags || []).length" class="text-xs text-text-muted">暂无</span>
          </div>
        </div>
        <div class="border-t border-border pt-4">
          <span class="font-semibold text-sm text-text-primary block mb-2">💡 建议</span>
          <p class="text-xs text-text-secondary leading-relaxed">{{ data.advice || '开始你的学习之旅！' }}</p>
        </div>
      </div>
    </div>

    <!-- 最近背诵 -->
    <div class="bg-surface rounded-2xl p-5 border border-border" v-if="data">
      <span class="font-semibold text-sm text-text-primary block mb-4">📝 最近背诵</span>
      <div class="space-y-1">
        <div v-for="r in (data.recentRecites || [])" :key="r.sessionId"
          @click="openReport(r.sessionId)"
          class="flex items-center gap-3 px-3 py-2.5 rounded-xl cursor-pointer hover:bg-warm transition-colors border border-transparent hover:border-border">
          <span class="text-xs text-text-muted w-20 shrink-0">{{ r.dateLabel }}</span>
          <span class="flex-1 text-sm text-text-primary">{{ r.moduleName || r.moduleKey }} · {{ r.questionCount }} 题 · 均分 <b :class="r.avgScore >= 7 ? 'text-green-600' : 'text-coral'">{{ r.avgScore }}</b></span>
          <span class="text-xs text-coral font-semibold shrink-0">查看报告 →</span>
        </div>
        <p v-if="!(data.recentRecites || []).length" class="text-sm text-text-muted text-center py-4">暂无背诵记录</p>
      </div>
    </div>

    <!-- 报告弹窗 -->
    <ReportModal :visible="reportVisible" :sessionId="selectedSid" @close="reportVisible = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getHomeDashboard } from '../api'
import ReportModal from '../components/home/ReportModal.vue'

const data = ref(null)
const reportVisible = ref(false)
const selectedSid = ref('')

const initial = computed(() => (data.value?.user?.nickname || 'U')[0].toUpperCase())

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

onMounted(async () => {
  try {
    const res = await getHomeDashboard()
    data.value = res.data
  } catch (e) {
    // ignore
  }
})

function openReport(sid) {
  selectedSid.value = sid
  reportVisible.value = true
}
</script>
```

- [ ] **Step 2: 前端构建验证**

```bash
cd frontend && npx vite build 2>&1 | tail -3
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "phase13: 个人主页前端 — HomePage + ReportModal"
```

---

### Task 14.1: 学习 DTO — LearnQuestionDTO + MarkRequestDTO

**Files:**
- Create: `recite-api/src/main/java/cn/bugstack/recite/api/dto/LearnQuestionDTO.java`
- Create: `recite-api/src/main/java/cn/bugstack/recite/api/dto/MarkRequestDTO.java`

- [ ] **Step 1: 创建 LearnQuestionDTO**

```java
package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 翻卡学习题目 DTO — 含掌握状态.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnQuestionDTO {
    private String id;
    private String question;
    private String content;         // 答案
    private String moduleKey;
    private String moduleName;
    private String category;
    private String tags;
    private int difficulty;
    private boolean mastered;       // 当前用户是否已掌握
}
```

- [ ] **Step 2: 创建 MarkRequestDTO**

```java
package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标记掌握度请求.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkRequestDTO {
    private String questionId;
    private boolean mastered;       // true=已掌握, false=未掌握
}
```

- [ ] **Step 3: 编译验证**

```bash
mvn compile -pl recite-api -q
```

---

### Task 14.2: 学习 REST 接口 — ILearnService

**Files:**
- Create: `recite-api/src/main/java/cn/bugstack/recite/api/ILearnService.java`

- [ ] **Step 1: 创建 ILearnService**

```java
package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.LearnQuestionDTO;
import cn.bugstack.recite.api.dto.MarkRequestDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 翻卡学习 REST 接口 — 2 端点.
 */
@RequestMapping("/learn")
public interface ILearnService {

    /** 获取题目列表（含掌握状态） */
    @GetMapping("/questions")
    Response<List<LearnQuestionDTO>> questions(
            @RequestParam(required = false) String moduleKey,
            @RequestParam(defaultValue = "seq") String order,   // seq | random
            @RequestParam(defaultValue = "all") String filter    // all | unmastered | mastered
    );

    /** 标记掌握/未掌握 */
    @PostMapping("/mark")
    Response<Void> mark(@RequestBody MarkRequestDTO request);
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl recite-api -q
```

---

### Task 14.3: 学习编排服务 — LearnService

**Files:**
- Create: `recite-domain/src/main/java/cn/bugstack/recite/domain/learn/service/LearnService.java`

**依赖注入 (4 个已有 Port + 1 个已有 Service):**
- `QuestionPort` — 题目查询
- `ProgressPort` — 掌握度查询/更新
- `ModulePort` — 模块名称查询
- `SpacedRepetitionService` — 间隔重复算法（标记后调用）

- [ ] **Step 1: 创建 LearnService**

```java
package cn.bugstack.recite.domain.learn.service;

import cn.bugstack.recite.api.dto.LearnQuestionDTO;
import cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity;
import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.model.valueobj.EmbeddedQuestionVO;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
import cn.bugstack.recite.domain.progress.port.out.ProgressPort;
import cn.bugstack.recite.domain.progress.service.SpacedRepetitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 翻卡学习编排服务 — 题目列表 + 标记掌握度.
 * 复用已有 QuestionPort / ProgressPort / SpacedRepetitionService，不创建新 SPI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearnService {

    private final QuestionPort questionPort;
    private final ProgressPort progressPort;
    private final ModulePort modulePort;
    private final SpacedRepetitionService spacedRepetitionService;

    /** 获取题目列表 */
    public List<LearnQuestionDTO> getQuestions(Long userId, String moduleKey, String order, String filter) {
        // 1. 拉题
        List<EmbeddedQuestionVO> vos;
        if ("random".equals(order)) {
            vos = questionPort.searchRandom(
                    moduleKey != null && !moduleKey.isEmpty() ? List.of(moduleKey) : List.of(), 10000);
        } else {
            vos = questionPort.searchByModule(
                    moduleKey != null && !moduleKey.isEmpty() ? moduleKey : null, 10000);
        }

        // 2. 查用户掌握度
        List<UserProgressEntity> progressList = progressPort.findByUserId(userId);
        Set<String> masteredIds = progressList.stream()
                .filter(p -> p.getMasteryScore() >= 80)
                .map(UserProgressEntity::getQuestionId)
                .collect(Collectors.toSet());

        // 3. 模块名称映射
        Map<String, String> moduleNameMap = modulePort.listAll().stream()
                .collect(Collectors.toMap(KnowledgeModuleEntity::getModuleKey,
                        m -> m.getModuleName() != null ? m.getModuleName() : m.getModuleKey(),
                        (a, b) -> a));

        // 4. 组装 DTO
        List<LearnQuestionDTO> result = new ArrayList<>();
        for (EmbeddedQuestionVO vo : vos) {
            QuestionEntity q = vo.question();
            boolean mastered = masteredIds.contains(q.getId());
            // 筛选
            if ("mastered".equals(filter) && !mastered) continue;
            if ("unmastered".equals(filter) && mastered) continue;

            result.add(new LearnQuestionDTO(
                    q.getId(), q.getQuestion(), q.getContent(),
                    q.getModuleKey(),
                    moduleNameMap.getOrDefault(q.getModuleKey(), q.getModuleKey()),
                    q.getCategory(), q.getTags(),
                    q.getDifficulty() != null ? q.getDifficulty() : 1,
                    mastered
            ));
        }
        return result;
    }

    /** 标记掌握度 */
    public void mark(Long userId, String questionId, boolean mastered) {
        QuestionEntity question = questionPort.getById(questionId);
        if (question == null) {
            log.warn("标记失败：题目不存在 id={}", questionId);
            return;
        }

        int aiScore = mastered ? 8 : 3;  // 已掌握→8分，未掌握→3分

        Optional<UserProgressEntity> current = progressPort.findByUserAndQuestion(userId, questionId);
        UserProgressEntity updated = spacedRepetitionService.calculateAfterScore(
                current.orElse(null), aiScore, userId, questionId, question.getModuleKey());

        if (current.isPresent()) {
            updated.setId(current.get().getId());
            progressPort.update(updated);
        } else {
            progressPort.save(updated);
        }
        log.info("标记完成: userId={}, qid={}, mastered={}, newMastery={}", userId, questionId, mastered, updated.getMasteryScore());
    }
}
```

- [ ] **Step 2: 编译验证**

```bash
mvn compile -pl recite-domain -q
```

---

### Task 14.4: 学习控制器 — LearnController

**Files:**
- Create: `recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/LearnController.java`

- [ ] **Step 1: 创建 LearnController**

```java
package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.ILearnService;
import cn.bugstack.recite.api.dto.LearnQuestionDTO;
import cn.bugstack.recite.api.dto.MarkRequestDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.learn.service.LearnService;
import cn.bugstack.recite.trigger.config.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 翻卡学习控制器 — 实现 ILearnService.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LearnController implements ILearnService {

    private final LearnService learnService;

    @Override
    public Response<List<LearnQuestionDTO>> questions(String moduleKey, String order, String filter) {
        Long userId = UserContext.getUserId();
        List<LearnQuestionDTO> list = learnService.getQuestions(userId, moduleKey, order, filter);
        return Response.ok(list);
    }

    @Override
    public Response<Void> mark(MarkRequestDTO request) {
        Long userId = UserContext.getUserId();
        learnService.mark(userId, request.getQuestionId(), request.isMastered());
        return Response.ok();
    }
}
```

- [ ] **Step 2: 编译 + install**

```bash
mvn install -DskipTests -q
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "phase14: 翻卡学习后端 — LearnQuestionDTO + ILearnService + LearnService + LearnController"
```

---

### Task 15.1: 前端 API 函数追加

**Files:**
- Modify: `frontend/src/api/index.js`

- [ ] **Step 1: 追加翻卡学习 API 函数**

```js
// ======== 翻卡学习 ========

export function getLearnQuestions(moduleKey, order, filter) {
  return api.get('/learn/questions', { params: { moduleKey, order, filter } })
}

export function markMastery(questionId, mastered) {
  return api.post('/learn/mark', { questionId, mastered })
}
```

---

### Task 15.2: 侧边索引组件 — SideIndex.vue

**Files:**
- Create: `frontend/src/components/learn/SideIndex.vue`

**职责:** 展示当前模块全部题目编号+标题+掌握状态。点击跳转，联动滚动。

- [ ] **Step 1: 创建 SideIndex.vue**

```vue
<template>
  <div class="w-44 shrink-0 bg-surface rounded-xl border border-border overflow-hidden flex flex-col h-full">
    <div class="px-3 py-2.5 border-b border-border text-xs font-semibold text-text-primary">
      题目索引
    </div>
    <div class="flex-1 overflow-y-auto p-1" ref="listRef">
      <div v-for="(q, i) in questions" :key="q.id"
        @click="$emit('select', q.id)"
        :class="['flex items-center gap-2 px-2.5 py-1.5 rounded-md cursor-pointer text-xs transition-colors',
          activeId === q.id ? 'bg-orange-50 border border-orange-200' : 'hover:bg-warm']">
        <span :class="activeId === q.id ? 'text-coral font-semibold' : 'text-text-muted'" class="w-5">{{ i + 1 }}</span>
        <span class="flex-1 truncate" :class="activeId === q.id ? 'text-text-primary font-semibold' : 'text-text-secondary'">{{ q.question }}</span>
        <span class="w-1.5 h-1.5 rounded-full shrink-0" :class="q.mastered ? 'bg-green-500' : 'bg-gray-300'" :title="q.mastered ? '已掌握' : '未掌握'"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  questions: { type: Array, required: true },
  activeId: { type: String, default: '' }
})

defineEmits(['select'])

const listRef = ref(null)

// 当 activeId 变化时自动滚动索引项到可见区域
watch(() => props.activeId, (id) => {
  if (!listRef.value) return
  const el = listRef.value.querySelector(`[data-qid="${id}"]`)
  if (el) el.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
})
</script>
```

---

### Task 15.3: 题目卡片组件 — QuestionCard.vue

**Files:**
- Create: `frontend/src/components/learn/QuestionCard.vue`

**职责:** 单题卡片，点击展开/收起答案，展示掌握状态和标记按钮。

- [ ] **Step 1: 创建 QuestionCard.vue**

```vue
<template>
  <div :class="['bg-surface rounded-xl border transition-all', expanded ? 'border-coral/30 shadow-sm' : 'border-border hover:border-coral/20']"
    :id="`q-${question.id}`">
    <!-- 题目行 -->
    <div @click="toggle"
      class="flex items-center gap-3 px-4 py-3 cursor-pointer select-none">
      <span :class="['w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0',
        question.mastered ? 'bg-green-500 text-white' : 'bg-gray-200 text-text-muted']">
        {{ index + 1 }}
      </span>
      <div class="flex-1 min-w-0">
        <p class="text-sm text-text-primary truncate">{{ question.question }}</p>
        <span class="text-2xs text-text-muted">{{ question.moduleName }} · {{ stars(question.difficulty) }} · {{ question.category || '' }}</span>
      </div>
      <span v-if="question.mastered" class="text-xs text-green-600 font-medium shrink-0">✅</span>
      <span class="text-text-muted text-sm shrink-0 transition-transform" :class="expanded && 'rotate-180'">▼</span>
    </div>

    <!-- 答案区 -->
    <div v-if="expanded" class="px-4 pb-4 border-t border-border pt-4">
      <div class="bg-warm rounded-xl p-4 mb-4">
        <p class="text-sm text-text-primary leading-relaxed whitespace-pre-wrap">{{ question.content }}</p>
      </div>
      <div class="flex gap-3">
        <button v-if="!question.mastered"
          @click.stop="$emit('mark', question.id, true)"
          class="flex-1 py-2.5 bg-green-500 text-white rounded-lg text-sm font-semibold hover:bg-green-600 transition-colors">
          ✅ 已掌握
        </button>
        <button v-if="question.mastered"
          @click.stop="$emit('mark', question.id, false)"
          class="flex-1 py-2.5 bg-white border border-border text-text-secondary rounded-lg text-sm hover:bg-gray-50 transition-colors">
          标记为未掌握
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const props = defineProps({
  question: { type: Object, required: true },
  index: { type: Number, default: 0 }
})

defineEmits(['mark'])

const expanded = ref(false)

function toggle() {
  expanded.value = !expanded.value
}

function stars(n) {
  return '⭐'.repeat(Math.min(n || 1, 5))
}
</script>
```

---

### Task 15.4: 翻卡学习页 — CardLearn.vue

**Files:**
- Create: `frontend/src/views/CardLearn.vue`

**职责:** 组装侧边索引+内容区，处理模块切换、顺序切换、筛选、标记后即时更新。

- [ ] **Step 1: 创建 CardLearn.vue**

```vue
<template>
  <div class="h-[calc(100vh-3.5rem)] flex flex-col -mx-4 -my-8">
    <!-- 控制栏 -->
    <div class="flex items-center gap-3 px-5 py-3 bg-surface border-b border-border flex-wrap shrink-0">
      <select v-model="moduleKey" @change="fetchQuestions"
        class="px-3 py-2 border border-border rounded-lg text-sm bg-white text-text-primary">
        <option value="">全部模块</option>
        <option v-for="m in modules" :key="m.moduleKey" :value="m.moduleKey">{{ m.moduleName }} ({{ m.questionCount }}题)</option>
      </select>

      <div class="flex gap-0 bg-gray-100 rounded-lg p-0.5">
        <button v-for="o in orderOptions" :key="o.value"
          @click="order = o.value; fetchQuestions()"
          :class="['px-3 py-1.5 rounded-md text-xs font-semibold transition-colors',
            order === o.value ? 'bg-coral text-white' : 'text-text-muted']">
          {{ o.label }}
        </button>
      </div>

      <div class="flex gap-0 bg-gray-100 rounded-lg p-0.5 ml-auto">
        <button v-for="f in filterOptions" :key="f.value"
          @click="filter = f.value; fetchQuestions()"
          :class="['px-3 py-1.5 rounded-md text-xs font-semibold transition-colors',
            filter === f.value ? 'bg-white text-coral' : 'text-text-muted']">
          {{ f.label }}
        </button>
      </div>

      <span class="text-xs text-text-muted">已掌握 {{ masteredCount }}/{{ questions.length }}</span>
    </div>

    <!-- 主体 -->
    <div class="flex-1 flex gap-0 overflow-hidden">
      <!-- 侧边索引 -->
      <SideIndex
        :questions="questions"
        :activeId="activeId"
        @select="scrollTo"
      />

      <!-- 内容区 -->
      <div class="flex-1 overflow-y-auto px-4 py-4 space-y-3" ref="contentRef" @scroll="onScroll">
        <QuestionCard
          v-for="(q, i) in questions"
          :key="q.id"
          :question="q"
          :index="i"
          :ref="el => setCardRef(q.id, el)"
          @mark="onMark"
        />
        <p v-if="questions.length === 0" class="text-center text-text-muted py-12">暂无题目</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getLearnQuestions, markMastery, listModules } from '../api'
import SideIndex from '../components/learn/SideIndex.vue'
import QuestionCard from '../components/learn/QuestionCard.vue'

const modules = ref([])
const questions = ref([])
const moduleKey = ref('')
const order = ref('seq')
const filter = ref('all')
const activeId = ref('')

const orderOptions = [
  { value: 'seq', label: '按顺序' },
  { value: 'random', label: '乱序' }
]
const filterOptions = [
  { value: 'all', label: '全部' },
  { value: 'unmastered', label: '未掌握' },
  { value: 'mastered', label: '已掌握' }
]

const masteredCount = computed(() => questions.value.filter(q => q.mastered).length)

const contentRef = ref(null)
const cardRefs = {}

function setCardRef(id, el) {
  if (el) cardRefs[id] = el
}

async function fetchQuestions() {
  try {
    const res = await getLearnQuestions(moduleKey.value || null, order.value, filter.value)
    questions.value = res.data || []
    activeId.value = ''
  } catch (e) {
    // ignore
  }
}

function scrollTo(id) {
  activeId.value = id
  const el = document.getElementById(`q-${id}`)
  if (el) el.scrollIntoView({ block: 'start', behavior: 'smooth' })
}

function onScroll() {
  // 索引联动：找出当前在视口内的第一道题
  if (!contentRef.value) return
  const containerTop = contentRef.value.getBoundingClientRect().top + 100
  for (const q of questions.value) {
    const el = document.getElementById(`q-${q.id}`)
    if (el) {
      const rect = el.getBoundingClientRect()
      if (rect.bottom > containerTop) {
        activeId.value = q.id
        break
      }
    }
  }
}

async function onMark(questionId, mastered) {
  try {
    await markMastery(questionId, mastered)
    // 即时更新本地状态
    const q = questions.value.find(q => q.id === questionId)
    if (q) q.mastered = mastered
  } catch (e) {
    // ignore
  }
}

onMounted(async () => {
  try {
    const res = await listModules()
    modules.value = (res.data || []).filter(m => m.status === 'ONLINE')
  } catch (e) {}
  await fetchQuestions()
})
</script>
```

- [ ] **Step 2: 前端构建验证**

```bash
cd frontend && npx vite build 2>&1 | tail -3
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "phase15: 翻卡学习前端 — CardLearn + SideIndex + QuestionCard"
```

---

### Task 16.1: 导航重构

**Files:**
- Modify: `frontend/src/App.vue`
- Modify: `frontend/src/router/index.js`
- Modify: `frontend/src/stores/authStore.js`

**职责:** 导航菜单加入"首页"和"学习"，登录后跳转 /home，"/" 重定向到 /home。

- [ ] **Step 1: 修改 App.vue 导航**

找到原导航链接区域（`<!-- 导航链接 -->` 附近），替换为：

```html
<router-link to="/home" class="nav-link" active-class="nav-active">首页</router-link>
<router-link to="/learn" class="nav-link" active-class="nav-active">学习</router-link>
<router-link to="/recite" class="nav-link" active-class="nav-active">背诵</router-link>
<router-link to="/achievements" class="nav-link" active-class="nav-active">成就</router-link>
<router-link v-if="auth.isAdmin" to="/admin/modules" class="nav-link" active-class="nav-active">管理</router-link>
```

- [ ] **Step 2: 修改 router/index.js /learn 路由**

在 routes 数组中追加（与 /home 同级）：

```js
{ path: '/learn', name: 'Learn', component: () => import('../views/CardLearn.vue'), meta: { auth: true } },
```

- [ ] **Step 3: 修改 authStore.js 登录跳转**

找到 `login()`、`register()`、`adminLogin()` 中 `router.push('/recite')` 的地方，改为：

```js
router.push('/home')
```

（共 3 处：登录、注册、管理员登录）

- [ ] **Step 4: 前端构建验证**

```bash
cd frontend && npx vite build 2>&1 | tail -3
```

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "phase16: 导航重构 — 首页+学习 Tab + /home 默认首页"
```

---

## Phase 依赖关系

```
Phase 12 (主页后端) ──→ Phase 13 (主页前端)
                           │
Phase 14 (学习后端) ──→ Phase 15 (学习前端)
                           │
                    Phase 16 (导航重构，依赖 13+15 的页面存在)
```

- 12 和 14 可并行（都是后端，互不依赖）
- 13 依赖 12（后端 API 就绪）
- 15 依赖 14（后端 API 就绪）
- 16 依赖 13+15（页面组件存在才可配路由）

## 涉及文件统计

| 类型 | Phase 12 | Phase 13 | Phase 14 | Phase 15 | Phase 16 | 合计 |
|------|:--:|:--:|:--:|:--:|:--:|:--:|
| 新增 Java | 4 | 0 | 4 | 0 | 0 | 8 |
| 新增 Vue | 1(占位) | 2 | 0 | 3 | 0 | 6 |
| 修改文件 | 2(api/router) | 0 | 0 | 1(api) | 3(App/router/store) | 6 |
| 新表 | 0 | 0 | 0 | 0 | 0 | 0 |
| 新 SPI | 0 | 0 | 0 | 0 | 0 | 0 |
