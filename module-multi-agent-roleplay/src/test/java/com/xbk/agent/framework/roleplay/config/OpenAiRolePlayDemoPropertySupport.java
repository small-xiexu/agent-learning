package com.xbk.agent.framework.roleplay.config;

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
 * OpenAI RolePlay Demo 配置支持类
 *
 * 职责：为真实 Demo 提供基于本地配置文件的 llm.api-key 检查，避免回退到环境变量或系统属性
 *
 * @author xiexu
 */
public final class OpenAiRolePlayDemoPropertySupport {

    private static final String API_KEY_PROPERTY = "llm.api-key";
    private static final String DEMO_ENABLED_PROPERTY = "demo.roleplay.openai.enabled";
    private static final String DEFAULT_MAIN_CONFIG = "application-openai-roleplay-demo.yml";
    private static final String DEFAULT_LOCAL_CONFIG = "application-openai-roleplay-demo-local.yml";
    private static final String EXAMPLE_API_KEY = "your-openai-api-key";

    private OpenAiRolePlayDemoPropertySupport() {
    }

    /**
     * 判断默认 Demo 配置文件中是否已经提供真实可用的 API Key。
     *
     * @return true 表示已配置
     */
    public static boolean hasConfiguredApiKey() {
        try {
            return hasConfiguredApiKey(loadEnvironment(DEFAULT_MAIN_CONFIG, DEFAULT_LOCAL_CONFIG));
        } catch (IOException exception) {
            throw new IllegalStateException("加载 OpenAI RolePlay Demo 配置失败", exception);
        }
    }

    /**
     * 判断指定环境中是否已经提供真实可用的 API Key。
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
     * 判断指定配置文件组合中是否已经提供真实可用的 API Key。
     *
     * @param mainConfigLocation 主配置文件 classpath 路径
     * @param localConfigLocation 本地覆盖配置文件 classpath 路径
     * @return true 表示已配置
     * @throws IOException 读取配置文件失败时抛出异常
     */
    public static boolean hasConfiguredApiKey(String mainConfigLocation, String localConfigLocation) throws IOException {
        return hasConfiguredApiKey(loadEnvironment(mainConfigLocation, localConfigLocation));
    }

    /**
     * 判断默认 Demo 配置文件中是否显式启用了真实 Demo。
     *
     * @return true 表示允许执行真实 Demo
     */
    public static boolean isDemoEnabled() {
        try {
            return isDemoEnabled(loadEnvironment(DEFAULT_MAIN_CONFIG, DEFAULT_LOCAL_CONFIG));
        } catch (IOException exception) {
            throw new IllegalStateException("加载 OpenAI RolePlay Demo 配置失败", exception);
        }
    }

    /**
     * 判断指定环境中是否显式启用了真实 Demo。
     *
     * @param environment Spring 环境
     * @return true 表示允许执行真实 Demo
     */
    public static boolean isDemoEnabled(Environment environment) {
        if (environment == null) {
            return false;
        }
        return Boolean.parseBoolean(environment.getProperty(DEMO_ENABLED_PROPERTY));
    }

    /**
     * 判断指定配置文件组合中是否显式启用了真实 Demo。
     *
     * @param mainConfigLocation 主配置文件 classpath 路径
     * @param localConfigLocation 本地覆盖配置文件 classpath 路径
     * @return true 表示允许执行真实 Demo
     * @throws IOException 读取配置文件失败时抛出异常
     */
    public static boolean isDemoEnabled(String mainConfigLocation, String localConfigLocation) throws IOException {
        return isDemoEnabled(loadEnvironment(mainConfigLocation, localConfigLocation));
    }

    /**
     * 加载 Demo 相关配置文件到隔离的 Spring 环境中。
     *
     * @param mainConfigLocation 主配置文件 classpath 路径
     * @param localConfigLocation 本地覆盖配置文件 classpath 路径
     * @return 已合并的 Spring 环境
     * @throws IOException 读取配置文件失败时抛出异常
     */
    public static ConfigurableEnvironment loadEnvironment(String mainConfigLocation, String localConfigLocation)
            throws IOException {
        MockEnvironment environment = new MockEnvironment();
        addYamlIfExists(environment, localConfigLocation);
        addYamlIfExists(environment, mainConfigLocation);
        return environment;
    }

    /**
     * 按 classpath 路径向环境中追加一个 YAML 配置文件。
     *
     * @param environment 目标环境
     * @param classpathLocation 配置文件路径
     * @throws IOException 读取配置文件失败时抛出异常
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
            environment.getPropertySources().addLast(propertySources.get(index));
        }
    }

    /**
     * 判断字符串是否包含非空白文本。
     *
     * @param value 待判断文本
     * @return true 表示包含非空白文本
     */
    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
