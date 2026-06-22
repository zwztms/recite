# 翻卡学习 + 个人主页 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan phase-by-phase.

**Goal:** 新增个人主页（学习概览+双入口）和翻卡学习（侧边索引+自由浏览+标记掌握度），重构导航结构

**Architecture:** 遵循现有 DDD 六模块架构。新增 8 个 Java 文件 + 5 个 Vue 文件，修改 4 个文件。全部复用现有 SPI Port，不新增数据库表，不新增 SPI 接口，不修改背诵流程。

**Tech Stack:** Java 17 · Spring Boot 3.4.3 · MyBatis Plus · Vue 3 + Pinia + Tailwind CSS v4

---

## 文件全貌

```
新增后端 (8 文件):
  recite-api/.../IHomeService.java           REST 契约：GET /home/dashboard
  recite-api/.../ILearnService.java          REST 契约：GET /learn/questions + POST /learn/mark
  recite-api/.../dto/HomeDashboardDTO.java   主页聚合响应（含 6 个内部静态类）
  recite-api/.../dto/LearnQuestionDTO.java   学习题目响应
  recite-api/.../dto/MarkRequestDTO.java     标记请求体
  recite-domain/.../home/service/HomeService.java   跨子域聚合编排
  recite-domain/.../learn/service/LearnService.java 题目列表+标记编排
  recite-trigger/.../http/HomeController.java       主页控制器
  recite-trigger/.../http/LearnController.java      学习控制器

新增前端 (5 文件):
  frontend/src/views/HomePage.vue                个人主页
  frontend/src/views/CardLearn.vue               翻卡学习页
  frontend/src/components/home/ReportModal.vue   背诵报告弹窗
  frontend/src/components/learn/SideIndex.vue    侧边索引
  frontend/src/components/learn/QuestionCard.vue 题目卡片

修改 (4 文件):
  frontend/src/api/index.js          新增 3 个 API 函数
  frontend/src/router/index.js       新增 /home /learn 路由，/ 重定向改为 /home
  frontend/src/App.vue               导航链接加入"首页""学习"
  frontend/src/stores/authStore.js   登录后跳转 /home
```

---

## Phase 12：个人主页后端 API

### 主题
新增 `GET /home/dashboard` 聚合 API，一次返回主页全部区域所需数据。

### 目的
前端只需一次请求即可渲染个人主页——问候统计、模块掌握度、7天趋势、徽章、薄弱标签、AI 建议、最近背诵列表。全部从已有 Port 聚合，不建新表、不加新 SPI。

### 影响
| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无（复用 7 个已有 Port） |
| 修改旧代码 | 无 |
| 前端 | 占位 HomePage.vue + api/index.js 追加 getHomeDashboard + router 预埋 /home |

### 子任务

- [ ] **1. 创建 HomeDashboardDTO**

文件：`recite-api/src/main/java/cn/bugstack/recite/api/dto/HomeDashboardDTO.java`

```java
package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

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

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class UserInfo {
        private String nickname;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class Stats {
        private int streakDays;
        private int totalRecites;
        private int masteredCount;
        private int totalProgress;  // 0-100
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ModuleMastery {
        private String moduleKey;
        private String moduleName;
        private int mastered;       // 已掌握题数
        private int total;          // 该模块总题数
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TrendBar {
        private String dayLabel;    // "一"~"日"
        private int count;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BadgeItem {
        private String key;
        private String name;
        private String icon;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class RecentRecite {
        private String sessionId;
        private String dateLabel;   // "今天 14:30" / "昨天 21:40" / "6月20日"
        private String moduleKey;
        private String moduleName;
        private int questionCount;
        private double avgScore;
    }
}
```

编译：`mvn compile -pl recite-api -q`

- [ ] **2. 创建 IHomeService**

文件：`recite-api/src/main/java/cn/bugstack/recite/api/IHomeService.java`

```java
package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.HomeDashboardDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/home")
public interface IHomeService {
    @GetMapping("/dashboard")
    Response<HomeDashboardDTO> dashboard();
}
```

编译：`mvn compile -pl recite-api -q`

- [ ] **3. 创建 HomeService**

