package cn.bugstack.recite.domain.achievement.exception;

import cn.bugstack.recite.types.exception.AppException;

/**
 * achievement 成就子域异常.
 *
 * <p>场景：徽章不存在、评估失败、数据为空.</p>
 */
public class AchievementException extends AppException {

    public AchievementException(String message) {
        super("500", message);
    }

    public AchievementException(String code, String message) {
        super(code, message);
    }
}
