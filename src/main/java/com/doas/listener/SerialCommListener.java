package com.doas.listener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Objects;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.doas.common.config.Constant;
import com.doas.common.config.DoasConfig;
import com.doas.common.config.SerialParamConfig;
import com.doas.common.utils.FileUtil;
import gnu.io.CommPortIdentifier;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;
import lombok.extern.slf4j.Slf4j;

/**
 * @author ：xiattong
 * @description：串口监听器
 * @version: $
 * @date ：Created in 2022/4/23 12:17
 * @modified By：
 */
@Slf4j
public class SerialCommListener implements SerialPortEventListener {
    // 检测系统中可用的通讯端口类
    private CommPortIdentifier commPortId;
    // 枚举类型
    private Enumeration<CommPortIdentifier> portList;
    // RS232串口
    private SerialPort serialPort;
    // 原始参数
    private SerialParamConfig paramConfig;
    // 数据筛选正则
    private final static Pattern DATA_PATTERN = Pattern.compile("ST(.*)ED");
    private final static Pattern DATA_PATTERN_CHECK = Pattern.compile("ST");
    // 数据
    private StringBuilder sb = new StringBuilder();
    // 连接断开时间设置
    private Long connectTimeOut = 10000L;
    // 断开开始时间
    private Long connectBreakStart= null;
    // 配置信息
    private DoasConfig doasConfig;

    /**
     * 初始化串口
     * @author LinWenLi
     * @date 2018年7月21日下午3:44:16
     * @Description: TODO
     * @param: paramConfig  存放串口连接必要参数的对象（会在下方给出类代码）
     * @return: void
     * @throws
     */
    public void init(DoasConfig doasConfig) throws InterruptedException {
        this.doasConfig = doasConfig;
        // 起动因子数据采集串口监听
        this.paramConfig = SerialParamConfig.builder()
                .serialNumber(doasConfig.getSerialNumber()).serialName(Constant.SERIAL_NAME_DATA)
                .baudRate(doasConfig.getBaudRate()).checkoutBit(doasConfig.getCheckoutBit())
                .dataBit(doasConfig.getDataBit()).stopBit(doasConfig.getStopBit())
                .dataFilePath(doasConfig.getDataFilePath()).fileRefreshTime(doasConfig.getFileRefreshTime())
                .build();
        // 记录是否含有指定串口
        boolean isExist = false;
        // 循环通讯端口
        for (int failCount = 0 ; failCount < 200; failCount ++) {
            paramConfig.setSerialNumber(doasConfig.getSerialNumber());
            // 获取系统中所有的通讯端口
            portList = CommPortIdentifier.getPortIdentifiers();
            while (portList.hasMoreElements()) {
                commPortId = portList.nextElement();
                // 判断是否是串口
                if (commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                    // 比较串口名称是否是指定串口
                    // if (paramConfig.getSerialNumber().equals(commPortId.getName())) {
                        // 打开串口
                        try {
                            // open:（应用程序名【随意命名】，阻塞时等待的毫秒数）
                            serialPort = (SerialPort) commPortId.open(paramConfig.getSerialName(), 2000);
                            // 设置串口通讯参数:波特率，数据位，停止位,校验方式
                            serialPort.setSerialPortParams(paramConfig.getBaudRate(), paramConfig.getDataBit(),
                                    paramConfig.getStopBit(), paramConfig.getCheckoutBit());
                            // 检查是否能够接收到有效数据
                            if (testReadData(serialPort)) {
                                // 设置串口监听
                                serialPort.addEventListener(this);
                                // 设置串口数据时间有效(可监听)
                                serialPort.notifyOnDataAvailable(true);
                                // 中断事件监听
                                serialPort.notifyOnBreakInterrupt(true);
                                // 串口存在
                                isExist = true;
                                log.info(paramConfig.getSerialNumber() + "初始化成功！");
                                break;
                            }
                            serialPort.close();
                            continue;
                        } catch (PortInUseException e) {
                            throw new RuntimeException("端口被占用");
                        } catch (TooManyListenersException e) {
                            throw new RuntimeException("监听器过多");
                        } catch (UnsupportedCommOperationException e) {
                            throw new RuntimeException("不支持的COMM端口操作异常");
                        }
                    }
                //}
            }
            if (isExist) {
                break;
            }
            // 三秒钟扫描一次，共扫描 100 次
            log.info("没有匹配到有效的COM口，继续查找。。。。");
            Thread.sleep(3000);
        }
        // 若不存在该串口则抛出异常
        if (!isExist) {
            throw new RuntimeException("不存在该串口！");
        }
    }

