use wardrobe;

DROP TABLE IF EXISTS `t_cart`;
CREATE TABLE `t_cart`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `cloth_id` int(0) NULL DEFAULT NULL,
  `cloth_size` varchar(225) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `amount` int(0) NULL DEFAULT NULL,
  `user_id` int(0) NULL DEFAULT NULL,
  `date` varchar(225) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_cart_user` (`user_id`) USING BTREE,
  KEY `idx_cart_user_cloth_size` (`user_id`, `cloth_id`, `cloth_size`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 154 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_cart
-- ----------------------------
INSERT INTO `t_cart` VALUES (1, 1, 'S', 1, 2, '2024-05-22 00:00:00');
INSERT INTO `t_cart` VALUES (2, 2, 'M', 2, 2, '2024-05-22 00:00:00');

-- ----------------------------
-- Table structure for t_clothes
-- ----------------------------
DROP TABLE IF EXISTS `t_clothes`;
CREATE TABLE `t_clothes`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '服装ID',
  `cloth_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '服装名称',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '服装图片',
  `type_id` int(0) NULL DEFAULT NULL COMMENT '服装类别',
  `style` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '服装风格',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '服装价格',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_clothes_type` (`type_id`) USING BTREE,
  KEY `idx_clothes_style` (`style`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 64 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_clothes
-- ----------------------------
INSERT INTO `t_clothes` VALUES (1, '连衣裙', 'lyq.jpg', 1, '时尚', 299.90);
INSERT INTO `t_clothes` VALUES (2, '衬衫', 'cs.png', 1, '休闲', 199.90);
INSERT INTO `t_clothes` VALUES (3, '遮阳帽', 'zym.png', 2, '休闲', 59.90);
INSERT INTO `t_clothes` VALUES (4, '皮鞋', 'px.jpg', 3, '正式', 599.90);
INSERT INTO `t_clothes` VALUES (5, 'T恤', 'tx.png', 1, '休闲', 120.00);
INSERT INTO `t_clothes` VALUES (6, '马甲', 'mj.png', 1, '时尚', 598.00);
INSERT INTO `t_clothes` VALUES (7, '棒球帽', 'bqm.png', 2, '运动', 99.00);
INSERT INTO `t_clothes` VALUES (10, '运动鞋', '20240723140210_ydx.jpg\r\n', 3, '运动', 166.00);

-- ----------------------------
-- Table structure for t_order
-- ----------------------------
DROP TABLE IF EXISTS `t_order`;
CREATE TABLE `t_order`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '订单编号',
  `clothes_details` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '服装详细信息',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '服装详细信息',
  `status` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '订单价格',
  `user_id` int(0) NULL DEFAULT NULL COMMENT '订单状态（未支付：0、未发货：1、已发货：2、已收货：3）',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '订单收货地址',
  `time` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '提交订单的时间',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_order_user` (`user_id`) USING BTREE,
  KEY `idx_order_status` (`status`) USING BTREE,
  KEY `idx_order_user_status` (`user_id`, `status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_order
-- ----------------------------
INSERT INTO `t_order` VALUES (11, '服装编号1，连衣裙S码（299.90）×2', 599.80, '2', 8, '北京市海淀区', '2024-07-22 08:51:41');
INSERT INTO `t_order` VALUES (12, '服装编号4，皮鞋37码（599.90）×4', 2399.60, '0', 8, '北京市海淀区', '2024-07-22 08:51:41');
INSERT INTO `t_order` VALUES (17, '服装编号5，T恤S码（120.00）×1', 120.00, '1', 9, '北京市丰台区', '2024-07-23 14:43:22');

-- ----------------------------
-- Table structure for t_order_outbox
-- ----------------------------
DROP TABLE IF EXISTS `t_order_outbox`;
CREATE TABLE `t_order_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_type` varchar(64) NOT NULL,
  `aggregate_id` bigint NOT NULL,
  `event_type` varchar(64) NOT NULL,
  `payload` varchar(255) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-pending, 1-sent, 2-failed',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_time` datetime NOT NULL,
  `last_error` varchar(512) DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_outbox_status_next_retry` (`status`, `next_retry_time`),
  KEY `idx_order_outbox_aggregate` (`aggregate_type`, `aggregate_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------
-- Table structure for t_size
-- ----------------------------
DROP TABLE IF EXISTS `t_size`;
CREATE TABLE `t_size`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `size_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  `type_id` int(0) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_size_type` (`type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_size
-- ----------------------------
INSERT INTO `t_size` VALUES (1, 'S', 1);
INSERT INTO `t_size` VALUES (2, 'M', 1);
INSERT INTO `t_size` VALUES (3, 'L', 1);
INSERT INTO `t_size` VALUES (4, 'S', 2);
INSERT INTO `t_size` VALUES (5, 'M', 2);
INSERT INTO `t_size` VALUES (6, 'L', 2);
INSERT INTO `t_size` VALUES (7, '36', 3);
INSERT INTO `t_size` VALUES (8, '37', 3);
INSERT INTO `t_size` VALUES (9, '38', 3);
INSERT INTO `t_size` VALUES (10, '39', 3);
INSERT INTO `t_size` VALUES (11, '40', 3);
INSERT INTO `t_size` VALUES (12, '41', 3);
INSERT INTO `t_size` VALUES (13, '42', 3);

-- ----------------------------
-- Table structure for t_type
-- ----------------------------
DROP TABLE IF EXISTS `t_type`;
CREATE TABLE `t_type`  (
  `id` int(0) NOT NULL AUTO_INCREMENT,
  `type_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_type
-- ----------------------------
INSERT INTO `t_type` VALUES (1, '衣服');
INSERT INTO `t_type` VALUES (2, '帽子');
INSERT INTO `t_type` VALUES (3, '鞋');

-- ----------------------------
-- Table structure for t_user
-- ----------------------------
DROP TABLE IF EXISTS `t_user`;
CREATE TABLE `t_user`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `user_name` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户姓名',
  `password` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户密码',
  `phone` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户电话',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '用户地址',
  `role` int(0) NULL DEFAULT NULL COMMENT '用户角色（普通用户2、管理员1）',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_user_user_name` (`user_name`) USING BTREE,
  UNIQUE KEY `uk_user_phone` (`phone`) USING BTREE,
  KEY `idx_user_role` (`role`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_user
-- ----------------------------
INSERT INTO `t_user` VALUES (1, 'admin', 'admin', '13111111111', '山西省大同市云冈区', 1);
INSERT INTO `t_user` VALUES (8, 'zhangsan', '123123', '13122222222', '北京市昌平区', 2);
INSERT INTO `t_user` VALUES (9, 'lisi', '111111', '13133333333', '北京市丰台区', 2);

SET FOREIGN_KEY_CHECKS = 1;
-- ----------------------------
-- Additional records of t_clothes from clothing_table.xlsx
-- ----------------------------
INSERT INTO `t_clothes` VALUES (11, '时尚连衣裙 runway dress', 'cloth_001.png', 1, '时尚', 682.66);
INSERT INTO `t_clothes` VALUES (12, '高级定制晚礼服 evening gown', 'cloth_002.png', 1, '时尚', 314.01);
INSERT INTO `t_clothes` VALUES (13, 'T恤牛仔裤 casual jeans tshirt', 'cloth_003.png', 1, '休闲', 203.26);
INSERT INTO `t_clothes` VALUES (14, '休闲连帽卫衣 hoodie casual', 'cloth_004.png', 1, '休闲', 189.27);
INSERT INTO `t_clothes` VALUES (15, '牛仔夹克外套 denim jacket', 'cloth_005.png', 1, '休闲', 327.85);
INSERT INTO `t_clothes` VALUES (16, '休闲连衣裙 casual dress', 'cloth_006.png', 1, '休闲', 311.71);
INSERT INTO `t_clothes` VALUES (17, '西装套装 business suit', 'cloth_007.png', 1, '正式', 1491.18);
INSERT INTO `t_clothes` VALUES (18, '男士礼服 tuxedo formal', 'cloth_008.png', 1, '正式', 685.94);
INSERT INTO `t_clothes` VALUES (19, '衬衫领带 dress shirt tie', 'cloth_009.png', 1, '正式', 1020.92);
INSERT INTO `t_clothes` VALUES (20, '女士正装衬衫 blouse formal', 'cloth_010.png', 1, '正式', 628.8);
INSERT INTO `t_clothes` VALUES (21, '运动服套装 sportswear', 'cloth_011.png', 1, '运动', 264.59);
INSERT INTO `t_clothes` VALUES (22, '健身运动服 gym outfit', 'cloth_012.png', 1, '运动', 350.61);
INSERT INTO `t_clothes` VALUES (23, '运动球衣 sports jersey', 'cloth_013.png', 1, '运动', 206.96);
INSERT INTO `t_clothes` VALUES (24, '瑜伽服 yoga outfit', 'cloth_014.png', 1, '运动', 258.65);
INSERT INTO `t_clothes` VALUES (25, '贝雷帽 beret hat', 'cloth_015.png', 2, '时尚', 225.48);
INSERT INTO `t_clothes` VALUES (26, '时尚礼帽 fedora hat', 'cloth_016.png', 2, '时尚', 203.44);
INSERT INTO `t_clothes` VALUES (27, '宽檐帽 wide brim hat', 'cloth_017.png', 2, '时尚', 135.29);
INSERT INTO `t_clothes` VALUES (28, '渔夫帽 bucket hat', 'cloth_018.png', 2, '时尚', 212.75);
INSERT INTO `t_clothes` VALUES (29, '时尚头饰帽 fascinator hat', 'cloth_019.png', 2, '时尚', 258.98);
INSERT INTO `t_clothes` VALUES (30, '棒球帽 baseball cap', 'cloth_020.png', 2, '休闲', 49.71);
INSERT INTO `t_clothes` VALUES (31, '毛线帽 beanie hat', 'cloth_021.png', 2, '休闲', 137.64);
INSERT INTO `t_clothes` VALUES (32, '草帽 straw hat summer', 'cloth_022.png', 2, '休闲', 125.8);
INSERT INTO `t_clothes` VALUES (33, '平顶帽 flat cap hat', 'cloth_023.png', 2, '休闲', 86.43);
INSERT INTO `t_clothes` VALUES (34, '卡车帽 trucker cap', 'cloth_024.png', 2, '休闲', 66.1);
INSERT INTO `t_clothes` VALUES (35, '高顶礼帽 top hat', 'cloth_025.png', 2, '正式', 387.45);
INSERT INTO `t_clothes` VALUES (36, '圆顶礼帽 bowler hat', 'cloth_026.png', 2, '正式', 219.88);
INSERT INTO `t_clothes` VALUES (37, '军帽 military cap hat', 'cloth_027.png', 2, '正式', 154.04);
INSERT INTO `t_clothes` VALUES (38, '商务礼帽 homburg hat', 'cloth_028.png', 2, '正式', 155.11);
INSERT INTO `t_clothes` VALUES (39, '优雅礼帽 cocktail hat', 'cloth_029.png', 2, '正式', 357.82);
INSERT INTO `t_clothes` VALUES (40, '运动帽 athletic cap', 'cloth_030.png', 2, '运动', 139.56);
INSERT INTO `t_clothes` VALUES (41, '骑行头盔 cycling helmet', 'cloth_031.png', 2, '运动', 170.07);
INSERT INTO `t_clothes` VALUES (42, '球队棒球帽 sports cap team', 'cloth_032.png', 2, '运动', 158.46);
INSERT INTO `t_clothes` VALUES (43, '泳帽 swim cap', 'cloth_033.png', 2, '运动', 129.43);
INSERT INTO `t_clothes` VALUES (44, '高跟鞋 stiletto high heel', 'cloth_034.png', 3, '时尚', 982.87);
INSERT INTO `t_clothes` VALUES (45, '时尚运动鞋 designer sneakers', 'cloth_035.png', 3, '时尚', 626.12);
INSERT INTO `t_clothes` VALUES (46, '时尚短靴 ankle boots', 'cloth_036.png', 3, '时尚', 730.22);
INSERT INTO `t_clothes` VALUES (47, '厚底鞋 platform shoes', 'cloth_037.png', 3, '时尚', 896.64);
INSERT INTO `t_clothes` VALUES (48, '乐福鞋 loafers shoes', 'cloth_038.png', 3, '时尚', 770.11);
INSERT INTO `t_clothes` VALUES (49, '白色运动鞋 white sneakers', 'cloth_039.png', 3, '休闲', 457.51);
INSERT INTO `t_clothes` VALUES (50, '人字拖 flip flops sandals', 'cloth_040.png', 3, '休闲', 372.21);
INSERT INTO `t_clothes` VALUES (51, '帆布鞋 canvas shoes', 'cloth_041.png', 3, '休闲', 410.37);
INSERT INTO `t_clothes` VALUES (52, '麻底鞋 espadrilles shoes', 'cloth_042.png', 3, '休闲', 212.75);
INSERT INTO `t_clothes` VALUES (53, '平底鞋 ballet flats', 'cloth_043.png', 3, '休闲', 267.37);
INSERT INTO `t_clothes` VALUES (54, '牛津皮鞋 oxford shoes', 'cloth_044.png', 3, '正式', 801.57);
INSERT INTO `t_clothes` VALUES (55, '商务皮鞋 leather loafers', 'cloth_045.png', 3, '正式', 654.85);
INSERT INTO `t_clothes` VALUES (56, '高跟单鞋 pumps heels office', 'cloth_046.png', 3, '正式', 761.95);
INSERT INTO `t_clothes` VALUES (57, '布洛克皮鞋 brogues shoes', 'cloth_047.png', 3, '正式', 669.7);
INSERT INTO `t_clothes` VALUES (58, '漆皮皮鞋 patent leather shoes', 'cloth_048.png', 3, '正式', 793.58);
INSERT INTO `t_clothes` VALUES (59, '篮球鞋 basketball shoes', 'cloth_049.png', 3, '运动', 616.84);
INSERT INTO `t_clothes` VALUES (60, '跑步运动鞋 running shoes', 'cloth_050.png', 3, '运动', 481.42);
INSERT INTO `t_clothes` VALUES (61, '足球鞋 soccer cleats', 'cloth_051.png', 3, '运动', 484.09);
INSERT INTO `t_clothes` VALUES (62, '登山鞋 hiking boots', 'cloth_052.png', 3, '运动', 403.75);
INSERT INTO `t_clothes` VALUES (63, '综训运动鞋 cross training shoes', 'cloth_053.png', 3, '运动', 432.49);

-- ----------------------------
-- Business fields of t_clothes
-- ----------------------------
ALTER TABLE `t_clothes` ADD COLUMN `brand_id` int(0) NULL DEFAULT NULL COMMENT '品牌ID' AFTER `type_id`;
ALTER TABLE `t_clothes` ADD COLUMN `description` varchar(1000) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '商品描述' AFTER `price`;
ALTER TABLE `t_clothes` ADD COLUMN `stock` int(0) NOT NULL DEFAULT 100 COMMENT '库存' AFTER `description`;
ALTER TABLE `t_clothes` ADD COLUMN `sales` int(0) NOT NULL DEFAULT 0 COMMENT '销量' AFTER `stock`;
ALTER TABLE `t_clothes` ADD KEY `idx_clothes_brand` (`brand_id`) USING BTREE;
ALTER TABLE `t_clothes` ADD KEY `idx_clothes_brand_type` (`brand_id`, `type_id`) USING BTREE;
ALTER TABLE `t_clothes` ADD FULLTEXT KEY `ft_clothes_search` (`cloth_name`, `style`, `description`);

UPDATE `t_clothes`
SET `brand_id` = CASE
    WHEN `style` = '运动' THEN 1
    WHEN `style` = '正式' THEN 3
    ELSE 2
  END,
  `description` = CONCAT(COALESCE(`cloth_name`, ''), '，', COALESCE(`style`, ''), '风格商品，适合日常穿搭与场景搭配。'),
  `stock` = CASE
    WHEN `type_id` = 1 THEN 120
    WHEN `type_id` = 2 THEN 80
    WHEN `type_id` = 3 THEN 60
    ELSE 100
  END,
  `sales` = MOD(`id` * 7, 120);

-- ----------------------------
-- Table structure for t_brand
-- ----------------------------
DROP TABLE IF EXISTS `t_brand`;
CREATE TABLE `t_brand`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '品牌ID',
  `brand_name` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '品牌名称',
  `brand_logo` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '品牌Logo',
  `description` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '品牌描述',
  `create_time` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_brand
-- ----------------------------
INSERT INTO `t_brand` VALUES (1, 'Nike', 'nike_logo.png', '全球知名运动品牌', '2024-01-01 00:00:00');
INSERT INTO `t_brand` VALUES (2, 'ZARA', 'zara_logo.png', '西班牙快时尚品牌', '2024-01-02 00:00:00');
INSERT INTO `t_brand` VALUES (3, '优衣库', 'uniqlo_logo.png', '日本休闲服装品牌', '2024-01-03 00:00:00');

-- ----------------------------
-- Table structure for t_review
-- ----------------------------
DROP TABLE IF EXISTS `t_review`;
CREATE TABLE `t_review`  (
  `id` int(0) NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `user_id` int(0) NOT NULL COMMENT '用户ID',
  `cloth_id` int(0) NOT NULL COMMENT '服装ID',
  `content` text CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL COMMENT '评论内容',
  `rating` int(0) NULL DEFAULT 5 COMMENT '评分1-5',
  `create_time` varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '评论时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `fk_review_user`(`user_id`) USING BTREE,
  INDEX `fk_review_cloth`(`cloth_id`) USING BTREE,
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `t_user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_review_cloth` FOREIGN KEY (`cloth_id`) REFERENCES `t_clothes` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of t_review
-- ----------------------------
INSERT INTO `t_review` VALUES (1, 8, 1, '连衣裙质量很好，穿着很舒服！', 5, '2024-07-20 10:00:00');
INSERT INTO `t_review` VALUES (2, 9, 1, '颜色和图片一致，非常满意', 4, '2024-07-21 14:30:00');
INSERT INTO `t_review` VALUES (3, 8, 5, 'T恤面料不错，性价比高', 4, '2024-07-22 09:15:00');
