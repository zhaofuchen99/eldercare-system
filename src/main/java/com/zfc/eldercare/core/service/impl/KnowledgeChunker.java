package com.zfc.eldercare.core.service.impl;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识库文本切片器（AI 模块 - RAG）。
 * 按段落聚合到约 maxLen 字符，块间保留 overlap 字符重叠，保证语义衔接。
 */
@Component
public class KnowledgeChunker {

    /** 按段落聚合切片 */
    public List<String> chunk(String text, int maxLen, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (text == null) {
            return chunks;
        }
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n").trim();
        if (normalized.isEmpty()) {
            return chunks;
        }
        String[] paragraphs = normalized.split("\n+");
        StringBuilder cur = new StringBuilder();
        for (String raw : paragraphs) {
            String para = raw.trim();
            if (para.isEmpty()) {
                continue;
            }
            // 段落本身超长：先落当前块，再按 maxLen 硬切该段
            if (para.length() > maxLen) {
                if (!cur.isEmpty()) {
                    chunks.add(cur.toString());
                    cur = new StringBuilder();
                }
                for (int i = 0; i < para.length(); i += maxLen) {
                    String piece = para.substring(i, Math.min(i + maxLen, para.length()));
                    chunks.add(piece);
                }
                continue;
            }
            if (!cur.isEmpty() && cur.length() + para.length() + 1 > maxLen) {
                chunks.add(cur.toString());
                // 重叠：继承上一块尾部 overlap 字符
                String tail = cur.length() > overlap ? cur.substring(cur.length() - overlap) : "";
                cur = new StringBuilder(tail);
            }
            if (!cur.isEmpty()) {
                cur.append('\n');
            }
            cur.append(para);
        }
        if (!cur.isEmpty()) {
            chunks.add(cur.toString());
        }
        return chunks;
    }
}
