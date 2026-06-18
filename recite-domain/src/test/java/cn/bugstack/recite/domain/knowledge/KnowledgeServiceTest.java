package cn.bugstack.recite.domain.knowledge;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.port.out.EmbeddingPort;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.knowledge.service.KnowledgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("知识库导入编排")
@ExtendWith(MockitoExtension.class)
class KnowledgeServiceTest {

    @Mock private EmbeddingPort embeddingPort;
    @Mock private QuestionPort questionPort;
    @Mock private ModulePort modulePort;

    private KnowledgeService service;

    @BeforeEach
    void setUp() {
        service = new KnowledgeService(embeddingPort, questionPort, modulePort);
    }

    @Nested
    @DisplayName("importQuestions")
    class ImportQuestions {

        @Test
        @DisplayName("null 列表 → 返回 0，不调任何 port")
        void nullListShouldReturnZero() {
            int count = service.importQuestions(null);
            assertThat(count).isZero();
            verifyNoInteractions(embeddingPort, questionPort, modulePort);
        }

        @Test
        @DisplayName("空列表 → 返回 0")
        void emptyListShouldReturnZero() {
            int count = service.importQuestions(List.of());
            assertThat(count).isZero();
            verifyNoInteractions(embeddingPort, questionPort, modulePort);
        }

        @Test
        @DisplayName("单题 → embedding → 入库 → 更新模块计数")
        void singleQuestionShouldImport() {
            var q = QuestionEntity.builder()
                    .question("Q1").content("C1").moduleKey("jvm").build();
            float[] vec = new float[]{0.1f, 0.2f};
            when(embeddingPort.embedBatch(anyList())).thenReturn(List.of(vec));
            when(questionPort.countByModule("jvm")).thenReturn(10);

            int count = service.importQuestions(List.of(q));

            assertThat(count).isEqualTo(1);
            verify(questionPort).index(argThat(qe -> qe.getEmbedding() == vec));
            verify(modulePort).updateQuestionCount("jvm", 1);
        }

        @Test
        @DisplayName("多题同模块 → embedding 批量 → 模块计数增量正确")
        void multipleSameModule() {
            var q1 = QuestionEntity.builder().question("Q1").content("C1").moduleKey("jvm").build();
            var q2 = QuestionEntity.builder().question("Q2").content("C2").moduleKey("jvm").build();
            float[] vec = new float[]{0.1f};
            when(embeddingPort.embedBatch(anyList())).thenReturn(List.of(vec, vec));
            when(questionPort.countByModule("jvm")).thenReturn(5);

            int count = service.importQuestions(List.of(q1, q2));
            assertThat(count).isEqualTo(2);
            verify(questionPort, times(2)).index(any());
            verify(modulePort).updateQuestionCount("jvm", 2);
        }

        @Test
        @DisplayName("多模块 → 分别更新各模块计数")
        void mixedModules() {
            var q1 = QuestionEntity.builder().question("Q1").content("C1").moduleKey("jvm").build();
            var q2 = QuestionEntity.builder().question("Q2").content("C2").moduleKey("juc").build();
            var q3 = QuestionEntity.builder().question("Q3").content("C3").moduleKey("jvm").build();
            float[] vec = new float[]{0.1f};
            when(embeddingPort.embedBatch(anyList())).thenReturn(List.of(vec, vec, vec));

            service.importQuestions(List.of(q1, q2, q3));

            verify(modulePort).updateQuestionCount("jvm", 2);
            verify(modulePort).updateQuestionCount("juc", 1);
        }

        @Test
        @DisplayName("content 为 null → 用空字符串 embedding")
        void nullContentShouldUseEmptyString() {
            var q = QuestionEntity.builder()
                    .question("Q1").content(null).moduleKey("jvm").build();
            float[] vec = new float[]{0.1f};
            when(embeddingPort.embedBatch(argThat(list -> list.get(0).equals(""))))
                    .thenReturn(List.of(vec));

            int count = service.importQuestions(List.of(q));
            assertThat(count).isEqualTo(1);
            verify(embeddingPort).embedBatch(List.of(""));
        }
    }
}
