package cn.bugstack.recite.domain.learn.service;

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
 * 翻卡学习领域服务 — 题目列表 + 掌握度标记.
 *
 * <p>复用 QuestionPort、ProgressPort、ModulePort、SpacedRepetitionService，
 * 不新增表、不新增 SPI.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearnService {

    private final QuestionPort questionPort;
    private final ProgressPort progressPort;
    private final ModulePort modulePort;
    private final SpacedRepetitionService spacedRepetitionService;

    /** 获取题目列表（含当前用户掌握状态） */
    public List<LearnQuestionVO> getQuestions(Long userId, String moduleKey,
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

        // 4. 组装 VO
        List<LearnQuestionVO> result = new ArrayList<>();
        for (EmbeddedQuestionVO vo : vos) {
            QuestionEntity q = vo.question();
            boolean mastered = masteredIds.contains(q.getId());
            if ("mastered".equals(filter) && !mastered) continue;
            if ("unmastered".equals(filter) && mastered) continue;

            result.add(new LearnQuestionVO(
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

    /** 标记掌握度 */
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

    // ======== 领域值对象 ========

    public record LearnQuestionVO(String id, String question, String content,
                                   String moduleKey, String moduleName,
                                   String category, String tags,
                                   int difficulty, boolean mastered) {}
}
