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
    /** 当前读取的文件名称*/
    private String currentFileName = "";
    /** 行的列数*/
    private int cellsNum = 0;
    /** 当前读取到的行号*/
    private int currentLineNo = 0;

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
                filePath = filePath +"/"+this.currentFileName;
            }else{
                setCurrentFileName(this.fileNameList.get(0));
                filePath = filePath +"/"+this.fileNameList.get(0);

            }
            File file = new File(filePath);
            this.cellsNum = 0;
            if(file != null) {
                String fileName = file.getName();
                long len = file.length();
                if (len > 0) {
                    log.info("Read the source file " + fileName + " : " + len);
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
                            for ( ;slideIndex < lineNum; slideIndex++) {
                                String line = lines[slideIndex];
                                if (this.cellsNum == 0) {
                                    tempDataList.add(Arrays.asList(line.split("~")));
                                    this.cellsNum = tempDataList.get(0).size();
                                } else if (line.split("~").length == this.cellsNum) {
                                    tempDataList.add(Arrays.asList(line.split("~")));
                                }
                            }
                            // 保存当前行号
                            this.currentLineNo = slideIndex;
                        }
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

    public void setCurrentFileName(String currentFileName){
        if(!StringUtils.isEmpty(currentFileName)){
            if(!StringUtils.isEmpty(this.currentFileName) && !this.currentFileName.equals(currentFileName)){
                // 当读取的文件发生变化时，重新开始读取文件
                this.currentLineNo = 0;
                this.dataList.clear();
                log.info("The source file has changed,reset the currentLineNo values to 0 ：{} -> {},",this.currentFileName,currentFileName);
            }
            this.currentFileName = currentFileName;
        }
    }

    public List<List<String>> getDataList() {
        return dataList;
    }

    public void setDataList(List<List<String>> dataList) {
        this.dataList = dataList;
    }

    public List<String> getFileNameList() {
        return fileNameList;
    }

    public void setFileNameList(List<String> fileNameList) {
        this.fileNameList = fileNameList;
    }

    public int getCurrentLineNo() {
        return currentLineNo;
    }
}
