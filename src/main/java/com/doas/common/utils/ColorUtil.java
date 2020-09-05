package com.doas.common.utils;

import java.math.BigDecimal;

public class ColorUtil {

    /**
     * 颜色转换
     * 思路 : 先做一个转换，把 0 ~ red 放大到 [0,0,0] ~ [0,ff,ff]
     * 然后和 ff 取反，把 [0,0,0] ~ [0,ff,ff] => [ff,ff,ff] ~ [ff,0,0]
     * 数据大于 red 后，直接返回ff,0,0
     * @param value ：待转换颜色值
     * @param red ：红色标记值
     * @return
     */
    public static int[] convertNumberToColor(double value,int red) {
        if(value > red){
            return new int[]{255,0,0};
        }
        int convertInt = (int)((value/red) * (255*256+255));
        int[] rgb = new int[3];
        // ^ 0xff
        rgb[0] = (convertInt & 0xff0000) >> 16;
        rgb[1] = (convertInt & 0xff00) >> 8;
        rgb[2] = (convertInt & 0xff);
        return rgb;
    }

    /**
     * RGB 颜色转 VertexColor
     * @param value
     * @param red
     * @return
     */
    public static double[] convertVertexColors(double value,int red){
        int[] rgbArray = convertNumberToColor(value,red);
        double[] vertexColors = new double[rgbArray.length];
        for (int i = 0 ; i < rgbArray.length ; i++){
            BigDecimal temp = new BigDecimal(rgbArray[i]/255.00);
            vertexColors[i] = temp.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return vertexColors;
    }
}
