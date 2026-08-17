package com.zfc.eldercare.core.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务（文档 5.5 体检报告流程）。
 * 当前提供本地磁盘实现；未来扩展对象存储（OSS/MinIO）时新增实现类即可，业务代码无需改动。
 */
public interface FileStorageService {

    /**
     * 保存体检报告 PDF，返回相对路径（如 report/202608/{uuid}.pdf）。
     *
     * @throws com.zfc.eldercare.core.exception.BusinessException 非 PDF、超过 20MB 或存储失败
     */
    String storeReport(MultipartFile file);

    /** 按相对路径读取文件字节 */
    byte[] loadReport(String relativePath);
}
