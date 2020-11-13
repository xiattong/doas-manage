package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;

import javax.swing.filechooser.FileSystemView;
import java.io.*;
import java.util.Map;

/**
 * 桌面快捷方式
 */
@Slf4j
public class DesktopUtil {
    public static void outputDesktop() {
        Map<String,String> addressMap = NetworkUtil.getInet4Address();
        for(String key : addressMap.keySet()){
            String fileName = "["+key+"]多组分气体走航分析显示系统";
            String content = "<script>window.location.href='http://"+addressMap.get(key)+":8086';</script>";
            doOutput(fileName,content);
        }
    }

    private static void doOutput(String fileName,String content){
        FileSystemView fsv = FileSystemView.getFileSystemView();
        File com=fsv.getHomeDirectory();
        File f=new File(com.getPath()+"\\"+fileName+".html");
        FileOutputStream fos = null;
        OutputStreamWriter dos = null;;
        try {
            fos = new FileOutputStream(f);
            dos=new OutputStreamWriter(fos);
            dos.write(content);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("生产桌面快捷方式异常："+e.getMessage());
        }finally {
            try {
                if (dos != null) {
                    dos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            }catch (Exception e){
                e.printStackTrace();
                log.error("生产桌面快捷方式异常："+e.getMessage());
            }
        }
    }
}
