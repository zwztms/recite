package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 追问请求.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpRequestDTO {
    private Long recordId;
    private String followUpAnswer;
}
