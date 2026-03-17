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

    private final String modelName;
    private final Double temperature;
    private final Double topP;
    private final Integer maxTokens;
    private final List<String> stopSequences;
    private final Duration timeout;
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

    /**
     * 返回模型名称
     *
     * @return 模型名称
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * 返回温度参数
     *
     * @return 温度参数
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     * 返回 topP 参数
     *
     * @return topP 参数
     */
    public Double getTopP() {
        return topP;
    }

    /**
     * 返回最大令牌数
     *
     * @return 最大令牌数
     */
    public Integer getMaxTokens() {
        return maxTokens;
    }

    /**
     * 返回停止序列
     *
     * @return 停止序列
     */
    public List<String> getStopSequences() {
        return stopSequences;
    }

    /**
     * 返回超时时间
     *
     * @return 超时时间
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * 返回厂商提示信息
     *
     * @return 厂商提示信息
     */
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
