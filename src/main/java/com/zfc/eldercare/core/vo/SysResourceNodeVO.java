package com.zfc.eldercare.core.vo;

import com.zfc.eldercare.core.entity.SysResource;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单资源树节点 VO（前端菜单显隐，文档 8.2）。
 * children 按 parent_id 组装成树。
 */
public record SysResourceNodeVO(
        Long id,
        String resourceCode,
        String resourceName,
        String resourceType,
        String path,
        Long parentId,
        Integer sortOrder,
        List<SysResourceNodeVO> children
) {
    public static SysResourceNodeVO from(SysResource r) {
        return new SysResourceNodeVO(r.getId(), r.getResourceCode(), r.getResourceName(), r.getResourceType(),
                r.getPath(), r.getParentId(), r.getSortOrder(), new ArrayList<>());
    }
}
