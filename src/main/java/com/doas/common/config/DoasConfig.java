package com.doas.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author ：xiattong
 * @description：文档配置
 * @version: $
 * @date ：Created in 2022/4/23 12:24
 * @modified By：
 */
@Component
@Getter
@Setter
public class DoasConfig {
    /** 数据文件存放位置*/
    @Value("${data.filePath}")
    private String dataFilePath;

    /** 数据刷新频率*/
    @Value("${data.refresh.hz}")
    private int refreshHz;

    /** 字符编码*/
    @Value("${data.charsetName}")
    private String charsetName;

    /** 地图展示类型（map-line 垂线； map-wall 垂面）*/
    @Value("${map-type}")
    private String mapType;

    /** 红色色等值(0:表示系统处理; 如需指定,请与因子一一对应,使用英文逗号隔开)*/
    @Value("${red-list}")
    private String defaultRedList;

    /** 红色最大值倍数(默认1，最多取两位有效数数字)*/
    @Value("${red-scale}")
    private String redScale;

    /** 公司名称*/
    @Value("${company-name}")
    private String companyName;

    /** 写入新文件间隔时间，单位分钟*/
    @Value("${file-refresh-time}")
    private Integer fileRefreshTime = 120;

    /** 因子数据采集 - 串口号*/
    @Value("${data.serial-number}")
    private String serialNumber;

    /** 因子数据采集 - 波特率*/
    @Value("${data.baud-rate}")
    private Integer baudRate;

    /** 因子数据采集 - 校验位*/
    @Value("${data.checkout-bit}")
    private Integer checkoutBit;

    /** 因子数据采集 - 数据位*/
    @Value("${data.data-bit}")
    private Integer dataBit;

    /** 因子数据采集 - 停止位*/
    @Value("${data.stop-bit}")
    private Integer stopBit;
}