文件：`recite-domain/src/main/java/cn/bugstack/recite/domain/home/service/HomeService.java`

```java
package cn.bugstack.recite.domain.home.service;

import cn.bugstack.recite.api.dto.HomeDashboardDTO;
import cn.bugstack.recite.domain.achievement.port.out.AchievementPort;
import cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.progress.model.entity.UserProgressEntity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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

    private HomeDashboardDTO.UserInfo buildUserInfo(Long userId) {
        return new HomeDashboardDTO.UserInfo("用户" + userId);
    }

    private HomeDashboardDTO.Stats buildStats(Long userId) {
        int streakDays = streakPort.findByUserId(userId)
                .map(UserStreakEntity::getCurrentStreak).orElse(0);
        int totalRecites = reciteRecordPort.countByUserId(userId);
        int masteredCount = progressPort.countMastered(userId);
        int totalQuestions = modulePort.listAll().stream()
                .mapToInt(m -> questionPort.countByModule(m.getModuleKey())).sum();
        int totalProgress = totalQuestions > 0 ? masteredCount * 100 / totalQuestions : 0;
        return new HomeDashboardDTO.Stats(streakDays, totalRecites, masteredCount, totalProgress);
    }

    private List<HomeDashboardDTO.ModuleMastery> buildModuleMastery(Long userId) {
        List<KnowledgeModuleEntity> modules = modulePort.listAll();
        List<UserProgressEntity> progressList = progressPort.findByUserId(userId);
        Set<String> masteredIds = progressList.stream()
                .filter(p -> p.getMasteryScore() >= 80)
                .map(UserProgressEntity::getQuestionId)
                .collect(Collectors.toSet());

        List<HomeDashboardDTO.ModuleMastery> result = new ArrayList<>();
        for (KnowledgeModuleEntity m : modules) {
            int total = questionPort.countByModule(m.getModuleKey());
            int mastered = (int) progressList.stream()
                    .filter(p -> m.getModuleKey().equals(p.getModuleKey())
                            && masteredIds.contains(p.getQuestionId()))
                    .count();
            result.add(new HomeDashboardDTO.ModuleMastery(
                    m.getModuleKey(), m.getModuleName(), mastered, total));
        }
        return result;
    }

    private List<HomeDashboardDTO.TrendBar> buildTrend(Long userId) {
        List<ReciteRecordEntity> records = reciteRecordPort.findByUserId(userId, 500);
        LocalDate today = LocalDate.now();
        Map<LocalDate, Long> dayCount = records.stream()
                .filter(r -> r.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().toLocalDate(), Collectors.counting()));

        String[] dayLabels = {"一", "二", "三", "四", "五", "六", "日"};
        List<HomeDashboardDTO.TrendBar> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            int count = dayCount.getOrDefault(d, 0L).intValue();
            trend.add(new HomeDashboardDTO.TrendBar(dayLabels[6 - i], count));
        }
        return trend;
    }

    private List<HomeDashboardDTO.BadgeItem> buildBadges(Long userId) {
        Map<String, LocalDateTime> earned = achievementPort.findEarnedBadgeMap(userId);
        return earned.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(e -> new HomeDashboardDTO.BadgeItem(e.getKey(), e.getKey(), "⭐"))
                .toList();
    }

    private List<String> buildWeakTags(Long userId) {
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
        } catch (Exception e) { return ""; }
    }

    private List<HomeDashboardDTO.RecentRecite> buildRecentRecites(Long userId) {
        List<ReciteRecordEntity> records = reciteRecordPort.findByUserId(userId, 200);
        Map<String, List<ReciteRecordEntity>> bySession = records.stream()
                .filter(r -> r.getSessionId() != null)
                .collect(Collectors.groupingBy(ReciteRecordEntity::getSessionId,
                        LinkedHashMap::new, Collectors.toList()));

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
                    first.getModuleKey(), first.getModuleKey(),
                    recs.size(),
                    Math.round(avg * 10.0) / 10.0
            ));
            count++;
        }
        return result;
    }

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

编译：`mvn compile -pl recite-domain -q`

- [ ] **4. 创建 HomeController**

文件：`recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/HomeController.java`

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

编译：`mvn compile -pl recite-trigger -q`

- [ ] **5. 前端预埋**

`frontend/src/api/index.js` 末尾 `export default api` 之前追加：

```js
// ======== 个人主页 ========
export function getHomeDashboard() {
  return api.get('/home/dashboard')
}
```

`frontend/src/router/index.js` 修改：

```js
// import 区追加:
const HomePage = () => import('../views/HomePage.vue')

