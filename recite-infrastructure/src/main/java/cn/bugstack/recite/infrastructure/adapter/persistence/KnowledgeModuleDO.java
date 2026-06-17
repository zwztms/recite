package cn.bugstack.recite.infrastructure.adapter.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 映射 knowledge_modules 表.
 */
@Data
@TableName("knowledge_modules")
public class KnowledgeModuleDO {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String moduleKey;
    private String moduleName;
    private String description;
    private String status;
    private Integer sortOrder;
    private Integer questionCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
