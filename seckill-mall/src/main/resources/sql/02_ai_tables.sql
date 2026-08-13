-- ============================================================
-- 02_ai_tables.sql — AI 现代化新增表（P0 阶段）
-- MySQL 8.0+ / InnoDB / utf8mb4
-- 主键策略：MyBatis-Plus 雪花算法（ASSIGN_ID）
-- 包含：t_user_event（行为埋点）、t_ai_audit（AI调用审计）、
--       t_ai_conversation（客服对话）、t_ai_message（客服消息）
-- 所有表均为 append-only 或软删除模式，无外键，按业务维度建索引
-- 生成时间：2026-08-14
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE `seckill_mall`;

-- ============================================================
-- P0：行为埋点表（append-only，参考 t_login_log 模式）
-- 记录用户浏览/点击/加购/下单等行为，供推荐与风控消费
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_user_event` (
  `id`          BIGINT      NOT NULL,
  `user_id`     BIGINT      NULL,
  `event_type`  VARCHAR(32) NOT NULL,
  `target_type` VARCHAR(32) NULL,
  `target_id`   BIGINT      NULL,
  `ext`         JSON        NULL,
  `device_id`   VARCHAR(64) NULL,
  `create_time` DATETIME    NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_user_time`  (`user_id`, `create_time`),
  INDEX `idx_event_time` (`event_type`, `create_time`),
  INDEX `idx_target`     (`target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为埋点（append-only）';

-- ============================================================
-- P0：AI 调用审计表（append-only）
-- 记录每次 LLM 调用的 caller/model/tokens/cost/elapsed，供运营分析与成本治理
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_ai_audit` (
  `id`          BIGINT        NOT NULL,
  `caller`      VARCHAR(64)   NOT NULL,
  `user_id`     BIGINT        NULL,
  `model`       VARCHAR(32)   NOT NULL,
  `prompt_hash` VARCHAR(64)   NULL,
  `tokens_in`   INT           NOT NULL DEFAULT 0,
  `tokens_out`  INT           NOT NULL DEFAULT 0,
  `cost`        DECIMAL(10,4) NOT NULL DEFAULT 0,
  `elapsed_ms`  INT           NOT NULL,
  `success`     TINYINT       NOT NULL DEFAULT 1,
  `escalated`   TINYINT       NOT NULL DEFAULT 0,
  `create_time` DATETIME      NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_caller_time` (`caller`, `create_time`),
  INDEX `idx_model_time`  (`model`, `create_time`),
  INDEX `idx_user_time`   (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用审计（append-only）';

-- ============================================================
-- P0：客服对话表
-- 一个用户多个对话，软删除
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_ai_conversation` (
  `id`          BIGINT      NOT NULL,
  `user_id`     BIGINT      NOT NULL,
  `title`       VARCHAR(128) NULL,
  `status`      VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  `create_time` DATETIME    NOT NULL,
  `update_time` DATETIME    NOT NULL,
  `is_deleted`  TINYINT     NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  INDEX `idx_user_time` (`user_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI客服对话';

-- ============================================================
-- P0：客服消息表
-- 一个对话多条消息（user/assistant/system），append-only
-- ============================================================
CREATE TABLE IF NOT EXISTS `t_ai_message` (
  `id`              BIGINT      NOT NULL,
  `conversation_id` BIGINT      NOT NULL,
  `role`            VARCHAR(16) NOT NULL,
  `content`         TEXT        NOT NULL,
  `tokens`          INT         NOT NULL DEFAULT 0,
  `create_time`     DATETIME    NOT NULL,
  PRIMARY KEY (`id`),
  INDEX `idx_conv_time` (`conversation_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI客服消息';

SET FOREIGN_KEY_CHECKS = 1;