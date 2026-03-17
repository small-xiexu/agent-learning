package com.xbk.agent.framework.core.llm.option;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型选项
 *
 * 职责：定义同步或流式调用的基础模型参数
 *
 * @author xiexu
 */
public final class ModelOptions {

    /**
     * 期望调用的模型名称，不指定时由底层适配器决定默认模型。
     */
    private final String modelName;
    /**
     * 温度参数，控制输出随机性与发散程度。
     */
    private final Double temperature;
    /**
     * topP 参数，控制采样概率分布截断范围。
     */
    private final Double topP;
    /**
     * 单次响应允许生成的最大 token 数。
     */
    private final Integer maxTokens;
    /**
     * 触发停止生成的序列列表。
     */
    private final List<String> stopSequences;
    /**
     * 模型调用超时时间。
     */
    private final Duration timeout;
    /**
     * 面向特定模型厂商的扩展提示，不应替代统一协议字段。
     */
    private final Map<String, Object> providerHints;

    /**
     * 使用构建器创建模型选项
     *
     * @param builder 构建器
     */
    private ModelOptions(Builder builder) {
        this.modelName = builder.modelName;
        this.temperature = builder.temperature;
        this.topP = builder.topP;
        this.maxTokens = builder.maxTokens;
        this.stopSequences = List.copyOf(builder.stopSequences);
        this.timeout = builder.timeout;
        this.providerHints = Collections.unmodifiableMap(new LinkedHashMap<String, Object>(builder.providerHints));
    }

    /**
     * 创建构建器
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getModelName() {
        return modelName;
    }

    public Double getTemperature() {
        return temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public List<String> getStopSequences() {
        return stopSequences;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Map<String, Object> getProviderHints() {
        return providerHints;
    }

    /**
     * 模型选项构建器
     *
     * 职责：组装不可变模型选项
     *
     * @author xiexu
     */
    public static final class Builder {

        private String modelName;
        private Double temperature;
        private Double topP;
        private Integer maxTokens;
        private List<String> stopSequences = Collections.emptyList();
        private Duration timeout;
        private Map<String, Object> providerHints = Collections.emptyMap();

        /**
         * 设置模型名称
         *
         * @param modelName 模型名称
         * @return 构建器
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        /**
         * 设置温度参数
         *
         * @param temperature 温度参数
         * @return 构建器
         */
        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        /**
         * 设置 topP 参数
         *
         * @param topP topP 参数
         * @return 构建器
         */
        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        /**
         * 设置最大令牌数
         *
         * @param maxTokens 最大令牌数
         * @return 构建器
         */
        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        /**
         * 设置停止序列
         *
         * @param stopSequences 停止序列
         * @return 构建器
         */
        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences == null ? Collections.<String>emptyList() : stopSequences;
            return this;
        }

        /**
         * 设置超时时间
         *
         * @param timeout 超时时间
         * @return 构建器
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * 设置厂商提示信息
         *
         * @param providerHints 厂商提示信息
         * @return 构建器
         */
        public Builder providerHints(Map<String, Object> providerHints) {
            this.providerHints = providerHints == null ? Collections.<String, Object>emptyMap() : providerHints;
            return this;
        }

        /**
         * 构建模型选项
         *
         * @return 模型选项
         */
        public ModelOptions build() {
            return new ModelOptions(this);
        }
    }
}
