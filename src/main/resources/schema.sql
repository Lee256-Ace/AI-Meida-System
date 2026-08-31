-- 执行以下 SQL 创建数据库及各表（IF NOT EXISTS 保证幂等，可重复执行）
-- CREATE DATABASE IF NOT EXISTS ai_writing DEFAULT CHARSET utf8mb4;
CREATE DATABASE IF NOT EXISTS `basesys` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE basesys;

-- 文案生成表（content 为汇总展示；各平台单独存储）
CREATE TABLE IF NOT EXISTS ai_generation (
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
CREATE TABLE IF NOT EXISTS ai_polish (
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
CREATE TABLE IF NOT EXISTS ai_continuation (
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
);

-- Markdown 编辑器保存的文案（多标签各自可对应一条记录，首次保存插入，再次保存更新）
CREATE TABLE IF NOT EXISTS ai_markdown_copy (
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

-- ------------------------------------------------------------
-- 管理员表（后台管理登录）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admins (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    real_name VARCHAR(50) NULL DEFAULT NULL COMMENT '真实姓名',
    phone VARCHAR(20) NULL DEFAULT NULL COMMENT '手机号',
    email VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(200) NULL DEFAULT NULL COMMENT '头像',
    permissions TEXT NULL COMMENT '权限',
    status TINYINT(4) NULL DEFAULT 1 COMMENT '状态(0:禁用 1:正常)',
    created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE INDEX uk_username (username)
) COMMENT '管理员表';

-- 默认管理员账号（幂等：username 唯一，重复执行自动跳过）
INSERT IGNORE INTO admins (username, password, real_name, status)
VALUES ('admin', 'admin123', '系统管理员', 1);

-- ------------------------------------------------------------
-- AI 任务表（记录每次 AI 调用的输入/输出/状态）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prompt TEXT NOT NULL COMMENT '用户输入',
    reasoning TEXT NULL COMMENT 'AI思考过程',
    content TEXT NULL COMMENT 'AI最终回复',
    model VARCHAR(50) NULL DEFAULT 'deepseek-v3.2',
    status TINYINT(4) NULL DEFAULT 0 COMMENT '0-处理中 1-成功 2-失败',
    error_msg VARCHAR(500) NULL DEFAULT NULL,
    created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL DEFAULT NULL,
    INDEX idx_status (status)
) COMMENT 'AI任务表';

-- ------------------------------------------------------------
-- 新闻热点表（含 summary 摘要缓存列）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS article_news (
    id VARCHAR(36) NOT NULL COMMENT '主键',
    create_by VARCHAR(50) NULL DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NULL DEFAULT NULL COMMENT '创建日期',
    update_by VARCHAR(50) NULL DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NULL DEFAULT NULL COMMENT '更新日期',
    sys_org_code VARCHAR(64) NULL DEFAULT NULL COMMENT '所属部门',
    remark VARCHAR(32) NULL DEFAULT NULL COMMENT '备注',
    del_flag INT(11) NULL DEFAULT 0 COMMENT '逻辑删除，0正常1已删除',
    source VARCHAR(32) NOT NULL COMMENT '新闻来源',
    title VARCHAR(255) NOT NULL COMMENT '新闻标题',
    content LONGTEXT NOT NULL COMMENT '新闻内容',
    url LONGTEXT NOT NULL COMMENT '新闻链接',
    pic LONGTEXT NULL COMMENT '新闻图片',
    hot INT(11) NULL DEFAULT NULL COMMENT '热度指数',
    type VARCHAR(32) NULL DEFAULT NULL COMMENT '新闻类型',
    news_date DATETIME NULL DEFAULT NULL COMMENT '新闻日期',
    media VARCHAR(255) NULL DEFAULT NULL COMMENT '媒体',
    pic_count INT(11) NOT NULL DEFAULT 0 COMMENT '图片数量',
    pic_list TEXT NULL COMMENT '图片列表',
    word_num INT(11) NULL DEFAULT NULL COMMENT '选择多少字数以内的文章发布',
    summary TEXT NULL COMMENT '新闻摘要（缓存，避免重复调用大模型）',
    PRIMARY KEY (id)
) COMMENT '新闻热点表';

-- ------------------------------------------------------------
-- 发布平台表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS article_platform (
    id VARCHAR(36) NOT NULL COMMENT '主键',
    create_by VARCHAR(50) NULL DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NULL DEFAULT NULL COMMENT '创建日期',
    update_by VARCHAR(50) NULL DEFAULT NULL COMMENT '更新人',
    update_time DATETIME NULL DEFAULT NULL COMMENT '更新日期',
    del_flag INT(11) NULL DEFAULT 0 COMMENT '逻辑删除，0正常1已删除',
    name VARCHAR(32) NULL DEFAULT NULL COMMENT '平台名称',
    remark VARCHAR(32) NULL DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id)
) COMMENT '发布平台表';