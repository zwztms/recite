package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.*;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库 REST 接口.
 */
@RequestMapping("/admin/knowledge")
public interface IKnowledgeService {

    // ==== 模块管理 ====

    @GetMapping("/modules")
    Response<List<KnowledgeModuleDTO>> listModules();

    @PostMapping("/modules")
    Response<Void> createModule(@RequestBody ModuleCreateRequestDTO request);

    @PutMapping("/modules/{moduleKey}/status")
    Response<Void> updateModuleStatus(@PathVariable String moduleKey,
                                      @RequestBody ModuleStatusRequestDTO request);

    @DeleteMapping("/modules/{moduleKey}")
    Response<Void> deleteModule(@PathVariable String moduleKey);

    // ==== 题目管理 ====

    @GetMapping("/modules/{moduleKey}/questions")
    Response<List<QuestionManageDTO>> listQuestions(@PathVariable String moduleKey);

    @PutMapping("/questions/{questionId}")
    Response<Void> updateQuestion(@PathVariable String questionId,
                                  @RequestBody QuestionEditDTO request);

    // ==== 数据导入 ====

    @PostMapping("/import")
    Response<ImportResultDTO> triggerImport();
}
