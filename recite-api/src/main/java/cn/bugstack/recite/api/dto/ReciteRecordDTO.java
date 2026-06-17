package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 历史记录响应.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReciteRecordDTO {
    private Long id;
    private String sessionId;
    private String mode;
    private String questionTitle;
    private Integer score;
    private String createdAt;
}
