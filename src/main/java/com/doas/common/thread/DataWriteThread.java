package com.doas.common.thread;

import com.doas.common.config.SerialParamConfig;
import com.doas.listener.SerialCommListener;
import lombok.SneakyThrows;

import java.io.InputStream;

/**
 * @author ：xiattong
 * @description：因子数据采集线程
 * @version: $
 * @date ：Created in 2022/4/23 11:08
 * @modified By：
 */
public class DataWriteThread extends Thread{

    private SerialCommListener serialCommListener;

    public DataWriteThread(SerialCommListener serialCommListener) {
        this.serialCommListener = serialCommListener;
    }

    @SneakyThrows
    @Override
    public void run() {

    }


}
