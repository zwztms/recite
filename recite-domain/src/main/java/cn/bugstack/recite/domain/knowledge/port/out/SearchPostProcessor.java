package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.List;

/**
 * 检索后处理器统一接口.
 * 对标 Ragent SearchResultPostProcessor: getOrder()/isEnabled()/process().
 * 实现类注册为 Spring Bean 即自动加入管线.
 */
public interface SearchPostProcessor {

    /** 处理器名称 */
    String getName();

    /** 执行顺序（数字越小越先执行，去重=2, MMR=3, Rerank=10） */
    int getOrder();

    /** 是否启用 */
    default boolean isEnabled() { return true; }

    /** 处理候选列表，返回处理后的列表 */
    List<String> process(List<String> candidates, String query, PostProcessContext ctx);

    record PostProcessContext(float[] queryEmbedding) {}
}
