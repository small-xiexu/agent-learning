package com.xbk.agent.framework.engineering.framework.web;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.framework.agent.FrameworkReceptionistService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 接待员 Consumer HTTP 控制器。
 *
 * 职责：暴露用户请求入口，把 `/engineering/handle` 的 HTTP 请求委托给
 * FrameworkReceptionistService，复用现有 A2A 路由与结果组装能力。
 *
 * @author xiexu
 */
@Profile("a2a-receptionist-consumer")
@RestController
@RequestMapping("/engineering")
public class ReceptionistHttpController {

    private final FrameworkReceptionistService receptionistService;

    /**
     * 创建接待员 HTTP 控制器。
     *
     * @param receptionistService 框架版接待员服务
     */
    public ReceptionistHttpController(FrameworkReceptionistService receptionistService) {
        this.receptionistService = receptionistService;
    }

    /**
     * 处理用户通过 HTTP 发起的工程咨询请求。
     *
     * @param request HTTP 请求体
     * @return 接待员处理后的统一运行结果
     */
    @PostMapping("/handle")
    public EngineeringRunResult handle(@Valid @RequestBody EngineeringHandleRequest request) {
        return receptionistService.handle(request.getRequest());
    }
}