// routes 数组追加:
{ path: '/home', name: 'Home', component: HomePage, meta: { auth: true } },

// 根路径重定向修改:
{ path: '/', redirect: '/home' },  // 原为 '/recite'
```

创建占位 `frontend/src/views/HomePage.vue`：

```vue
<template>
  <div class="p-8 text-center text-text-muted">个人主页 — 开发中</div>
</template>
```

构建验证：`cd frontend && npx vite build`

- [ ] **6. 全量编译 + 提交**

```bash
mvn install -DskipTests -q
git add -A
git commit -m "phase12: 个人主页后端 — GET /home/dashboard 聚合 API"
```

---

## Phase 13：翻卡学习后端 API

### 主题
新增 `GET /learn/questions` 和 `POST /learn/mark`，支持题目列表浏览和掌握度标记。

### 目的
前端翻卡学习页面获取题目列表（含已掌握/未掌握状态），标记掌握度后更新间隔重复数据。复用 QuestionPort、ProgressPort、SpacedRepetitionService。

### 影响
| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无 |
| 修改旧代码 | 无 |
| 前端 | api/index.js 追加 getLearnQuestions + markMastery |

### 子任务

- [ ] **1. 创建 LearnQuestionDTO**

文件：`recite-api/src/main/java/cn/bugstack/recite/api/dto/LearnQuestionDTO.java`

```java
package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearnQuestionDTO {
    private String id;
    private String question;       // 题目
    private String content;        // 答案
    private String moduleKey;
    private String moduleName;
    private String category;
    private String tags;
    private int difficulty;
    private boolean mastered;      // 当前用户是否已掌握
}
```

- [ ] **2. 创建 MarkRequestDTO**

文件：`recite-api/src/main/java/cn/bugstack/recite/api/dto/MarkRequestDTO.java`

```java
package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkRequestDTO {
    private String questionId;
    private boolean mastered;      // true=已掌握, false=未掌握
}
```

编译：`mvn compile -pl recite-api -q`

- [ ] **3. 创建 ILearnService**

文件：`recite-api/src/main/java/cn/bugstack/recite/api/ILearnService.java`

```java
package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.LearnQuestionDTO;
import cn.bugstack.recite.api.dto.MarkRequestDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/learn")
public interface ILearnService {

    @GetMapping("/questions")
    Response<List<LearnQuestionDTO>> questions(
            @RequestParam(required = false) String moduleKey,
            @RequestParam(defaultValue = "seq") String order,
            @RequestParam(defaultValue = "all") String filter
    );

