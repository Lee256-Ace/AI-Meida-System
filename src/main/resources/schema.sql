-- 执行以下 SQL 创建三个表
# CREATE DATABASE IF NOT EXISTS ai_writing DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `basesys` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE basesys;

-- 文案生成表（content 为汇总展示；各平台单独存储）
CREATE TABLE ai_generation (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               prompt TEXT NOT NULL,
                               reasoning TEXT,
                               content LONGTEXT,
                               douyin_copy LONGTEXT COMMENT '抖音文案',
                               xiaohongshu_copy LONGTEXT COMMENT '小红书文案',
                               video_account_copy LONGTEXT COMMENT '视频号文案',
                               toutiao_copy LONGTEXT COMMENT '今日头条文案',
                               model VARCHAR(50) DEFAULT 'deepseek-v3',
                               status TINYINT DEFAULT 0,
                               word_count INT,
                               style VARCHAR(20),
                               theme VARCHAR(20),
                               created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                               completed_at DATETIME
);

-- 文章美化表（platform 标识所属平台，与文案生成一致）
CREATE TABLE ai_polish (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           original_content LONGTEXT NOT NULL,
                           polish_type VARCHAR(50) NOT NULL,
                           target_style VARCHAR(50),
                           platform VARCHAR(30) NULL COMMENT '所属平台: douyin/xiaohongshu/video_account/toutiao',
                           result_content LONGTEXT,
                           changes TEXT,
                           model VARCHAR(50) DEFAULT 'deepseek-v3',
                           status TINYINT DEFAULT 0,
                           created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                           completed_at DATETIME
);

-- 文章续写表（platform 标识所属平台）
CREATE TABLE ai_continuation (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 original_content LONGTEXT NOT NULL,
                                 context_hint TEXT,
                                 platform VARCHAR(30) NULL COMMENT '所属平台: douyin/xiaohongshu/video_account/toutiao',
                                 continuation LONGTEXT,
                                 total_length INT,
                                 model VARCHAR(50) DEFAULT 'deepseek-v3',
                                 status TINYINT DEFAULT 0,
                                 created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                                 completed_at DATETIME
);

-- 错误日志表（用于排查各模块异常）
CREATE TABLE ai_error_log (
                              id BIGINT AUTO_INCREMENT PRIMARY KEY,
                              module VARCHAR(50) NOT NULL COMMENT '模块: generate/polish/continue/coze/auth等',
                              phase VARCHAR(100) NULL COMMENT '阶段/步骤，如: coze_request, parse_result, save_db',
                              error_message TEXT NOT NULL COMMENT '错误摘要',
                              error_detail LONGTEXT NULL COMMENT '堆栈或详细内容',
                              request_info VARCHAR(500) NULL COMMENT '请求上下文，如 taskId、简要参数',
                              created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                              INDEX idx_module (module),
                              INDEX idx_created_at (created_at)
);

-- Markdown 编辑器保存的文案（多标签各自可对应一条记录，首次保存插入，再次保存更新）
CREATE TABLE ai_markdown_copy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL DEFAULT '未命名' COMMENT '文档标题',
    content LONGTEXT NOT NULL COMMENT 'Markdown 正文',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_updated_at (updated_at)
) COMMENT 'Markdown 文案草稿/已保存副本';

-- ------------------------------------------------------------
-- 普通用户表（B：普通用户自动识别）
-- 注意：旧数据从 admins 迁移到 users 的逻辑在 db/init.sql 里完成
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NULL DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(200) NULL DEFAULT NULL COMMENT '头像',
    status TINYINT(4) NULL DEFAULT 1 COMMENT '状态(0:禁用 1:正常)',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_username (username)
) COMMENT '普通用户表';

CREATE TABLE IF NOT EXISTS publish_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform_name VARCHAR(100) NULL DEFAULT NULL COMMENT '发布平台名称',
    title VARCHAR(255) NOT NULL DEFAULT '' COMMENT '标题或主题',
    content LONGTEXT NULL COMMENT '发布文案正文',
    publish_url VARCHAR(500) NULL DEFAULT NULL COMMENT '发布后的链接',
    status VARCHAR(32) NULL DEFAULT 'recorded' COMMENT '状态',
    remark VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_created_at (created_at),
    INDEX idx_platform_name (platform_name)
) COMMENT '历史发布记录';