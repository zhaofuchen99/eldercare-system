-- =====================================================================
-- 清理 RAG 知识库残留（2026-08-19 AI 模块定型：RAG 已从项目代码中移除）
-- ---------------------------------------------------------------------
-- 适用：曾执行过 ai-module.sql / init.sql RAG 段的已有库。
--      清理后与移除 RAG 后的新 init.sql 保持一致（24 张表）。
-- 幂等：可重复执行（IF EXISTS / 子查询无匹配时自动跳过）。
-- 可选：Redis 中残留的向量索引 knowledge_chunk_idx 不影响运行，
--      如需彻底清除可在 redis-stack（6380）执行：
--      FT.DROPINDEX knowledge_chunk_idx DD
-- =====================================================================

-- 1. 删除知识库切片/文档表
DROP TABLE IF EXISTS `knowledge_chunk`;
DROP TABLE IF EXISTS `knowledge_doc`;

-- 2. 删除 RAG 系统配置（保留 ai_assessment_system_prompt，属 AI 评测）
DELETE FROM `sys_config`
WHERE `config_key` IN ('knowledge_enabled', 'knowledge_top_k', 'knowledge_search_threshold');

-- 3. 解除 ADMIN 角色对 admin:knowledge:manage 的授权
DELETE FROM `role_permission`
WHERE `permission_id` = (SELECT `id` FROM `permission` WHERE `permission_code` = 'admin:knowledge:manage');

-- 4. 解除 admin:knowledge:manage 权限与资源的关联
DELETE FROM `permission_resource`
WHERE `permission_id` = (SELECT `id` FROM `permission` WHERE `permission_code` = 'admin:knowledge:manage');

-- 5. 删除知识库管理权限与资源种子
DELETE FROM `permission` WHERE `permission_code` = 'admin:knowledge:manage';
DELETE FROM `resource`
WHERE `resource_code` IN ('api:admin:knowledge', 'menu:admin:knowledge',
                          'btn:knowledge:upload', 'btn:knowledge:delete', 'btn:knowledge:reparse');
