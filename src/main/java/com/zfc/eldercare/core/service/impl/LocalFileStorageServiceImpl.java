package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 本地磁盘文件存储实现（文档 5.5）。
 * 上传目录：data/upload/report/yyyyMM/{uuid}.pdf（相对路径写入 appointment.report_url）。
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    /** 上传根目录（配置 eldercare.upload-dir） */
    private final String uploadDir;
    /** 报告子目录 */
    private static final String REPORT_DIR = "report";
    /** 文件大小上限 20MB（文档 5.5） */
    private static final long MAX_SIZE = 20L * 1024 * 1024;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    public LocalFileStorageServiceImpl(@Value("${eldercare.upload-dir:data/upload}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public String storeReport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要上传的体检报告文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException("体检报告文件不能超过 20MB");
        }
        String original = file.getOriginalFilename();
        String lower = original == null ? "" : original.toLowerCase();
        String contentType = file.getContentType();
        // 仅允许 PDF：MIME 或扩展名任一命中即可（部分浏览器传的 content-type 不规范）
        if (!"application/pdf".equalsIgnoreCase(contentType) && !lower.endsWith(".pdf")) {
            throw new BusinessException("仅支持 PDF 格式的体检报告");
        }

        String month = LocalDate.now().format(MONTH_FMT);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        String relative = REPORT_DIR + "/" + month + "/" + fileName;
        Path dir = Paths.get(uploadDir, REPORT_DIR, month);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            log.error("保存体检报告失败，relativePath={}", relative, e);
            throw new BusinessException("体检报告保存失败，请稍后重试");
        }
        return relative;
    }

    @Override
    public byte[] loadReport(String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            throw new BusinessException(404, "体检报告不存在");
        }
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = root.resolve(relativePath).normalize();
        // 防路径穿越：必须位于上传根目录内
        if (!file.startsWith(root)) {
            throw new BusinessException(403, "非法的文件路径");
        }
        if (!Files.exists(file) || !Files.isRegularFile(file)) {
            throw new BusinessException(404, "体检报告文件不存在");
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            log.error("读取体检报告失败，path={}", file, e);
            throw new BusinessException("体检报告读取失败，请稍后重试");
        }
    }
}
