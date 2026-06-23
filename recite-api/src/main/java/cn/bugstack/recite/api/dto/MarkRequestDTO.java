package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 掌握度标记请求体.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkRequestDTO {

    private String questionId;
    /** true=已掌握, false=未掌握 */
    private boolean mastered;
}
