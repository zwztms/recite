package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 开始背诵响应.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReciteStartResultDTO {
    private String sessionId;
    private QuestionDTO question;
    private int questionIndex;
    private int totalQuestions;
}
