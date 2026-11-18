-- ============================================================
-- CRM 邮件生成历史
--
--   邮件引擎每次成功生成的邮件都落一份，便于回看"这封是怎么生成的"、
--   复用上一版继续改、以及回溯生成参数与 token 消耗。
-- ============================================================

CREATE TABLE `t_crm_email_generation` (
    `id` VARCHAR(36) NOT NULL,
    `request_id` VARCHAR(64) NOT NULL COMMENT '一次生成的请求 ID（与 t_llm_request_log 对齐）',
    `subject` VARCHAR(512) NOT NULL DEFAULT '' COMMENT '主题',
    `content` LONGTEXT NOT NULL COMMENT '正文',
    `language` VARCHAR(16) NOT NULL DEFAULT 'en' COMMENT '目标语言码',
    `tone` VARCHAR(32) NOT NULL DEFAULT 'new_customer' COMMENT '语气：new_customer / existing_customer',
    `scene` VARCHAR(32) NOT NULL DEFAULT 'inquiry_reply' COMMENT '业务场景',
    `model` VARCHAR(128) NOT NULL DEFAULT '' COMMENT '模型（provider:model）',
    `source_content` LONGTEXT COMMENT '输入的草稿；为空表示直接生成',
    `custom_instruction` VARCHAR(1000) DEFAULT NULL COMMENT '使用者额外提出的特殊场景与要求',
    `customer_name` VARCHAR(128) DEFAULT NULL COMMENT '客户称呼',
    `sender_name` VARCHAR(64) DEFAULT NULL COMMENT '我方发件人',
    `sender_company` VARCHAR(128) DEFAULT NULL COMMENT '我方公司',
    `sender_title` VARCHAR(64) DEFAULT NULL COMMENT '我方职位',
    `elapsed_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '生成耗时（毫秒）',
    `prompt_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入 token',
    `completion_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出 token（含推理模型的思考 token）',
    `total_tokens` INT NOT NULL DEFAULT 0 COMMENT '总 token',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '生成人',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_create_time` (`deleted`, `create_time`),
    KEY `idx_owner` (`owner_id`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 邮件生成历史';
