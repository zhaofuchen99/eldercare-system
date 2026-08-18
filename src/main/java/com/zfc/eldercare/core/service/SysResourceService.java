package com.zfc.eldercare.core.service;

import com.zfc.eldercare.core.dto.SysResourceDTO;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.SysResourceVO;

/**
 * 受控资源管理服务（RBAC 授权管理，文档 5.1）。
 */
public interface SysResourceService {

    /** 资源分页（可按类型/关键字筛选） */
    PageVO<SysResourceVO> resourcePage(String type, String keyword, int page, int size);

    /** 资源详情 */
    SysResourceVO resourceDetail(Long id);

    /** 新增资源 */
    Long createResource(SysResourceDTO dto);

    /** 更新资源 */
    void updateResource(Long id, SysResourceDTO dto);

    /** 删除资源 */
    void deleteResource(Long id);
}
