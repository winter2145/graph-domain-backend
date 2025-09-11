package com.xin.graphdomainbackend.utils;

/**
 *  让你从 Redis 取回来的“字节数组”里，精确读出某一位是 0 还是 1
 *  也就是告诉你“某一天有没有签到
 */
public class BitMapUtil {
    public static boolean getBit(byte[] data, int pos) {
        int byteIndex = pos / 8;
        int bitIndex  = pos % 8;
        return (data[byteIndex] & (1 << bitIndex)) != 0;
    }
}