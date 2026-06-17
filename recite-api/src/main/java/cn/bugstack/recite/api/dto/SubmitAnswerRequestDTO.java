package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 提交答案请求.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitAnswerRequestDTO {
    private String questionId;
    private String answer;
}
