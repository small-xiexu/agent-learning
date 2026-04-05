package com.xbk.agent.framework.engineering.framework;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.xbk.agent.framework.engineering.framework.support.A2aHttpTransportSupport;
import com.xbk.agent.framework.engineering.framework.support.A2aResponseExtractor;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A2A HTTP 传输支持测试。
 *
 * 职责：验证 Consumer 侧自定义 A2A 传输层会按 JSON-RPC 契约发送 `message/send`
 * 请求，并能把 Provider 返回的 Task 结果解析成 SendMessageResponse。
 *
 * @author xiexu
 */
class A2aHttpTransportSupportTest {

    /**
     * 验证 A2A HTTP 传输层会发送标准 JSON-RPC 请求并正确解析响应。
     *
     * @throws Exception 本地 HTTP Server 或传输过程异常
     */
    @Test
    void shouldSendJsonRpcMessageAndParseTaskResponse() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>("");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> writeSuccessResponse(exchange, requestBody));
        server.start();

        try {
            int port = server.getAddress().getPort();
            Message message = new Message(
                    Message.Role.USER,
                    List.of(new TextPart("测试请求")),
                    "msg-test",
                    "ctx-test",
                    null,
                    null,
                    null);
            MessageSendParams params = new MessageSendParams(message, null, null);

            SendMessageResponse response = A2aHttpTransportSupport.sendMessage("http://127.0.0.1:" + port, params);

            assertEquals("传输层测试答复", A2aResponseExtractor.extractText(response));
            assertTrue(requestBody.get().contains("\"method\":\"message/send\""));
            assertTrue(requestBody.get().contains("\"kind\":\"message\""));
            assertTrue(requestBody.get().contains("\"text\":\"测试请求\""));
        }
        finally {
            server.stop(0);
        }
    }

    /**
     * 向测试客户端返回一个已完成的 A2A Task 响应。
     *
     * @param exchange HTTP 交换对象
     * @param requestBody 请求体记录器
     * @throws IOException 响应写出异常
     */
    private void writeSuccessResponse(HttpExchange exchange, AtomicReference<String> requestBody) throws IOException {
        requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        String responseJson = "{\"jsonrpc\":\"2.0\",\"id\":\"test-response\",\"result\":{\"kind\":\"task\",\"id\":\"task-1\",\"contextId\":\"ctx-test\",\"status\":{\"state\":\"completed\"},\"artifacts\":[{\"artifactId\":\"artifact-1\",\"parts\":[{\"kind\":\"text\",\"text\":\"传输层测试答复\"}]}]}}";
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
