package com.xbk.agent.framework.engineering.framework.client;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

import java.util.List;

/**
 * 专家 Agent 远端定位器。
 *
 * 职责：通过 Nacos 服务发现，把逻辑 Agent 名（如 "tech-support-agent"）
 * 转换成可访问的 HTTP URL（如 "http://192.168.1.100:8081"）。
 *
 * <p>这是框架版解决"发现问题"的核心类（对照设计文档第 9.4 节）：
 * Consumer 不需要硬编码 Provider 的 IP 和端口，而是通过服务名从 Nacos 查找。
 * Nacos 维护"服务名 → 可用实例列表"的动态映射，Consumer 只需要关心逻辑名。
 *
 * <p>与手写版的对照：
 * 手写版通过 MessageTopic 路由（targetTopic = "support.tech.request"），
 * 框架版通过 Nacos 服务名发现（serviceName = "tech-support-agent"）。
 * 两者都在 RoutingDecision 中记录了对应的目标，只是寻址机制不同。
 *
 * @author xiexu
 */
public class SpecialistRemoteAgentLocator {

    private final DiscoveryClient discoveryClient;

    /**
     * 创建定位器。
     *
     * @param discoveryClient Spring Cloud 服务发现客户端（Nacos 实现）
     */
    public SpecialistRemoteAgentLocator(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * 根据 Agent 服务名查找 Provider URL。
     *
     * <p>实现逻辑：
     * <ol>
     *   <li>向 Nacos 查询指定服务名的所有健康实例；
     *   <li>取第一个可用实例（生产环境可加入负载均衡策略）；
     *   <li>拼接 http://host:port 格式的 URL 返回给 Facade。
     * </ol>
     *
     * @param agentServiceName Nacos 中注册的服务名，如 "tech-support-agent"
     * @return Provider 的 HTTP URL
     * @throws IllegalStateException 找不到任何可用实例时抛出
     */
    public String findAgentUrl(String agentServiceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(agentServiceName);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException(
                    "No available instance found for agent service: " + agentServiceName
                            + ". Make sure the Provider application is running and registered to Nacos.");
        }
        ServiceInstance instance = instances.get(0);
        return "http://" + instance.getHost() + ":" + instance.getPort();
    }

    /**
     * 检查指定 Agent 服务是否有可用实例。
     *
     * @param agentServiceName 服务名
     * @return true 表示有可用实例
     */
    public boolean isAvailable(String agentServiceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(agentServiceName);
        return instances != null && !instances.isEmpty();
    }
}
