package com.doas.common.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 颜色处理工具类
 * @author xiattong
 */
public class ColorUtil {


    private static List<String> defaultColors = Arrays.asList(
            "#ed1299", "#09f9f5", "#246b93", "#cc8e12", "#d561dd", "#c93f00", "#925bea", "#63ff4f",
            "#280f7a", "#6373ed", "#5b910f" ,"#dd27ce", "#07a301", "#167275", "#391c82", "#2baeb5",
            "#4aef7b", "#e86502", "#9ed84e", "#39ba30", "#6ad157", "#8249aa", "#99db27", "#e07233",
            "#ce2523", "#f7aa5d", "#cebb10", "#03827f", "#931635", "#373bbf", "#a1ce4c", "#ef3bb6",
            "#1a918f", "#ff66fc", "#2927c4", "#7149af" ,"#57e559" ,"#8e3af4", "#f9a270" ,"#22547f",
            "#edd05e", "#6f25e8", "#0dbc21", "#7b34c1" ,"#0cf29a" ,"#d80fc1", "#ff523f", "#db5e92", "#d66551"
    );


    /**
     * 颜色转换（方案一）
     * 目的 : 做一个转换，把 0 ~ red 放大到 [0,0,255] ~ [255,0,0] ,即蓝色到红色之间
     *   1. 先把 0 ~ red 放大到 [0,0,0] ~ [254,255,1]
     *   2. 再把 [0,0,0] ~ [254,255,1] 平移至 [0,0,255] ~ [255,0,0]
     *   3. 数据大于 red 后，直接返回 [255,0,0]
     * @param value ：待转换颜色值
     * @param red ：红色标记值
     * @return
     */
    public static int[] convert(double value,int red) {
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
     * 颜色转换（方案三）
     */
    public static int[] convertP3(double value,int red) {
        if(value >= red){
            return new int[]{255,0,0};
        }
        int[] rgb = new int[3];
        double scale =red/4.0;//浓度区间四分
        if(value < scale){
            rgb[0] = 0;
            rgb[1] = (int)(value*255/scale);
            rgb[2] = 255;
        } else if(value >= scale && value < (2*scale)){
            rgb[0] = 0;
            rgb[1] = 255;
            rgb[2] = (int) (255-(value-scale)*255/scale);
        } else if(value >= (2*scale) && value < (3*scale)){
            rgb[0] = (int) ((value-2*scale)*255/scale);
            rgb[1] = 255;
            rgb[2] = 0;
        } else if(value >= (3*scale) && value < (4*scale)){
            rgb[0] = 255;
            rgb[1] = (int) (255-(value-3*scale)*255/scale);
            rgb[2] = 0;
        } else {
            return new int[]{255,0,0};
        }
        return rgb;
    }

    /**
     * RGB 颜色转 VertexColor
     * @param value
     * @param scale
     * @return
     */
    public static double[] convertVertexColors(double value,int scale){
        int[] rgbArray = convertP3(value,scale);
        double[] vertexColors = new double[rgbArray.length];
        for (int i = 0 ; i < rgbArray.length ; i++){
            BigDecimal temp = new BigDecimal(rgbArray[i]/255.00);
            vertexColors[i] = temp.setScale(4,BigDecimal.ROUND_HALF_UP).doubleValue();
        }
        return vertexColors;
    }

    /**
     * 从默认颜色中挑颜色
     * @param size
     * @return
     */
    public static String[] getVariantColors(int size) {
        String[] colors = new String[size];
       int defaultColorSize = defaultColors.size();
       for (int i = 0 ; i < size ; i++) {
           colors[i] = defaultColors.get(i % defaultColorSize);
       }
       return colors;
    }
}
