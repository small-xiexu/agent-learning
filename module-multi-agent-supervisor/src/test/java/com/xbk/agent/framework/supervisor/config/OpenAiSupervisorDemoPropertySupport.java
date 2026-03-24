package com.xbk.agent.framework.supervisor.config;

import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.util.List;

/**
 * OpenAI Supervisor Demo 配置支持类
 *
 * 职责：为真实 Demo 提供基于本地配置文件的 llm.api-key 检查与开关判断，
 * 让真实模型演示在默认测试链路之外以受控方式开启
 *
 * @author xiexu
 */
public final class OpenAiSupervisorDemoPropertySupport {

    private static final String API_KEY_PROPERTY = "llm.api-key";
    private static final String DEMO_ENABLED_PROPERTY = "demo.supervisor.openai.enabled";
    private static final String DEFAULT_MAIN_CONFIG = "application-openai-supervisor-demo.yml";
    private static final String DEFAULT_LOCAL_CONFIG = "application-openai-supervisor-demo-local.yml";
    private static final String EXAMPLE_API_KEY = "your-openai-api-key";

    private OpenAiSupervisorDemoPropertySupport() {
    }

    /**
     * 判断默认配置文件中是否存在真实 API Key。
     *
     * @return true 表示已配置
     */
    public static boolean hasConfiguredApiKey() {
        try {
            return hasConfiguredApiKey(loadEnvironment(DEFAULT_MAIN_CONFIG, DEFAULT_LOCAL_CONFIG));
        }
        catch (IOException exception) {
            throw new IllegalStateException("加载 OpenAI Supervisor Demo 配置失败", exception);
        }
    }

    /**
     * 判断指定环境中是否存在真实 API Key。
     *
     * @param environment Spring 环境
     * @return true 表示已配置
     */
    public static boolean hasConfiguredApiKey(Environment environment) {
        if (environment == null) {
            return false;
        }
        String apiKey = environment.getProperty(API_KEY_PROPERTY);
        return hasText(apiKey) && !EXAMPLE_API_KEY.equals(apiKey.trim());
    }

    /**
     * 判断指定配置文件组合中是否存在真实 API Key。
     *
     * @param mainConfigLocation 主配置文件
     * @param localConfigLocation 本地覆盖配置文件
     * @return true 表示已配置
     * @throws IOException 读取配置失败时抛出异常
     */
    public static boolean hasConfiguredApiKey(String mainConfigLocation, String localConfigLocation) throws IOException {
        return hasConfiguredApiKey(loadEnvironment(mainConfigLocation, localConfigLocation));
    }

    /**
     * 判断默认配置中是否启用了真实 Demo。
     *
     * @return true 表示启用
     */
    public static boolean isDemoEnabled() {
        try {
            return isDemoEnabled(loadEnvironment(DEFAULT_MAIN_CONFIG, DEFAULT_LOCAL_CONFIG));
        }
        catch (IOException exception) {
            throw new IllegalStateException("加载 OpenAI Supervisor Demo 配置失败", exception);
        }
    }

    /**
     * 判断指定环境中是否启用了真实 Demo。
     *
     * @param environment Spring 环境
     * @return true 表示启用
     */
    public static boolean isDemoEnabled(Environment environment) {
        if (environment == null) {
            return false;
        }
        return Boolean.parseBoolean(environment.getProperty(DEMO_ENABLED_PROPERTY));
    }

    /**
     * 判断指定配置文件组合中是否启用了真实 Demo。
     *
     * @param mainConfigLocation 主配置文件
     * @param localConfigLocation 本地覆盖配置文件
     * @return true 表示启用
     * @throws IOException 读取配置失败时抛出异常
     */
    public static boolean isDemoEnabled(String mainConfigLocation, String localConfigLocation) throws IOException {
        return isDemoEnabled(loadEnvironment(mainConfigLocation, localConfigLocation));
    }

    /**
     * 加载配置文件到隔离环境。
     *
     * @param mainConfigLocation 主配置文件
     * @param localConfigLocation 本地覆盖配置文件
     * @return 环境
     * @throws IOException 读取配置失败时抛出异常
     */
    public static ConfigurableEnvironment loadEnvironment(String mainConfigLocation, String localConfigLocation)
            throws IOException {
        MockEnvironment environment = new MockEnvironment();
        // 先加载 local，再加载 main，但都以 addLast 追加，最终 local 会保持更高优先级。
        addYamlIfExists(environment, localConfigLocation);
        addYamlIfExists(environment, mainConfigLocation);
        return environment;
    }

    /**
     * 向环境中追加 YAML。
     *
     * @param environment 目标环境
     * @param classpathLocation classpath 路径
     * @throws IOException 读取配置失败时抛出异常
     */
    private static void addYamlIfExists(ConfigurableEnvironment environment, String classpathLocation)
            throws IOException {
        if (!hasText(classpathLocation)) {
            return;
        }
        Resource resource = new ClassPathResource(classpathLocation);
        if (!resource.exists()) {
            return;
        }
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> propertySources = loader.load(resource.getFilename(), resource);
        for (int index = propertySources.size() - 1; index >= 0; index--) {
            // 反向追加可以保留同一个 YAML 内部多文档段的原始优先级。
            environment.getPropertySources().addLast(propertySources.get(index));
        }
    }

    /**
     * 判断文本是否包含非空白内容。
     *
     * @param value 待判断文本
     * @return true 表示包含内容
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
