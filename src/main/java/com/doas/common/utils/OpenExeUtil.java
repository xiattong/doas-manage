package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 打开exe应用程序工具
 * @author xiattong
 */
@Slf4j
public class OpenExeUtil {

    public static void openExe(String exePath) throws IOException {
        String exeName = exePath.substring(exePath.lastIndexOf("/")+1);
        Process pro = Runtime.getRuntime().exec(
                "cmd.exe /c tasklist |find \""+exeName+"\"");
        BufferedReader reader=new BufferedReader(new InputStreamReader(pro.getInputStream()));
        String read =reader.readLine();
        if (read == null){
            Runtime.getRuntime().exec("\""+exePath+"\"");
            log.info("程序"+exePath+"启动成功!");
        }else{
            log.info("已经存在启动的程序"+exePath+"!");
        }
    }


}
