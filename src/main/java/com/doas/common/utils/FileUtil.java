package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.text.ParseException;
import java.util.*;

/**
 * 文件读写工具类
 *  @author xiattong
 */
@Slf4j
public class FileUtil implements FilenameFilter {

    private static final String SPLIT_SYMBOL = " ";

    // 接受的文件类型，小写
    private String[] acceptSuffix;

    private FileUtil(String[] acceptSuffix){
        this.acceptSuffix = acceptSuffix;
    }

    /***
     * 指定查询哪些文件
     * @param dir
     * @param name
     * @return
     */
    @Override
    public boolean accept(File dir, String name) {
        if(this.acceptSuffix == null || this.acceptSuffix.length <= 0) {
            return true;
        }
        for(String suffix : acceptSuffix){
            if (name.toLowerCase().endsWith(suffix)){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取需要读取的文件名列表
     * @param excelFilePath
     * @param acceptSuffix
     * @return
     */
    public static List<String> getSortedFileNameList(String excelFilePath, String... acceptSuffix){
        List<String> fileNameList = new ArrayList<>();
        File file = new File(excelFilePath);
        if (file != null) {
            if (file.isDirectory()) {
                // 倒序，最新的文件在最前面
                File[] files = getSortedDescFiles(file,acceptSuffix);
                if (files == null || files.length == 0) {
                    return null;
                }
                for(File f : files){
                    fileNameList.add(f.getName());
                }
            }
        }
        // 最多只取20个文件
        if (fileNameList.size() > 20) {
            fileNameList = fileNameList.subList(0,20);
        }
        return fileNameList;
    }

    /**
     * 按照文件名倒序排列，最新的文件在最前面
     * @param directory
     * @param acceptSuffix
     * @return
     */
    private static File[] getSortedDescFiles(File directory,String... acceptSuffix){
        File[] files = directory.listFiles(new FileUtil(acceptSuffix));
        if (files.length == 1) {
            return files;
        }
        if (files.length > 0) {
            if (files.length == 1) {
                return files;
            }
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file1, File file2) {
                    try {
                        return doCompareFileNameDesc(file1.getName(), file2.getName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        log.error("Error ! FileUtil#getSortedFiles: {}", e.getMessage());
                        return 1;
                    }
                }
            });
            return files;
        }
        return null;
    }

    /**
     * 正序排列
     * @param fileNames
     * @return
     */
    public static List<String> getSortedAscList(List<String> fileNames){
        if (CollectionUtils.isEmpty(fileNames)) {
            return new ArrayList<>();
        }
        if (fileNames.size() == 1) {
            return fileNames;
        }
        Collections.sort(fileNames, new Comparator<String>() {
            @Override
            public int compare(String file1, String file2) {
                try {
                    return doCompareFileNameAsc(file1, file2);
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Error ! FileUtil#getSortedFiles: {}", e.getMessage());
                    return 1;
                }
            }
        });
        return fileNames;
    }

    /**
     * 升序排列文件
     * @param file1Name
     * @param file2Name
     * @return
     */
    private static int doCompareFileNameAsc(String file1Name, String file2Name) {
        if (file1Name.lastIndexOf(".") > 0) {
            file1Name = file1Name.substring(0, file1Name.lastIndexOf("."));
        }
        if (file2Name.lastIndexOf(".") > 0) {
            file2Name = file2Name.substring(0, file2Name.lastIndexOf("."));
        }
        if (StringUtils.isEmpty(file2Name) || StringUtils.isEmpty(file1Name)) {
            return 1;
        }
        long cp = (Long.parseLong(file1Name) - Long.parseLong(file2Name));
        if (cp > 0) {
            return 1;
        } else if (cp == 0) {
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * 降序序排列文件
     * @param file1Name
     * @param file2Name
     * @return
     */
    private static int doCompareFileNameDesc(String file1Name, String file2Name) {
        return doCompareFileNameAsc(file1Name, file2Name) * -1;
    }


    /**
     * 写入一条中断数据
     * @param pathName
     * @param fileRefreshTime
     */
    public static void writeBreakData(String pathName, Integer fileRefreshTime) {
        // 获取文件
        File file = getFile(pathName, fileRefreshTime);
        if (file == null) {
            return ;
        }
        // 读取文件的最后一行
        String lastLine = FileUtil.readLastLine(file);
        if (StringUtils.isEmpty(lastLine)) {
            return;
        }
        // 修改最后一行内容
        String[] lastLineArray = lastLine.split(SPLIT_SYMBOL);
        if (lastLineArray.length < 4) {
            return;
        }
        if ("0".equals(lastLineArray[lastLineArray.length - 3]) && "0".equals(lastLineArray[lastLineArray.length - 4])) {
            return;
        }
        lastLineArray[lastLineArray.length - 4] = "0";
        lastLineArray[lastLineArray.length - 3] = "0";
        lastLine = String.join(SPLIT_SYMBOL, lastLineArray);
        // 写入数据
        log.info("写入中断数据:{}", lastLine);
        writeDataDirect(file, lastLine, 3);
    }


    /**
     * 这个方法需要考虑线程安全
     * @param pathName
     * @param fileRefreshTime 文件更新时间
     * @param lineData
     */
    public static void writeData(String pathName, Integer fileRefreshTime, String lineData) {
        if (StringUtils.isEmpty(lineData)) {
            return;
        }
        // 获取文件
        File file = getFile(pathName, fileRefreshTime);
        if (file == null) {
            return ;
        }
        // 写入数据
        writeData(file, lineData);
    }

    /**
     * 这个方法需要考虑线程安全
     * @param pathName
     * @param isCreateFile 是否创建文件
     * @param lineData
     */
    public static void writeData(String pathName, boolean isCreateFile, String lineData) {
        if (StringUtils.isEmpty(lineData)) {
            return;
        }
        // 获取文件
        File file = getFile(pathName, isCreateFile);
        if (file == null) {
            return ;
        }
        // 写入数据
        writeData(file, lineData);
    }

    /**
     * 按规则获取文件
     * @param pathName
     * @param fileRefreshTime
     * @return
     */
    public static File getFile(String pathName, Integer fileRefreshTime) {
        if (Objects.isNull(pathName)) {
            return null;
        }
        String writeFileName = "";
        // 查询已存在的文件
        List<String> fileNameList = FileUtil.getSortedFileNameList(pathName, ".txt");
        try {
            if (CollectionUtils.isEmpty(fileNameList) || DateUtil.diffMinutes(fileNameList.get(0).substring(0, 14), DateUtil.defaultFormat(new Date())) > fileRefreshTime) {
                writeFileName = DateUtil.defaultFormat(new Date()) + ".txt";
            } else {
                writeFileName = fileNameList.get(0);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // 获取写入文件
        File file = new File(pathName + "/" + writeFileName);
        return file;
    }

    /**
     * 按规则获取文件
     * @param pathName
     * @param isCreateFile 是否创建新的文件
     * @return
     */
    public static File getFile(String pathName, boolean isCreateFile) {
        if (Objects.isNull(pathName)) {
            return null;
        }
        String writeFileName = "";
        // 查询已存在的文件
        List<String> fileNameList = FileUtil.getSortedFileNameList(pathName, ".txt");
        if (isCreateFile) {
            writeFileName = DateUtil.defaultFormat(new Date()) + ".txt";
        } else {
            writeFileName = fileNameList.get(0);
        }
        // 获取写入文件
        File file = new File(pathName + "/" + writeFileName);
        return file;
    }

    /**
     * 这个方法需要考虑线程安全
     * @param file
     * @param lineData
     */
    public static void writeData(File file, String lineData) {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            // 读取文件最后一行行号
            int lineNum = readLastLineNum(file);
            // 解析文件内容
            String parsedLineData = parseLineData(lineNum, lineData);
            if (StringUtils.isEmpty(parsedLineData)) {
                return;
            }
            // 把内容追加到文件最后
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(parsedLineData);
            bw.newLine();
            bw.close();
        } catch (Exception e) {
            log.error("Write lineData error!:{}", e.getStackTrace());
        }
    }

    /**
     * 这个方法需要考虑线程安全
     * @param file
     * @param lineData
     * @param writeLineNum 写入行数
     */
    public static void writeDataDirect(File file, String lineData, int writeLineNum) {
        if (writeLineNum <= 0) {
            return;
        }
        try {
            if (StringUtils.isEmpty(lineData) || !file.exists()) {
                file.createNewFile();
            }
            // 把内容追加到文件最后
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            BufferedWriter bw = new BufferedWriter(fw);
            for (int i = 0; i < writeLineNum; i ++) {
                bw.write(lineData);
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            log.error("Write lineData error!:{}", e.getStackTrace());
        }
    }

    /**
     * 解析文件内容
     * @param lineNum
     * @param lineData
     * @return
     */
    private static String parseLineData(int lineNum, String lineData) {
        try {
            String[] lineDateArray = lineData.split(";");
            int size = lineDateArray.length;
            // 预热中的数据也要
            if (size < 10) {
                return null;
            }
            StringBuilder parsedLineData = new StringBuilder();
            if (lineNum == 0) {
                // 解析成文件头 (例如：时间 SO2 NO NO2 NH3 O3 HCHO 苯 甲苯 二甲苯 乙苯 GPS.x GPS.y 单位 系统状态 GPS状态)
                parsedLineData.append("时间"+SPLIT_SYMBOL);
                for (int index = 2; index < size - 7; index++) {
                    // 因子带上单位
                    parsedLineData.append(lineDateArray[index].substring(0, lineDateArray[index].indexOf(")") + 1) + SPLIT_SYMBOL);
                }
                parsedLineData.append("GPS.x" + SPLIT_SYMBOL + "GPS.y" + SPLIT_SYMBOL + "单位" + SPLIT_SYMBOL + "系统状态" + SPLIT_SYMBOL + "GPS状态" + SPLIT_SYMBOL + "光源光强" + SPLIT_SYMBOL + "光源已使用时间");
            } else {
                // 解析成数据 (例如：15:33:51 10 4 64 19 32 1 234 58 34 642 114.363922 36.381824 ug/m3 1 1)
                parsedLineData.append(DateUtil.formatTime(new Date()) + SPLIT_SYMBOL);
                for (int index = 2; index < size - 7; index++) {
                    parsedLineData.append(lineDateArray[index].substring(lineDateArray[index].indexOf("@") + 1) + SPLIT_SYMBOL);
                }
                // GPS.x
                parsedLineData.append(lineDateArray[size - 7] + SPLIT_SYMBOL);
                // GPS.y
                parsedLineData.append(lineDateArray[size - 6] + SPLIT_SYMBOL);
                // 单位
                String temp = lineDateArray[2];
                parsedLineData.append(temp.substring(temp.indexOf("(") + 1, temp.indexOf(")")) + SPLIT_SYMBOL);
                // 系统状态
                parsedLineData.append(lineDateArray[size - 5] + SPLIT_SYMBOL);
                // GPS状态
                parsedLineData.append(lineDateArray[size - 4] + SPLIT_SYMBOL);
                // 光源光强
                parsedLineData.append(lineDateArray[size - 3] + SPLIT_SYMBOL);
                // 光源已使用时间
                parsedLineData.append(lineDateArray[size - 2]);
            }
            return parsedLineData.toString();
        } catch (Exception e) {
            log.error("Write lineData error!:{}", e.getStackTrace());
        }
        return null;
    }

    /**
     * 读取文件的行数
     * @param file
     * @return
     */
    public static int readLastLineNum(File file) {
        try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file))){
            lineNumberReader.skip(Long.MAX_VALUE);
            int lineNumber = lineNumberReader.getLineNumber();
            return lineNumber;//实际上是读取换行符数量 , 所以需要+1
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * 读取文件最后一行内容
     * @param file
     * @return
     */
    public static String readLastLine(File file) {
        // 如果文件只有一行，不读
        if (file == null || readLastLineNum(file) <= 1) {
            return "";
        }
        // 存储结果
        StringBuilder builder = new StringBuilder();
        try (RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
            // 指针位置开始为0，所以最大长度为 length-1
            long fileLastPointer = randomAccessFile.length() - 1;
            // 从后向前读取文件
            for (long filePointer = fileLastPointer; filePointer != -1; filePointer--) {
                // 移动指针指向
                randomAccessFile.seek(filePointer);
                int readByte = randomAccessFile.readByte();
                if (0xA == readByte) {
                    //  LF='\n'=0x0A 换行
                    if (filePointer == fileLastPointer) {
                        // 如果是最后的换行，过滤掉
                        continue;
                    }
                    break;
                }
                if (0xD == readByte) {
                    //  CR ='\r'=0x0D 回车
                    if (filePointer == fileLastPointer - 1) {
                        // 如果是倒数的回车也过滤掉
                        continue;
                    }
                    break;
                }
                builder.append((char) readByte);
            }
        } catch (Exception e) {
            log.error("file read error, msg:{}", e.getMessage(), e);
        }
        return builder.reverse().toString();
    }
}

