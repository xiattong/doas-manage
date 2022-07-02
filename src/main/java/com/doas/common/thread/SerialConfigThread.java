package com.doas.common.thread;

import com.doas.common.config.DoasConfig;
import com.doas.listener.SerialCommListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author ：xiattong
 * @description：串口配置线程
 * @version: $
 * @date ：Created in 2022/6/23 15:10
 * @modified By：
 */
@Slf4j
@Component
public class SerialConfigThread extends Thread {

    @Resource
    private DoasConfig doasConfig;

    /**
     * 串口配置线程
     * @return
     */
    @Override
    public void run() {
        SerialCommListener dataCommListener = new SerialCommListener();
        try {
            System.out.println("SerialConfigThread:run" + Thread.currentThread());
            dataCommListener.init(doasConfig);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
