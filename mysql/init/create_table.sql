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

-- 聊天信息表
CREATE TABLE `chat_message`
(
    `id`                bigint          NOT NULL    AUTO_INCREMENT          COMMENT '聊天Id',
    `senderId`          bigint          NOT NULL                            COMMENT '发送者Id',
    `receiverId`        bigint          DEFAULT NULL                        COMMENT '接收者Id（在图片聊天室内可以不指定）',
    `pictureId`         bigint          DEFAULT NULL                        COMMENT '图片Id，对应图片聊天室',
    `content`           text                                                COMMENT '消息内容',
    `type`              tinyint(1)      NOT NULL DEFAULT '1'                COMMENT '消息类型 1 - 私聊  2 - 图片聊天室',
    `status`            tinyint(1)      NOT NULL DEFAULT '0'                COMMENT '状态 0 - 未读 1 - 已读',
    `replyId`           varchar(255)    DEFAULT NULL                        COMMENT '回复消息Id',
    `rootId`            varchar(255)    DEFAULT NULL                        COMMENT '会话跟消息Id',
    `createTime`        datetime        NOT NULL ON UPDATE CURRENT_TIMESTAMP   COMMENT '创建时间',
    `updateTime`        datetime        NOT NULL ON UPDATE CURRENT_TIMESTAMP   COMMENT '更新时间',
    `privateChatId`     bigint          DEFAULT NULL                         COMMENT '私聊Id',
    `isDelete`          tinyint(1)      NOT NULL DEFAULT '0'                 COMMENT '是否删除',
    `spaceId`           bigint          DEFAULT NULL                         COMMENT '空间Id',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci  COMMENT='聊天信息表';

CREATE INDEX idx_picture
    ON chat_message (pictureId);

CREATE INDEX idx_private_chat
    ON chat_message (privateChatId);

CREATE INDEX idx_reply
    ON chat_message (replyId);

CREATE INDEX idx_root
    ON chat_message (rootId);

CREATE INDEX idx_sender_receiver
    ON chat_message (senderId, receiverId);

CREATE INDEX idx_space
    ON chat_message (spaceId);

-- 私聊表
CREATE TABLE private_chat
(
    id                    bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    userId                bigint                             NOT NULL COMMENT '用户id',
    targetUserId          bigint                             NOT NULL COMMENT '目标用户id',
    lastMessage           text                               NULL COMMENT '最后一条消息内容',
    lastMessageTime       datetime                           NULL COMMENT '最后一条消息时间',
    userUnreadCount       int      DEFAULT 0                 NULL COMMENT '用户未读消息数',
    targetUserUnreadCount int      DEFAULT 0                 NULL COMMENT '目标用户未读消息数',
    userChatName          varchar(50)                        NULL COMMENT '用户自定义的私聊名称',
    targetUserChatName    varchar(50)                        NULL COMMENT '目标用户自定义的私聊名称',
    chatType              tinyint  DEFAULT 0                 NOT NULL COMMENT '聊天类型：0-私信 1-好友(双向关注)',
    createTime            datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime            datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete              tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '私聊表' COLLATE = utf8mb4_unicode_ci;

ALTER TABLE private_chat
    ADD COLUMN userDeleted TINYINT(1) DEFAULT 0 COMMENT '用户是否删除',
    ADD COLUMN targetUserDeleted TINYINT(1) DEFAULT 0 COMMENT '目标用户是否删除';

-- 空间用户关联
CREATE TABLE space_user
(
    id         bigint AUTO_INCREMENT COMMENT 'id'
        PRIMARY KEY,
    spaceId    bigint                                 NOT NULL COMMENT '空间 id',
    userId     bigint                                 NOT NULL COMMENT '用户 id',
    spaceRole  varchar(128) DEFAULT 'viewer'          NULL COMMENT '空间角色：viewer/editor/admin',
    status     tinyint      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核 1-已通过 2-已拒绝',
    createTime datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime datetime     DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    CONSTRAINT uk_spaceId_userId
        UNIQUE (spaceId, userId)
)
    COMMENT '空间用户关联' COLLATE = utf8mb4_unicode_ci;

ALTER TABLE space_user ADD INDEX idx_spaceId (spaceId);                    -- 提升按空间查询的性能
ALTER TABLE space_user ADD INDEX idx_userId  (userId);                     -- 提升按用户查询的性能


-- 标签表
CREATE TABLE tag
(
    id         bigint AUTO_INCREMENT COMMENT '标签id'
        PRIMARY KEY,
    tagName    varchar(256)                       NOT NULL COMMENT '标签名称',
    createTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',
    updateTime datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    isDelete   tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '标签' COLLATE = utf8mb4_unicode_ci;

-- 分类表
CREATE TABLE category
(
    id           bigint AUTO_INCREMENT COMMENT '分类id'
        PRIMARY KEY,
    categoryName varchar(256)                       NOT NULL COMMENT '分类名称',
    type         tinyint  DEFAULT 0                 NOT NULL COMMENT '分类类型：0-图片分类 ',
    createTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    editTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '分类编辑时间',
    updateTime   datetime DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '分类更新时间',
    isDelete     tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除'
)
    COMMENT '分类' COLLATE = utf8mb4_unicode_ci;

-- 关注列表
CREATE TABLE user_follows
(
    followId            bigint AUTO_INCREMENT
        PRIMARY KEY,
    followerId          bigint                             NOT NULL COMMENT '关注者的用户 ID',
    followingId         bigint                             NOT NULL COMMENT '被关注者的用户 ID',
    followStatus        tinyint                            NULL COMMENT '关注状态，0 表示取消关注，1 表示关注',
    isMutual            tinyint                            NULL COMMENT '是否为双向关注，0 表示单向，1 表示双向',
    lastInteractionTime datetime                           NULL COMMENT '最后交互时间',
    createTime          datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '关注关系创建时间，默认为当前时间',
    editTime            datetime DEFAULT CURRENT_TIMESTAMP NULL COMMENT '关注关系编辑时间，默认为当前时间',
    updateTime          datetime DEFAULT CURRENT_TIMESTAMP NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '关注关系更新时间，更新时自动更新',
    isDelete            tinyint  DEFAULT 0                 NULL COMMENT '是否删除，0 表示未删除，1 表示已删除'
);

CREATE INDEX idx_followStatus
    ON user_follows (followStatus);

ALTER TABLE user_follows ADD INDEX idx_follower (followerId, followStatus);
ALTER TABLE user_follows ADD INDEX idx_following (followingId, followStatus);

-- 点赞表
CREATE TABLE like_record
(
    id            bigint AUTO_INCREMENT COMMENT '主键 ID'
        PRIMARY KEY,
    userId        bigint                               NOT NULL COMMENT '用户 ID',
    targetId      bigint                               NOT NULL COMMENT '被点赞内容的ID',
    targetType    tinyint                              NOT NULL COMMENT '内容类型：1-图片 2-帖子 3-空间',
    targetUserId  bigint                               NOT NULL COMMENT '被点赞内容所属用户ID',
    isLiked       tinyint(1)                           NOT NULL COMMENT '是否点赞',
    firstLikeTime datetime   DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '第一次点赞时间',
    lastLikeTime  datetime                             NOT NULL COMMENT '最近一次点赞时间',
    isRead        tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）',
    CONSTRAINT uk_user_target
        UNIQUE (userId, targetId, targetType)
);

-- 评论表
CREATE TABLE comments
(
    commentId       bigint AUTO_INCREMENT
        PRIMARY KEY,
    userId          bigint                               NOT NULL,
    targetId        bigint                               NOT NULL COMMENT '评论目标ID',
    targetType      tinyint    DEFAULT 1                 NOT NULL COMMENT '评论目标类型：1-图片 2-帖子',
    targetUserId    bigint                               NOT NULL COMMENT '评论目标所属用户ID',
    content         text                                 NOT NULL COMMENT '评论内容',
    createTime      datetime   DEFAULT CURRENT_TIMESTAMP NULL COMMENT '创建时间',
    parentCommentId bigint     DEFAULT 0                 NULL COMMENT '0表示顶级',
    isDelete        tinyint(1) DEFAULT 0                 NULL COMMENT '是否删除，0 表示未删除，1 表示已删除',
    likeCount       bigint     DEFAULT 0                 NULL COMMENT '喜欢数量',
    dislikeCount    bigint     DEFAULT 0                 NULL COMMENT '不喜欢数量',
    isRead          tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）'
);
ALTER TABLE comments ADD INDEX idx_comments_parent_deleted (parentCommentId, isDelete);
ALTER TABLE comments ADD INDEX idx_comments_query_top (targetId, targetType, parentCommentId, createTime DESC);

-- 分享表
CREATE TABLE share_record
(
    id           bigint AUTO_INCREMENT COMMENT '分享ID'
        PRIMARY KEY,
    userId       bigint                               NOT NULL COMMENT '用户ID',
    targetId     bigint                               NOT NULL COMMENT '被分享内容的ID',
    targetType   tinyint                              NOT NULL COMMENT '内容类型：1-图片 2-帖子',
    targetUserId bigint                               NOT NULL COMMENT '被分享内容所属用户ID',
    isShared     tinyint(1) DEFAULT 1                 NOT NULL COMMENT '是否分享',
    sharedCount  bigint                               NOT NULL COMMENT '分享的数量',
    shareTime    datetime   DEFAULT CURRENT_TIMESTAMP NULL COMMENT '分享时间',
    isRead       tinyint(1) DEFAULT 0                 NOT NULL COMMENT '是否已读（0-未读，1-已读）'
)
    COMMENT '分享记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_target
    ON share_record (targetId, targetType);

CREATE INDEX idx_targetUserId_isRead
    ON share_record (targetUserId, isRead);

CREATE INDEX idx_userId_isRead
    ON share_record (userId, isRead);

CREATE INDEX idx_userId_targetType
    ON share_record (userId, targetType);

-- 分享流水表
CREATE TABLE share_history
(
   id           bigint AUTO_INCREMENT PRIMARY KEY,
   userId       bigint                     NOT NULL COMMENT '用户ID',
   targetId     bigint                     NOT NULL COMMENT '被分享内容的ID',
   targetType   tinyint                    NOT NULL COMMENT '内容类型：1-图片 ',
   targetUserId bigint                     NOT NULL COMMENT '被分享内容所属用户ID',
   shareTime    datetime  DEFAULT CURRENT_TIMESTAMP COMMENT '分享时间'
) COMMENT='记录用户每一次真实的分享行为' COLLATE = utf8mb4_unicode_ci;

-- 热门搜索表
CREATE TABLE hot_search
(
    id             bigint AUTO_INCREMENT COMMENT '主键'
        PRIMARY KEY,
    keyword        varchar(128)                       NOT NULL COMMENT '搜索关键词',
    type           varchar(32)                        NOT NULL COMMENT '搜索类型',
    count          bigint   DEFAULT 0                 NOT NULL COMMENT '搜索次数',
    createTime     datetime DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    lastUpdateTime datetime                           NOT NULL COMMENT '最后更新时间',
    isDelete       tinyint  DEFAULT 0                 NOT NULL COMMENT '是否删除',
    CONSTRAINT uk_type_keyword
        UNIQUE (type, keyword)
)
    COMMENT '热门搜索记录表' COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_type_updateTime_count_delete
    ON hot_search (type, lastUpdateTime, count, isDelete);

-- 用户积分账户表
CREATE TABLE user_points_account
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY       COMMENT '主键',
    userId          BIGINT       NOT NULL                   COMMENT '用户 id',
    totalPoints     INT DEFAULT 0 NOT NULL                  COMMENT '累计积分（历史总和）',
    availablePoints INT DEFAULT 0 NOT NULL                  COMMENT '当前可用积分（余额）',
    createTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updateTime      DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uniq_user (userId)
) COMMENT '用户积分账户表' COLLATE = utf8mb3_unicode_ci;
-- 用户积分流水表（明细）
CREATE TABLE user_points_log
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    userId       BIGINT       NOT NULL             COMMENT '用户ID',
    changeType   TINYINT      NOT NULL             COMMENT '变动类型：1-签到 2-兑换 3-系统赠送 4-其他',
    changePoints INT          NOT NULL             COMMENT '积分变化值（正数表示增加，负数表示减少）',
    beforePoints INT          NOT NULL             COMMENT '变动前积分',
    afterPoints  INT          NOT NULL             COMMENT '变动后积分',
    bizId        BIGINT                            COMMENT '业务关联ID（如spaceId、签到记录id）',
    remark       VARCHAR(255)                      COMMENT '备注',
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    KEY idx_user (userId)
) COMMENT '用户积分流水表' COLLATE = utf8mb4_unicode_ci;

-- 签到记录表
CREATE TABLE user_signin_record
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    userId     BIGINT       NOT NULL             COMMENT '用户ID',
    signDate   DATE         NOT NULL             COMMENT '签到日期',
    points     INT    DEFAULT 1 NOT NULL         COMMENT '签到获得积分',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '签到时间',
    UNIQUE KEY uniq_sign (userId, signDate)
) COMMENT '用户签到记录表' COLLATE = utf8mb4_unicode_ci;

