package com.doas.common.thread;

import com.doas.common.config.DoasConfig;
import com.doas.common.utils.DateUtil;
import com.doas.common.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.stream.Collectors;

/**
 * txt 文件解析工具
 * 使用单独的线程把数据读取到内存中，客户端线程直接从内存中读取数据
 * 避免客户端每次请求都去解析磁盘文件，造成资源浪费。
 * @author xiattong
 */
@Slf4j
@Component
public class DataReadThread extends Thread {

    @Resource
    private DoasConfig doasConfig;


    /** 文件解析后得到的数据*/
    private List<List<String>> dataList = new ArrayList<>();
    /** 文件列表*/
    private List<String> fileNameList;
    /** 当前请求读取的文件名称*/
    private List<String> selectedFiles = new ArrayList<>();
    /** 当前需要解析的文件*/
    private List<String> parsingFiles = new ArrayList<>();
    /** 行的列数*/
    private int cellsNum = 0;
    /** 当前读取到的行号*/
    private int currentLineNo = 0;
    /** 时间段*/
    private String timeRange = "";
    /** 当权读取的最新文件*/
    private String currentNewFile = "";
    /** txt 文件分隔符正则*/
    private String regexTxt = "~| ";
    /** csv 文件分隔符正则*/
    private String regexCsv = ",";


    @Override
    public ClassLoader getContextClassLoader() {
        return super.getContextClassLoader();
    }

    /**
     * 读取txt、csv文件线程
     * @return
     */
    @Override
    public void run() {
        while(true) {
            // 更新文件名称列表
            this.fileNameList = FileUtil.getSortedFileNameList(doasConfig.getDataFilePath(),".txt",".csv");
            if (CollectionUtils.isEmpty(this.fileNameList)) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            // 文件名集合
            String filePath = doasConfig.getDataFilePath();
            if(CollectionUtils.isEmpty(this.selectedFiles)){
                // 前端未选择文件，认为读取最新文件
                parsingFiles.clear();
                parsingFiles.add(this.fileNameList.get(0));
                // 检查当最新文件发生变更时，刷新数据
                checkAndRefreshNewFile(this.fileNameList.get(0));
            }else{
                parsingFiles.clear();
                parsingFiles.addAll(this.selectedFiles);
            }
            if (Objects.isNull(parsingFiles)) {
                return;
            }
            int lineNum = 0;
            this.cellsNum = 0;
            for (String name : parsingFiles) {
                File file = new File(filePath + "/" + name);
                if(file != null) {
                    String fileName = file.getName();
                    String suffixName = fileName.substring(fileName.lastIndexOf("."));
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
                            Scanner scan = new Scanner(new ByteArrayInputStream(ds), doasConfig.getCharsetName()).useDelimiter("  ");
                            int slideIndex = this.currentLineNo; //15
                            while (scan.hasNext()) {
                                List<List<String>> tempDataList = new ArrayList<>();
                                String[] lines = scan.next().split("\r\n");
                                lineNum = lineNum + lines.length;
                                boolean markBreak = false;
                                for ( ; slideIndex < lineNum ; slideIndex++) {
                                    // slideIndex - (lineNum - lines.length)
                                    // 15 - (20 - 10) = 5
                                    // 16 - (20 - 10) = 6
                                    // 17 - (20 - 10) = 7
                                    // 18 - (20 - 10) = 8
                                    // 19 - (20 - 10) = 9
                                    String line = lines[slideIndex - lineNum + lines.length];
                                    if (this.cellsNum == 0) {
                                        List<String> data = Arrays.asList(line.split(getSplitRegex(suffixName))).stream()
                                                .filter(i -> !i.equals("单位"))
                                                .collect(Collectors.toList());
                                        tempDataList.add(data);
                                        this.cellsNum = tempDataList.get(0).size();
                                    } else {
                                        List<String> data = Arrays.asList(line.split(getSplitRegex(suffixName))).stream()
                                                .filter(i -> !i.contains("ug"))
                                                .collect(Collectors.toList());
                                        // 加入时间段判断
                                        if ("time".equals(data.get(0).toLowerCase()) || "时间".equals(data.get(0))) {
                                            continue;
                                        }
                                        if (data.size() == this.cellsNum && DateUtil.isBetweenDateTimeRange(data.get(0), this.timeRange)) {
                                            tempDataList.add(data);
                                            markBreak = true;
                                        } else if (markBreak) {
                                            break;
                                        }
                                    }
                                }
                                // 保存当前行号
                                this.currentLineNo = slideIndex;
                                this.dataList.addAll(tempDataList);
                                log.info("Read the source file " + fileName + " tempDataList：" + tempDataList.size());
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            log.error("Read source file exception! Read reset! :" + e.getMessage());
                        }
                    }
                } else {
                    log.error("No source file to read! wait and try again!");
                }
            }
            try {
                Thread.sleep(doasConfig.getRefreshHz() * 1000);
                System.gc();
            } catch (InterruptedException e) {
                e.printStackTrace();
                log.error("Source file reader thread exception! :" + e.getMessage());
            }
        }
    }

    /**
     * 检查当最新文件发生变更时，刷新数据
     * @param newFile
     */
    private void checkAndRefreshNewFile(String newFile) {
        // 当最新文件发生变更时，也要重新刷新数据
        if (StringUtils.isEmpty(currentNewFile)) {
            currentNewFile = newFile;
        } else if (!currentNewFile.equals(newFile)) {
            refresh();
            currentNewFile = newFile;
        }
    }

    /**
     * 当前读取的文件名称
     * @param selectedFiles
     */
    public void setSelectedFiles(List<String> selectedFiles){
        // 文件排序
        selectedFiles = FileUtil.getSortedAscList(selectedFiles);
        String newStrSelectedFiles = StringUtils.collectionToDelimitedString(selectedFiles, "|");
        String strSelectedFiles = StringUtils.collectionToDelimitedString(this.selectedFiles, "|");
        if (! newStrSelectedFiles.equals(strSelectedFiles)) {
            // 当读取的文件发生变化时，重新开始读取文件
            refresh();
            this.selectedFiles = selectedFiles;
            log.info("The source file has changed !, {} -> {},", strSelectedFiles, newStrSelectedFiles);
        }
    }

    public List<List<String>> getDataList() {
        return dataList;
    }

    public List<String> getFileNameList() {
        return fileNameList;
    }

    public void setTimeRange(String timeRange) {
        if (!timeRange.equals(this.timeRange)) {
            refresh();
        }
        this.timeRange = timeRange;
    }



    /**
     * 刷新缓存，重新读取文件
     */
    private void refresh() {
        this.currentLineNo = 0;
        this.dataList.clear();
    }

    /**
     * 根据文件后缀名获取分隔符
     * @param suffixName
     * @return
     */
    private String getSplitRegex(String suffixName) {
        if (suffixName.toLowerCase().equals(".csv")){
            return this.regexCsv;
        }
        return this.regexTxt;
    }
}
