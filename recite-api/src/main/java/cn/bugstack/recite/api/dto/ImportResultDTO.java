package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 导入结果响应.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResultDTO {
    /** 成功导入数量 */
    private int imported;
    /** 提示消息 */
    private String message;
    /** 失败文件信息 */
    private List<String> errors = new ArrayList<>();
}
