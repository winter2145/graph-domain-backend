package com.xin.graphdomainbackend.utils;

/**
 * 将颜色字符串标准化为 0xRRGGBB 格式，可选是否将3位简写形式扩展为6位。
 */
public class HexColorExpanderUtils {


    /**
     * @param rawColor 原始颜色字符串，可能包含前缀、简写、无效字符等
     * @param expandShortFormat 是否将 abc → aabbcc 形式扩展
     * @return 修正后的标准颜色字符串，格式为 0xRRGGBB
     */
    public static String normalizeHexColor(String rawColor, boolean expandShortFormat) {
        if (rawColor == null || rawColor.isEmpty()) {
            return "0x000000"; // 默认黑色
        }

        // 去除前缀 "0x" 或 "#"
        String input = rawColor.toLowerCase().replaceFirst("^(0x|#)", "");

        // 去除所有非十六进制字符
        input = input.replaceAll("[^0-9a-f]", "");

        // 扩展 3 位颜色格式（如 abc → aabbcc），仅在开启开关时启用
        if (expandShortFormat && input.length() == 3) {
            input = input.replaceAll("(.)", "$1$1");
        }

        // 超过6位截断
        if (input.length() > 6) {
            input = input.substring(0, 6);
        }

        // 不足6位前补0
        input = String.format("%6s", input).replace(' ', '0');

        return "0x" + input;
    }

    /**
     * 默认不开启简写扩展（向后兼容）
     */
    public static String normalizeHexColor(String rawColor) {
        return normalizeHexColor(rawColor, false);
    }
}
