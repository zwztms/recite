package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学习档案列表项 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JournalItemDTO {

    private Long id;
    private LocalDateTime createdAt;
    private String summary;
}