    @PostMapping("/mark")
    Response<Void> mark(@RequestBody MarkRequestDTO request);
}
```

编译：`mvn compile -pl recite-api -q`

- [ ] **4. 创建 LearnService**

文件：`recite-domain/src/main/java/cn/bugstack/recite/domain/learn/service/LearnService.java`

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

@Slf4j
@Service
@RequiredArgsConstructor
public class LearnService {

    private final QuestionPort questionPort;
    private final ProgressPort progressPort;
    private final ModulePort modulePort;
    private final SpacedRepetitionService spacedRepetitionService;

    public List<LearnQuestionDTO> getQuestions(Long userId, String moduleKey,
                                                String order, String filter) {
        // 1. 拉题
        List<EmbeddedQuestionVO> vos;
        if ("random".equals(order)) {
            vos = questionPort.searchRandom(
                    moduleKey != null && !moduleKey.isEmpty()
                            ? List.of(moduleKey) : List.of(), 10000);
        } else {
            vos = questionPort.searchByModule(
                    moduleKey != null && !moduleKey.isEmpty() ? moduleKey : null, 10000);
        }

        // 2. 查掌握度
        List<UserProgressEntity> progressList = progressPort.findByUserId(userId);
        Set<String> masteredIds = progressList.stream()
                .filter(p -> p.getMasteryScore() >= 80)
                .map(UserProgressEntity::getQuestionId)
                .collect(Collectors.toSet());

        // 3. 模块名映射
        Map<String, String> nameMap = modulePort.listAll().stream()
                .collect(Collectors.toMap(KnowledgeModuleEntity::getModuleKey,
                        m -> m.getModuleName() != null ? m.getModuleName() : m.getModuleKey(),
                        (a, b) -> a));

        // 4. 组装 DTO
        List<LearnQuestionDTO> result = new ArrayList<>();
        for (EmbeddedQuestionVO vo : vos) {
            QuestionEntity q = vo.question();
            boolean mastered = masteredIds.contains(q.getId());
            if ("mastered".equals(filter) && !mastered) continue;
            if ("unmastered".equals(filter) && mastered) continue;

            result.add(new LearnQuestionDTO(
                    q.getId(), q.getQuestion(), q.getContent(),
                    q.getModuleKey(),
                    nameMap.getOrDefault(q.getModuleKey(), q.getModuleKey()),
                    q.getCategory(), q.getTags(),
                    q.getDifficulty() != null ? q.getDifficulty() : 1,
                    mastered
            ));
        }
        return result;
    }

    public void mark(Long userId, String questionId, boolean mastered) {
        QuestionEntity question = questionPort.getById(questionId);
        if (question == null) {
            log.warn("标记失败：题目不存在 id={}", questionId);
            return;
        }
        int aiScore = mastered ? 8 : 3;
        Optional<UserProgressEntity> current = progressPort.findByUserAndQuestion(userId, questionId);
        UserProgressEntity updated = spacedRepetitionService.calculateAfterScore(
                current.orElse(null), aiScore, userId, questionId, question.getModuleKey());
        if (current.isPresent()) {
            updated.setId(current.get().getId());
            progressPort.update(updated);
        } else {
            progressPort.save(updated);
        }
    }
}
```

编译：`mvn compile -pl recite-domain -q`

- [ ] **5. 创建 LearnController**

文件：`recite-trigger/src/main/java/cn/bugstack/recite/trigger/http/LearnController.java`

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

@Slf4j
@RestController
@RequiredArgsConstructor
public class LearnController implements ILearnService {

    private final LearnService learnService;

    @Override
    public Response<List<LearnQuestionDTO>> questions(String moduleKey, String order, String filter) {
        Long userId = UserContext.getUserId();
        return Response.ok(learnService.getQuestions(userId, moduleKey, order, filter));
    }

    @Override
    public Response<Void> mark(MarkRequestDTO request) {
        Long userId = UserContext.getUserId();
        learnService.mark(userId, request.getQuestionId(), request.isMastered());
        return Response.ok();
    }
}
```

编译：`mvn compile -pl recite-trigger -q`

- [ ] **6. 前端预埋 + 全量编译 + 提交**

`frontend/src/api/index.js` 追加：

```js
// ======== 翻卡学习 ========
export function getLearnQuestions(moduleKey, order, filter) {
  return api.get('/learn/questions', { params: { moduleKey, order, filter } })
}

