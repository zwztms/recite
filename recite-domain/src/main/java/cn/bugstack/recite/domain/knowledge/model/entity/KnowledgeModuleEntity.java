package cn.bugstack.recite.domain.knowledge.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 知识模块实体.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeModuleEntity {

    private Long id;
    /** 唯一标识，如 "java-basics" */
    private String moduleKey;
    /** 展示名，如 "Java基础" */
    private String moduleName;
    /** 描述 */
    private String description;
    /** ONLINE / OFFLINE */
    private String status;
    /** 排序权重 */
    private Integer sortOrder;
    /** 题目数量（冗余计数） */
    private Integer questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
