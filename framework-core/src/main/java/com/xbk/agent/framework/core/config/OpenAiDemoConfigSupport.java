package com.xbk.agent.framework.core.config;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * OpenAI Demo 配置支持类
 *
 * 职责：为各模块真实 Demo 的 YAML 配置加载、递归 import 解析和公共属性判断提供共享实现
 *
 * @author xiexu
 */
public final class OpenAiDemoConfigSupport {

    private static final String API_KEY_PROPERTY = "llm.api-key";
    private static final String EXAMPLE_API_KEY = "your-openai-api-key";
    private static final String OPTIONAL_PREFIX = "optional:";
    private static final String CLASSPATH_PREFIX = "classpath:";

    private OpenAiDemoConfigSupport() {
    }

    /**
     * 判断环境中是否已经提供真实可用的 API Key。
     *
     * @param environment Spring 环境
     * @return true 表示已配置真实 Key
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
     * @return true 表示已配置真实 Key
     * @throws IOException 读取配置文件失败时抛出异常
     */
    public static boolean hasConfiguredApiKey(String mainConfigLocation, String localConfigLocation) throws IOException {
        return hasConfiguredApiKey(loadEnvironment(mainConfigLocation, localConfigLocation));
    }

    /**
     * 判断环境中是否显式启用了真实 Demo。
     *
     * @param environment Spring 环境
     * @param demoEnabledProperty Demo 开关键名
     * @return true 表示允许执行真实 Demo
     */
    public static boolean isDemoEnabled(Environment environment, String demoEnabledProperty) {
        if (environment == null || !hasText(demoEnabledProperty)) {
            return false;
        }
        return Boolean.parseBoolean(environment.getProperty(demoEnabledProperty));
    }

    /**
     * 判断指定配置文件组合中是否显式启用了真实 Demo。
     *
     * @param mainConfigLocation 主配置文件 classpath 路径
     * @param localConfigLocation 本地覆盖配置文件 classpath 路径
     * @param demoEnabledProperty Demo 开关键名
     * @return true 表示允许执行真实 Demo
     * @throws IOException 读取配置文件失败时抛出异常
     */
    public static boolean isDemoEnabled(String mainConfigLocation, String localConfigLocation, String demoEnabledProperty)
            throws IOException {
        return isDemoEnabled(loadEnvironment(mainConfigLocation, localConfigLocation), demoEnabledProperty);
    }

    /**
     * 把主配置、本地覆盖配置及其递归 import 加载到隔离环境中。
     *
     * @param mainConfigLocation 主配置文件 classpath 路径
     * @param localConfigLocation 本地覆盖配置文件 classpath 路径
     * @return 已合并的 Spring 环境
     * @throws IOException 读取配置文件失败时抛出异常
     */
    public static ConfigurableEnvironment loadEnvironment(String mainConfigLocation, String localConfigLocation)
            throws IOException {
        StandardEnvironment environment = isolatedEnvironment();
        Set<String> loadedLocations = new LinkedHashSet<String>();
        Deque<String> loadingStack = new ArrayDeque<String>();
        addYamlIfExists(environment, localConfigLocation, loadedLocations, loadingStack);
        addYamlIfExists(environment, mainConfigLocation, loadedLocations, loadingStack);
        return environment;
    }

    /**
     * 按 classpath 路径向环境追加 YAML，并递归处理 `spring.config.import`。
     *
     * @param environment 目标环境
     * @param classpathLocation 配置文件路径
     * @param loadedLocations 已完成加载的路径集合
     * @param loadingStack 当前递归栈
     * @throws IOException 读取配置文件失败时抛出异常
     */
    private static void addYamlIfExists(ConfigurableEnvironment environment, String classpathLocation,
                                        Set<String> loadedLocations, Deque<String> loadingStack)
            throws IOException {
        String normalizedLocation = normalizeLocation(classpathLocation);
        if (!hasText(normalizedLocation) || loadedLocations.contains(normalizedLocation)
                || loadingStack.contains(normalizedLocation)) {
            return;
        }
        Resource resource = new ClassPathResource(normalizedLocation);
        if (!resource.exists()) {
            return;
        }

        loadingStack.push(normalizedLocation);
        try {
            List<PropertySource<?>> propertySources = loadPropertySources(resource);
            for (String importLocation : resolveImportLocations(normalizedLocation, propertySources)) {
                addYamlIfExists(environment, importLocation, loadedLocations, loadingStack);
            }
            addPropertySources(environment, propertySources);
            loadedLocations.add(normalizedLocation);
        } finally {
            loadingStack.pop();
        }
    }

