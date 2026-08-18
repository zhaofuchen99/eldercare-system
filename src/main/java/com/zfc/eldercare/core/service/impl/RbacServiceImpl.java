package com.zfc.eldercare.core.service.impl;

import com.zfc.eldercare.core.entity.Permission;
import com.zfc.eldercare.core.entity.Role;
import com.zfc.eldercare.core.entity.SysResource;
import com.zfc.eldercare.core.mapper.PermissionMapper;
import com.zfc.eldercare.core.mapper.PermissionResourceMapper;
import com.zfc.eldercare.core.mapper.RoleMapper;
import com.zfc.eldercare.core.mapper.RolePermissionMapper;
import com.zfc.eldercare.core.mapper.SysResourceMapper;
import com.zfc.eldercare.core.mapper.UserRoleMapper;
import com.zfc.eldercare.core.service.RbacService;
import com.zfc.eldercare.core.vo.CurrentUserAuthVO;
import com.zfc.eldercare.core.vo.SysResourceNodeVO;
import com.zfc.eldercare.core.vo.SysResourceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 权限解析服务实现（文档 5.1 / 8.2）。
 * 解析链路：user_role→角色 → role_permission→权限 → permission_resource→资源。
 * 缓存策略：只缓存角色码/权限码/资源 ID（字符串与数字，避免实体 LocalDateTime 经 JSON 序列化出错），
 * 实体信息每次从库批量查询（selectByIds），授权变更调用 evictRbac 全量刷新。
 */
@Service
@RequiredArgsConstructor
public class RbacServiceImpl implements RbacService {

    private static final String CACHE_PREFIX = "rbac:";
    private static final String CACHE_ROLES_PREFIX = CACHE_PREFIX + "roles:";
    private static final String CACHE_PERMS_PREFIX = CACHE_PREFIX + "perms:";
    private static final String CACHE_RESOURCES_PREFIX = CACHE_PREFIX + "resources:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);
    private static final String RESOURCE_TYPE_MENU = "MENU";
    private static final String RESOURCE_TYPE_BUTTON = "BUTTON";

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final PermissionMapper permissionMapper;
    private final PermissionResourceMapper permissionResourceMapper;
    private final SysResourceMapper sysResourceMapper;

    @Override
    public Set<String> rolesOf(Long userId) {
        Set<String> cached = readSetCache(CACHE_ROLES_PREFIX + userId);
        if (cached != null) {
            return cached;
        }
        Set<String> roles = loadRoleCodes(userId);
        writeSetCache(CACHE_ROLES_PREFIX + userId, roles);
        return roles;
    }

    @Override
    public Set<String> permissionsOf(Long userId) {
        Set<String> cached = readSetCache(CACHE_PERMS_PREFIX + userId);
        if (cached != null) {
            return cached;
        }
        Set<String> permissions = loadPermissionCodes(userId);
        writeSetCache(CACHE_PERMS_PREFIX + userId, permissions);
        return permissions;
    }

    @Override
    public List<SysResource> resourcesOf(Long userId) {
        List<Long> cached = readIdListCache(CACHE_RESOURCES_PREFIX + userId);
        if (cached != null) {
            return cached.isEmpty() ? List.of() : sysResourceMapper.selectByIds(cached);
        }
        List<Long> resourceIds = loadResourceIds(userId);
        writeIdListCache(CACHE_RESOURCES_PREFIX + userId, resourceIds);
        return resourceIds.isEmpty() ? List.of() : sysResourceMapper.selectByIds(resourceIds);
    }

    @Override
    public CurrentUserAuthVO currentUserAuth(Long userId) {
        List<SysResource> resources = resourcesOf(userId);
        List<SysResource> menus = resources.stream()
                .filter(r -> RESOURCE_TYPE_MENU.equals(r.getResourceType()))
                .toList();
        List<SysResource> buttons = resources.stream()
                .filter(r -> RESOURCE_TYPE_BUTTON.equals(r.getResourceType()))
                .toList();
        return new CurrentUserAuthVO(
                new ArrayList<>(rolesOf(userId)),
                new ArrayList<>(permissionsOf(userId)),
                buildMenuTree(menus),
                buttons.stream().map(SysResourceVO::from).toList());
    }

    @Override
    public void evictRbac() {
        Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    // ===== 权限解析 =====

    private Set<String> loadRoleCodes(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return roleMapper.selectByIds(roleIds).stream()
                .filter(r -> Integer.valueOf(1).equals(r.getStatus()))
                .map(Role::getRoleCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> loadPermissionCodes(Long userId) {
        List<Long> activeRoleIds = activeRoleIdsOf(userId);
        if (activeRoleIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleIds(activeRoleIds);
        if (permissionIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        return permissionMapper.selectByIds(permissionIds).stream()
                .map(Permission::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Long> loadResourceIds(Long userId) {
        List<Long> activeRoleIds = activeRoleIdsOf(userId);
        if (activeRoleIds.isEmpty()) {
            return List.of();
        }
        List<Long> permissionIds = rolePermissionMapper.selectPermissionIdsByRoleIds(activeRoleIds);
        if (permissionIds.isEmpty()) {
            return List.of();
        }
        List<Long> resourceIds = permissionResourceMapper.selectResourceIdsByPermissionIds(permissionIds);
        return resourceIds.stream().distinct().toList();
    }

    /** 用户启用的角色 ID 集合（过滤 status=0 停用角色） */
    private List<Long> activeRoleIdsOf(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleMapper.selectByIds(roleIds).stream()
                .filter(r -> Integer.valueOf(1).equals(r.getStatus()))
                .map(Role::getId)
                .toList();
    }

    // ===== 菜单树 =====

    /** 菜单按 parent_id 组装树（排序号升序，并列按 id） */
    private List<SysResourceNodeVO> buildMenuTree(List<SysResource> menus) {
        if (menus.isEmpty()) {
            return List.of();
        }
        Map<Long, List<SysResource>> byParent = menus.stream()
                .collect(Collectors.groupingBy(r -> r.getParentId() == null ? 0L : r.getParentId()));
        return buildChildren(0L, byParent);
    }

    private List<SysResourceNodeVO> buildChildren(Long parentId, Map<Long, List<SysResource>> byParent) {
        List<SysResource> children = byParent.get(parentId);
        if (children == null) {
            return List.of();
        }
        return children.stream()
                .sorted(Comparator.comparing(SysResource::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysResource::getId))
                .map(r -> new SysResourceNodeVO(r.getId(), r.getResourceCode(), r.getResourceName(),
                        r.getResourceType(), r.getPath(), r.getParentId(), r.getSortOrder(),
                        buildChildren(r.getId(), byParent)))
                .toList();
    }

    // ===== 缓存读写 =====

    @SuppressWarnings("unchecked")
    private Set<String> readSetCache(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Set<?> set) {
            return (Set<String>) set;
        }
        return null;
    }

    private void writeSetCache(String key, Set<String> values) {
        redisTemplate.opsForValue().set(key, new LinkedHashSet<>(values), CACHE_TTL);
    }

    @SuppressWarnings("unchecked")
    private List<Long> readIdListCache(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof List<?> list) {
            return (List<Long>) list;
        }
        return null;
    }

    private void writeIdListCache(String key, List<Long> values) {
        redisTemplate.opsForValue().set(key, new ArrayList<>(values), CACHE_TTL);
    }
}
