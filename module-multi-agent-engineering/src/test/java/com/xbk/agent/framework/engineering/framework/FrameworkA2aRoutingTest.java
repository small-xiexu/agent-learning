package com.xbk.agent.framework.engineering.framework;

import com.xbk.agent.framework.engineering.api.EngineeringRunResult;
import com.xbk.agent.framework.engineering.domain.routing.CustomerIntentType;
import com.xbk.agent.framework.engineering.domain.routing.SpecialistType;
import com.xbk.agent.framework.engineering.framework.agent.FrameworkReceptionistService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 框架版 A2A 完整路由集成测试。
 *
 * <p>本测试需要真实的外部基础设施（Nacos + 两个 Provider 应用），
 * 因此通过系统属性 {@code a2a.integration=true} 手动激活。
 *
 * <p>激活方式：
 * <pre>
 *   mvn test -Da2a.integration=true -pl module-multi-agent-engineering
 * </pre>
 *
 * <p>前置条件：
 * <ol>
 *   <li>Nacos Server 已启动（127.0.0.1:8848）；
 *   <li>TechSupportProviderApplication 以 a2a-tech-provider,a2a-nacos-local profile 运行；
 *   <li>SalesProviderApplication 以 a2a-sales-provider,a2a-nacos-local profile 运行；
 *   <li>应用 properties 中补充真实 LLM API Key。
 * </ol>
 *
 * <p>如未满足前置条件，本测试会自动跳过，不影响 CI 正常运行。
 *
 * @author xiexu
 */
@SpringBootTest(classes = com.xbk.agent.framework.engineering.framework.app.ReceptionistConsumerApplication.class)
@ActiveProfiles({"a2a-receptionist-consumer", "a2a-nacos-local", "llm-local"})
@EnabledIfSystemProperty(named = "a2a.integration", matches = "true")
class FrameworkA2aRoutingTest {

    @Autowired
    private FrameworkReceptionistService receptionistService;

    /**
     * 集成验证：技术问题经 Nacos 发现后路由到真实技术专家 Provider。
     */
    @Test
    void shouldRouteTechQuestionToRemoteTechProvider() {
        EngineeringRunResult result = receptionistService.handle("我的服务启动时报 NullPointerException，帮我排查。");

        assertEquals(CustomerIntentType.TECH_SUPPORT, result.getIntentType());
        assertEquals(SpecialistType.TECH_SUPPORT, result.getSpecialistType());
        assertNotNull(result.getSpecialistResponse());
        assertNotNull(result.getFinalResponse());
    }

    /**
     * 集成验证：销售问题经 Nacos 发现后路由到真实销售顾问 Provider。
     */
    @Test
    void shouldRouteSalesQuestionToRemoteSalesProvider() {
        EngineeringRunResult result = receptionistService.handle("我想了解企业版购买方案和报价。");

        assertEquals(CustomerIntentType.SALES_CONSULTING, result.getIntentType());
        assertEquals(SpecialistType.SALES, result.getSpecialistType());
        assertNotNull(result.getSpecialistResponse());
    }
}