export function markMastery(questionId, mastered) {
  return api.post('/learn/mark', { questionId, mastered })
}
```

```bash
mvn install -DskipTests -q
git add -A
git commit -m "phase13: 翻卡学习后端 — GET /learn/questions + POST /learn/mark"
```

---

## Phase 14：个人主页前端

### 主题
实现 HomePage.vue 和 ReportModal.vue，用户登录后看到完整学习概览首页。

### 目的
用户进入首页即可看到学习概览——问候+统计、翻卡/背诵双入口、模块掌握度柱状图、7天趋势、徽章+薄弱标签+AI建议、最近背诵列表（点击弹出背诵报告）。一次 API 调用拿到全部数据。

### 影响
| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无 |
| 修改旧代码 | 无（HomePage.vue 覆盖占位文件，router 已预埋） |
| 依赖 | Phase 12 后端 API 就绪 |

### 子任务

- [ ] **1. 创建 ReportModal.vue**

文件：`frontend/src/components/home/ReportModal.vue`

```vue
<template>
  <Teleport to="body">
    <div v-if="visible" class="fixed inset-0 z-50 flex items-center justify-center"
         @click.self="$emit('close')">
      <div class="absolute inset-0 bg-black/40 backdrop-blur-sm"></div>
      <div class="relative bg-white rounded-2xl shadow-2xl max-w-lg w-full mx-4 max-h-[80vh] overflow-y-auto">
        <div class="sticky top-0 bg-white border-b border-border px-6 py-4 flex items-center justify-between rounded-t-2xl">
          <h3 class="font-semibold text-text-primary">背诵报告</h3>
          <button @click="$emit('close')" class="text-text-muted hover:text-text-primary text-xl leading-none">&times;</button>
        </div>
        <div v-if="loading" class="p-8 text-center text-text-muted">加载中...</div>
        <div v-else-if="report" class="p-6 space-y-5">
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
          <div v-if="report.strengths?.length">
            <p class="text-sm font-medium text-success-text mb-2">✅ 优势模块</p>
            <div class="flex flex-wrap gap-2">
              <span v-for="s in report.strengths" :key="s"
                class="px-3 py-1 bg-green-50 text-green-700 rounded-full text-xs">{{ s }}</span>
            </div>
          </div>
          <div v-if="report.weaknesses?.length">
            <p class="text-sm font-medium text-danger-text mb-2">❗ 薄弱模块</p>
            <div class="flex flex-wrap gap-2">
              <span v-for="w in report.weaknesses" :key="w"
                class="px-3 py-1 bg-red-50 text-red-700 rounded-full text-xs">{{ w }}</span>
            </div>
          </div>
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
  } catch (e) {}
  loading.value = false
})
</script>
```

- [ ] **2. 创建 HomePage.vue（完整版，覆盖占位）**

文件：`frontend/src/views/HomePage.vue`

```vue
<template>
  <div class="max-w-4xl mx-auto px-4 py-6 space-y-5">
    <!-- 问候 + 统计 -->
    <div class="bg-surface rounded-2xl p-6 border border-border shadow-sm flex items-center gap-5">
      <div class="w-16 h-16 rounded-full bg-gradient-to-br from-coral to-orange-600 flex items-center justify-center text-white text-2xl font-bold shrink-0">
        {{ initial }}
      </div>
      <div class="flex-1" v-if="data">
        <p class="text-lg font-bold text-text-primary mb-2">{{ data.user?.nickname }}，{{ greeting }} ☀️</p>
        <div class="flex gap-5 flex-wrap">
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
        <span class="font-semibold text-sm text-text-primary block mb-4">📊 模块掌握</span>
        <div class="space-y-3">
          <div v-for="m in (data.moduleMastery || []).slice(0,5)" :key="m.moduleKey">
            <div class="flex justify-between text-xs mb-1">
              <span class="text-text-primary">{{ m.moduleName }}</span>
              <span :class="m.mastered > m.total/2 ? 'text-coral font-semibold' : 'text-text-muted'">{{ m.mastered }} / {{ m.total }}</span>
            </div>
            <div class="h-2 bg-warm rounded-full overflow-hidden">
              <div class="h-full rounded-full transition-all"
                :style="{ width: (m.total > 0 ? m.mastered * 100 / m.total : 0) + '%',
                  background: m.mastered > m.total/2 ? 'linear-gradient(90deg,#f97316,#fb923c)' : '#fed7aa' }"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- 趋势 -->
      <div class="bg-surface rounded-2xl p-5 border border-border">
        <span class="font-semibold text-sm text-text-primary block mb-4">📈 近 7 天背诵</span>
        <div class="flex items-end justify-between h-28 gap-2 px-1">
          <div v-for="t in (data.trend || [])" :key="t.dayLabel" class="flex flex-col items-center flex-1">
            <span class="text-2xs text-text-muted mb-1">{{ t.count }}</span>
            <div class="w-full rounded-t-sm" :style="{ height: Math.min(Math.max(t.count * 2.5, 4), 100) + 'px',
              background: t.count >= 20 ? '#f97316' : '#fed7aa' }"></div>
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
              class="w-10 h-10 rounded-full bg-gradient-to-br from-amber-400 to-amber-600 flex items-center justify-center text-lg" :title="b.name">⭐</div>
            <span v-if="!(data.badges || []).length" class="text-xs text-text-muted">暂无徽章</span>
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
  } catch (e) {}
})

