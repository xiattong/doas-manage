package com.doas;

import com.doas.common.utils.DateUtil;
import com.doas.common.utils.FileUtil;
import com.doas.common.utils.ResultObject;

import java.io.File;
import java.util.Date;

public class Test {
    public static void main(String[] args) {
        String str = DateUtil.formatTime(new Date());
        System.out.println(str);

        try {
            File file = FileUtil.getLatestFile("D:/excel/202008260945.xlsx");
            if(file != null) {
                System.out.println(file.getName());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
