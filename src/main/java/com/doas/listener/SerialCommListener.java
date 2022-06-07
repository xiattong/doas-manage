package com.doas.listener;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Objects;
import java.util.TooManyListenersException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final Pattern DATA_PATTERN = Pattern.compile("^ST(.*)ED$");
    // 数据
    private StringBuilder sb = new StringBuilder();

    /**
     * 初始化串口
     * @author LinWenLi
     * @date 2018年7月21日下午3:44:16
     * @Description: TODO
     * @param: paramConfig  存放串口连接必要参数的对象（会在下方给出类代码）
     * @return: void
     * @throws
     */
    public void init(SerialParamConfig paramConfig) {
        this.paramConfig = paramConfig;
        // 获取系统中所有的通讯端口
        portList = CommPortIdentifier.getPortIdentifiers();
        // 记录是否含有指定串口
        boolean isExist = false;
        // 循环通讯端口
        while (portList.hasMoreElements()) {
            commPortId = portList.nextElement();
            // 判断是否是串口
            if (commPortId.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                // 比较串口名称是否是指定串口
                if (paramConfig.getSerialNumber().equals(commPortId.getName())) {
                    // 串口存在
                    isExist = true;
                    // 打开串口
                    try {
                        // open:（应用程序名【随意命名】，阻塞时等待的毫秒数）
                        serialPort = (SerialPort) commPortId.open(paramConfig.getSerialName(), 2000);
                        // 设置串口监听
                        serialPort.addEventListener(this);
                        // 设置串口数据时间有效(可监听)
                        serialPort.notifyOnDataAvailable(true);
                        // 设置串口通讯参数:波特率，数据位，停止位,校验方式
                        serialPort.setSerialPortParams(paramConfig.getBaudRate(), paramConfig.getDataBit(),
                                paramConfig.getStopBit(), paramConfig.getCheckoutBit());
                        log.info(commPortId.getCurrentOwner() + "初始化成功！");
                    } catch (PortInUseException e) {
                        throw new RuntimeException("端口被占用");
                    } catch (TooManyListenersException e) {
                        throw new RuntimeException("监听器过多");
                    } catch (UnsupportedCommOperationException e) {
                        throw new RuntimeException("不支持的COMM端口操作异常");
                    }
                    // 结束循环
                    break;
                }
            }
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
        switch (event.getEventType()) {
            case SerialPortEvent.BI: // 通讯中断
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
                readComm();
                break;
            default:
                break;
        }
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
                    FileUtil.writeData(this.paramConfig.getDataFilePath(), m.group(0), this.paramConfig.getFileRefreshTime());
                    inputStream.close();
                }
                break;
            }
        } catch (IOException e) {
            sb = new StringBuilder();
            if (Objects.nonNull(inputStream)) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            throw new RuntimeException("读取串口数据时发生IO异常");
        }
    }

    public CommPortIdentifier getCommPortId() {
        return commPortId;
    }

    public void setCommPortId(CommPortIdentifier commPortId) {
        this.commPortId = commPortId;
    }

    public Enumeration<CommPortIdentifier> getPortList() {
        return portList;
    }

    public void setPortList(Enumeration<CommPortIdentifier> portList) {
        this.portList = portList;
    }

    public SerialPort getSerialPort() {
        return serialPort;
    }

    public void setSerialPort(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    public SerialParamConfig getParamConfig() {
        return paramConfig;
    }

    public void setParamConfig(SerialParamConfig paramConfig) {
        this.paramConfig = paramConfig;
    }
}
