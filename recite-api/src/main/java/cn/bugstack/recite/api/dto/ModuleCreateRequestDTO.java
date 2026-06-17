package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建模块请求.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleCreateRequestDTO {
    private String moduleKey;
    private String moduleName;
    private String description;
    private Integer sortOrder;
}
