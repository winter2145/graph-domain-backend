-- 创建库
create database if not exists yu_picture;

-- 切换表
use yu_picture;

-- 用户表
CREATE TABLE user
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'id',
    userAccount  VARCHAR(256)  NOT NULL UNIQUE COMMENT '账号',
    email        VARCHAR(256)  NULL UNIQUE COMMENT '用户邮箱',
    userPassword VARCHAR(512)  NOT NULL COMMENT '密码',
    userName     VARCHAR(256)  NULL COMMENT '用户昵称',
    userAvatar   VARCHAR(1024) NULL COMMENT '用户头像',
    userProfile  VARCHAR(512)  NULL COMMENT '用户简介',
    userRole     VARCHAR(256)  NOT NULL DEFAULT 'user' COMMENT '用户角色：user/admin/ban',
    createTime   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updateTime   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete     TINYINT(1)    NOT NULL DEFAULT 0 COMMENT '是否删除'
) COMMENT = '用户' COLLATE = utf8mb4_unicode_ci;

-- 为user添加联合索引
CREATE INDEX idx_isDelete_userRole ON user (isDelete, userRole);

-- 图片表
CREATE TABLE picture
(
    id            bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    url           varchar(512)                       NOT NULL COMMENT '图片 url',
    name          varchar(128)                       NOT NULL COMMENT '图片名称',
    introduction  varchar(512)                       NULL COMMENT '简介',
    category      varchar(64)                        NULL COMMENT '分类',
    tags          varchar(512)                       NULL COMMENT '标签（JSON 数组）',
    picSize       bigint                             NULL COMMENT '图片体积',
    picWidth      int                                NULL COMMENT '图片宽度',
    picHeight     int                                NULL COMMENT '图片高度',
    picScale      double                             NULL COMMENT '图片宽高比例',
    picFormat     varchar(32)                        NULL COMMENT '图片格式',
    userId        bigint                             NOT NULL COMMENT '创建用户 id',
    createTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime      datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime    datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete      tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    reviewStatus  int      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    reviewMessage varchar(512)                       NULL COMMENT '审核信息',
    reviewerId    bigint                             NULL COMMENT '审核人 ID',
    reviewTime    datetime                           NULL COMMENT '审核时间',
    thumbnailUrl  varchar(512)                       NULL COMMENT '缩略图 url',
    spaceId       bigint                             NULL COMMENT '空间 id（为空表示公共空间）',
    picColor      varchar(16)                        NULL COMMENT '图片主色调',
    commentCount  bigint   DEFAULT 0                 NOT NULL COMMENT '评论数',
    likeCount     bigint   DEFAULT 0                 NOT NULL COMMENT '点赞数',
    shareCount    bigint   DEFAULT 0                 NOT NULL COMMENT '分享数',
    viewCount     bigint   DEFAULT 0                 NOT NULL COMMENT '浏览量'
)
    COMMENT '图片' COLLATE = utf8mb4_unicode_ci;

ALTER TABLE picture ADD webpUrl VARCHAR(512) NULL COMMENT 'webp url' AFTER reviewTime;

CREATE INDEX idx_category
    ON picture (category);

CREATE INDEX idx_introduction
    ON picture (introduction);

CREATE INDEX idx_name
    ON picture (name);

CREATE INDEX idx_reviewStatus
    ON picture (reviewStatus);

CREATE INDEX idx_spaceId
    ON picture (spaceId);

CREATE INDEX idx_tags
    ON picture (tags);

CREATE INDEX idx_userId
    ON picture (userId);

CREATE INDEX idx_viewCount
    ON picture (viewCount);

-- 空间表
CREATE  TABLE space
(
    id         bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    spaceName  varchar(128)                       NULL COMMENT '空间名称',
    spaceLevel int      DEFAULT 0                 NULL COMMENT '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大总大小',
    maxCount   bigint   DEFAULT 0                 NULL COMMENT '空间图片的最大数量',
    totalSize  bigint   DEFAULT 0                 NULL COMMENT '当前空间下图片的总大小',
    totalCount bigint   DEFAULT 0                 NULL COMMENT '当前空间下的图片数量',
    userId     bigint                             NOT NULL COMMENT '创建用户 id',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    spaceType  int      DEFAULT 0                 NOT NULL COMMENT '空间类型：0-私有 1-团队'
)
    COMMENT '空间' COLLATE  = utf8mb3_unicode_ci;

CREATE INDEX idx_spaceLevel
    ON space (spaceLevel);

CREATE INDEX idx_spaceName
    ON space (spaceName);

CREATE INDEX idx_spaceType
    ON space (spaceType);

CREATE INDEX idx_userId
    ON space (userId);