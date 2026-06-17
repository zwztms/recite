package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模块状态变更请求.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleStatusRequestDTO {
    /** ONLINE / OFFLINE */
    private String status;
}
