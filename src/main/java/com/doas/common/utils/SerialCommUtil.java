package com.doas.common.utils;

import com.doas.common.config.Constant;
import com.doas.common.config.DoasConfig;
import com.doas.common.config.SerialParamConfig;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author ：xiattong
 * @description：串口数据读取工具
 * @version: $
 * @date ：Created in 2022/7/2 11:05
 * @modified By：
 */
@Slf4j
public class SerialCommUtil {
    // 配置信息
    private static DoasConfig doasConfig;
    // 数据筛选正则
    private final static Pattern DATA_PATTERN = Pattern.compile("ST(.*)ED");
    // 定时器 10秒
    private static long startTimer = System.currentTimeMillis();
    // 系统是否已经链接
    public volatile static boolean isConnected = false;

    /**
     * 初始化串口
     *
     * @throws
     * @author LinWenLi
     * @date 2018年7月21日下午3:44:16
     * @Description: TODO
     * @param: paramConfig  存放串口连接必要参数的对象（会在下方给出类代码）
     * @return: void
     */
    public static void init(DoasConfig doasConfig) throws InterruptedException {
        SerialCommUtil.doasConfig = doasConfig;
        // 起动因子数据采集串口监听
        SerialParamConfig paramConfig = SerialParamConfig.builder()
                .serialName(Constant.SERIAL_NAME_DATA)
                .baudRate(doasConfig.getBaudRate()).checkoutBit(doasConfig.getCheckoutBit())
                .dataBit(doasConfig.getDataBit()).stopBit(doasConfig.getStopBit())
                .dataFilePath(doasConfig.getDataFilePath()).fileRefreshTime(doasConfig.getFileRefreshTime())
                .build();
        // 循环通讯端口
        for (int failCount = 0; failCount < 2000; failCount++) {
            // 获取系统中所有的通讯端口
            Enumeration<CommPortIdentifier> portList = CommPortIdentifier.getPortIdentifiers();
            while (portList.hasMoreElements()) {
                CommPortIdentifier commPortId = portList.nextElement();
                // 判断是否是串口
                if (commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    try {
                        // 开启串口 阻塞时等待的毫秒数
                        SerialPort serialPort = (SerialPort) commPortId.open(paramConfig.getSerialName(), 2000);
                        // 设置串口通讯参数:波特率，数据位，停止位,校验方式
                        serialPort.setSerialPortParams(paramConfig.getBaudRate(), paramConfig.getDataBit(),
                                paramConfig.getStopBit(), paramConfig.getCheckoutBit());
                        // 检查是否能够接收到有效数据,有就开始读取
                        testAndReadData(serialPort, paramConfig);
                        serialPort.close();
                    } catch (PortInUseException e) {
                        throw new RuntimeException("端口被占用");
                    } catch (UnsupportedCommOperationException e) {
                        throw new RuntimeException("不支持的COMM端口操作异常");
                    }
                }
            }
            // 三秒钟扫描一次，共扫描 100 次
            log.info("休息一会，继续下一轮串口扫描...");
            Thread.sleep(1000);
        }
    }

    /**
     * 从串口读取数据
     * @param serialPort 要读取的串口
     * @return 读取的数据
     */
    public static void testAndReadData(SerialPort serialPort, SerialParamConfig paramConfig) throws InterruptedException {
        InputStream inputStream = null;
        StringBuilder sb = new StringBuilder();
        try {
            for (int reTry = 0; reTry < 3; reTry ++) {
                inputStream = serialPort.getInputStream();
                // 通过输入流对象的available方法获取数组字节长度
                byte[] readBuffer = new byte[inputStream.available()];
                // 从线路上读取数据流
                int len = 0;
                log.info(serialPort.getName() + "扫描中：" + readBuffer.length);
                while ((len = inputStream.read(readBuffer)) != -1) {
                    // 直接获取到的数据
                    String data = new String(readBuffer, 0, len).trim();
                    sb.append(data);
                    Matcher m = DATA_PATTERN.matcher(sb.toString());
                    if (m.find()) {
                        isConnected = true;
                        // 读取数据
                        FileUtil.writeData(paramConfig.getDataFilePath(), paramConfig.getFileRefreshTime(), m.group(0));
                        // 更新即时器
                        startTimer = System.currentTimeMillis();
                        reTry = 0;
                        sb.delete(0, sb.length());
                    }
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            isConnected = false;
            serialPort.close();
            log.info("通讯中断，写入一条中断数据...");
            FileUtil.writeBreakData(paramConfig.getDataFilePath(), paramConfig.getFileRefreshTime());
            log.info("重新扫描端口...");
            init(SerialCommUtil.doasConfig);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 超过 10s 写入断开数据
            log.info("timer:{}", System.currentTimeMillis());
            if (System.currentTimeMillis() - startTimer > 10000) {
                isConnected = false;
                log.info("超过10s,写入断开数据");
                FileUtil.writeBreakData(paramConfig.getDataFilePath(), paramConfig.getFileRefreshTime());
            }
        }
    }
}
