package com.doas.common.config;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * @author ：xiattong
 * @description：串口配置
 * @version: $
 * @date ：Created in 2022/4/23 12:15
 * @modified By：
 */
@Getter
@Setter
@Builder
public class SerialParamConfig {

    /** 数据文件存放位置*/
    private String dataFilePath;

    /** 写入新文件间隔时间，单位分钟*/
    private Integer fileRefreshTime = 120;

    private String serialName;   // 串口名
    private int baudRate;        // 波特率
    private int checkoutBit;     // 校验位
    private int dataBit;         // 数据位
    private int stopBit;         // 停止位
}
