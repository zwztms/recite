package cn.bugstack.recite.domain.recite.service;

import cn.bugstack.recite.domain.recite.port.out.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 记忆压缩器 — LLM 将旧窗口压缩为薄弱点摘要.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryCompressor {

    private final LlmPort llmPort;

    /** 压缩单条旧记录为摘要（≤100字） */
    public String compress(Object entry) {
        String text = entry.toString();
        String prompt = String.format("""
                将以下背诵记录压缩为一段简短的薄弱点摘要（≤100字），
                只保留反复出错的概念和关键遗漏点，丢弃已经掌握的。

                %s

                输出纯文本摘要，不要 JSON。
                """, text);

        try {
            return llmPort.compress(prompt);
        } catch (Exception e) {
            log.warn("LLM压缩失败, 降级为截断: {}", e.getMessage());
            return text.length() > 100 ? text.substring(0, 100) : text;
        }
    }
}
