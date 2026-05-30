ALTER TABLE tb_shop_comments ADD COLUMN status INT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-正常 2-标记 3-拒绝' AFTER score;
UPDATE tb_shop_comments SET status = 1;
