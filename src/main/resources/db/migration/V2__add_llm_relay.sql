/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

-- LLM 中转（OpenAI 兼容）新增表：
--   t_llm_provider       — 上游 provider 配置（baseUrl / apiKey 加密存储 / enabled）
--   t_llm_request_log    — 每次调用落库一行（含 traceId + token 用量 + 状态码 + 延迟）
--
-- 注意：
--   1. api_key 走 AES-GCM 应用层加密后落库（密文 + IV + tag 一起存），密钥从环境变量注入。
--   2. trace_id 与 OpenTelemetry 生成的 32 位 hex traceId 对齐，便于和日志关联。
--   3. user_id 暂不强外键约束（沿用 t_user.id 的 VARCHAR(36) 形态），避免后续 auth 模型变动影响本表。
--   4. endpoint 取值约定：chat_completions / embeddings（与 Done when #5 对齐）。

CREATE TABLE `t_llm_provider` (
    `id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(64) NOT NULL COMMENT 'provider 唯一名（与 model 拼接用，如 openai / deepseek / ollama）',
    `base_url` VARCHAR(512) NOT NULL COMMENT '上游 baseUrl，例如 https://api.openai.com',
    `api_key_ciphertext` VARBINARY(512) NOT NULL COMMENT 'AES-GCM 加密后的 API key（含 IV + tag + ciphertext）',
    `enabled` TINYINT NOT NULL DEFAULT '1' COMMENT '1: 启用, 0: 停用',
    `priority` INT NOT NULL DEFAULT '100' COMMENT '路由优先级，数值越小越优先（同 model 多 provider 时备用）',
    `timeout_ms_override` INT DEFAULT NULL COMMENT '单 provider 超时覆盖（毫秒），NULL 表示走 llm.upstream.timeout-ms 默认值',
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `name` (`name`),
    KEY `idx_enabled_priority` (`enabled`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `t_llm_request_log` (
    `id` VARCHAR(36) NOT NULL,
    `user_id` VARCHAR(36) NOT NULL COMMENT 'JWT subject（t_user.id）',
    `provider` VARCHAR(64) NOT NULL COMMENT '上游 provider 名',
    `model` VARCHAR(128) NOT NULL COMMENT '请求里的 model 字段（原值，未拆 provider 前缀）',
    `endpoint` VARCHAR(32) NOT NULL COMMENT 'chat_completions / embeddings',
    `prompt_tokens` INT NOT NULL DEFAULT '0',
    `completion_tokens` INT NOT NULL DEFAULT '0',
    `total_tokens` INT NOT NULL DEFAULT '0',
    `status_code` INT NOT NULL COMMENT 'HTTP 状态码（上游返回码；流式断连写 499）',
    `latency_ms` INT NOT NULL DEFAULT '0' COMMENT '端到端耗时（毫秒）',
    `request_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端请求 ID（X-Request-Id header）',
    `trace_id` VARCHAR(32) DEFAULT NULL COMMENT 'OpenTelemetry traceId，32 位 hex',
    `error_message` VARCHAR(512) DEFAULT NULL COMMENT '上游错误摘要；成功时 NULL',
    `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_created` (`user_id`, `created_at`),
    KEY `idx_provider_model_created` (`provider`, `model`, `created_at`),
    KEY `idx_trace` (`trace_id`),
    KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;