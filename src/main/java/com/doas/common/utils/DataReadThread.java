package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

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
    /** 读取起始位置*/
    private long position = 0;
    /** 文件名称*/
    private String currentFileName = "";

    @Value("${data.filePath}")
    private String excelFilePath;

    @Value("${data.refresh.hz}")
    private int refreshHz;

    /**
     * 读取txt线程
     * @return
     */
    @Override
    public void run() {
        while(true) {
            File file = FileUtil.getLatestFile(excelFilePath, ".txt");
            String fileName = file.getName();
            // 文件变更时，清空 dataMap,重置读取位置
            if (!currentFileName.equals(fileName)) {
                dataList.clear();
                position = 0;
                currentFileName = fileName;
                log.info("读取文件重置:" + fileName);
            }
            // 增量读取文件
            long len = file.length() - position;
            if(len > position) {
                log.info("读取文件" + fileName + " : " + position + " , " + len);
                byte[] ds = new byte[(int) len];
                try {
                    MappedByteBuffer mappedByteBuffer = new RandomAccessFile(file, "rw")
                            .getChannel()
                            .map(FileChannel.MapMode.READ_ONLY, position, len);
                    for (int offset = 0; offset < len; offset++) {
                        byte b = mappedByteBuffer.get();
                        ds[offset] = b;
                    }
                    Scanner scan = new Scanner(new ByteArrayInputStream(ds)).useDelimiter(" ");
                    while (scan.hasNext()) {
                        String[] lines = scan.next().split("\r\n");
                        for (String line : lines) {
                            dataList.add(Arrays.asList(line.split("~")));
                        }
                    }
                    position = len;
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("读取文件异常，读取重置" + e.getMessage());
                    dataList.clear();
                    position = 0;
                    currentFileName = "";
                }
            }
            try {
                Thread.sleep(refreshHz*1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("读取文件线程异常" + e.getMessage());
            }
        }
    }
}
