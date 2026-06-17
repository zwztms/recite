package cn.bugstack.recite.domain.knowledge.port.out;

import cn.bugstack.recite.domain.knowledge.model.valueobj.ImportResultVO;

/**
 * 文件导入 SPI — infra 层实现（扫描目录 + 解析文件 + 调 KnowledgeService）.
 */
public interface FileImportPort {

    /** 执行导入 → 扫描、解析、入库、归档 */
    ImportResultVO doImport();
}
