package cn.bugstack.recite.infrastructure.adapter.imports;

import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.model.valueobj.ImportResultVO;
import cn.bugstack.recite.domain.knowledge.port.out.EmbeddingPort;
import cn.bugstack.recite.domain.knowledge.port.out.FileImportPort;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.knowledge.service.KnowledgeService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件导入适配器 — 扫描 docs/import/ → 解析 JSON/MD → embedding → 入库 → 归档.
 */
@Slf4j
@Service
public class FileImportAdapter implements FileImportPort {

    private final Path importDir;
    private final Path backupDir;
    private final KnowledgeService knowledgeService;
    private final ModulePort modulePort;
    private final QuestionPort questionPort;
    private final EmbeddingPort embeddingPort;
    private final Gson gson = new Gson();
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public FileImportAdapter(
            @Value("${recite.import.dir:docs/import}") String importPath,
            KnowledgeService knowledgeService,
            ModulePort modulePort,
            QuestionPort questionPort,
            EmbeddingPort embeddingPort) {
        this.importDir = Path.of(importPath).toAbsolutePath();
        this.backupDir = this.importDir.resolve("backup");
        this.knowledgeService = knowledgeService;
        this.modulePort = modulePort;
        this.questionPort = questionPort;
        this.embeddingPort = embeddingPort;
    }

    /**
     * 执行导入 — 扫描、解析、入库、归档.
     */
    public ImportResultVO doImport() {
        ImportResultVO result = new ImportResultVO();
        result.setErrors(new ArrayList<>());

        if (!Files.isDirectory(importDir)) {
            String msg = "导入目录不存在: " + importDir;
            log.warn(msg);
            result.setMessage(msg);
            return result;
        }

        // 确保 backup 目录存在
        try {
            Files.createDirectories(backupDir);
        } catch (IOException e) {
            result.setMessage("无法创建 backup 目录: " + e.getMessage());
            return result;
        }

        List<QuestionEntity> allQuestions = new ArrayList<>();

        try (var stream = Files.newDirectoryStream(importDir, "*.{json,md}")) {
            for (Path file : stream) {
                String fileName = file.getFileName().toString();
                try {
                    List<QuestionEntity> parsed;
                    if (fileName.endsWith(".json")) {
                        parsed = parseJson(file);
                    } else {
                        parsed = parseMarkdown(file);
                    }
                    allQuestions.addAll(parsed);
                    // 入库后归档
                    archiveFile(file);
                    log.info("已导入并归档: {} ({} 题)", fileName, parsed.size());
                } catch (Exception e) {
                    log.error("导入文件失败: {} — {}", fileName, e.getMessage(), e);
                    result.getErrors().add(fileName + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            result.setMessage("扫描导入目录失败: " + e.getMessage());
            return result;
        }

        if (allQuestions.isEmpty()) {
            result.setMessage("未发现可导入的文件（请将 .json 或 .md 放入 " + importDir + "）");
            return result;
        }

        // 逐模块注册（不存在则自动创建）
        allQuestions.stream()
                .map(QuestionEntity::getModuleKey)
                .distinct()
                .forEach(key -> {
                    if (modulePort.findByKey(key).isEmpty()) {
                        var m = cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity.builder()
                                .moduleKey(key).moduleName(key).description("自动创建")
                                .status("ONLINE").sortOrder(0).questionCount(0)
                                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
                        modulePort.save(m);
                    }
                });

        int count = knowledgeService.importQuestions(allQuestions);
        result.setImported(count);
        result.setMessage("成功导入 " + count + " 题");
        return result;
    }

    // ---- 文件解析 ----

    private List<QuestionEntity> parseJson(Path file) throws IOException {
        String content = Files.readString(file);
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> rawList = gson.fromJson(content, listType);

        List<QuestionEntity> list = new ArrayList<>();
        for (Map<String, Object> item : rawList) {
            QuestionEntity q = QuestionEntity.builder()
                    .question(str(item, "question"))
                    .content(str(item, "content"))
                    .moduleKey(str(item, "module_key") != null ? str(item, "module_key") : str(item, "moduleKey"))
                    .category(str(item, "category"))
                    .tags(str(item, "tags"))
                    .difficulty(intVal(item, "difficulty", 1))
                    .build();
            list.add(q);
        }
        return list;
    }

    private List<QuestionEntity> parseMarkdown(Path file) throws IOException {
        String content = Files.readString(file);
        String moduleKey = deriveModuleKey(file.getFileName().toString());

        // 按 ## 标题分段
        String[] sections = content.split("\n## ");
        List<QuestionEntity> list = new ArrayList<>();

        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();
            if (section.isEmpty()) continue;

            // 首行（标题行）如果以 ## 开头则去掉
            String title = section;
            String body = "";
            int newlineIdx = section.indexOf('\n');
            if (newlineIdx > 0) {
                title = section.substring(0, newlineIdx).trim();
                if (title.startsWith("#")) {
                    title = title.replaceAll("^#+\\s*", "");
                }
                body = section.substring(newlineIdx + 1).trim();
            }

            if (title.isEmpty()) continue;

            int difficulty = estimateDifficulty(body);
            QuestionEntity q = QuestionEntity.builder()
                    .question(title)
                    .content(body)
                    .moduleKey(moduleKey)
                    .difficulty(difficulty)
                    .build();
            list.add(q);
        }
        return list;
    }

    private String deriveModuleKey(String fileName) {
        // "Java基础.md" → "java-basics"
        String base = fileName.replaceAll("\\.(md|json)$", "");
        // 简单拼音映射：直接用小写 + 下划线
        return base.toLowerCase().replaceAll("\\s+", "-").replaceAll("[^a-z0-9\\-]", "");
    }

    private int estimateDifficulty(String content) {
        if (content == null) return 1;
        int len = content.length();
        if (len < 200) return 1;
        if (len < 800) return 2;
        if (len < 2000) return 3;
        if (len < 5000) return 4;
        return 5;
    }

    // ---- 辅助 ----

    private void archiveFile(Path file) throws IOException {
        String name = file.getFileName().toString();
        String stamped = name + "_" + LocalDateTime.now().format(TS);
        Files.move(file, backupDir.resolve(stamped), StandardCopyOption.REPLACE_EXISTING);
    }

    private String str(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private int intVal(Map<String, Object> map, String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v != null) {
            try { return Integer.parseInt(v.toString()); } catch (NumberFormatException ignored) {}
        }
        return def;
    }
}
