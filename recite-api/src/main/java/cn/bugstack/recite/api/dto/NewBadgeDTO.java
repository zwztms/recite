package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 新徽章轮询返回 DTO — 前端 Toast 展示用.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewBadgeDTO {

    private String key;
    private String name;
    private String description;
    private String icon;
}
