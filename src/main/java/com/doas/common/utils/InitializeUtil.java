package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.net.URI;

/**
 * 自动访问前端工具
 */
@Slf4j
public class InitializeUtil {
    // 自动打开
    public static void autoOpenWeb() {
        try {
            String url = "http://127.0.0.1:8086";
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