-- 积分兑换规则表
CREATE TABLE points_exchange_rule
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    fromLevel  INT          NOT NULL             COMMENT '原等级：0-普通版 1-专业版 2-旗舰版',
    toLevel    INT          NOT NULL             COMMENT '目标等级：0-普通版 1-专业版 2-旗舰版',
    costPoints INT          NOT NULL             COMMENT '需要的积分',
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间'
) COMMENT '积分兑换规则表' COLLATE = utf8mb4_unicode_ci;

-- ai 聊天会话表
CREATE TABLE ai_chat_session
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID，主键',
    userId     VARCHAR(64) NOT NULL              COMMENT '用户ID',
    title      VARCHAR(255)                      COMMENT '会话标题',
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT 'AI聊天会话表，记录用户的聊天会话信息' COLLATE = utf8mb4_unicode_ci;

-- ai 聊天记录表
CREATE TABLE ai_chat_message
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID，主键',
    sessionId  BIGINT NOT NULL                   COMMENT '会话ID，外键关联ai_chat_session表',
    roundId    BIGINT DEFAULT 1                  COMMENT '轮次ID',
    role       VARCHAR(32) NOT NULL              COMMENT '消息角色：user / assistant / system',
    content    TEXT                              COMMENT '消息内容',
    imageUrl   TEXT                              COMMENT '图片URL',
    createTime TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (sessionId) REFERENCES ai_chat_session(id)
) COMMENT 'AI聊天消息表，存储用户与AI的对话记录' COLLATE = utf8mb4_unicode_ci;

