package cn.bugstack.recite.infrastructure.adapter.rag;

import cn.bugstack.recite.domain.knowledge.port.out.DocumentParserPort;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Apache Tika 统一文档解析适配器.
 * 支持 PDF/Word(docx)/HTML/Markdown/纯文本.
 */
@Slf4j
@Service
public class TikaDocumentParser implements DocumentParserPort {

    @Override
    public String parse(InputStream stream, String mimeType) {
        try {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // 无长度限制
            Metadata metadata = new Metadata();
            if (mimeType != null && !mimeType.isBlank()) {
                metadata.set(Metadata.CONTENT_TYPE, mimeType);
            }
            parser.parse(stream, handler, metadata, new ParseContext());
            return handler.toString();
        } catch (Exception e) {
            log.error("Tika 解析失败", e);
            throw new RuntimeException("文档解析失败: " + e.getMessage(), e);
        }
    }
}