function openReport(sid) {
  selectedSid.value = sid
  reportVisible.value = true
}
</script>
```

- [ ] **3. 构建验证 + 提交**

```bash
cd frontend && npx vite build 2>&1 | tail -3
git add -A
git commit -m "phase14: 个人主页前端 — HomePage + ReportModal"
```

---

## Phase 15：翻卡学习前端

### 主题
实现 CardLearn.vue + SideIndex.vue + QuestionCard.vue，侧边索引联动内容区。

### 目的
用户可自由浏览题目列表、点击展开答案、标记掌握度。侧边索引快速跳转到任意题，滚动内容区时索引联动高亮当前题。标记后即时更新间隔重复数据。

### 影响
| 维度 | 结论 |
|------|------|
| 新表 | 无 |
| 新 SPI | 无 |
| 修改旧代码 | 无 |
| 依赖 | Phase 13 后端 API 就绪 |

### 子任务

- [ ] **1. 创建 SideIndex.vue**

文件：`frontend/src/components/learn/SideIndex.vue`

```vue
<template>
  <div class="w-44 shrink-0 bg-surface rounded-xl border border-border overflow-hidden flex flex-col h-full">
    <div class="px-3 py-2.5 border-b border-border text-xs font-semibold text-text-primary">题目索引</div>
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
defineProps({
  questions: { type: Array, required: true },
  activeId: { type: String, default: '' }
})
defineEmits(['select'])
</script>
```

- [ ] **2. 创建 QuestionCard.vue**

文件：`frontend/src/components/learn/QuestionCard.vue`

```vue
<template>
  <div :class="['bg-surface rounded-xl border transition-all', expanded ? 'border-coral/30 shadow-sm' : 'border-border hover:border-coral/20']"
    :id="`q-${question.id}`">
    <!-- 题目行 -->
    <div @click="toggle" class="flex items-center gap-3 px-4 py-3 cursor-pointer select-none">
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

