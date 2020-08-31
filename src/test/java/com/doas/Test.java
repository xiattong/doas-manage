package com.doas;

import com.doas.common.utils.DateUtils;

import java.util.Date;

public class Test {
    public static void main(String[] args) {
        String str = DateUtils.formatTime(new Date());
        System.out.println(str);
    }
}
