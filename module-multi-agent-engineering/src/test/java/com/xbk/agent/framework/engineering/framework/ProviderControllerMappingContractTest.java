package com.xbk.agent.framework.engineering.framework;

import com.xbk.agent.framework.engineering.framework.config.SalesA2aServerConfig;
import com.xbk.agent.framework.engineering.framework.config.TechSupportA2aServerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provider 控制器映射契约测试。
 *
 * 职责：验证 A2A Provider 暴露出来的控制器类型能被 Spring MVC 识别为 handler，
 * 否则 `/.well-known/agent.json` 与 `POST /` 这些协议端点不会注册成功。
 *
 * @author xiexu
 */
class ProviderControllerMappingContractTest {

    private final ExposedRequestMappingHandlerMapping handlerMapping = new ExposedRequestMappingHandlerMapping();

    /**
     * 验证技术专家 Provider 控制器会被 Spring MVC 识别。
     */
    @Test
    void techSupportProviderControllerShouldBeRecognizedAsHandler() {
        assertTrue(handlerMapping.isHandlerType(TechSupportA2aServerConfig.TechSupportA2aController.class));
    }

    /**
     * 验证销售顾问 Provider 控制器会被 Spring MVC 识别。
     */
    @Test
    void salesProviderControllerShouldBeRecognizedAsHandler() {
        assertTrue(handlerMapping.isHandlerType(SalesA2aServerConfig.SalesA2aController.class));
    }

    /**
     * 暴露 Spring MVC 的 handler 判定逻辑，便于验证控制器契约。
     *
     * @author xiexu
     */
    private static class ExposedRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

        /**
         * 判断给定类型是否会被注册为 Spring MVC handler。
         *
         * @param beanType 控制器类型
         * @return true 表示会被识别为 handler
         */
        boolean isHandlerType(Class<?> beanType) {
            return super.isHandler(beanType);
        }
    }
}
