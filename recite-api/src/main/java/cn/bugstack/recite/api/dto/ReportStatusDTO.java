package cn.bugstack.recite.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 报告轮询状态 DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportStatusDTO {

    /** "generating" 或 "done" */
    private String status;
    /** 报告生成完毕时非空 */
    private JournalDetailDTO journal;
}
