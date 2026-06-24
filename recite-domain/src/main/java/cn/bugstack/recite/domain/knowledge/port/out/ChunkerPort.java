package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 文本分块 SPI — 按段落标题边界切分文本.
 */
public interface ChunkerPort {

    /**
     * @param text     待切分的纯文本
     * @param docTitle 文档标题（用于元信息记录）
     * @return 分块列表，按原文顺序排列
     */
    List<Chunk> chunk(String text, String docTitle);

    record Chunk(String chunkText, int index, String docTitle) {}
}
