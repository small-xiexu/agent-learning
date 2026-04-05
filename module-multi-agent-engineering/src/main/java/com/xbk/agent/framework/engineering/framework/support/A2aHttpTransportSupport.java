package com.xbk.agent.framework.engineering.framework.support;

import com.fasterxml.jackson.core.type.TypeReference;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.JSONRPCMessage;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageRequest;
import io.a2a.spec.SendMessageResponse;
import io.a2a.util.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * A2A HTTP 传输支持。
 *
 * 职责：以标准 JSON-RPC over HTTP 的方式发送 A2A `message/send` 请求，
 * 规避 SDK 默认 JDK HTTP 客户端在本地 Provider 场景下的协议兼容问题。
 *
 * @author xiexu
 */
public final class A2aHttpTransportSupport {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final TypeReference<SendMessageResponse> SEND_MESSAGE_RESPONSE_TYPE = new TypeReference<SendMessageResponse>() {
    };

    private A2aHttpTransportSupport() {
    }

    /**
     * 发送 A2A `message/send` 请求并解析响应。
     *
     * @param agentUrl Provider URL
     * @param params A2A 消息参数
     * @return A2A 发送响应
     * @throws IOException HTTP 或 JSON 处理异常
     * @throws InterruptedException 线程中断异常
     */
    public static SendMessageResponse sendMessage(String agentUrl, MessageSendParams params)
            throws IOException, InterruptedException {
        SendMessageRequest request = new SendMessageRequest.Builder()
                .jsonrpc(JSONRPCMessage.JSONRPC_VERSION)
                .method(SendMessageRequest.METHOD)
                .params(params)
                .build();

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(normalizeAgentEndpoint(agentUrl))
                .version(HttpClient.Version.HTTP_1_1)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        Utils.OBJECT_MAPPER.writeValueAsString(request),
                        StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> httpResponse = HTTP_CLIENT.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            throw new IOException("Request failed " + httpResponse.statusCode() + ": " + httpResponse.body());
        }

        SendMessageResponse response = Utils.unmarshalFrom(httpResponse.body(), SEND_MESSAGE_RESPONSE_TYPE);
        JSONRPCError error = response.getError();
        if (error != null) {
            throw new IOException(error.getMessage());
        }
        return response;
    }

    /**
     * 规范化 Provider 端点 URI。
     *
     * <p>Nacos 发现出来的地址通常是 {@code http://host:port}，没有显式路径。
     * JDK HttpClient 在不同 Host 形态下对空路径的请求目标表现不一致，Tomcat 可能直接返回 400。
     * 因此这里统一把空路径补成根路径 {@code /}，确保始终调用 Provider 的 POST / 端点。
     *
     * @param agentUrl Provider URL
     * @return 可安全发送请求的标准化 URI
     */
    static URI normalizeAgentEndpoint(String agentUrl) {
        URI endpoint = URI.create(agentUrl);
        String rawPath = endpoint.getRawPath();
        if (rawPath != null && !rawPath.isEmpty()) {
            return endpoint;
        }
        try {
            return new URI(
                    endpoint.getScheme(),
                    endpoint.getRawUserInfo(),
                    endpoint.getHost(),
                    endpoint.getPort(),
                    "/",
                    endpoint.getRawQuery(),
                    endpoint.getRawFragment());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("非法的 Agent URL: " + agentUrl, ex);
        }
    }
}