    /**
     * 实现接口SerialPortEventListener中的方法 读取从串口中接收的数据
     */
    @Override
    public void serialEvent(SerialPortEvent event) {
        System.out.println("来事件了。。。。");
        switch (event.getEventType()) {
            case SerialPortEvent.BI: // 通讯中断
                log.info(paramConfig.getSerialNumber() + "通讯中断事件");
                break;
            case SerialPortEvent.OE: // 溢位错误
            case SerialPortEvent.FE: // 帧错误
            case SerialPortEvent.PE: // 奇偶校验错误
            case SerialPortEvent.CD: // 载波检测
            case SerialPortEvent.CTS: // 清除发送
            case SerialPortEvent.DSR: // 数据设备准备好
            case SerialPortEvent.RI: // 响铃侦测
            case SerialPortEvent.OUTPUT_BUFFER_EMPTY: // 输出缓冲区已清空
                break;
            case SerialPortEvent.DATA_AVAILABLE: // 有数据到达
                // 调用读取数据的方法
                log.info(paramConfig.getSerialNumber() + "有数据到达");
                readComm();
                break;
            default:
                break;
        }
    }

    /**
     * 从串口读取数据
     * @param serialPort 要读取的串口
     * @return 读取的数据
     */
    public static boolean testReadData(SerialPort serialPort) {
        InputStream inputStream = null;
        StringBuilder sb = new StringBuilder();

        try {
            for (int reTry = 0; reTry < 3; reTry ++) {
                inputStream = serialPort.getInputStream();
                // 通过输入流对象的available方法获取数组字节长度
                byte[] readBuffer = new byte[inputStream.available()];
                // 从线路上读取数据流
                int len = 0;
                System.out.println(serialPort.getName() + "扫描中：" + readBuffer.length);
                while ((len = inputStream.read(readBuffer)) != -1) {
                    // 直接获取到的数据
                    String data = new String(readBuffer, 0, len).trim();
                    sb.append(data);
                    Matcher m = DATA_PATTERN_CHECK.matcher(sb.toString());
                    if (m.find()) {
                        return true;
                    }
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 读取串口返回信息
     * @author LinWenLi
     * @date 2018年7月21日下午3:43:04
     * @return: void
     */
    public void readComm() {
        InputStream inputStream = null;
        try {
            inputStream = serialPort.getInputStream();
            // 通过输入流对象的available方法获取数组字节长度
            byte[] readBuffer = new byte[inputStream.available()];
            // 从线路上读取数据流
            int len = 0;
            while ((len = inputStream.read(readBuffer)) != -1) {
                // 直接获取到的数据
                String data = new String(readBuffer, 0, len).trim();
                sb.append(data);
                Matcher m = DATA_PATTERN.matcher(sb.toString());
                if (m.find()) {
                    sb = new StringBuilder();
                    // 写入数据
                    FileUtil.writeData(this.paramConfig.getDataFilePath(), this.paramConfig.getFileRefreshTime(), m.group(0));
                    inputStream.close();
                }
                break;
            }
        } catch (IOException e) {
            // 开始即时
            if (connectBreakStart == null) {
                connectBreakStart = System.currentTimeMillis();
            }
            sb = new StringBuilder();
            if (Objects.nonNull(inputStream)) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (System.currentTimeMillis() - connectBreakStart > connectTimeOut) {
                connectBreakStart = null;
                log.info(Thread.currentThread() + "链接中断。。。");
                serialPort.close();
                try {
                    // 写入数据
                    FileUtil.writeBreakData(this.paramConfig.getDataFilePath(), this.paramConfig.getFileRefreshTime());
                } catch (Exception writeBreakDataError) {
                    log.error("写入中断数据异常：{}", writeBreakDataError.getMessage());
                }
                // 重启串口监听线程
                try {
                    this.init(doasConfig);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}
