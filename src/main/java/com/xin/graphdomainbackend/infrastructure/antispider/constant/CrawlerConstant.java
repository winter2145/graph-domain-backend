package com.xin.graphdomainbackend.infrastructure.antispider.constant;

/**
 * 爬虫防护相关常量配置类
 */
public class CrawlerConstant {

    /**
     * 警告阈值 - 普通请求
     * 当用户或IP在时间窗口内的访问次数超过此值但未达禁用阈值时，会记录警告日志
     */
    public static final int WARN_COUNT = 30;

    /**
     * 禁用/封禁阈值 - 普通请求
     * 当用户或IP在时间窗口内的访问次数超过此值时，会触发封禁逻辑
     */
    public static final int BAN_COUNT = 50;

    /**
     * Redis计数器的过期时间（单位：秒）
     * 此时间应略大于或等于统计的时间窗口长度，用于自动清理Redis中的旧数据，防止 Redis 内存无限增长。
     */
    public static final int EXPIRE_TIME = 150;

    /**
     * 已登录用户访问次数计数器的Redis Key前缀
     * 完整Key格式：user:access:{userId}:{timeFactor}
     */
    public static final String USER_KEY = "user:access";

    /**
     * 未登录用户（IP）访问次数计数器的Redis Key前缀
     * 完整Key格式：ip:access:{ipAddress}:{timeFactor}
     */
    public static final String IP_KEY = "ip:access";

    /**
     * IP封禁列表的Redis Key前缀
     * 完整Key格式：ban:ip:{ipAddress}
     */
    public static final String BAN_IP_KEY = "ban:ip";
}