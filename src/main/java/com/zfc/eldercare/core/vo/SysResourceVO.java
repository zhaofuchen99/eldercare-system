package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.SysResource;

import java.time.LocalDateTime;

/**
 * 受控资源 VO（RBAC 管理端）。
 */
public record SysResourceVO(
        Long id,
        String resourceCode,
        String resourceName,
        String resourceType,
        String path,
        Long parentId,
        Integer sortOrder,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
    public static SysResourceVO from(SysResource r) {
        return new SysResourceVO(r.getId(), r.getResourceCode(), r.getResourceName(), r.getResourceType(),
                r.getPath(), r.getParentId(), r.getSortOrder(), r.getCreateTime(), r.getUpdateTime());
    }
}
