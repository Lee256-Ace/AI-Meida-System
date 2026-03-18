/*
 Navicat Premium Data Transfer

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 50729 (5.7.29)
 Source Host           : localhost:3306
 Source Schema         : ai_service

 Target Server Type    : MySQL
 Target Server Version : 50729 (5.7.29)
 File Encoding         : 65001

 Date: 26/02/2026 11:47:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_continuation
-- ----------------------------
DROP TABLE IF EXISTS `ai_continuation`;
CREATE TABLE `ai_continuation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `original_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原文内容',
  `context_hint` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '续写提示/方向',
  `platform` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属平台: douyin/xiaohongshu/video_account/toutiao',
  `continuation` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '续写内容',
  `total_length` int(11) NULL DEFAULT NULL COMMENT '续写后总字数',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'deepseek-v3',
  `status` tinyint(4) NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_generation
-- ----------------------------
DROP TABLE IF EXISTS `ai_generation`;
CREATE TABLE `ai_generation`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户输入提示',
  `reasoning` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT 'AI思考过程',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '生成内容汇总',
  `douyin_copy` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '抖音文案',
  `xiaohongshu_copy` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '小红书文案',
  `video_account_copy` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '视频号文案',
  `toutiao_copy` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '今日头条文案',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'deepseek-v3',
  `status` tinyint(4) NULL DEFAULT 0 COMMENT '0-处理中 1-成功 2-失败',
  `word_count` int(11) NULL DEFAULT NULL COMMENT '字数统计',
  `style` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '排版风格',
  `theme` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主题',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for ai_polish
-- ----------------------------
DROP TABLE IF EXISTS `ai_polish`;
CREATE TABLE `ai_polish`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `original_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '原始内容',
  `polish_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '美化类型: simplify/enhance/grammar/rewrite',
  `target_style` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '目标风格',
  `platform` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '所属平台: douyin/xiaohongshu/video_account/toutiao',
  `result_content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '美化结果',
  `changes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '修改说明',
  `model` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'deepseek-v3',
  `status` tinyint(4) NULL DEFAULT 0,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `completed_at` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type`(`polish_type`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
