package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 徽章详情 DTO — 单枚徽章完整信息.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadgeDetailDTO {

    private String key;
    private String name;
    private String description;
    private String icon;
    private String category;
    private boolean hidden;
    private boolean earned;
    private LocalDateTime earnedAt;
    private int progressPercent;
    /** 获得条件详细描述 */
    private String earnCondition;
    /** 详细说明文本 */
    private String detailedDescription;
}