function toggle() { expanded.value = !expanded.value }
function stars(n) { return '⭐'.repeat(Math.min(n || 1, 5)) }
</script>
```

- [ ] **3. 创建 CardLearn.vue**

文件：`frontend/src/views/CardLearn.vue`

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
        <button v-for="o in [{v:'seq',l:'按顺序'},{v:'random',l:'乱序'}]" :key="o.v"
          @click="order = o.v; fetchQuestions()"
          :class="['px-3 py-1.5 rounded-md text-xs font-semibold transition-colors',
            order === o.v ? 'bg-coral text-white' : 'text-text-muted']">{{ o.l }}</button>
      </div>
      <div class="flex gap-0 bg-gray-100 rounded-lg p-0.5 ml-auto">
        <button v-for="f in [{v:'all',l:'全部'},{v:'unmastered',l:'未掌握'},{v:'mastered',l:'已掌握'}]" :key="f.v"
          @click="filter = f.v; fetchQuestions()"
          :class="['px-3 py-1.5 rounded-md text-xs font-semibold transition-colors',
            filter === f.v ? 'bg-white text-coral' : 'text-text-muted']">{{ f.l }}</button>
      </div>
      <span class="text-xs text-text-muted">已掌握 {{ masteredCount }}/{{ questions.length }}</span>
    </div>

    <!-- 主体 -->
    <div class="flex-1 flex gap-0 overflow-hidden">
      <SideIndex :questions="questions" :activeId="activeId" @select="scrollTo" />
      <div class="flex-1 overflow-y-auto px-4 py-4 space-y-3" ref="contentRef" @scroll="onScroll">
        <QuestionCard v-for="(q, i) in questions" :key="q.id" :question="q" :index="i" @mark="onMark" />
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
const contentRef = ref(null)

const masteredCount = computed(() => questions.value.filter(q => q.mastered).length)

async function fetchQuestions() {
  try {
    const res = await getLearnQuestions(moduleKey.value || null, order.value, filter.value)
    questions.value = res.data || []
    activeId.value = ''
  } catch (e) {}
}

function scrollTo(id) {
  activeId.value = id
  const el = document.getElementById(`q-${id}`)
  if (el) el.scrollIntoView({ block: 'start', behavior: 'smooth' })
}

function onScroll() {
  if (!contentRef.value) return
  const top = contentRef.value.getBoundingClientRect().top + 100
  for (const q of questions.value) {
    const el = document.getElementById(`q-${q.id}`)
    if (el) {
      const rect = el.getBoundingClientRect()
      if (rect.bottom > top) { activeId.value = q.id; break }
    }
  }
}

async function onMark(questionId, mastered) {
  try {
    await markMastery(questionId, mastered)
    const q = questions.value.find(q => q.id === questionId)
    if (q) q.mastered = mastered
  } catch (e) {}
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

- [ ] **4. 构建验证 + 提交**

```bash
cd frontend && npx vite build 2>&1 | tail -3
git add -A
git commit -m "phase15: 翻卡学习前端 — CardLearn + SideIndex + QuestionCard"
```

---

## Phase 16：导航收尾

### 主题
更新顶部导航菜单、路由配置和登录跳转，串联全部页面。

### 目的
用户登录后进入个人主页，导航栏可访问首页/学习/背诵/成就/管理。改动最小——仅调整已有文件的链接和跳转路径，不新建文件。

### 影响
| 维度 | 结论 |
|------|------|
| 新文件 | 0 |
| 修改旧代码 | App.vue、router/index.js、authStore.js |
| 影响现有功能 | 背诵/成就/管理路由不变，仅新增首页和学习入口 |

### 子任务

- [ ] **1. App.vue — 导航链接**

将现有导航链接替换为（顺序：首页 | 学习 | 背诵 | 成就 | 管理）：

```html
<router-link to="/home" class="nav-link" active-class="nav-active">首页</router-link>
<router-link to="/learn" class="nav-link" active-class="nav-active">学习</router-link>
<router-link to="/recite" class="nav-link" active-class="nav-active">背诵</router-link>
<router-link to="/achievements" class="nav-link" active-class="nav-active">成就</router-link>
<router-link v-if="auth.isAdmin" to="/admin/modules" class="nav-link" active-class="nav-active">管理</router-link>
```

- [ ] **2. router/index.js — /learn 路由**

在 routes 数组中追加：

```js
{ path: '/learn', name: 'Learn', component: () => import('../views/CardLearn.vue'), meta: { auth: true } },
```

- [ ] **3. authStore.js — 登录跳转**

将 `login()`、`register()`、`adminLogin()` 中的 `router.push('/recite')` 改为 `router.push('/home')`（共 3 处）。

- [ ] **4. 构建验证 + 提交**

```bash
cd frontend && npx vite build 2>&1 | tail -3
git add -A
git commit -m "phase16: 导航收尾 — 首页+学习入口 + 登录跳转 /home"
```

---

## Phase 依赖图

```
Phase 12 (主页后端) ──→ Phase 14 (主页前端)
Phase 13 (学习后端) ──→ Phase 15 (学习前端)
                              │
                         Phase 16 (导航收尾，依赖 14+15)
```

12 和 13 可并行。14 和 15 可并行。16 最后执行。

## 汇总

| Phase | 主题 | 新增文件 | 修改文件 | 新表 | 新 SPI |
|:--:|------|:--:|:--:|:--:|:--:|
| 12 | 个人主页后端 API | 4 Java + 1 Vue(占位) | 2 JS(预埋) | 无 | 无 |
| 13 | 翻卡学习后端 API | 4 Java | 1 JS | 无 | 无 |
| 14 | 个人主页前端 | 2 Vue | 1 Vue(替占位) | 无 | 无 |
| 15 | 翻卡学习前端 | 3 Vue | — | 无 | 无 |
| 16 | 导航收尾 | — | 3 Vue/JS | 无 | 无 |
