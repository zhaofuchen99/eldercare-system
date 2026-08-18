package com.zfc.eldercare.core.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.zfc.eldercare.core.dto.SysResourceDTO;
import com.zfc.eldercare.core.entity.SysResource;
import com.zfc.eldercare.core.exception.BusinessException;
import com.zfc.eldercare.core.mapper.SysResourceMapper;
import com.zfc.eldercare.core.service.RbacService;
import com.zfc.eldercare.core.service.SysResourceService;
import com.zfc.eldercare.core.vo.PageVO;
import com.zfc.eldercare.core.vo.SysResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 受控资源管理服务实现（RBAC 授权管理，文档 5.1）。
 */
@Service
@RequiredArgsConstructor
public class SysResourceServiceImpl implements SysResourceService {

    private final SysResourceMapper sysResourceMapper;
    private final RbacService rbacService;

    @Override
    public PageVO<SysResourceVO> resourcePage(String type, String keyword, int page, int size) {
        PageHelper.startPage(page, size);
        List<SysResource> list = sysResourceMapper.selectPage(type, keyword);
        PageInfo<SysResource> pageInfo = new PageInfo<>(list);
        List<SysResourceVO> voList = list.stream().map(SysResourceVO::from).toList();
        return new PageVO<>(pageInfo.getPageNum(), pageInfo.getPageSize(),
                pageInfo.getTotal(), pageInfo.getPages(), voList);
    }

    @Override
    public SysResourceVO resourceDetail(Long id) {
        return SysResourceVO.from(requireResource(id));
    }

    @Override
    @Transactional
    public Long createResource(SysResourceDTO dto) {
        if (sysResourceMapper.selectByCode(dto.resourceCode()) != null) {
            throw new BusinessException(409, "资源编码已存在");
        }
        SysResource resource = new SysResource();
        resource.setResourceCode(dto.resourceCode());
        resource.setResourceName(dto.resourceName());
        resource.setResourceType(dto.resourceType());
        resource.setPath(dto.path());
        resource.setParentId(dto.parentId() == null ? 0L : dto.parentId());
        resource.setSortOrder(dto.sortOrder() == null ? 0 : dto.sortOrder());
        sysResourceMapper.insert(resource);
        return resource.getId();
    }

    @Override
    @Transactional
    public void updateResource(Long id, SysResourceDTO dto) {
        requireResource(id);
        SysResource update = new SysResource();
        update.setId(id);
        update.setResourceCode(dto.resourceCode());
        update.setResourceName(dto.resourceName());
        update.setResourceType(dto.resourceType());
        update.setPath(dto.path());
        update.setParentId(dto.parentId());
        update.setSortOrder(dto.sortOrder());
        sysResourceMapper.update(update);
        rbacService.evictRbac();
    }

    @Override
    @Transactional
    public void deleteResource(Long id) {
        requireResource(id);
        sysResourceMapper.delete(id);
        rbacService.evictRbac();
    }

    private SysResource requireResource(Long id) {
        SysResource resource = sysResourceMapper.selectById(id);
        if (resource == null) {
            throw new BusinessException(404, "资源不存在");
        }
        return resource;
    }
}
