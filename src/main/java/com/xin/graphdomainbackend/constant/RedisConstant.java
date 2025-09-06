package com.xin.graphdomainbackend.constant;

/**
 * redis 常量
 */
public interface RedisConstant {
    /**
     * 用户签到记录的 Redis key 前缀
     */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    /**
     * top100
     */
    String TOP_10_PIC_REDIS_KEY_PREFIX = "top10Picture:";

    /**
     * top100过期时间为1天
     */
    int TOP_100_PIC_REDIS_KEY_EXPIRE_TIME =  24 * 60 * 60;

    /**
     * 公共图库前置
     */
    String PUBLIC_PIC_REDIS_KEY_PREFIX = "tuyu:listPictureVOByPage:";

    /**
     * 空间聊天记录缓存前缀
     */
    String SPACE_CHAT_HISTORY_PREFIX = "chat:space:";

    /**
     * 图片聊天记录缓存前缀
     */
    String PICTURE_CHAT_HISTORY_PREFIX = "chat:picture:";

    /**
     * 私聊记录缓存前缀
     */
    String PRIVATE_CHAT_HISTORY_PREFIX = "chat:private:";

    /**
     * 热门搜索词缓存前缀
     */
    String HOT_SEARCH_CACHE_KEY = "hot_search:";

    /**
     * 热门搜索过期时间 15分钟
     */
    int CACHE_EXPIRE_TIME = 15 * 60;  // 15分钟

    /**
     * 获取用户签到记录的 Redis Key
     * @param year 年份
     * @param userId 用户 id
     * @return 拼接好的 Redis Key
     */
    static String getUserSignInRedisKey(int year, long userId) {
        return String.format("%s:%s:%S", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userId);
    }
}
