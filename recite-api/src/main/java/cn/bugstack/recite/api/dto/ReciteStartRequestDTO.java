package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 开始背诵请求.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReciteStartRequestDTO {
    /** CATEGORY / RANDOM / REVIEW */
    private String mode;
    /** 模块范围 */
    private List<String> moduleKeys;
    /** 出题数量 */
    private int count;
}
