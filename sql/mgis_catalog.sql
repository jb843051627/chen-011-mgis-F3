-- ----------------------------------------------------------------------------
-- 年度图幅目录册整批核收（成果管理处规矩落库）
--
-- 口径一句话：一册递进系统逐行给结果——带着自己在原表里的行次、图幅代号、
-- 成果类别、属地名址四样；收下的并进总账，没收下的挂着，行次、代号、缘故
-- 三样齐着写。一处填错只挂那一行，绝不按住整册；同册同代号占两行的，
-- 并成一条进账，幅数只加一次。重递只认最早那一遍；越线、白表整册不收。
-- 挂着行数 + 进账行数 = 表上总行数，翻账两头都翻得动。
-- ----------------------------------------------------------------------------

-- 册头：同一册号只认最早那一遍——后到的顶不掉先收的（唯一键兜底并发）
CREATE TABLE IF NOT EXISTS `t_mgis_catalog_booklet` (
  `id`              BIGINT        NOT NULL COMMENT '主键',
  `booklet_no`      VARCHAR(64)   NOT NULL COMMENT '图幅目录册号',
  `region`          VARCHAR(128)  NOT NULL COMMENT '属地名址(市/州,整本一册一地,以册子封面为准;每行照带)',
  `total_rows`      INT           NOT NULL COMMENT '表上总行数(收到时几行就是几行,含挂着的那几行)',
  `hanging_count`   INT           NOT NULL DEFAULT 0 COMMENT '挂着几行(逐行解析没并进总账的原始行)',
  `accepted_count`  INT           NOT NULL DEFAULT 0 COMMENT '进账几行(好行数,POSTED+MERGED;挂着加入账=总行数)',
  `posted_count`    INT           NOT NULL DEFAULT 0 COMMENT '进账总账条数(同册同代号并一条后的条数)',
  `accepted_qty`    DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '进账总幅数(合并后各条幅数之和,重号只加一次)',
  `first_submit_by` VARCHAR(64)   NOT NULL COMMENT '头一遍递送经办(重递交幂,第二遍谁送都一样)',
  `first_submit_at` DATETIME      NOT NULL COMMENT '头一遍核收时刻(以秒计;两市同日各递一册,分两日办账也一样)',
  `create_by`       VARCHAR(64)   NULL,
  `create_time`     DATETIME      NULL,
  `update_by`       VARCHAR(64)   NULL,
  `update_time`     DATETIME      NULL,
  `remark`          VARCHAR(500)  NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_booklet_no` (`booklet_no`),
  KEY `idx_catalog_booklet_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度图幅目录册核收册头';

-- 册内行：每一原始行都落一条（含挂着的），行次、代号、类别、属地四样带着，
-- 挂着的再带上缘故；只增不改——不设 del_flag，服务层无改删入口
CREATE TABLE IF NOT EXISTS `t_mgis_catalog_line` (
  `id`          BIGINT       NOT NULL COMMENT '主键',
  `booklet_id`  BIGINT       NOT NULL COMMENT '册头主键',
  `row_no`      INT          NOT NULL COMMENT '自己在原表里的行次(从1连排)',
  `item_code`   VARCHAR(64)  NOT NULL COMMENT '图幅代号',
  `prod_type`   VARCHAR(32)  NOT NULL COMMENT '成果类别 TOPO_MAP地形图 IMAGE_MAP影像图 VECTOR_FEATURE矢量要素 PLACE_NAME地名注记',
  `region`      VARCHAR(128) NOT NULL COMMENT '属地名址',
  `qty`         DECIMAL(18,2) NULL COMMENT '本行所报幅数;挂着的存原值(含负数),进账的存所报数',
  `outcome`     VARCHAR(16)  NOT NULL COMMENT '行结局 POSTED已进账 MERGED并入前一条 HUNG挂着',
  `reason`      VARCHAR(500) NULL COMMENT '挂账缘故(挂着的行才写,只写这一遍,重递不补第二句)',
  `ledger_id`   BIGINT       NULL COMMENT '并入的那一条总账主键(已进账/已合并的行指过去)',
  `create_by`   VARCHAR(64)  NULL,
  `create_time` DATETIME     NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_line_row` (`booklet_id`, `row_no`),
  KEY `idx_catalog_line_booklet` (`booklet_id`),
  KEY `idx_catalog_line_code` (`item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度图幅目录册册内行(逐行明细,只增不改)';

-- 进账总账：同一图幅代号一条；同册重号只加一次，跨册各市各报正常累加
CREATE TABLE IF NOT EXISTS `t_mgis_catalog_ledger` (
  `id`             BIGINT        NOT NULL COMMENT '主键',
  `booklet_id`     BIGINT        NOT NULL COMMENT '头一回收进该代号的册头主键',
  `booklet_no`     VARCHAR(64)   NOT NULL COMMENT '头一回收进的册号(快照)',
  `item_code`      VARCHAR(64)   NOT NULL COMMENT '图幅代号',
  `prod_type`      VARCHAR(32)   NOT NULL COMMENT '成果类别',
  `region`         VARCHAR(128)  NOT NULL COMMENT '属地名址',
  `qty`            DECIMAL(18,2) NOT NULL COMMENT '累计幅数(同册重复只加一次)',
  `first_at`       DATETIME      NOT NULL COMMENT '头一回进账时刻(以秒计)',
  `last_booklet_id` BIGINT       NULL COMMENT '最近一次追加的册头主键',
  `last_at`        DATETIME      NULL COMMENT '最近一次追加时刻(以秒计)',
  `create_by`      VARCHAR(64)   NULL,
  `create_time`    DATETIME      NULL,
  `update_by`      VARCHAR(64)   NULL,
  `update_time`    DATETIME      NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_catalog_ledger_code` (`item_code`),
  KEY `idx_catalog_ledger_booklet` (`booklet_id`),
  KEY `idx_catalog_ledger_region` (`region`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='年度图幅目录进账总账(按图幅代号一条)';
