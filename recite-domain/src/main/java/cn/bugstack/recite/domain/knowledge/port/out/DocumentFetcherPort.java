package cn.bugstack.recite.domain.knowledge.port.out;

import java.io.InputStream;

/**
 * 文档获取 SPI — 支持本地文件和 HTTP URL.
 * 返回 InputStream，由 infra 层关闭.
 */
public interface DocumentFetcherPort {

    InputStream fetch(KnowledgeSource source);

    record KnowledgeSource(String title, String sourceUrl, String mimeType) {}
}
