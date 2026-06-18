package cn.bugstack.recite.domain.recite;

import cn.bugstack.recite.domain.recite.exception.ReciteException;
import cn.bugstack.recite.domain.recite.model.entity.ReciteSession;
import cn.bugstack.recite.domain.recite.service.ReciteGateService;
import cn.bugstack.recite.types.enums.ReciteMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("背诵门控校验")
class ReciteGateServiceTest {

    private ReciteGateService service;

    @BeforeEach
    void setUp() {
        service = new ReciteGateService();
    }

    @Nested
    @DisplayName("会话校验")
    class ValidateSession {

        @Test
        @DisplayName("正常会话应通过")
        void validSessionShouldPass() {
            var session = ReciteSession.builder()
                    .sessionId("sid1").userId(1L).status("ACTIVE")
                    .mode(ReciteMode.CATEGORY).questionIds(List.of("q1"))
                    .currentIndex(0).totalQuestions(1)
                    .createdAt(LocalDateTime.now()).build();
            assertThatCode(() -> service.validateSession(session, 1L))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("session=null → SESSION_NOT_FOUND")
        void nullSessionShouldThrow() {
            assertThatThrownBy(() -> service.validateSession(null, 1L))
                    .isInstanceOf(ReciteException.class)
                    .extracting("code").isEqualTo("S001");
        }

        @Test
        @DisplayName("userId 不匹配 → FORBIDDEN")
        void differentUserIdShouldThrow() {
            var session = ReciteSession.builder()
                    .sessionId("sid1").userId(1L).status("ACTIVE")
                    .mode(ReciteMode.CATEGORY).questionIds(List.of("q1"))
                    .currentIndex(0).totalQuestions(1)
                    .createdAt(LocalDateTime.now()).build();
            assertThatThrownBy(() -> service.validateSession(session, 2L))
                    .isInstanceOf(ReciteException.class)
                    .extracting("code").isEqualTo("403");
        }

        @Test
        @DisplayName("FINISHED 状态 → SESSION_EXPIRED")
        void finishedSessionShouldThrow() {
            var session = ReciteSession.builder()
                    .sessionId("sid1").userId(1L).status("FINISHED")
                    .mode(ReciteMode.CATEGORY).questionIds(List.of("q1"))
                    .currentIndex(0).totalQuestions(1)
                    .createdAt(LocalDateTime.now()).build();
            assertThatThrownBy(() -> service.validateSession(session, 1L))
                    .isInstanceOf(ReciteException.class)
                    .extracting("code").isEqualTo("S002");
        }
    }

    @Nested
    @DisplayName("答案校验")
    class ValidateAnswer {

        @Test
        @DisplayName("正常答案应通过")
        void validAnswerShouldPass() {
            assertThatCode(() -> service.validateAnswer("JVM 包含堆和栈"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("null → ANSWER_EMPTY")
        void nullAnswerShouldThrow() {
            assertThatThrownBy(() -> service.validateAnswer(null))
                    .isInstanceOf(ReciteException.class)
                    .extracting("code").isEqualTo("A001");
        }

        @Test
        @DisplayName("空字符串 → ANSWER_EMPTY")
        void emptyAnswerShouldThrow() {
            assertThatThrownBy(() -> service.validateAnswer("   "))
                    .isInstanceOf(ReciteException.class)
                    .extracting("code").isEqualTo("A001");
        }

        @Test
        @DisplayName("超过 5000 字 → ANSWER_TOO_LONG")
        void tooLongAnswerShouldThrow() {
            String longAnswer = "x".repeat(5001);
            assertThatThrownBy(() -> service.validateAnswer(longAnswer))
                    .isInstanceOf(ReciteException.class)
                    .extracting("code").isEqualTo("A002");
        }

        @Test
        @DisplayName("恰好 5000 字应通过")
        void maxLengthAnswerShouldPass() {
            String ok = "x".repeat(5000);
            assertThatCode(() -> service.validateAnswer(ok)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("分数校验")
    class ValidateScore {

        @Test
        @DisplayName("1-10 范围内应通过")
        void scoreInRangeShouldPass() {
            for (int s : new int[]{1, 5, 10}) {
                int score = s;
                assertThatCode(() -> service.validateScore(score)).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("0 分应抛异常")
        void scoreZeroShouldThrow() {
            assertThatThrownBy(() -> service.validateScore(0))
                    .isInstanceOf(ReciteException.class);
        }

        @Test
        @DisplayName("11 分应抛异常")
        void scoreElevenShouldThrow() {
            assertThatThrownBy(() -> service.validateScore(11))
                    .isInstanceOf(ReciteException.class);
        }

        @Test
        @DisplayName("负分应抛异常")
        void negativeScoreShouldThrow() {
            assertThatThrownBy(() -> service.validateScore(-1))
                    .isInstanceOf(ReciteException.class);
        }
    }

    @Nested
    @DisplayName("追问深度校验")
    class ValidateFollowUpDepth {

        @Test
        @DisplayName("0-2 层应通过")
        void depthInRangeShouldPass() {
            for (int d : new int[]{0, 1, 2}) {
                int depth = d;
                assertThatCode(() -> service.validateFollowUpDepth(depth))
                        .doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("≥3 层应抛异常")
        void depthExceedsMaxShouldThrow() {
            assertThatThrownBy(() -> service.validateFollowUpDepth(3))
                    .isInstanceOf(ReciteException.class);

            assertThatThrownBy(() -> service.validateFollowUpDepth(5))
                    .isInstanceOf(ReciteException.class);
        }
    }
}
