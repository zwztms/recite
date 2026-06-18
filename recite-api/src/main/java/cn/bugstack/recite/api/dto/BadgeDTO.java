package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 徽章卡片 DTO — 徽章墙列表项.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BadgeDTO {

    private String key;
    private String name;
    private String description;
    private String icon;
    private String category;
    private boolean hidden;
    private boolean earned;
    private LocalDateTime earnedAt;
    private Progress progress;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Progress {
        private int current;
        private int target;
        private int percent;
    }
}
