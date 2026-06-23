package cn.bugstack.recite.trigger.http;

import cn.bugstack.recite.api.ILearnService;
import cn.bugstack.recite.api.dto.LearnQuestionDTO;
import cn.bugstack.recite.api.dto.MarkRequestDTO;
import cn.bugstack.recite.api.response.Response;
import cn.bugstack.recite.domain.learn.service.LearnService;
import cn.bugstack.recite.trigger.config.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 翻卡学习控制器 — 实现 ILearnService 2 端点.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LearnController implements ILearnService {

    private final LearnService learnService;

    @Override
    public Response<List<LearnQuestionDTO>> questions(String moduleKey, String order, String filter) {
        Long userId = UserContext.getUserId();
        List<LearnQuestionDTO> dtos = learnService.getQuestions(userId, moduleKey, order, filter)
                .stream()
                .map(vo -> new LearnQuestionDTO(
                        vo.id(), vo.question(), vo.content(),
                        vo.moduleKey(), vo.moduleName(),
                        vo.category(), vo.tags(),
                        vo.difficulty(), vo.mastered()))
                .toList();
        return Response.ok(dtos);
    }

    @Override
    public Response<Void> mark(MarkRequestDTO request) {
        Long userId = UserContext.getUserId();
        learnService.mark(userId, request.getQuestionId(), request.isMastered());
        return Response.ok();
    }
}
