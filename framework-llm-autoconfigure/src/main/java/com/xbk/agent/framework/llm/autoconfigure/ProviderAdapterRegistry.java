package com.xbk.agent.framework.llm.autoconfigure;

import java.util.List;

/**
 * ProviderAdapterRegistry
 *
 * 职责：在已注册 adapter 中解析唯一匹配项
 *
 * @author xiexu
 */
public class ProviderAdapterRegistry {

    private final List<ProviderAdapter> adapters;

    public ProviderAdapterRegistry(List<ProviderAdapter> adapters) {
        this.adapters = adapters == null ? List.of() : List.copyOf(adapters);
    }

    /**
     * 返回指定 provider 的唯一 adapter。
     *
     * @param providerId provider 标识
     * @return 唯一匹配 adapter
     */
    public ProviderAdapter getRequiredAdapter(String providerId) {
        List<ProviderAdapter> matches = adapters.stream()
                .filter(adapter -> adapter.supports(providerId))
                .toList();
        if (matches.isEmpty()) {
            throw new IllegalStateException("No ProviderAdapter found for provider: " + providerId);
        }
        if (matches.size() > 1) {
            throw new IllegalStateException("Multiple ProviderAdapters found for provider: " + providerId);
        }
        return matches.getFirst();
    }
}
