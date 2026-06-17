package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 会话状态响应.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReciteSessionDTO {
    private String sessionId;
    private String mode;
    private int currentIndex;
    private int totalQuestions;
    private String status;
}
