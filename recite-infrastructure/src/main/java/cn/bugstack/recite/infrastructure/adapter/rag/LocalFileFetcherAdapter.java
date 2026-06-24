package cn.bugstack.recite.infrastructure.adapter.rag;

import cn.bugstack.recite.domain.knowledge.port.out.DocumentFetcherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;

/**
 * 文档获取适配器 — 支持本地文件和 HTTP URL.
 */
@Slf4j
@Service
public class LocalFileFetcherAdapter implements DocumentFetcherPort {

    @Override
    public InputStream fetch(KnowledgeSource source) {
        try {
            String url = source.sourceUrl();
            if (url.startsWith("http://") || url.startsWith("https://")) {
                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(30_000);
                return conn.getInputStream();
            } else {
                // 本地文件路径
                File file = new File(url);
                if (!file.exists()) {
                    throw new FileNotFoundException("文件不存在: " + url);
                }
                return new FileInputStream(file);
            }
        } catch (IOException e) {
            log.error("获取文档失败: {}", source.sourceUrl(), e);
            throw new RuntimeException("文档获取失败: " + e.getMessage(), e);
        }
    }
}
