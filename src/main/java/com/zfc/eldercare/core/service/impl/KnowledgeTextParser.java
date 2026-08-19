package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 知识库文本解析器（AI 模块 - RAG）。
 * 当前支持 TXT/MD（UTF-8 读取）；PDF/DOCX 暂不支持，抛出明确提示。
 */
@Component
@Slf4j
public class KnowledgeTextParser {

    private static final java.util.Set<String> SUPPORTED = java.util.Set.of("txt", "md");

    /** 从磁盘文件解析文本（扩展名小写，不含点） */
    public String extractText(Path path, String ext) {
        String lower = ext == null ? "" : ext.toLowerCase();
        if (!SUPPORTED.contains(lower)) {
            throw new BusinessException("当前仅支持 txt/md 文档（pdf/docx 暂不支持）");
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("知识文档读取失败 {}", path, e);
            throw new BusinessException("文档读取失败");
        }
    }
}
