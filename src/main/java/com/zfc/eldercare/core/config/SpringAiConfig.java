package com.zfc.eldercare.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI 配置：由 starter 自动装配 ChatClient.Builder，这里构建单例 ChatClient。
 * DeepSeek 走 OpenAI 兼容接口（application.yaml spring.ai.openai）。
 */
@Configuration
public class SpringAiConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
