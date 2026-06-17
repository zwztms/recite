package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.*;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * 背诵 REST 接口 — 6 端点.
 */
@RequestMapping("/recite")
public interface IReciteService {

    /** 开始背诵 */
    @PostMapping("/start")
    Response<ReciteStartResultDTO> startRecite(@RequestBody ReciteStartRequestDTO request);

    /** 提交答案 → SSE 流式评分 */
    @PostMapping("/{sid}/answer")
    SseEmitter submitAnswer(@PathVariable String sid, @RequestBody SubmitAnswerRequestDTO request);

    /** 追问回答 */
    @PostMapping("/{sid}/followup")
    Response<String> submitFollowUp(@PathVariable String sid, @RequestBody FollowUpRequestDTO request);

    /** 结束背诵 */
    @PostMapping("/{sid}/finish")
    Response<SessionReportDTO> finishRecite(@PathVariable String sid);

    /** 查询会话状态 */
    @GetMapping("/{sid}")
    Response<ReciteSessionDTO> getSession(@PathVariable String sid);

    /** 查看历史 */
    @GetMapping("/history")
    Response<List<ReciteRecordDTO>> getHistory(@RequestParam(defaultValue = "20") int limit);
}
