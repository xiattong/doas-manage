package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
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

    /** 数据文件存放位置*/
    @Value("${data.filePath}")
    private String excelFilePath;
    /** 数据刷新频率*/
    @Value("${data.refresh.hz}")
    private int refreshHz;
    @Value("${data.charsetName}")
    private String charsetName;
    /** 读取的文件内容*/
    private List<List<String>> dataList = new ArrayList<>();
    /** 文件列表*/
    private List<String> fileNameList;
    /** 当前请求读取的文件名称*/
    private String currentFileName = "";
    /** 当前实际读取的文件名*/
    private String actualCurrentFileName = "";
    /** 行的列数*/
    private int cellsNum = 0;
    /** 当前读取到的行号*/
    private int currentLineNo = 0;
    /** 时间段-开始*/
    private String timeStart;
    /** 时间段-结束*/
    private String timeEnd;

    /**
     * 读取txt线程
     * @return
     */
    @Override
    public void run() {
        while(true) {
            // 更新文件名称列表
            this.fileNameList = FileUtil.getSortedFileNameList(this.excelFilePath,".txt");
            // 文件名集合
            String filePath = this.excelFilePath;
            if(!StringUtils.isEmpty(this.currentFileName)
                    && this.fileNameList.contains(this.currentFileName)){
                // 指定文件
                filePath = filePath +"/"+this.currentFileName;
                setActualCurrentFileName(this.currentFileName);
            }else{
                filePath = filePath +"/"+this.fileNameList.get(0);
                setActualCurrentFileName(this.fileNameList.get(0));
            }
            File file = new File(filePath);
            this.cellsNum = 0;
            if(file != null) {
                String fileName = file.getName();
                long len = file.length();
                if (len > 0) {
                    byte[] ds = new byte[(int) len];
                    try {
                        MappedByteBuffer mappedByteBuffer = new RandomAccessFile(file, "r")
                                .getChannel()
                                .map(FileChannel.MapMode.READ_ONLY, 0, len);
                        for (int offset = 0; offset < len; offset++) {
                            byte b = mappedByteBuffer.get();
                            ds[offset] = b;
                        }
                        Scanner scan = new Scanner(new ByteArrayInputStream(ds), this.charsetName).useDelimiter(" ");
                        List<List<String>> tempDataList = new ArrayList<>();
                        int slideIndex = this.currentLineNo;
                        int lineNum = 0;
                        while (scan.hasNext()) {
                            String[] lines = scan.next().split("\r\n");
                            lineNum = lineNum + lines.length;
                            boolean markBreak = false;
                            for ( ;slideIndex < lineNum; slideIndex++) {
                                String line = lines[slideIndex];
                                if (this.cellsNum == 0) {
                                    tempDataList.add(Arrays.asList(line.split("~")));
                                    this.cellsNum = tempDataList.get(0).size();
                                } else if (line.split("~").length == this.cellsNum) {
                                    // 加入时间段判断
                                    String[] lineArray = line.split("~");
                                    if (DateUtil.isBetweenDateTime(lineArray[0], this.timeStart, this.timeEnd)) {
                                        tempDataList.add(Arrays.asList(lineArray));
                                        markBreak = true;
                                    } else if (markBreak) {
                                        break;
                                    }
                                }
                            }
                            // 保存当前行号
                            this.currentLineNo = slideIndex;
                        }
                        log.info("Read the source file " + fileName + " tempDataList：" + tempDataList.size());
                        this.dataList.addAll(tempDataList);
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("Read source file exception! Read reset! :" + e.getMessage());
                    }
                }
            } else {
                log.error("No source file to read! wait and try again!");
            }
            try {
                Thread.sleep(this.refreshHz*1000);
                System.gc();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("Source file reader thread exception! :" + e.getMessage());
            }
        }
    }

    /**
     * 控制读取文件的刷新
     * @param actualCurrentFileName
     */
    public void setActualCurrentFileName(String actualCurrentFileName) {
        if (StringUtils.isEmpty(this.actualCurrentFileName)) {
            this.actualCurrentFileName = actualCurrentFileName;
        } else {
            if (!this.actualCurrentFileName.equals(actualCurrentFileName)) {
                // 当读取的文件发生变化时，重新开始读取文件
                this.currentLineNo = 0;
                this.dataList.clear();
                log.info("The source file has changed, {} -> {},",this.currentFileName,currentFileName);
                this.actualCurrentFileName = actualCurrentFileName;
            }
        }
    }

    /**
     * 当前读取的文件名称
     * @param currentFileName
     */
    public void setCurrentFileName(String currentFileName){
        this.currentFileName = currentFileName;
    }

    public List<List<String>> getDataList() {
        return dataList;
    }

    public List<String> getFileNameList() {
        return fileNameList;
    }

    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }

    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }
}
