package cn.bugstack.recite.domain.recite.model.aggregate;

import cn.bugstack.recite.domain.recite.model.event.SessionFinishedEvent;
import cn.bugstack.recite.domain.recite.model.valueobj.SessionId;
import cn.bugstack.recite.shared.model.AggregateRoot;

import java.time.Instant;
import java.util.Objects;

/**
 * 背诵会话聚合根 — recite 子域的核心聚合。
 * <p>
 * 负责：维护会话生命周期、推进题目进度、在完成时发出领域事件。
 */
public class ReciteSession extends AggregateRoot<SessionId> {

    private final SessionId sessionId;
    private final Long userId;
    private int currentIndex;
    private int totalQuestions;
    private SessionStatus status;

    public ReciteSession(SessionId sessionId, Long userId, int totalQuestions) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.userId = Objects.requireNonNull(userId);
        this.totalQuestions = totalQuestions;
        this.currentIndex = 0;
        this.status = SessionStatus.IN_PROGRESS;
    }

    // ---- 领域行为 ----

    /** 推进到下一题。如果已是最后一题，标记完成并发出事件。 */
    public void advance() {
        if (status != SessionStatus.IN_PROGRESS) {
            throw new IllegalStateException("会话已结束，无法推进");
        }
        if (currentIndex >= totalQuestions - 1) {
            status = SessionStatus.FINISHED;
            registerEvent(new SessionFinishedEvent(
                    sessionId.value(), Instant.now().toEpochMilli()));
            return;
        }
        currentIndex++;
    }

    /** 当前答题序号（1-based） */
    public int currentQuestionNumber() {
        return currentIndex + 1;
    }

    public boolean isFinished() {
        return status == SessionStatus.FINISHED;
    }

    // ---- getters ----

    public SessionId sessionId() { return sessionId; }
    public Long userId() { return userId; }
    public int totalQuestions() { return totalQuestions; }

    @Override
    public SessionId getId() { return sessionId; }

    // ---- 内部枚举 ----

    enum SessionStatus { IN_PROGRESS, FINISHED }
}
