package com.doas.common.utils;

import java.math.BigDecimal;

/**
 * 颜色处理工具类
 * @author xiattong
 */
public class ColorUtil {

    // 初始化一个7种颜色的二维数组 : 黄、绿、青、蓝、紫、橙、红
    private static int[][] rainbow = {
            {255,255,0},{0,255,0},{	0,255,255},{0,0,255},{255,0,255},{255,69,0},{255,0,0}
    };

    /**
     * 颜色转换
     * 目的 : 做一个转换，把 0 ~ red 放大到 [0,0,255] ~ [255,0,0] ,即蓝色到红色之间
     *   1. 先把 0 ~ red 放大到 [0,0,0] ~ [254,255,1]
     *   2. 再把 [0,0,0] ~ [254,255,1] 平移至 [0,0,255] ~ [255,0,0]
     *   3. 数据大于 red 后，直接返回 [255,0,0]
     * @param value ：待转换颜色值
     * @param red ：红色标记值
     * @return
     */
    public static int[] convertNumberToColor(double value,int red) {
        if(value > red){
            return new int[]{255,0,0};
        }
        int convertInt = (int)((value/red) * (254*256*256 + 255*256 + 1)) + 255;
        int[] rgb = new int[3];
        rgb[0] = (convertInt & 0xff0000) >> 16;
        rgb[1] = (convertInt & 0xff00) >> 8;
        rgb[2] = (convertInt & 0xff);
        return rgb;
    }

    /**
     * 颜色转换（方式二）
     * 按照数值，在colorRainbow中选
     * @param value ：待转换颜色值
     * @param scale ：色域值
     */
    public static int[] convertColorByRainbow(double value,int scale) {
        int colorIdx = (int)(value/scale);
        if(colorIdx > 6){
            colorIdx = 6;
        }
        return rainbow[colorIdx];
    }

    /**
     * RGB 颜色转 VertexColor
     * @param value
     * @param scale
     * @return
     */
    public static double[] convertVertexColors(double value,int scale){
        int[] rgbArray = convertColorByRainbow(value,scale);
        double[] vertexColors = new double[rgbArray.length];
        for (int i = 0 ; i < rgbArray.length ; i++){
            BigDecimal temp = new BigDecimal(rgbArray[i]/255.00);
            vertexColors[i] = temp.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return vertexColors;
    }
}
