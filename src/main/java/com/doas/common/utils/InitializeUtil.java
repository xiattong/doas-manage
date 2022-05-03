package com.doas.common.utils;

import com.doas.common.config.SerialParamConfig;
import lombok.extern.slf4j.Slf4j;

import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.Map;

/**
 * 自动访问前端工具
 */
@Slf4j
public class InitializeUtil {
    // 自动打开
    public static void autoOpenWeb() {
        try {
            String url = "http://127.0.0.1:8086";
            //Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
            Desktop desktop = Desktop.getDesktop();
            if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                URI uri = new URI(url);
                desktop.browse(uri);
            }
        } catch(Exception e) {
            e.printStackTrace();
            log.error("打开浏览器失败："+e.getMessage());
        }
    }
}
