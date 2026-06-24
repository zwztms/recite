package cn.bugstack.recite.domain.knowledge.service;

import cn.bugstack.recite.domain.knowledge.port.out.*;
import cn.bugstack.recite.domain.knowledge.port.out.DocumentFetcherPort.KnowledgeSource;
import cn.bugstack.recite.domain.knowledge.port.out.ChunkerPort.Chunk;
import cn.bugstack.recite.domain.knowledge.port.out.ChunkEnricherPort.EnrichedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * 6 节点知识入库管线编排.
 * <p>
 * 流程: Fetcher → Parser → Chunker → Enricher → Embedder → Indexer
 * 通过 Consumer<String> 回调推送 SSE 进度.
 * 对标 Ragent 的 6 节点 Pipeline.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgePipelineService {

    private final DocumentFetcherPort fetcher;
    private final DocumentParserPort parser;
    private final ChunkerPort chunker;
    private final ChunkEnricherPort enricher;
    private final EmbeddingPort embeddingPort;
    private final KnowledgeChunkPort chunkPort;

    /** 执行完整入库管线，progress 回调推送进度描述 */
    public PipelineResult execute(KnowledgeSource source, Consumer<String> progress) {
        String docTitle = source.title();
        progress.accept("开始获取文档: " + docTitle);

        // [1] Fetcher —— 获取文档流
        InputStream stream = fetcher.fetch(source);
        progress.accept("文档获取完成，开始解析...");

        // [2] Parser —— 解析为纯文本
        String rawText = parser.parse(stream, source.mimeType());
        progress.accept("解析完成 (" + rawText.length() + " 字符)，开始分块...");

        // [3] Chunker —— 结构感知分块
        List<Chunk> chunks = chunker.chunk(rawText, docTitle);
        progress.accept("分块完成 (" + chunks.size() + " 块)，开始富化...");

        // [4] Enricher —— LLM 生成上下文摘要
        List<EnrichedChunk> enriched = enricher.enrich(chunks);
        progress.accept("富化完成，开始向量化...");

        int indexed = 0;
        for (EnrichedChunk ec : enriched) {
            // [5] Embedder —— 向量化
            float[] embedding = embeddingPort.embed(ec.getFullText());
            // [6] Indexer —— 写入 pgvector
            chunkPort.insert(docTitle, source.sourceUrl(), ec, embedding);
            indexed++;
            if (indexed % 10 == 0) {
                progress.accept("向量化入库: " + indexed + "/" + enriched.size());
            }
        }
        progress.accept("入库完成，共 " + indexed + " 条 chunks");

        return new PipelineResult(docTitle, indexed);
    }

    public record PipelineResult(String docTitle, int chunkCount) {}
}
