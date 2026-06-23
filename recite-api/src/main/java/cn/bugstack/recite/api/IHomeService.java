package cn.bugstack.recite.api;

import cn.bugstack.recite.api.dto.HomeDashboardDTO;
import cn.bugstack.recite.api.response.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 个人主页 REST 接口 — 1 端点.
 */
@RequestMapping("/home")
public interface IHomeService {

    /** 个人主页仪表盘聚合数据 */
    @GetMapping("/dashboard")
    Response<HomeDashboardDTO> dashboard();
}
