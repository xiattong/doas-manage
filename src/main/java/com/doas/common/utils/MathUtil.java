package com.doas.common.utils;

import java.math.BigDecimal;

/**
 * @author ：xiattong
 * @description：数字处理
 * @version: $
 * @date ：Created in 2021/8/29 1:31
 * @modified By：
 */
public class MathUtil {

    public static final int DEFAULT_SCALE = 2;

    /**
     * 四舍五入，保留2位有效小数
     * @param value
     * @return
     */
    public static double roundHalfUp(double value) {
        BigDecimal bgValue = BigDecimal.valueOf(value);
        return bgValue.setScale(DEFAULT_SCALE,   BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
