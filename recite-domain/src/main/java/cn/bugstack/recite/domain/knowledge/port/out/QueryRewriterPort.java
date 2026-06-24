package cn.bugstack.recite.domain.knowledge.port.out;

import java.util.ArrayList;
import java.util.List;

/**
 * 查询改写 SPI — 口语化回答 → 精确技术检索查询.
 * 对标 Ragent rewriteQuery.
 */
public interface QueryRewriterPort {

    RewriteResult rewrite(String userAnswer, String questionTitle, String moduleKey);

    record RewriteResult(String rewrittenQuery, List<String> subQueries) {
        /** 合并所有 query 为一个列表（主 query + 子 query） */
        public List<String> allQueries() {
            List<String> all = new ArrayList<>();
            all.add(rewrittenQuery);
            all.addAll(subQueries);
            return all;
        }
    }
}
