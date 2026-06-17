package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 文本转向量 SPI — infra 层实现（SiliconFlow API）.
 */
public interface EmbeddingPort {

    /** 单条文本 → 1024 维向量 */
    float[] embed(String text);

    /** 批量文本 → 向量列表 */
    List<float[]> embedBatch(List<String> texts);
}
