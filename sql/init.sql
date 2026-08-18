-- =====================================================================
-- AI 智能养老社区管理系统 - 数据库初始化脚本
-- 数据库：eldercare
-- 引擎：InnoDB    字符集：utf8mb4    排序规则：utf8mb4_0900_ai_ci
-- 依据：详细设计文档 v1.0.8（24 张表）
-- 枚举值约定：数据库存储英文编码，前端展示层映射中文
-- 说明：在全新数据库上执行一次即可（建库 + 24 张表 + 初始数据）
-- 使用：Navicat / IDEA Database 中「运行 SQL 文件」执行本脚本
-- =====================================================================

CREATE DATABASE IF NOT EXISTS eldercare
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE eldercare;

-- =====================================================================
-- 1. 用户表 user
-- =====================================================================
CREATE TABLE IF NOT EXISTS `user`
(
    `id`                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `phone`             VARCHAR(20)  NOT NULL                COMMENT '手机号',
    `password`          VARCHAR(255) NOT NULL                COMMENT '密码（BCrypt 加密）',
    `real_name`         VARCHAR(50)  NULL                    COMMENT '真实姓名',
    `gender`            VARCHAR(10)  NULL                    COMMENT '性别',
    `birth_date`        DATE         NULL                    COMMENT '出生日期',
    `height`            DECIMAL(5,1) NULL                    COMMENT '身高（cm），用于计算 BMI',
    `avatar`            VARCHAR(500) NULL                    COMMENT '头像 URL',
    `emergency_contact` VARCHAR(20)  NULL                    COMMENT '紧急联系人电话',
    `member_level`      VARCHAR(20)  NULL DEFAULT 'NORMAL'   COMMENT '会员等级：NORMAL普通/SILVER白银/GOLD黄金/PLATINUM铂金/DIAMOND钻石',
    `points`            INT          NULL DEFAULT 0          COMMENT '积分余额（与 point_transaction 流水对账）',
    `status`            VARCHAR(20)  NULL DEFAULT 'ENABLED'  COMMENT '状态：ENABLED启用/DISABLED禁用',
    `role`              VARCHAR(20)  NOT NULL                COMMENT '角色编码冗余字段（MEMBER/ADMIN），与 user_role 关联表保持一致',
    `create_time`       DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`       DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`           TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除：0 未删除/1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_status` (`status`),
    KEY `idx_role` (`role`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表';

-- =====================================================================
-- 2. 刷新令牌表 refresh_token（物理删除，无 deleted 字段）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `refresh_token`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户 ID',
    `token`       VARCHAR(500) NOT NULL                COMMENT 'Refresh Token 值',
    `expire_time` DATETIME     NOT NULL                COMMENT '过期时间',
    `create_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '刷新令牌表';

-- =====================================================================
-- 3. 短信验证码表 sms_code（用 used 字段代替逻辑删除）
-- =====================================================================
CREATE TABLE IF NOT EXISTS `sms_code`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `phone`       VARCHAR(20) NOT NULL                COMMENT '手机号',
    `code`        VARCHAR(10) NOT NULL                COMMENT '验证码',
    `expire_time` DATETIME    NOT NULL                COMMENT '过期时间',
    `used`        TINYINT     NULL DEFAULT 0          COMMENT '是否已使用：0 未使用/1 已使用',
    `create_time` DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_phone` (`phone`),
    KEY `idx_expire_time` (`expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '短信验证码表';

-- =====================================================================
-- 4. 健康记录表 health_record
-- =====================================================================
CREATE TABLE IF NOT EXISTS `health_record`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录 ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '用户 ID',
    `systolic`      INT          NULL                    COMMENT '收缩压（mmHg）',
    `diastolic`     INT          NULL                    COMMENT '舒张压（mmHg）',
    `blood_sugar`   DECIMAL(4,1) NULL                    COMMENT '血糖（mmol/L）',
    `heart_rate`    INT          NULL                    COMMENT '心率（次/分）',
    `weight`        DECIMAL(4,1) NULL                    COMMENT '体重（kg）',
    `bmi`           DECIMAL(3,1) NULL                    COMMENT 'BMI 指数',
    `memo`          VARCHAR(500) NULL                    COMMENT '备注',
    `recorded_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
    `create_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_recorded` (`user_id`, `recorded_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康记录表';

-- =====================================================================
-- 5. 问卷表 questionnaire
-- =====================================================================
CREATE TABLE IF NOT EXISTS `questionnaire`
(
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '问卷 ID',
    `title`       VARCHAR(200)  NOT NULL                COMMENT '问卷标题',
    `description` VARCHAR(1000) NULL                    COMMENT '问卷描述',
    `status`      VARCHAR(20)   NULL DEFAULT 'DRAFT'    COMMENT '状态：DRAFT草稿/PUBLISHED已发布',
    `total_score` INT           NOT NULL DEFAULT 0      COMMENT '问卷满分（仅计分题 max_score 之和，用于规则分百分制换算）',
    `pass_score`  INT           NOT NULL DEFAULT 60     COMMENT '及格分数线（百分制）',
    `grade_rules` JSON          NULL                    COMMENT '评分等级规则 JSON，如 [{"min":90,"label":"优秀"},{"min":60,"label":"及格"}]',
    `create_time` DATETIME      NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT       NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '问卷表';

-- =====================================================================
-- 6. 题目表 question
-- =====================================================================
CREATE TABLE IF NOT EXISTS `question`
(
    `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '题目 ID',
    `questionnaire_id` BIGINT       NOT NULL                COMMENT '问卷 ID',
    `content`          VARCHAR(500) NOT NULL                COMMENT '题目内容',
    `type`             VARCHAR(20)  NOT NULL                COMMENT '类型：SINGLE单选/MULTIPLE多选/TEXT文本',
    `options`          JSON         NULL                    COMMENT '选项 JSON：计分题 [{"text":"很好","meaning":"...","score":10}]；非计分题 [{"text":"18-30岁","meaning":"..."}]；文本题为 NULL',
    `score_mode`       VARCHAR(20)  NOT NULL DEFAULT 'SCORED' COMMENT '计分模式：SCORED计分/NON_SCORED非计分',
    `max_score`        INT          NOT NULL DEFAULT 0      COMMENT '题目满分（计分题取最高选项分值；非计分题 0；文本题 AI 评估上限，不参与规则分）',
    `sort_order`       INT          NULL DEFAULT 0          COMMENT '排序号',
    `create_time`      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_questionnaire_id` (`questionnaire_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '题目表';

-- =====================================================================
-- 7. 评测结果表 assessment_result
-- =====================================================================
CREATE TABLE IF NOT EXISTS `assessment_result`
(
    `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '评测结果 ID',
    `user_id`          BIGINT   NOT NULL                COMMENT '用户 ID',
    `questionnaire_id` BIGINT   NOT NULL                COMMENT '问卷 ID',
    `answers`          JSON     NULL                    COMMENT '答案快照 JSON',
    `rule_score`       INT      NULL                    COMMENT '规则分（百分制，仅计分题按选项分值加总换算）',
    `ai_score`         INT      NULL                    COMMENT 'AI 智能评分（百分制，最终展示分）',
    `ai_suggestion`    TEXT     NULL                    COMMENT 'AI 建议',
    `create_time`      DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`          TINYINT  NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_questionnaire_id` (`questionnaire_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评测结果表';

-- =====================================================================
-- 8. 体检套餐表 appointment_package
-- =====================================================================
CREATE TABLE IF NOT EXISTS `appointment_package`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '套餐 ID',
    `name`            VARCHAR(200) NOT NULL                COMMENT '套餐名称',
    `cover_url`       VARCHAR(500) NULL                    COMMENT '封面图 URL',
    `description`     TEXT         NULL                    COMMENT '套餐描述',
    `price`           INT          NULL DEFAULT 0          COMMENT '价格（积分抵扣）',
    `suitable_people` VARCHAR(200) NULL                    COMMENT '适合人群',
    `items`           JSON         NULL                    COMMENT '包含项目列表 JSON',
    `status`          VARCHAR(20)  NULL DEFAULT 'ENABLED'  COMMENT '状态：ENABLED启用/DISABLED禁用',
    `create_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '体检套餐表';

-- =====================================================================
-- 9. 预约时段表 appointment_slot
-- =====================================================================
CREATE TABLE IF NOT EXISTS `appointment_slot`
(
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '时间段 ID',
    `package_id`    BIGINT      NOT NULL                COMMENT '套餐 ID',
    `appoint_date`  DATE        NOT NULL                COMMENT '预约日期',
    `time_range`    VARCHAR(50) NOT NULL                COMMENT '时间段，如 09:00-10:00',
    `max_count`     INT         NULL DEFAULT 10         COMMENT '最大预约人数',
    `current_count` INT         NULL DEFAULT 0          COMMENT '当前已预约人数',
    `status`        VARCHAR(20) NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE可预约/FULL已满/CLOSED已关闭',
    `create_time`   DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT     NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_package_date` (`package_id`, `appoint_date`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约时段表';

-- =====================================================================
-- 10. 预约表 appointment
-- =====================================================================
CREATE TABLE IF NOT EXISTS `appointment`
(
    `id`                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '预约 ID',
    `user_id`             BIGINT       NOT NULL                COMMENT '用户 ID',
    `slot_id`             BIGINT       NOT NULL                COMMENT '时间段 ID',
    `package_id`          BIGINT       NOT NULL                COMMENT '套餐 ID',
    `status`              VARCHAR(20)  NULL DEFAULT 'PENDING'  COMMENT '状态：PENDING待确认/CONFIRMED已确认/CANCELED已取消/COMPLETED已完成',
    `report_url`          VARCHAR(500) NULL                    COMMENT '体检报告 URL',
    `original_filename`   VARCHAR(255) NULL                    COMMENT '体检报告原始文件名',
    `report_upload_time`  DATETIME     NULL                    COMMENT '报告上传时间',
    `upload_admin_id`     BIGINT       NULL                    COMMENT '上传管理员用户 ID（关联 user.id）',
    `create_time`         DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`         DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`             TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_slot_id` (`slot_id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '预约表';

-- =====================================================================
-- 11. 社区活动表 community_activity
-- =====================================================================
CREATE TABLE IF NOT EXISTS `community_activity`
(
    `id`                      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '活动 ID',
    `title`                   VARCHAR(200) NOT NULL                COMMENT '活动标题',
    `cover_url`               VARCHAR(500) NULL                    COMMENT '封面图 URL',
    `content`                 TEXT         NULL                    COMMENT '活动内容',
    `location`                VARCHAR(200) NULL                    COMMENT '活动地点',
    `registration_start_time` DATETIME     NULL                    COMMENT '报名开始时间',
    `registration_end_time`   DATETIME     NULL                    COMMENT '报名结束时间',
    `activity_start_time`     DATETIME     NULL                    COMMENT '活动开始时间',
    `activity_end_time`       DATETIME     NULL                    COMMENT '活动结束时间',
    `max_participants`        INT          NULL                    COMMENT '人数上限',
    `current_participants`    INT          NULL DEFAULT 0          COMMENT '当前报名人数',
    `status`                  VARCHAR(20)  NULL DEFAULT 'DRAFT'    COMMENT '状态：DRAFT草稿/REGISTRATING报名中/IN_PROGRESS进行中/ENDED已结束',
    `create_time`             DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`             DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`                 TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '社区活动表';

-- =====================================================================
-- 12. 活动报名表 activity_registration
-- =====================================================================
CREATE TABLE IF NOT EXISTS `activity_registration`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '报名 ID',
    `user_id`         BIGINT      NOT NULL                COMMENT '用户 ID',
    `activity_id`     BIGINT      NOT NULL                COMMENT '活动 ID',
    `check_in_status` VARCHAR(20) NULL DEFAULT 'NOT_CHECKED_IN' COMMENT '签到状态：NOT_CHECKED_IN未签到/CHECKED_IN已签到',
    `check_in_time`   DATETIME    NULL                    COMMENT '签到时间（未签到为 NULL）',
    `create_time`     DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
    `update_time`     DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT     NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_activity` (`user_id`, `activity_id`),
    KEY `idx_activity_id` (`activity_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '活动报名表';

-- =====================================================================
-- 13. 健康指导表 health_guidance
-- =====================================================================
CREATE TABLE IF NOT EXISTS `health_guidance`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '指导 ID',
    `user_id`     BIGINT      NOT NULL                COMMENT '用户 ID',
    `type`        VARCHAR(20) NOT NULL                COMMENT '类型：DIET饮食/EXERCISE运动/DAILY作息/DATA_SUMMARY数据小结',
    `indicator`   VARCHAR(20) NULL                    COMMENT '触发指标：SYSTOLIC收缩压/DIASTOLIC舒张压/BLOOD_SUGAR血糖/HEART_RATE心率/BMI体质指数/WEIGHT体重（用于同日去重）',
    `content`     TEXT        NOT NULL                COMMENT '指导内容',
    `is_read`     TINYINT     NULL DEFAULT 0          COMMENT '是否已读：0 未读/1 已读',
    `create_time` DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
    `update_time` DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_user_type` (`user_id`, `type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '健康指导表';

-- =====================================================================
-- 14. AI 会话表 ai_conversation_session
-- =====================================================================
CREATE TABLE IF NOT EXISTS `ai_conversation_session`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '会话 ID',
    `user_id`      BIGINT       NOT NULL                COMMENT '用户 ID',
    `session_name` VARCHAR(100) NULL                    COMMENT '会话名称',
    `create_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 会话表';

-- =====================================================================
-- 15. AI 对话消息表 ai_conversation_message
-- =====================================================================
CREATE TABLE IF NOT EXISTS `ai_conversation_message`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
    `session_id`  BIGINT      NOT NULL                COMMENT '会话 ID',
    `user_id`     BIGINT      NOT NULL                COMMENT '用户 ID',
    `role`        VARCHAR(20) NOT NULL                COMMENT '角色：USER用户/ASSISTANT助手',
    `message`     TEXT        NOT NULL                COMMENT '消息内容',
    `status`      VARCHAR(20) NULL DEFAULT 'SUCCESS'  COMMENT '消息状态：SUCCESS成功/FAILED失败（补：文档 5.4 要求失败标记持久化）',
    `create_time` DATETIME    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT     NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_session` (`user_id`, `session_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI 对话消息表';

-- =====================================================================
-- 16. 消息表 message
-- =====================================================================
CREATE TABLE IF NOT EXISTS `message`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '消息 ID',
    `user_id`     BIGINT       NOT NULL                COMMENT '用户 ID',
    `title`       VARCHAR(200) NOT NULL                COMMENT '消息标题',
    `content`     TEXT         NULL                    COMMENT '消息内容',
    `type`        VARCHAR(20)  NULL                    COMMENT '消息类型：APPOINTMENT预约/ACTIVITY活动/SYSTEM系统/HEALTH_REMINDER健康提醒',
    `is_read`     TINYINT      NULL DEFAULT 0          COMMENT '是否已读：0 未读/1 已读',
    `create_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_is_read` (`is_read`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息表';

-- =====================================================================
-- 17. 系统配置表 sys_config
-- =====================================================================
CREATE TABLE IF NOT EXISTS `sys_config`
(
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '配置 ID',
    `config_key`   VARCHAR(100) NOT NULL                COMMENT '配置键',
    `config_value` TEXT         NULL                    COMMENT '配置值',
    `description`  VARCHAR(500) NULL                    COMMENT '配置描述',
    `create_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`      TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表';

-- =====================================================================
-- 18. 积分流水表 point_transaction
-- =====================================================================
CREATE TABLE IF NOT EXISTS `point_transaction`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '流水 ID',
    `user_id`       BIGINT       NOT NULL                COMMENT '用户 ID',
    `type`          VARCHAR(30)  NOT NULL                COMMENT '类型：REGISTER_BONUS注册赠送/ACTIVITY_CHECKIN活动签到/ASSESSMENT_COMPLETE评测完成/ADMIN_ADJUST管理员调整/APPOINTMENT_CONSUME体检预约消费/EXPIRE积分过期',
    `change_amount` INT          NOT NULL                COMMENT '变动积分（正=获得，负=扣减）',
    `balance_after` INT          NOT NULL                COMMENT '变动后积分余额',
    `remain_amount` INT          NOT NULL DEFAULT 0      COMMENT '获得类流水的剩余可用积分，消费按 FIFO 扣减；消耗类流水为 0',
    `expire_time`   DATETIME     NULL                    COMMENT '获得类流水的过期时间（获得时间 + 1 年）；消耗类流水为空',
    `batch_tx_id`   BIGINT       NULL                    COMMENT '消耗类流水关联的被扣获得批次流水 ID',
    `description`   VARCHAR(200) NULL                    COMMENT '业务说明',
    `ref_id`        BIGINT       NULL                    COMMENT '关联业务记录 ID（预约 ID、报名 ID、评测 ID 等）',
    `create_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除（取消退还时消费流水标记 deleted=1）',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_expire_remain` (`expire_time`, `remain_amount`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '积分流水表';

-- =====================================================================
-- 19. 角色表 role
-- =====================================================================
CREATE TABLE IF NOT EXISTS `role`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '角色 ID',
    `role_code`   VARCHAR(50)  NOT NULL                COMMENT '角色编码：MEMBER会员/ADMIN管理员',
    `role_name`   VARCHAR(50)  NOT NULL                COMMENT '角色名称',
    `description` VARCHAR(200) NULL                    COMMENT '角色描述',
    `status`      TINYINT      NULL DEFAULT 1          COMMENT '状态：1 启用/0 停用',
    `create_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色表';

-- =====================================================================
-- 20. 权限表 permission
-- =====================================================================
CREATE TABLE IF NOT EXISTS `permission`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '权限 ID',
    `permission_code` VARCHAR(100) NOT NULL                COMMENT '权限编码（域:模块:操作，如 member:health:list）',
    `permission_name` VARCHAR(100) NOT NULL                COMMENT '权限名称',
    `description`     VARCHAR(200) NULL                    COMMENT '权限描述',
    `create_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限表';

-- =====================================================================
-- 21. 资源表 resource
-- =====================================================================
CREATE TABLE IF NOT EXISTS `resource`
(
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '资源 ID',
    `resource_code` VARCHAR(100) NOT NULL                COMMENT '资源编码（api:member:health / menu:admin:dashboard / btn:assessment:publish）',
    `resource_name` VARCHAR(100) NOT NULL                COMMENT '资源名称',
    `resource_type` VARCHAR(20)  NOT NULL                COMMENT '资源类型：API接口/MENU菜单/BUTTON按钮',
    `path`          VARCHAR(200) NULL                    COMMENT '资源路径（接口类型为接口路径模式，如 /api/member/health/**；菜单/按钮可为空）',
    `parent_id`     BIGINT       NULL DEFAULT 0          COMMENT '父资源 ID（菜单树形结构）',
    `sort_order`    INT          NULL DEFAULT 0          COMMENT '排序号',
    `create_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`       TINYINT      NULL DEFAULT 0          COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`resource_type`),
    KEY `idx_parent` (`parent_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '资源表';

-- =====================================================================
-- 22. 用户角色关联表 user_role
-- =====================================================================
CREATE TABLE IF NOT EXISTS `user_role`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
    `user_id`     BIGINT   NOT NULL                COMMENT '用户 ID（关联 user.id）',
    `role_id`     BIGINT   NOT NULL                COMMENT '角色 ID（关联 role.id）',
    `create_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户角色关联表';

-- =====================================================================
-- 23. 角色权限关联表 role_permission
-- =====================================================================
CREATE TABLE IF NOT EXISTS `role_permission`
(
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
    `role_id`       BIGINT   NOT NULL                COMMENT '角色 ID（关联 role.id）',
    `permission_id` BIGINT   NOT NULL                COMMENT '权限 ID（关联 permission.id）',
    `create_time`   DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色权限关联表';

-- =====================================================================
-- 24. 权限资源关联表 permission_resource
-- =====================================================================
CREATE TABLE IF NOT EXISTS `permission_resource`
(
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联 ID',
    `permission_id` BIGINT   NOT NULL                COMMENT '权限 ID（关联 permission.id）',
    `resource_id`   BIGINT   NOT NULL                COMMENT '资源 ID（关联 resource.id）',
    `create_time`   DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_resource` (`permission_id`, `resource_id`),
    KEY `idx_resource_id` (`resource_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '权限资源关联表';

-- =====================================================================
-- 初始数据
-- =====================================================================

-- 系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `description`) VALUES
('ai_chat_system_prompt', '你是一位专业的健康顾问，请用亲切、易懂的语言回答用户的健康问题。', 'AI 对话系统提示词'),
('register_bonus_points', '100', '注册赠送积分'),
('checkin_bonus_points', '50', '活动签到赠送积分'),
('health_assessment_bonus_points', '20', '完成健康评测赠送积分'),
('health_assessment_min_score', '60', '健康评测及格分数线'),
('access_token_expire_hours', '2', 'Access Token 有效期（小时）'),
('refresh_token_expire_days', '7', 'Refresh Token 有效期（天）');

-- 默认用户
-- 管理员：13800000000 / Admin@123456
-- 测试会员：13800138000 / Test@123456
INSERT INTO `user` (`id`, `phone`, `password`, `real_name`, `member_level`, `points`, `status`, `role`) VALUES
(1, '13800000000', '$2b$10$5xxJYAxX3bB35VkjlRAuauILyrcKEUXJINVQXrWPYl6vhfZlIiy46', '系统管理员', 'PLATINUM', 99999, 'ENABLED', 'ADMIN'),
(2, '13800138000', '$2b$10$La.Q.aZ.SUB5Ej3neFdzGOUYLva/QuO7sALyOPaBCxyYkro9Cpzjm', '测试用户', 'NORMAL', 1000, 'ENABLED', 'MEMBER');

-- 内置角色（RBAC）
INSERT INTO `role` (`id`, `role_code`, `role_name`, `description`) VALUES
(1, 'ADMIN', '管理员', '系统管理员，可访问管理端与会员端全部功能'),
(2, 'MEMBER', '会员', '社区老人会员，可访问会员端功能');

-- 用户角色关联
INSERT INTO `user_role` (`user_id`, `role_id`) VALUES
(1, 1),
(2, 2);

-- 示例问卷
-- 问卷 1 总分 20（仅计分题：睡眠 10 + 运动 10），及格线 60（百分制）
INSERT INTO `questionnaire` (`id`, `title`, `description`, `status`, `total_score`, `pass_score`, `grade_rules`) VALUES
(1, '基础健康状况调查问卷', '通过简单的问题了解您的基本健康状况', 'PUBLISHED', 20, 60,
 '[{"min":90,"label":"优秀","description":"整体健康状况良好"},{"min":80,"label":"良好","description":"整体状况较好，个别指标需关注"},{"min":60,"label":"及格","description":"整体状况一般，建议加强健康管理"},{"min":0,"label":"需关注","description":"整体状况欠佳，建议及时就医咨询"}]');

INSERT INTO `question` (`id`, `questionnaire_id`, `content`, `type`, `score_mode`, `options`, `max_score`, `sort_order`) VALUES
(1, 1, '您的年龄是？', 'SINGLE', 'NON_SCORED', '[{"text":"18-30岁","meaning":"青壮年人群，身体机能良好"},{"text":"31-45岁","meaning":"中年人群，机能开始缓慢下降"},{"text":"46-60岁","meaning":"中老年人群，慢病风险逐渐上升"},{"text":"60岁以上","meaning":"老年人群，慢病风险相对较高"}]', 0, 1),
(2, 1, '您的睡眠质量如何？', 'SINGLE', 'SCORED', '[{"text":"很好，每天睡 7-8 小时","meaning":"睡眠充足，质量良好","score":10},{"text":"一般，偶尔失眠","meaning":"睡眠基本正常，偶有波动","score":6},{"text":"较差，经常失眠","meaning":"存在睡眠障碍，需引起关注","score":3},{"text":"非常差，严重影响生活","meaning":"严重睡眠障碍，建议就医","score":1}]', 10, 2),
(3, 1, '您每周运动几次？', 'SINGLE', 'SCORED', '[{"text":"几乎不运动","meaning":"缺乏运动，体能下降风险高","score":1},{"text":"1-2 次","meaning":"运动量偏少，建议增加","score":4},{"text":"3-4 次","meaning":"运动量适中，习惯良好","score":8},{"text":"5 次以上","meaning":"运动频繁，习惯优良","score":10}]', 10, 3),
(4, 1, '请简要描述您目前的健康状况', 'TEXT', 'NON_SCORED', NULL, 10, 4);

-- 示例体检套餐
INSERT INTO `appointment_package` (`id`, `name`, `description`, `price`, `suitable_people`, `items`, `status`) VALUES
(1, '基础体检套餐', '适合健康人群的基础体检，包含常规检查项目', 500, '所有人群',
 '["血常规", "尿常规", "肝功能", "肾功能", "心电图", "胸部 X 光"]', 'ENABLED'),
(2, '中老年体检套餐', '针对中老年人的全面体检，包含心脑血管等专项检查', 1000, '45 岁以上人群',
 '["基础套餐全部项目", "肿瘤标志物", "颈动脉彩超", "骨密度检测", "甲状腺功能"]', 'ENABLED'),
(3, '女性专属套餐', '针对女性健康特点设计的专项体检套餐', 800, '女性人群',
 '["基础套餐全部项目", "妇科检查", "乳腺彩超", "HPV 检测", "TCT 检查"]', 'ENABLED');

-- 示例预约时段
INSERT INTO `appointment_slot` (`id`, `package_id`, `appoint_date`, `time_range`, `max_count`, `current_count`, `status`) VALUES
(1, 1, '2026-08-20', '09:00-10:00', 10, 1, 'AVAILABLE'),
(2, 2, '2026-08-20', '10:00-11:00', 10, 1, 'AVAILABLE'),
(3, 3, '2026-08-21', '09:00-10:00', 10, 1, 'AVAILABLE');

-- 示例预约记录（关联上述时段）
INSERT INTO `appointment` (`id`, `user_id`, `slot_id`, `package_id`, `status`) VALUES
(1, 2, 1, 1, 'COMPLETED'),
(2, 2, 2, 2, 'PENDING'),
(3, 2, 3, 3, 'CANCELED');

-- =====================================================================
-- RBAC 权限/资源种子数据（文档 5.1 / 6.3.19~6.3.24 / 8.2）
-- 覆盖现有 5.x 模块全部接口；幂等可重复执行：
--   permission / role_permission / permission_resource 靠唯一键去重；
--   resource 表无唯一键，整段仅当 resource 表为空时写入。
-- 对全新库：随本脚本一并执行；对已有库：单独执行本段一次。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 权限 permission（域:模块:操作），uk_permission_code 去重
-- ---------------------------------------------------------------------
INSERT INTO `permission` (`permission_code`, `permission_name`, `description`) VALUES
-- 会员端：健康记录
('member:health:add', '新增健康记录', '会员新增健康记录'),
('member:health:list', '健康记录列表', '会员查看自己的健康记录'),
('member:health:trend', '健康趋势查询', '会员查看健康指标趋势'),
-- 会员端：健康评测
('member:assessment:list', '问卷列表', '会员查看已发布问卷'),
('member:assessment:view', '问卷详情', '会员查看问卷详情并答题'),
('member:assessment:submit', '提交评测', '会员提交健康评测'),
('member:assessment:history', '评测历史', '会员查看评测历史'),
('member:assessment:detail', '评测报告', '会员查看评测报告'),
-- 会员端：AI 对话
('member:chat:create', '新建会话', '会员创建 AI 对话会话'),
('member:chat:list', '会话列表', '会员查看会话列表'),
('member:chat:delete', '删除会话', '会员删除会话'),
('member:chat:history', '会话消息', '会员查看会话历史消息'),
('member:chat:send', '发送消息', '会员发送消息（含流式对话）'),
-- 会员端：体检预约
('member:appointment:packages', '体检套餐列表', '会员查看体检套餐'),
('member:appointment:slots', '套餐时段查询', '会员查看套餐可预约时段'),
('member:appointment:create', '预约体检', '会员提交体检预约'),
('member:appointment:cancel', '取消预约', '会员取消预约'),
('member:appointment:list', '我的预约', '会员查看我的预约列表'),
('member:appointment:report', '下载体检报告', '会员下载体检报告'),
-- 会员端：社区活动
('member:activity:list', '活动列表', '会员查看社区活动'),
('member:activity:detail', '活动详情', '会员查看活动详情'),
('member:activity:register', '报名活动', '会员报名社区活动'),
('member:activity:mine', '我的报名', '会员查看我的报名'),
('member:activity:checkin', '活动签到', '会员活动签到（获取积分）'),
-- 会员端：消息通知
('member:message:list', '消息列表', '会员查看消息'),
('member:message:unread', '未读消息数', '会员查看未读消息数'),
('member:message:read', '标记已读', '会员标记消息已读'),
-- 会员端：积分 / 个人中心 / 权限
('member:points:list', '积分明细', '会员查看积分明细'),
('member:profile:view', '个人信息', '会员查看个人信息'),
('member:profile:update', '更新个人信息', '会员更新个人信息'),
('member:permissions:view', '我的权限/菜单', '会员获取自己的权限与菜单资源'),
-- 管理端：健康记录
('admin:health:list', '健康记录管理', '管理端查看会员健康记录'),
('admin:health:trend', '健康趋势管理', '管理端查看会员健康趋势'),
-- 管理端：评测 / 预约 / 活动 / 会员 / 消息 / 配置 / 仪表盘
('admin:assessment:manage', '评测管理', '管理端问卷与题目管理'),
('admin:appointment:manage', '体检管理', '管理端套餐/时段/预约管理'),
('admin:appointment:report', '上传体检报告', '管理端上传体检报告'),
('admin:activity:manage', '活动管理', '管理端活动管理'),
('admin:activity:registrations', '报名管理', '管理端查看活动报名'),
('admin:member:manage', '会员管理', '管理端会员启停/等级/积分/重置密码'),
('admin:message:manage', '消息管理', '管理端消息列表/详情/删除'),
('admin:message:push', '消息推送', '管理端推送消息'),
('admin:config:manage', '系统配置', '管理端系统配置管理'),
('admin:dashboard:view', '仪表盘', '管理端查看数据仪表盘'),
-- 管理端：RBAC 授权管理
('admin:role:manage', '角色管理', '管理端角色增删改查与授权分配'),
('admin:permission:manage', '权限管理', '管理端权限增删改查与挂资源'),
('admin:resource:manage', '资源管理', '管理端资源增删改查')
ON DUPLICATE KEY UPDATE `permission_name` = `permission_name`;

-- ---------------------------------------------------------------------
-- 2. 资源 resource（API/MENU/BUTTON 三类）
--    显式 ID 保证菜单树 parent_id 确定；仅当 resource 表为空时写入
-- ---------------------------------------------------------------------
INSERT INTO `resource` (`id`, `resource_code`, `resource_name`, `resource_type`, `path`, `parent_id`, `sort_order`)
SELECT * FROM (
-- API 接口资源（接口访问控制，Ant 路径模式）
SELECT 1, 'api:member:health', '会员健康接口', 'API', '/api/member/health/**', 0, 1
UNION ALL SELECT 2, 'api:member:assessment', '会员评测接口', 'API', '/api/member/assessment/**', 0, 2
UNION ALL SELECT 3, 'api:member:chat', 'AI 对话接口', 'API', '/api/member/chat/**', 0, 3
UNION ALL SELECT 4, 'api:member:appointment', '体检预约接口', 'API', '/api/member/appointment/**', 0, 4
UNION ALL SELECT 5, 'api:member:activity', '社区活动接口', 'API', '/api/member/activity/**', 0, 5
UNION ALL SELECT 6, 'api:member:message', '消息接口', 'API', '/api/member/message/**', 0, 6
UNION ALL SELECT 7, 'api:member:points', '积分接口', 'API', '/api/member/points/**', 0, 7
UNION ALL SELECT 8, 'api:member:profile', '个人中心接口', 'API', '/api/member/profile/**', 0, 8
UNION ALL SELECT 9, 'api:member:permissions', '权限菜单接口', 'API', '/api/member/permissions', 0, 9
UNION ALL SELECT 10, 'api:admin:health-record', '健康档案管理接口', 'API', '/api/admin/health-record/**', 0, 10
UNION ALL SELECT 11, 'api:admin:assessment', '评测管理接口', 'API', '/api/admin/assessment/**', 0, 11
UNION ALL SELECT 12, 'api:admin:appointment', '体检管理接口', 'API', '/api/admin/appointment/**', 0, 12
UNION ALL SELECT 13, 'api:admin:activity', '活动管理接口', 'API', '/api/admin/activity/**', 0, 13
UNION ALL SELECT 14, 'api:admin:members', '会员管理接口', 'API', '/api/admin/members/**', 0, 14
UNION ALL SELECT 15, 'api:admin:message', '消息管理接口', 'API', '/api/admin/message/**', 0, 15
UNION ALL SELECT 16, 'api:admin:config', '系统配置接口', 'API', '/api/admin/config/**', 0, 16
UNION ALL SELECT 17, 'api:admin:dashboard', '仪表盘接口', 'API', '/api/admin/dashboard/**', 0, 17
UNION ALL SELECT 18, 'api:admin:role', '角色管理接口', 'API', '/api/admin/role/**', 0, 18
UNION ALL SELECT 19, 'api:admin:permission', '权限管理接口', 'API', '/api/admin/permission/**', 0, 19
UNION ALL SELECT 20, 'api:admin:resource', '资源管理接口', 'API', '/api/admin/resource/**', 0, 20
-- 会员端菜单
UNION ALL SELECT 21, 'menu:member:home', '首页', 'MENU', NULL, 0, 1
UNION ALL SELECT 22, 'menu:member:health', '健康档案', 'MENU', NULL, 0, 2
UNION ALL SELECT 23, 'menu:member:assessment', '健康评测', 'MENU', NULL, 0, 3
UNION ALL SELECT 24, 'menu:member:chat', 'AI 助手', 'MENU', NULL, 0, 4
UNION ALL SELECT 25, 'menu:member:appointment', '体检预约', 'MENU', NULL, 0, 5
UNION ALL SELECT 26, 'menu:member:activity', '社区活动', 'MENU', NULL, 0, 6
UNION ALL SELECT 27, 'menu:member:message', '消息中心', 'MENU', NULL, 0, 7
UNION ALL SELECT 28, 'menu:member:points', '我的积分', 'MENU', NULL, 0, 8
UNION ALL SELECT 29, 'menu:member:profile', '个人中心', 'MENU', NULL, 0, 9
-- 管理端菜单
UNION ALL SELECT 30, 'menu:admin:dashboard', '工作台', 'MENU', NULL, 0, 1
UNION ALL SELECT 31, 'menu:admin:health', '健康档案管理', 'MENU', NULL, 0, 2
UNION ALL SELECT 32, 'menu:admin:assessment', '评测管理', 'MENU', NULL, 0, 3
UNION ALL SELECT 33, 'menu:admin:appointment', '体检管理', 'MENU', NULL, 0, 4
UNION ALL SELECT 34, 'menu:admin:activity', '活动管理', 'MENU', NULL, 0, 5
UNION ALL SELECT 35, 'menu:admin:member', '会员管理', 'MENU', NULL, 0, 6
UNION ALL SELECT 36, 'menu:admin:message', '消息管理', 'MENU', NULL, 0, 7
UNION ALL SELECT 37, 'menu:admin:config', '系统配置', 'MENU', NULL, 0, 8
UNION ALL SELECT 38, 'menu:admin:rbac', '权限管理', 'MENU', NULL, 0, 9
UNION ALL SELECT 39, 'menu:admin:rbac:role', '角色管理', 'MENU', NULL, 38, 1
UNION ALL SELECT 40, 'menu:admin:rbac:permission', '权限管理', 'MENU', NULL, 38, 2
UNION ALL SELECT 41, 'menu:admin:rbac:resource', '资源管理', 'MENU', NULL, 38, 3
-- 按钮资源（页面操作按钮显隐）
UNION ALL SELECT 42, 'btn:activity:checkin', '活动签到', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 43, 'btn:appointment:cancel', '取消预约', 'BUTTON', NULL, 0, 2
UNION ALL SELECT 44, 'btn:assessment:publish', '发布问卷', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 45, 'btn:assessment:manage', '问卷/题目管理', 'BUTTON', NULL, 0, 2
UNION ALL SELECT 46, 'btn:appointment:manage', '套餐/时段管理', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 47, 'btn:appointment:report', '上传报告', 'BUTTON', NULL, 0, 2
UNION ALL SELECT 48, 'btn:appointment:status', '预约状态处理', 'BUTTON', NULL, 0, 3
UNION ALL SELECT 49, 'btn:activity:manage', '活动编辑/删除', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 50, 'btn:activity:registrations', '报名管理', 'BUTTON', NULL, 0, 2
UNION ALL SELECT 51, 'btn:member:manage', '会员操作', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 52, 'btn:message:push', '消息推送', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 53, 'btn:message:delete', '消息删除', 'BUTTON', NULL, 0, 2
UNION ALL SELECT 54, 'btn:config:manage', '配置编辑', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 55, 'btn:role:assign', '角色授权/分配用户', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 56, 'btn:permission:manage', '权限编辑', 'BUTTON', NULL, 0, 1
UNION ALL SELECT 57, 'btn:resource:manage', '资源编辑', 'BUTTON', NULL, 0, 1
) t
WHERE NOT EXISTS (SELECT 1 FROM `resource` LIMIT 1);

-- ---------------------------------------------------------------------
-- 3. 角色→权限 关联 role_permission（uk_role_permission 去重）
--    ADMIN 授全部权限；MEMBER 授会员端权限（满足需求文档「ADMIN 可访问所有接口」）
-- ---------------------------------------------------------------------
INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
CROSS JOIN `permission` p
WHERE r.`role_code` = 'ADMIN'
ON DUPLICATE KEY UPDATE `permission_id` = `permission_id`;

INSERT INTO `role_permission` (`role_id`, `permission_id`)
SELECT r.`id`, p.`id`
FROM `role` r
CROSS JOIN `permission` p ON p.`permission_code` LIKE 'member:%'
WHERE r.`role_code` = 'MEMBER'
ON DUPLICATE KEY UPDATE `permission_id` = `permission_id`;

-- ---------------------------------------------------------------------
-- 4. 权限→资源 关联 permission_resource（uk_permission_resource 去重）
--    每个权限挂其对应模块的 API 资源 + 菜单资源 + 相关按钮资源
-- ---------------------------------------------------------------------
-- 会员端：健康记录
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:health', 'menu:member:health')
WHERE p.`permission_code` IN ('member:health:add', 'member:health:list', 'member:health:trend')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：健康评测
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:assessment', 'menu:member:assessment')
WHERE p.`permission_code` IN ('member:assessment:list', 'member:assessment:view', 'member:assessment:submit', 'member:assessment:history', 'member:assessment:detail')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：AI 对话
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:chat', 'menu:member:chat')
WHERE p.`permission_code` IN ('member:chat:create', 'member:chat:list', 'member:chat:delete', 'member:chat:history', 'member:chat:send')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：体检预约（含取消按钮）
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:appointment', 'menu:member:appointment', 'btn:appointment:cancel')
WHERE p.`permission_code` IN ('member:appointment:packages', 'member:appointment:slots', 'member:appointment:create', 'member:appointment:cancel', 'member:appointment:list', 'member:appointment:report')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：社区活动（含签到按钮）
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:activity', 'menu:member:activity', 'btn:activity:checkin')
WHERE p.`permission_code` IN ('member:activity:list', 'member:activity:detail', 'member:activity:register', 'member:activity:mine', 'member:activity:checkin')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：消息通知
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:message', 'menu:member:message')
WHERE p.`permission_code` IN ('member:message:list', 'member:message:unread', 'member:message:read')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：积分明细
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:points', 'menu:member:points')
WHERE p.`permission_code` = 'member:points:list'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：个人中心
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:member:profile', 'menu:member:profile')
WHERE p.`permission_code` IN ('member:profile:view', 'member:profile:update')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 会员端：我的权限/菜单
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` = 'api:member:permissions'
WHERE p.`permission_code` = 'member:permissions:view'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：健康记录
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:health-record', 'menu:admin:health')
WHERE p.`permission_code` IN ('admin:health:list', 'admin:health:trend')
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：评测管理（含发布/编辑按钮）
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:assessment', 'menu:admin:assessment', 'btn:assessment:publish', 'btn:assessment:manage')
WHERE p.`permission_code` = 'admin:assessment:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：体检管理（含套餐/状态按钮）
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:appointment', 'menu:admin:appointment', 'btn:appointment:manage', 'btn:appointment:status')
WHERE p.`permission_code` = 'admin:appointment:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：上传体检报告
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:appointment', 'btn:appointment:report')
WHERE p.`permission_code` = 'admin:appointment:report'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：活动管理（含编辑/报名管理按钮）
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:activity', 'menu:admin:activity', 'btn:activity:manage', 'btn:activity:registrations')
WHERE p.`permission_code` = 'admin:activity:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：报名管理
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:activity', 'btn:activity:registrations')
WHERE p.`permission_code` = 'admin:activity:registrations'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：会员管理
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:members', 'menu:admin:member', 'btn:member:manage')
WHERE p.`permission_code` = 'admin:member:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：消息管理（列表/详情/删除）
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:message', 'menu:admin:message', 'btn:message:delete')
WHERE p.`permission_code` = 'admin:message:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：消息推送
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:message', 'btn:message:push')
WHERE p.`permission_code` = 'admin:message:push'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：系统配置
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:config', 'menu:admin:config', 'btn:config:manage')
WHERE p.`permission_code` = 'admin:config:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：仪表盘
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:dashboard', 'menu:admin:dashboard')
WHERE p.`permission_code` = 'admin:dashboard:view'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：角色管理（含 RBAC 菜单）
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:role', 'menu:admin:rbac', 'menu:admin:rbac:role', 'btn:role:assign')
WHERE p.`permission_code` = 'admin:role:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：权限管理
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:permission', 'menu:admin:rbac', 'menu:admin:rbac:permission', 'btn:permission:manage')
WHERE p.`permission_code` = 'admin:permission:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;

-- 管理端：资源管理
INSERT INTO `permission_resource` (`permission_id`, `resource_id`)
SELECT p.`id`, r.`id`
FROM `permission` p
JOIN `resource` r ON r.`resource_code` IN ('api:admin:resource', 'menu:admin:rbac', 'menu:admin:rbac:resource', 'btn:resource:manage')
WHERE p.`permission_code` = 'admin:resource:manage'
ON DUPLICATE KEY UPDATE `resource_id` = `resource_id`;
