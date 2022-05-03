package com.doas.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author ：xiattong
 * @description：串口配置
 * @version: $
 * @date ：Created in 2022/4/23 12:15
 * @modified By：
 */
@Component
@Getter
@Setter
public class SerialParamConfig {

    /** 数据文件存放位置*/
    private String dataFilePath;

    /** 写入新文件间隔时间，单位分钟*/
    private Integer fileRefreshTime = 120;

    private String serialNumber; // 串口号
    private String serialName;   // 串口名
    private int baudRate;        // 波特率
    private int checkoutBit;     // 校验位
    private int dataBit;         // 数据位
    private int stopBit;         // 停止位
    public SerialParamConfig() {}
    /**
     * 构造方法
     * @param serialNumber    串口号
     * @param baudRate        波特率
     * @param checkoutBit    校验位
     * @param dataBit        数据位
     * @param stopBit        停止位
     */
    public SerialParamConfig(String serialNumber, String serialName, int baudRate, int checkoutBit, int dataBit, int stopBit, String dataFilePath, Integer fileRefreshTime) {
        this.serialNumber = serialNumber;
        this.serialName = serialName;
        this.baudRate = baudRate;
        this.checkoutBit = checkoutBit;
        this.dataBit = dataBit;
        this.stopBit = stopBit;
        this.dataFilePath = dataFilePath;
        this.fileRefreshTime = fileRefreshTime;
    }
}
