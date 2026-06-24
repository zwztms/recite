package cn.bugstack.recite.infrastructure.adapter.rag;

import cn.bugstack.recite.domain.knowledge.port.out.ChunkerPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 结构感知分块器.
 * 按段落边界切分，500 字窗口 + 100 字重叠.
 * 识别 Markdown 标题作为自然边界.
 */
@Slf4j
@Service
public class StructureAwareChunker implements ChunkerPort {

    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 100;

    @Override
    public List<Chunk> chunk(String text, String docTitle) {
        if (text == null || text.isBlank()) return List.of();

        // 先按段落切分（空行或 Markdown 标题前）
        String[] paragraphs = text.split("\\n\\s*\\n");
        List<String> segments = new ArrayList<>();
        for (String p : paragraphs) {
            String trimmed = p.trim();
            if (trimmed.isEmpty()) continue;
            // 如果段落太长，按句子再切
            if (trimmed.length() > CHUNK_SIZE * 2) {
                segments.addAll(splitLongParagraph(trimmed));
            } else {
                segments.add(trimmed);
            }
        }

        // 滑动窗口合并
        List<Chunk> chunks = new ArrayList<>();
        StringBuilder window = new StringBuilder();
        int chunkIndex = 0;

        for (String seg : segments) {
            if (window.length() + seg.length() > CHUNK_SIZE && window.length() > 0) {
                chunks.add(new Chunk(window.toString().trim(), chunkIndex++, docTitle));
                // 保留最后 OVERLAP 字作为重叠
                String prev = window.toString();
                int cutStart = Math.max(0, prev.length() - OVERLAP);
                window = new StringBuilder(prev.substring(cutStart));
            }
            if (window.length() > 0) window.append("\n\n");
            window.append(seg);
        }
        // 收尾
        if (!window.isEmpty()) {
            chunks.add(new Chunk(window.toString().trim(), chunkIndex, docTitle));
        }

        log.debug("分块完成: {} 字符 → {} 块", text.length(), chunks.size());
        return chunks;
    }

    /** 长段落按句号/分号切分 */
    private List<String> splitLongParagraph(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(c);
            if ((c == '。' || c == '；' || c == '！' || c == '？') && sb.length() >= CHUNK_SIZE / 2) {
                parts.add(sb.toString().trim());
                sb.setLength(0);
            }
        }
        if (!sb.isEmpty()) parts.add(sb.toString().trim());
        return parts.isEmpty() ? List.of(text) : parts;
    }
}
