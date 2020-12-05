package com.doas.common.utils;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * txt 文件解析工具
 * 使用单独的线程把数据读取到内存中，客户端线程直接从内存中读取数据
 * 避免客户端每次请求都去解析磁盘文件，造成资源浪费。
 * @author xiattong
 */
@Slf4j
@Component
public class DataReadThread extends Thread {

    /** 读取的文件内容*/
    public static List<List<Object>> dataList = new ArrayList<>();
    /** 文件名称*/
    private String currentFileName = "";

    @Value("${data.filePath}")
    private String excelFilePath;

    @Value("${data.refresh.hz}")
    private int refreshHz;

    @Value("${data.charsetName}")
    private String charsetName;
    // 行的列数
    private int cellsNum = 0;

    /**
     * 读取txt线程
     * @return
     */
    @Override
    public void run() {
        List<List<Object>> tempDataList = null;
        while(true) {
            File file = FileUtil.getLatestFile(excelFilePath, ".txt");
            tempDataList = new ArrayList<>();
            cellsNum = 0;
            if(file != null) {
                String fileName = file.getName();
                long len = file.length();
                if (len > 0) {
                    log.info("Read the file " + fileName + " : " + len);
                    byte[] ds = new byte[(int) len];
                    try {
                        MappedByteBuffer mappedByteBuffer = new RandomAccessFile(file, "r")
                                .getChannel()
                                .map(FileChannel.MapMode.READ_ONLY, 0, len);
                        for (int offset = 0; offset < len; offset++) {
                            byte b = mappedByteBuffer.get();
                            ds[offset] = b;
                        }
                        Scanner scan = new Scanner(new ByteArrayInputStream(ds), charsetName).useDelimiter(" ");
                        while (scan.hasNext()) {
                            String[] lines = scan.next().split("\r\n");
                            for (String line : lines) {
                                if (cellsNum == 0) {
                                    tempDataList.add(Arrays.asList(line.split("~")));
                                    cellsNum = tempDataList.get(0).size();
                                } else if (line.split("~").length == cellsNum) {
                                    List<Object> dataLine = Arrays.asList(line.split("~"));
                                    List<Object> coordinate = dataLine.subList(dataLine.size() - 5, dataLine.size() - 3);
                                    // 舍弃坐标中包含0的数据
                                    try {
                                        if (Double.valueOf(coordinate.get(0).toString()) == 0
                                                || Double.valueOf(coordinate.get(1).toString()) == 0)
                                            continue;
                                    } catch (NumberFormatException e){
                                        log.info(coordinate.toString());
                                        log.error("坐标转化异常："+e.getMessage());
                                    }
                                     tempDataList.add(dataLine );
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("Read file exception! Read reset! :" + e.getMessage());
                    }
                }
                dataList = tempDataList;
            } else {
                log.error("No file to read! wait and try again!");
            }
            System.gc();
            try {
                Thread.sleep(refreshHz*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("File reader thread exception! :" + e.getMessage());
            }

        }
    }
}
