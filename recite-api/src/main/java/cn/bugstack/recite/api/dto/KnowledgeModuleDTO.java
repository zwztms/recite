package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模块列表响应.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeModuleDTO {
    private Long id;
    private String moduleKey;
    private String moduleName;
    private String description;
    private String status;
    private Integer sortOrder;
    private Integer questionCount;
}
