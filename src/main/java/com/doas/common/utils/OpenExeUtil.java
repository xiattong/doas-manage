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
            log.info("The program started successfully! :"+exePath);
        }else{
            log.info("Existing programs! :"+exePath);
        }
    }
}
