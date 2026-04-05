package com.xbk.agent.framework.engineering.framework.support;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A2A HTTP 端点规范化测试。
 *
 * 职责：验证传输层会把没有显式路径的 Provider URL 规范为根路径 `/`，
 * 避免 JDK HttpClient 在某些 Host 场景下触发 Tomcat 400。
 *
 * @author xiexu
 */
class A2aHttpTransportSupportNormalizationTest {

    /**
     * 缺省根路径时，应该补上 `/`。
     */
    @Test
    void shouldAppendRootPathWhenAgentUrlHasNoExplicitPath() {
        URI normalized = A2aHttpTransportSupport.normalizeAgentEndpoint("http://192.168.124.4:8081");

        assertEquals("http://192.168.124.4:8081/", normalized.toString());
    }

    /**
     * 已有显式路径时，应该保持原样。
     */
    @Test
    void shouldKeepExplicitPathUnchanged() {
        URI normalized = A2aHttpTransportSupport.normalizeAgentEndpoint("http://192.168.124.4:8081/a2a");

        assertEquals("http://192.168.124.4:8081/a2a", normalized.toString());
    }
}
