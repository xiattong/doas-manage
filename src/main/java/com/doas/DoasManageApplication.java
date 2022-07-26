package com.doas;

import com.doas.common.utils.InitializeUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DoasManageApplication {
    public static void main(String[] args) {
        SpringApplication.run(DoasManageApplication.class, args);
        // 生产桌面快捷方式
        InitializeUtil.autoOpenWeb();
    }
}
