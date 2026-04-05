package com.xbk.agent.framework.reflection.config;

import com.xbk.agent.framework.core.config.OpenAiDemoConfigSupport;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.io.IOException;

/**
 * OpenAI Reflection Demo 配置支持类
 *
 * 职责：为真实 Demo 提供基于本地配置文件的 llm.api-key 检查，避免回退到环境变量或系统属性
 *
 * @author xiexu
 */
public final class OpenAiReflectionDemoPropertySupport {

    private static final String DEMO_ENABLED_PROPERTY = "demo.reflection.openai.enabled";
    private static final String DEFAULT_MAIN_CONFIG = "application-openai-reflection-demo.yml";
    private static final String DEFAULT_LOCAL_CONFIG = "application-openai-reflection-demo-local.yml";

    private OpenAiReflectionDemoPropertySupport() {
    }

    /**
     * 判断默认 Demo 配置文件中是否已经提供真实可用的 API Key。
     *
     * @return true 表示已配置
     */
    public static boolean hasConfiguredApiKey() {
        try {
            return OpenAiDemoConfigSupport.hasConfiguredApiKey(DEFAULT_MAIN_CONFIG, DEFAULT_LOCAL_CONFIG);
        } catch (IOException exception) {
            throw new IllegalStateException("加载 OpenAI Reflection Demo 配置失败", exception);
        }
    }

    /**
     * 判断指定环境中是否已经提供真实可用的 API Key。
     *
     * @param environment Spring 环境
     * @return true 表示已配置
     */
    public static boolean hasConfiguredApiKey(Environment environment) {
        return OpenAiDemoConfigSupport.hasConfiguredApiKey(environment);
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
        return OpenAiDemoConfigSupport.hasConfiguredApiKey(mainConfigLocation, localConfigLocation);
    }

    /**
     * 判断默认 Demo 配置文件中是否显式启用了真实 Demo。
     *
     * @return true 表示允许执行真实 Demo
     */
    public static boolean isDemoEnabled() {
        try {
            return OpenAiDemoConfigSupport.isDemoEnabled(DEFAULT_MAIN_CONFIG, DEFAULT_LOCAL_CONFIG, DEMO_ENABLED_PROPERTY);
        } catch (IOException exception) {
            throw new IllegalStateException("加载 OpenAI Reflection Demo 配置失败", exception);
        }
    }

    /**
     * 判断指定环境中是否显式启用了真实 Demo。
     *
     * @param environment Spring 环境
     * @return true 表示允许执行真实 Demo
     */
    public static boolean isDemoEnabled(Environment environment) {
        return OpenAiDemoConfigSupport.isDemoEnabled(environment, DEMO_ENABLED_PROPERTY);
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
        return OpenAiDemoConfigSupport.isDemoEnabled(mainConfigLocation, localConfigLocation, DEMO_ENABLED_PROPERTY);
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
        return OpenAiDemoConfigSupport.loadEnvironment(mainConfigLocation, localConfigLocation);
    }
}
