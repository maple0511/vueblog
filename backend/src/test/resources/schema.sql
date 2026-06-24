CREATE TABLE users (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(30) UNIQUE, email VARCHAR(120) UNIQUE,
 password_hash VARCHAR(100), role VARCHAR(20), status VARCHAR(20), profile_completed BOOLEAN, created_at TIMESTAMP
);
CREATE TABLE posts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, author_id BIGINT, title VARCHAR(100), summary VARCHAR(500),
 content CLOB, ai_summary VARCHAR(180), ai_metadata_status VARCHAR(20), ai_summary_edited BOOLEAN,
 ai_generated_at TIMESTAMP, review_status VARCHAR(20), review_reason VARCHAR(300), reviewer_id BIGINT,
 reviewed_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP
);
CREATE TABLE tags (id BIGINT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(20) UNIQUE);
CREATE TABLE post_tags (post_id BIGINT, tag_id BIGINT, source VARCHAR(10), PRIMARY KEY(post_id,tag_id,source));
CREATE TABLE user_tag_preferences (user_id BIGINT, tag_name VARCHAR(20), created_at TIMESTAMP, PRIMARY KEY(user_id, tag_name));
CREATE TABLE comments (id BIGINT AUTO_INCREMENT PRIMARY KEY, post_id BIGINT, author_id BIGINT, content VARCHAR(1000), created_at TIMESTAMP);
CREATE TABLE ai_request_log (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, post_id BIGINT, feature VARCHAR(30),
 provider VARCHAR(40), model VARCHAR(80), status VARCHAR(20), latency_ms BIGINT,
 prompt_tokens INT, completion_tokens INT, error_code VARCHAR(80), created_at TIMESTAMP
);
