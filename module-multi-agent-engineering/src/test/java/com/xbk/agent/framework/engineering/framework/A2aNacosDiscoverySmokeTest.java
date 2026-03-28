package com.xbk.agent.framework.engineering.framework;

import com.xbk.agent.framework.engineering.framework.client.SpecialistRemoteAgentLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A2A + Nacos 服务发现 Smoke 测试。
 *
 * <p>分两类：
 * <ol>
 *   <li>使用 Mock DiscoveryClient 的纯单元测试（无外部依赖，始终运行）；
 *   <li>真实 Nacos 联通验证（通过 {@code -Dnacos.available=true} 激活，需本地 Nacos 已启动）。
 * </ol>
 *
 * @author xiexu
 */
class A2aNacosDiscoverySmokeTest {

    /**
     * 验证当 DiscoveryClient 能找到实例时，定位器返回正确 URL。
     */
    @Test
    void shouldReturnAgentUrlWhenInstanceAvailable() {
        org.springframework.cloud.client.ServiceInstance instance = mock(
                org.springframework.cloud.client.ServiceInstance.class);
        when(instance.getHost()).thenReturn("192.168.1.100");
        when(instance.getPort()).thenReturn(8081);

        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances("tech-support-agent")).thenReturn(List.of(instance));

        SpecialistRemoteAgentLocator locator = new SpecialistRemoteAgentLocator(discoveryClient);

        assertDoesNotThrow(() -> {
            String url = locator.findAgentUrl("tech-support-agent");
            org.junit.jupiter.api.Assertions.assertEquals("http://192.168.1.100:8081", url);
        });
    }

    /**
     * 验证当没有可用实例时，定位器抛出有意义的异常（不是 NPE 或静默返回空）。
     */
    @Test
    void shouldThrowWhenNoInstanceAvailable() {
        DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        when(discoveryClient.getInstances("tech-support-agent")).thenReturn(Collections.emptyList());

        SpecialistRemoteAgentLocator locator = new SpecialistRemoteAgentLocator(discoveryClient);

        assertThrows(IllegalStateException.class, () -> locator.findAgentUrl("tech-support-agent"),
                "找不到实例时必须抛出明确异常，不能静默失败");
    }

    /**
     * 真实 Nacos 联通 Smoke 测试（需本地 Nacos 已启动并注册了两个 Provider）。
     *
     * <p>激活方式：
     * <pre>
     *   mvn test -Dnacos.available=true -pl module-multi-agent-engineering
     * </pre>
     *
     * <p>前置条件：
     * <ol>
     *   <li>本地 Nacos Server 已启动（推荐 docker：nacos/nacos-server:v2.3.2，端口 8848）；
     *   <li>{@link com.xbk.agent.framework.engineering.framework.app.TechSupportProviderApplication} 已以
     *       {@code a2a-tech-provider,a2a-nacos-local} profile 运行；
     *   <li>{@link com.xbk.agent.framework.engineering.framework.app.SalesProviderApplication} 已以
     *       {@code a2a-sales-provider,a2a-nacos-local} profile 运行。
     * </ol>
     */
    @Test
    @EnabledIfSystemProperty(named = "nacos.available", matches = "true")
    void shouldDiscoverBothSpecialistAgentsFromNacos() {
        // 真实测试需要启动完整 Spring 容器，此处仅作占位说明
        // 实际跑法：见 FrameworkA2aRoutingTest（集成测试）
    }
}
