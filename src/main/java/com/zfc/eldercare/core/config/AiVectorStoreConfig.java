package com.zfc.eldercare.core.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

/**
 * AI 向量库配置（AI 模块 - RAG 知识库）。
 * Spring AI 2.0 的 RedisVectorStore.builder(RedisClient, EmbeddingModel) 使用 Jedis 7 的 RedisClient：
 * Jedis 7 已废弃 JedisPooled，且 DefaultJedisClientConfig 不再有 host/port 方法，
 * 连接地址改为 RedisClient.builder().hostAndPort(...) 配置。这里按官方 autoconfigure 同款方式构建。
 * 注意：RedisVectorStore 初始化（afterPropertiesSet）会调用 embedding 探测向量维度，
 * 因此 Ollama 未启动时应用启动会失败；需先启动 Ollama（本机或容器）并 pull bge-m3。
 */
@Configuration
public class AiVectorStoreConfig {

    @Bean
    public RedisClient aiRedisClient(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            @Value("${spring.data.redis.database:0}") int database) {
        DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder()
                .database(database);
        if (StringUtils.hasText(password)) {
            config.password(password);
        }
        return RedisClient.builder()
                .hostAndPort(host, port)
                .clientConfig(config.build())
                .build();
    }

    @Bean
    public VectorStore knowledgeVectorStore(RedisClient redisClient, EmbeddingModel embeddingModel) {
        return RedisVectorStore.builder(redisClient, embeddingModel)
                .indexName("knowledge_chunk_idx")
                .prefix("knowledge:")
                .metadataFields(
                        // 注意：tag 字段必须存 String（docId 用 String.valueOf），numeric 存 Number
                        RedisVectorStore.MetadataField.tag("docId"),
                        RedisVectorStore.MetadataField.numeric("chunkIndex"),
                        RedisVectorStore.MetadataField.text("docTitle"))
                .initializeSchema(true)
                .build();
    }
}
