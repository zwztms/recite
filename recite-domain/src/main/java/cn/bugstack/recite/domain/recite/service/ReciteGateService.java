package cn.bugstack.recite.domain.recite.service;

import cn.bugstack.recite.domain.recite.exception.ReciteException;
import cn.bugstack.recite.domain.recite.model.entity.ReciteSession;
import cn.bugstack.recite.types.enums.ResponseCode;
import cn.bugstack.recite.types.common.Constants;

/**
 * 背诵门控校验 — 纯规则，无外部依赖.
 */
public class ReciteGateService {

    /** 校验会话：存在 + userId 匹配 + 未结束 */
    public void validateSession(ReciteSession session, Long userId) {
        if (session == null) {
            throw new ReciteException(ResponseCode.SESSION_NOT_FOUND.getCode(), "背诵会话不存在或已过期");
        }
        if (!session.getUserId().equals(userId)) {
            throw new ReciteException(ResponseCode.FORBIDDEN.getCode(), "无权访问此会话");
        }
        if ("FINISHED".equals(session.getStatus())) {
            throw new ReciteException(ResponseCode.SESSION_EXPIRED.getCode(), "会话已结束");
        }
    }

    /** 校验答案：非空 + 长度限制 */
    public void validateAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new ReciteException(ResponseCode.ANSWER_EMPTY.getCode(), "答案不能为空");
        }
        if (answer.length() > Constants.MAX_ANSWER_LENGTH) {
            throw new ReciteException(ResponseCode.ANSWER_TOO_LONG.getCode(),
                    "答案不能超过 " + Constants.MAX_ANSWER_LENGTH + " 字");
        }
    }

    /** 校验评分范围 */
    public void validateScore(int score) {
        if (score < 1 || score > 10) {
            throw new ReciteException("500", "AI 评分异常: " + score);
        }
    }

    /** 校验追问深度 < 3 */
    public void validateFollowUpDepth(int depth) {
        if (depth >= Constants.FOLLOW_UP_MAX_DEPTH) {
            throw new ReciteException("400", "追问已达最大层数(" + Constants.FOLLOW_UP_MAX_DEPTH + ")");
        }
    }
}
