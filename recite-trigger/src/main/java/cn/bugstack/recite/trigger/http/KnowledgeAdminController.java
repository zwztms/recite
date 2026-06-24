package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.knowledge.port.out.KnowledgeChunkPort;
import cn.bugstack.recite.domain.knowledge.port.out.KnowledgeChunkPort.DocSummary;
import cn.dev33.satoken.annotation.SaCheckRole;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理 API（管理员）.
 */
@RestController
@RequestMapping("/admin/knowledge")
@SaCheckRole("ADMIN")
@RequiredArgsConstructor
public class KnowledgeAdminController {

    private final KnowledgeChunkPort chunkPort;

    /** 文档列表 */
    @GetMapping("/chunks")
    public Response<List<DocSummary>> listDocuments() {
        return Response.ok(chunkPort.listDocuments());
    }

    /** 删除文档所有 chunks */
    @DeleteMapping("/chunks")
    public Response<?> deleteDocument(@RequestParam String docTitle) {
        chunkPort.deleteByDocTitle(docTitle);
        return Response.ok();
    }
}
