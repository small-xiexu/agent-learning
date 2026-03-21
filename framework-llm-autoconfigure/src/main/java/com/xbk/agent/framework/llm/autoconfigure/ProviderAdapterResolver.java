package com.xbk.agent.framework.llm.autoconfigure;

import java.util.List;

/**
 * ProviderAdapter 解析器
 *
 * 职责：在启动阶段从已注册的 adapter 中，
 * 按 provider 标识解析出唯一匹配项，供自动装配层继续创建统一网关
 *
 * @author xiexu
 */
public class ProviderAdapterResolver {

    private final List<ProviderAdapter> adapters;

    /**
     * 创建解析器
     *
     * @param adapters 已注册 adapter
     */
    public ProviderAdapterResolver(List<ProviderAdapter> adapters) {
        this.adapters = adapters == null ? List.of() : List.copyOf(adapters);
    }

    /**
     * 返回指定 provider 的唯一 adapter
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
