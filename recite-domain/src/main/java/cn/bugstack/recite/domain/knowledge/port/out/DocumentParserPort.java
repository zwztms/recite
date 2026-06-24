package cn.bugstack.recite.domain.knowledge.port.out;

import java.io.InputStream;

/**
 * 文档解析 SPI — 将文档流解析为纯文本.
 */
public interface DocumentParserPort {

    /**
     * @param stream   文档输入流
     * @param mimeType MIME 类型提示（可为 null，解析器自动检测）
     * @return 纯文本内容
     */
    String parse(InputStream stream, String mimeType);
}
