-- 修复：article_news 缺少 summary 列（Unknown column 'summary'）
-- MySQL 5.7+ 兼容写法
USE basesys;

DELIMITER //
DROP PROCEDURE IF EXISTS add_article_news_summary_if_missing//
CREATE PROCEDURE add_article_news_summary_if_missing()
BEGIN
  IF (SELECT COUNT(*) FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'article_news' AND COLUMN_NAME = 'summary') = 0 THEN
    ALTER TABLE article_news
        ADD COLUMN summary LONGTEXT NULL COMMENT 'LLM热点摘要' AFTER word_num;
  END IF;
END//
DELIMITER ;
CALL add_article_news_summary_if_missing();
DROP PROCEDURE IF EXISTS add_article_news_summary_if_missing;
