package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.LearnQuestionDTO;
import cn.bugstack.recite.api.dto.MarkRequestDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 翻卡学习 REST 接口 — 2 端点.
 */
@RequestMapping("/learn")
public interface ILearnService {

    /** 题目列表（含掌握状态） */
    @GetMapping("/questions")
    Response<List<LearnQuestionDTO>> questions(
            @RequestParam(required = false) String moduleKey,
            @RequestParam(defaultValue = "seq") String order,
            @RequestParam(defaultValue = "all") String filter
    );

    /** 标记掌握度 */
    @PostMapping("/mark")
    Response<Void> mark(@RequestBody MarkRequestDTO request);
}
