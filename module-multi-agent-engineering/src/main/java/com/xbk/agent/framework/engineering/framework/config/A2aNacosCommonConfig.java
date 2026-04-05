package com.xbk.agent.framework.engineering.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A2A + Nacos 公共配置。
 *
 * 职责：收敛框架版所需的 Nacos 注册发现参数和 A2A 超时/命名配置，
 * 让 Provider 和 Consumer 应用共享同一套属性约定，避免属性散落各处。
 *
 * <p>配置示例（application-a2a-nacos-local.yml）：
 * <pre>
 *   engineering:
 *     a2a:
 *       timeout: 30000
 *       agent:
 *         tech:
 *           name: tech-support-agent
 *         sales:
 *           name: sales-agent
 *         receptionist:
 *           name: receptionist-agent
 * </pre>
 *
 * <p>Nacos 连接参数（server-addr、namespace）已在 Java 代码中提供默认值，
 * 与 spring.cloud.nacos.discovery 保持一致，无需在 YAML 中重复配置。
 *
 * @author xiexu
 */
@ConfigurationProperties(prefix = "engineering.a2a")
public class A2aNacosCommonConfig {

    /**
     * A2A 调用超时时间（毫秒）。默认 30 秒。
     * 专家 Agent 生成回复可能较慢，需要适当加大超时。
     */
    private long timeout = 30_000L;

    /**
     * Nacos 相关配置。
     */
    private NacosConfig nacos = new NacosConfig();

    /**
     * Agent 命名配置。
     */
    private AgentNaming agent = new AgentNaming();

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public NacosConfig getNacos() {
        return nacos;
    }

    public void setNacos(NacosConfig nacos) {
        this.nacos = nacos;
    }

    public AgentNaming getAgent() {
        return agent;
    }

    public void setAgent(AgentNaming agent) {
        this.agent = agent;
    }

    /**
     * Nacos 连接与命名空间配置。
     */
    public static class NacosConfig {

        /**
         * Nacos 服务地址。Provider 启动时自动注册到这个地址的 Nacos 实例。
         */
        private String serverAddr = "127.0.0.1:8848";

        /**
         * Nacos 命名空间。用于隔离不同环境的 Agent 注册信息。
         */
        private String namespace = "engineering-a2a";

        public String getServerAddr() {
            return serverAddr;
        }

        public void setServerAddr(String serverAddr) {
            this.serverAddr = serverAddr;
        }

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }
    }

    /**
     * Agent 服务命名约定。
     *
     * <p>这套命名是框架版与手写版的关键对照点：
     * 手写版通过 targetAgentName 字段路由，框架版通过这套 Nacos 服务名发现 endpoint。
     * 两者使用不同机制，但决策出的"目标 Agent"语义完全一致。
     */
    public static class AgentNaming {

        /**
         * 技术专家 Agent 在 Nacos 的服务名。Consumer 通过这个名字查找 Provider。
         */
        private String techName = "tech-support-agent";

        /**
         * 销售顾问 Agent 在 Nacos 的服务名。
         */
        private String salesName = "sales-agent";

        /**
         * 接待员 Agent 在 Nacos 的服务名（Consumer 侧注册标识）。
         */
        private String receptionistName = "receptionist-agent";

        public String getTechName() {
            return techName;
        }

        public void setTechName(String techName) {
            this.techName = techName;
        }

        public String getSalesName() {
            return salesName;
        }

        public void setSalesName(String salesName) {
            this.salesName = salesName;
        }

        public String getReceptionistName() {
            return receptionistName;
        }

        public void setReceptionistName(String receptionistName) {
            this.receptionistName = receptionistName;
        }
    }
}