    /**
     * 读取指定 YAML 资源的属性源列表。
     *
     * @param resource YAML 资源
     * @return 属性源列表
     * @throws IOException 读取配置文件失败时抛出异常
     */
    private static List<PropertySource<?>> loadPropertySources(Resource resource) throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        return loader.load(resource.getFilename(), resource);
    }

    /**
     * 把 YAML 属性源按原有优先级追加到环境中。
     *
     * @param environment 目标环境
     * @param propertySources 属性源列表
     */
    private static void addPropertySources(ConfigurableEnvironment environment, List<PropertySource<?>> propertySources) {
        for (int index = propertySources.size() - 1; index >= 0; index--) {
            environment.getPropertySources().addLast(propertySources.get(index));
        }
    }

    /**
     * 解析当前 YAML 中声明的 `spring.config.import` 列表。
     *
     * @param currentLocation 当前配置文件路径
     * @param propertySources 当前配置文件的属性源
     * @return 归一化后的 import 路径列表
     */
    private static List<String> resolveImportLocations(String currentLocation, List<PropertySource<?>> propertySources) {
        StandardEnvironment importEnvironment = isolatedEnvironment();
        addPropertySources(importEnvironment, propertySources);
        List<String> importedLocations = Binder.get(importEnvironment)
                .bind("spring.config.import", Bindable.listOf(String.class))
                .orElse(Collections.<String>emptyList());
        if (importedLocations.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> resolvedLocations = new ArrayList<String>();
        for (int index = importedLocations.size() - 1; index >= 0; index--) {
            String resolvedLocation = resolveImportLocation(currentLocation, importedLocations.get(index));
            if (hasText(resolvedLocation)) {
                resolvedLocations.add(resolvedLocation);
            }
        }
        return resolvedLocations;
    }

    /**
     * 把 import 声明解析为当前配置文件所在目录下的 classpath 路径。
     *
     * @param currentLocation 当前配置文件路径
     * @param importLocation import 声明值
     * @return 归一化后的 classpath 路径
     */
    private static String resolveImportLocation(String currentLocation, String importLocation) {
        String normalizedImport = normalizeLocation(importLocation);
        if (!hasText(normalizedImport)) {
            return null;
        }
        if (normalizedImport.startsWith("/")) {
            return normalizePath(normalizedImport.substring(1));
        }
        Path parentPath = parentPath(currentLocation);
        Path resolvedPath = parentPath == null ? Paths.get(normalizedImport) : parentPath.resolve(normalizedImport);
        return normalizePath(resolvedPath.toString());
    }

    /**
     * 归一化 import 或配置路径，移除 `optional:` 与 `classpath:` 前缀。
     *
     * @param location 原始路径
     * @return 归一化后的路径
     */
    private static String normalizeLocation(String location) {
        if (!hasText(location)) {
            return null;
        }
        String normalizedLocation = location.trim();
        while (normalizedLocation.startsWith(OPTIONAL_PREFIX)) {
            normalizedLocation = normalizedLocation.substring(OPTIONAL_PREFIX.length()).trim();
        }
        while (normalizedLocation.startsWith(CLASSPATH_PREFIX)) {
            normalizedLocation = normalizedLocation.substring(CLASSPATH_PREFIX.length()).trim();
        }
        if (!hasText(normalizedLocation)) {
            return null;
        }
        return normalizePath(normalizedLocation);
    }

    /**
     * 返回当前配置文件所在目录。
     *
     * @param currentLocation 当前配置文件路径
     * @return 父目录路径；无父目录时返回 null
     */
    private static Path parentPath(String currentLocation) {
        String normalizedLocation = normalizePath(currentLocation);
        Path currentPath = Paths.get(normalizedLocation);
        return currentPath.getParent();
    }

    /**
     * 创建不暴露系统属性与环境变量的隔离环境。
     *
     * @return 隔离环境
     */
    private static StandardEnvironment isolatedEnvironment() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        return environment;
    }

    /**
     * 统一路径分隔符并消除 `.` / `..` 段。
     *
     * @param path 原始路径
     * @return 归一化后的路径
     */
    private static String normalizePath(String path) {
        return Paths.get(path).normalize().toString().replace('\\', '/');
    }

    /**
     * 判断文本是否包含非空白内容。
     *
     * @param value 待判断文本
     * @return true 表示包含非空白内容
     */
    private static boolean hasText(String value) {
        return StringUtils.hasText(value);
    }
}
