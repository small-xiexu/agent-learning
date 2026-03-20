package com.xbk.agent.framework.llm.autoconfigure;

import com.xbk.agent.framework.core.llm.AgentLlmGateway;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ProviderAdapterRegistry 测试
 *
 * 职责：验证注册表能按 provider 标识解析唯一 adapter，并对异常情况给出明确失败
 *
 * @author xiexu
 */
class ProviderAdapterRegistryTest {

    /**
     * 验证存在唯一匹配时返回对应 adapter。
     */
    @Test
    void shouldReturnMatchingAdapterWhenExactlyOneAdapterSupportsProvider() {
        TestProviderAdapter openAiAdapter = new TestProviderAdapter("openai-compatible");
        ProviderAdapterRegistry registry = new ProviderAdapterRegistry(List.of(
                openAiAdapter,
                new TestProviderAdapter("dashscope")));

        ProviderAdapter adapter = registry.getRequiredAdapter("openai-compatible");

        assertSame(openAiAdapter, adapter);
    }

    /**
     * 验证没有匹配时抛出清晰异常。
     */
    @Test
    void shouldThrowWhenNoAdapterMatchesProvider() {
        ProviderAdapterRegistry registry = new ProviderAdapterRegistry(List.of(new TestProviderAdapter("dashscope")));

        assertThrows(IllegalStateException.class, () -> registry.getRequiredAdapter("openai-compatible"));
    }

    /**
     * 验证有多个匹配时抛出清晰异常。
     */
    @Test
    void shouldThrowWhenMultipleAdaptersMatchProvider() {
        ProviderAdapterRegistry registry = new ProviderAdapterRegistry(List.of(
                new TestProviderAdapter("openai-compatible"),
                new TestProviderAdapter("openai-compatible")));

        assertThrows(IllegalStateException.class, () -> registry.getRequiredAdapter("openai-compatible"));
    }

    /**
     * 测试 adapter。
     *
     * 职责：按固定 provider 标识返回匹配结果
     *
     * @author xiexu
     */
    private static final class TestProviderAdapter implements ProviderAdapter {

        private final String providerId;

        private TestProviderAdapter(String providerId) {
            this.providerId = providerId;
        }

        @Override
        public boolean supports(String providerId) {
            return this.providerId.equals(providerId);
        }

        @Override
        public AgentLlmGateway createGateway(LlmProperties properties) {
            throw new UnsupportedOperationException("test adapter does not create gateway");
        }
    }
}
