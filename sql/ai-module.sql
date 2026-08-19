-- =====================================================================
-- AI 模块增量脚本（独立、幂等）
-- ---------------------------------------------------------------------
-- 适用：已执行过 init.sql 的库，补 2026-08-19 AI 模块新增内容
--   · 知识库两表      knowledge_doc / knowledge_chunk
--   · 系统配置 4 条    RAG 开关 / topK / 相似度阈值 + AI 评分提示词
--   · RBAC 种子        permission admin:knowledge:manage
--                      resource 58~62（接口/按钮/菜单）
--                      permission_resource 关联 + ADMIN 角色授权
-- 幂等性：全部使用 IF NOT EXISTS / INSERT IGNORE / ON DUPLICATE KEY，可重复执行
-- 注意：MySQL 客户端执行时请“整段运行”，不要选中片段；此文件每段都是完整语句。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 知识库文档表 knowledge_doc / 切片表 knowledge_chunk
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `knowledge_doc`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '文档 ID',
    `title`        VARCHAR(255) NOT NULL                COMMENT '文档标题（默认取原始文件名）',
    `file_name`    VARCHAR(255) NOT NULL                COMMENT '原始文件名',
    `file_path`    VARCHAR(500) NULL                    COMMENT '存储相对路径 knowledge/yyyyMM/{uuid}.ext（重新解析用）',
    `file_type`    VARCHAR(20)  NOT NULL                COMMENT '文件类型：TXT/MD/PDF/DOCX（当前支持 TXT/MD）',
    `file_size`    BIGINT       NULL                    COMMENT '文件大小（字节）',
    `chunk_count`  INT          NOT NULL DEFAULT 0      COMMENT '切片数量',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PARSING' COMMENT '处理状态：PARSING解析中/READY可用/FAILED失败',
    `create_by`    BIGINT       NULL                    COMMENT '上传管理员 ID',
    `create_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_create_by` (`create_by`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库文档表';

CREATE TABLE IF NOT EXISTS `knowledge_chunk`
(
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '切片 ID',
    `doc_id`       BIGINT      NOT NULL                COMMENT '文档 ID（关联 knowledge_doc.id）',
    `chunk_index`  INT         NOT NULL                COMMENT '切片序号（从 0 开始）',
    `chunk_text`   MEDIUMTEXT  NOT NULL                COMMENT '切片文本（与 Redis 向量关联，检索时直接返回）',
    `vector_id`    VARCHAR(64) NULL                    COMMENT 'Redis 向量 ID（vectorStore.add 生成，删除文档时回删）',
    `token_count`  INT         NULL                    COMMENT '切片字符数',
    `create_time`  DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `deleted`      TINYINT     NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_doc_id` (`doc_id`),
    KEY `idx_vector_id` (`vector_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '知识库切片表';

-- ---------------------------------------------------------------------
-- 2. 系统配置（RAG 参数 + AI 评分提示词），uk_config_key 幂等
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `description`) VALUES
('knowledge_enabled', 'true', '知识库 RAG 检索开关'),
('knowledge_top_k', '3', '知识库检索返回切片数'),
('knowledge_search_threshold', '0.6', '知识库检索命中相似度阈值（0-1）'),
('ai_assessment_system_prompt', '你是一位资深的健康评测专家，请根据问卷答案与规则分，输出 JSON：{"aiScore":0-100整数,"suggestion":不超过200字建议}', 'AI 健康评测系统提示词');

-- ---------------------------------------------------------------------
-- 3. 权限 admin:knowledge:manage，uk_permission_code 幂等
-- ---------------------------------------------------------------------
INSERT INTO `permission` (`permission_code`, `permission_name`, `description`)
VALUES ('admin:knowledge:manage', '知识库管理', '管理端知识库文档上传/删除/管理')
ON DUPLICATE KEY UPDATE `permission_name` = `permission_name`;

-- ---------------------------------------------------------------------
-- 4. 资源 58~62，INSERT IGNORE 按主键 id 幂等（重复执行自动跳过）
-- ---------------------------------------------------------------------
INSERT IGNORE INTO `resource` (`id`, `resource_code`, `resource_name`, `resource_type`, `path`, `parent_id`, `sort_order`) VALUES
(58, 'api:admin:knowledge', '知识库管理接口', 'API', '/api/admin/knowledge/**', 0, 21),
(59, 'btn:knowledge:upload', '知识库上传', 'BUTTON', NULL, 0, 1),
(60, 'btn:knowledge:delete', '知识库删除', 'BUTTON', NULL, 0, 2),
(61, 'btn:knowledge:reparse', '知识库重新解析', 'BUTTON', NULL, 0, 3),
(62, 'menu:admin:knowledge', '知识库管理', 'MENU', NULL, 0, 10);

-- ---------------------------------------------------------------------
-- 5. 权限→资源 关联（管理端知识库菜单/接口/按钮挂到权限下），uk 幂等
-- ---------------------------------------------------------------------
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:knowledge', 'menu:admin:knowledge',
                                          'btn:knowledge:upload', 'btn:knowledge:delete', 'btn:knowledge:reparse')
WHERE p.`permission_code` = 'admin:knowledge:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- ---------------------------------------------------------------------
-- 6. ADMIN 角色授权（ADMIN 全量权限；其他角色如需知识库管理请自行挂权限）
-- ---------------------------------------------------------------------
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
JOIN `permission` p ON p.`permission_code` = 'admin:knowledge:manage'
WHERE r.`role_code` = 'ADMIN'
ON DUPLICATE KEY UPDATE `permission_id` = `permission_id`;
