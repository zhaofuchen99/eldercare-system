package com.zfc.eldercare.api.controller;

import com.zfc.eldercare.core.common.Result;
import com.zfc.eldercare.core.service.KnowledgeBaseService;
import com.zfc.eldercare.core.vo.KnowledgeChunkVO;
import com.zfc.eldercare.core.vo.KnowledgeDocVO;
import com.zfc.eldercare.core.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 知识库管理（管理端，AI 模块 - RAG，接口文档 15.x）。
 * 按钮级鉴权：admin:knowledge:manage（路径级 ADMIN 角色由 SecurityConfig 覆盖）。
 */
@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final KnowledgeBaseService knowledgeBaseService;

    /** 上传知识文档（multipart/form-data，支持 txt/md） */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('admin:knowledge:manage')")
    public Result<Long> upload(@AuthenticationPrincipal Long userId, @RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", knowledgeBaseService.upload(userId, file));
    }

    /** 文档分页 */
    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('admin:knowledge:manage')")
    public Result<PageVO<KnowledgeDocVO>> documents(@RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return Result.success(knowledgeBaseService.pageDocs(page, size));
    }

    /** 某文档切片分页 */
    @GetMapping("/documents/{id}/chunks")
    @PreAuthorize("hasAuthority('admin:knowledge:manage')")
    public Result<PageVO<KnowledgeChunkVO>> chunks(@PathVariable Long id,
                                                   @RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int size) {
        return Result.success(knowledgeBaseService.pageChunks(id, page, size));
    }

    /** 删除文档（回删向量） */
    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasAuthority('admin:knowledge:manage')")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.deleteDoc(id);
        return Result.success();
    }

    /** 重新解析并重建向量 */
    @PostMapping("/documents/{id}/reparse")
    @PreAuthorize("hasAuthority('admin:knowledge:manage')")
    public Result<Void> reparse(@PathVariable Long id) {
        knowledgeBaseService.reparse(id);
        return Result.success();
    }
}
