ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER' AFTER password_hash,
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD COLUMN profile_completed BOOLEAN NOT NULL DEFAULT FALSE AFTER status;

UPDATE users
SET role = 'ADMIN'
WHERE id = (SELECT id FROM (SELECT MIN(id) AS id FROM users) first_user);

CREATE TABLE user_tag_preferences (
    user_id BIGINT NOT NULL,
    tag_name VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (user_id, tag_name),
    CONSTRAINT fk_user_tag_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE posts
    ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER ai_generated_at,
    ADD COLUMN review_reason VARCHAR(300) AFTER review_status,
    ADD COLUMN reviewer_id BIGINT AFTER review_reason,
    ADD COLUMN reviewed_at DATETIME AFTER reviewer_id,
    ADD CONSTRAINT fk_posts_reviewer FOREIGN KEY (reviewer_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD INDEX idx_posts_review_created (review_status, created_at);
