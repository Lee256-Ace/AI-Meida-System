-- ============================================================
-- 数据库初始化与迁移（整合所有 db 下 SQL，可重复执行，已存在则跳过）
-- 使用库：basesys。执行前请确认连接目标库，或取消下一行注释并修改库名。
-- ============================================================

USE basesys;

-- ------------------------------------------------------------
-- 1. 错误日志表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_error_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    module VARCHAR(50) NOT NULL COMMENT '模块: generate/polish/continue/coze/auth等',
    phase VARCHAR(100) NULL COMMENT '阶段/步骤，如: coze_request, parse_result, save_db',
    error_message TEXT NOT NULL COMMENT '错误摘要',
    error_detail LONGTEXT NULL COMMENT '堆栈或详细内容',
    request_info VARCHAR(500) NULL COMMENT '请求上下文，如 taskId、简要参数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module (module),
    INDEX idx_created_at (created_at)
) COMMENT '系统错误日志，便于排查';

-- ------------------------------------------------------------
-- 2. ai_generation 表：补充各平台文案列（douyin_copy / xiaohongshu_copy / video_account_copy / toutiao_copy）
-- ------------------------------------------------------------
DELIMITER //
DROP PROCEDURE IF EXISTS add_generation_platform_columns_if_missing//
CREATE PROCEDURE add_generation_platform_columns_if_missing()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_generation' AND COLUMN_NAME = 'douyin_copy') = 0 THEN
    ALTER TABLE ai_generation ADD COLUMN douyin_copy LONGTEXT NULL COMMENT '抖音文案' AFTER content;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_generation' AND COLUMN_NAME = 'xiaohongshu_copy') = 0 THEN
    ALTER TABLE ai_generation ADD COLUMN xiaohongshu_copy LONGTEXT NULL COMMENT '小红书文案' AFTER douyin_copy;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_generation' AND COLUMN_NAME = 'video_account_copy') = 0 THEN
    ALTER TABLE ai_generation ADD COLUMN video_account_copy LONGTEXT NULL COMMENT '视频号文案' AFTER xiaohongshu_copy;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_generation' AND COLUMN_NAME = 'toutiao_copy') = 0 THEN
    ALTER TABLE ai_generation ADD COLUMN toutiao_copy LONGTEXT NULL COMMENT '今日头条文案' AFTER video_account_copy;
  END IF;
END//
DELIMITER ;
CALL add_generation_platform_columns_if_missing();
DROP PROCEDURE IF EXISTS add_generation_platform_columns_if_missing;

-- ------------------------------------------------------------
-- 3. ai_polish / ai_continuation 表：补充 platform 列
-- ------------------------------------------------------------
DELIMITER //
DROP PROCEDURE IF EXISTS add_platform_columns_if_missing//
CREATE PROCEDURE add_platform_columns_if_missing()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_polish' AND COLUMN_NAME = 'platform') = 0 THEN
    ALTER TABLE ai_polish
        ADD COLUMN platform VARCHAR(30) NULL DEFAULT NULL COMMENT '所属平台: douyin/xiaohongshu/video_account/toutiao' AFTER target_style;
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ai_continuation' AND COLUMN_NAME = 'platform') = 0 THEN
    ALTER TABLE ai_continuation
        ADD COLUMN platform VARCHAR(30) NULL DEFAULT NULL COMMENT '所属平台: douyin/xiaohongshu/video_account/toutiao' AFTER context_hint;
  END IF;
END//
DELIMITER ;
CALL add_platform_columns_if_missing();
DROP PROCEDURE IF EXISTS add_platform_columns_if_missing;

-- ------------------------------------------------------------
-- 4. Markdown 编辑器保存表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_markdown_copy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT '未命名' COMMENT '文档标题',
    content LONGTEXT NOT NULL COMMENT 'Markdown 正文',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated_at (updated_at)
) COMMENT 'Markdown 文案草稿/已保存副本';
