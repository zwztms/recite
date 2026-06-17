package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.IKnowledgeService;
import cn.bugstack.recite.api.dto.*;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.knowledge.exception.KnowledgeException;
import cn.bugstack.recite.domain.knowledge.model.entity.KnowledgeModuleEntity;
import cn.bugstack.recite.domain.knowledge.model.entity.QuestionEntity;
import cn.bugstack.recite.domain.knowledge.port.out.ModulePort;
import cn.bugstack.recite.domain.knowledge.port.out.QuestionPort;
import cn.bugstack.recite.domain.knowledge.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

/**
 * 知识库管理控制器.
 */
@RestController
@RequiredArgsConstructor
public class KnowledgeController implements IKnowledgeService {

    private final ModulePort modulePort;
    private final QuestionPort questionPort;
    private final KnowledgeService knowledgeService;

    // ==== 模块管理 ====

    @Override
    public Response<List<KnowledgeModuleDTO>> listModules() {
        List<KnowledgeModuleEntity> list = modulePort.listAll();
        List<KnowledgeModuleDTO> dtos = list.stream().map(m -> {
            KnowledgeModuleDTO d = new KnowledgeModuleDTO();
            d.setId(m.getId()); d.setModuleKey(m.getModuleKey());
            d.setModuleName(m.getModuleName()); d.setDescription(m.getDescription());
            d.setStatus(m.getStatus()); d.setSortOrder(m.getSortOrder());
            d.setQuestionCount(m.getQuestionCount());
            return d;
        }).toList();
        return Response.ok(dtos);
    }

    @Override
    public Response<Void> createModule(ModuleCreateRequestDTO request) {
        Optional<KnowledgeModuleEntity> exist = modulePort.findByKey(request.getModuleKey());
        if (exist.isPresent()) {
            throw new KnowledgeException("模块标识已存在");
        }
        KnowledgeModuleEntity entity = KnowledgeModuleEntity.builder()
                .moduleKey(request.getModuleKey()).moduleName(request.getModuleName())
                .description(request.getDescription())
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .status("ONLINE").questionCount(0).build();
        modulePort.save(entity);
        return Response.ok();
    }

    @Override
    public Response<Void> updateModuleStatus(String moduleKey, ModuleStatusRequestDTO request) {
        Optional<KnowledgeModuleEntity> exist = modulePort.findByKey(moduleKey);
        if (exist.isEmpty()) {
            throw new KnowledgeException("模块不存在");
        }
        modulePort.updateStatus(moduleKey, request.getStatus());
        return Response.ok();
    }

    @Override
    public Response<Void> deleteModule(String moduleKey) {
        modulePort.delete(moduleKey);
        return Response.ok();
    }

    // ==== 题目管理 ====

    @Override
    public Response<List<QuestionManageDTO>> listQuestions(String moduleKey) {
        var vos = questionPort.searchByModule(moduleKey, 500);
        var dtos = vos.stream().map(vo -> {
            QuestionEntity q = vo.question();
            QuestionManageDTO d = new QuestionManageDTO();
            d.setId(q.getId()); d.setQuestion(q.getQuestion()); d.setContent(q.getContent());
            d.setModuleKey(q.getModuleKey()); d.setCategory(q.getCategory());
            d.setDifficulty(q.getDifficulty());
            String tags = q.getTags();
            d.setTags(tags);
            d.setStatus(tags != null && tags.contains("status:OFFLINE") ? "OFFLINE" : "ONLINE");
            return d;
        }).toList();
        return Response.ok(dtos);
    }

    @Override
    public Response<Void> updateQuestion(String questionId, QuestionEditDTO request) {
        QuestionEntity q = questionPort.getById(questionId);
        if (q == null) {
            throw new KnowledgeException("题目不存在");
        }
        if (request.getQuestion() != null) q.setQuestion(request.getQuestion());
        if (request.getContent() != null) q.setContent(request.getContent());
        if (request.getModuleKey() != null) q.setModuleKey(request.getModuleKey());
        questionPort.update(q);
        return Response.ok();
    }

    // ==== 数据导入 ====

    @Override
    public Response<ImportResultDTO> triggerImport() {
        // 简化版：导入逻辑由 infra 层的 FileImportAdapter（后续实现）解析文件后调用 KnowledgeService
        ImportResultDTO result = new ImportResultDTO();
        result.setImported(0);
        result.setMessage("导入功能待实现 — 需配置 docs/import/ 目录并实现 FileImportAdapter");
        return Response.ok(result);
    }
}
