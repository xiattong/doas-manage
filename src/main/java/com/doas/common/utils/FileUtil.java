package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.*;
import java.util.*;

/**
 * 文件读写工具类
 *  @author xiattong
 */
@Slf4j
public class FileUtil implements FilenameFilter {

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
     * 这个方法需要考虑线程安全
     * @param pathName
     * @param lineData
     * @param fileRefreshTime 文件更新时间
     */
    public static void writeData(String pathName, String lineData, Integer fileRefreshTime) {

        if (Objects.isNull(pathName) || Objects.isNull(lineData)) {
            return;
        }
        try {
            String writeFileName = "";
            // 查询已存在的文件
            List<String> fileNameList = FileUtil.getSortedFileNameList(pathName, ".txt");
            if (CollectionUtils.isEmpty(fileNameList) || DateUtil.diffMinutes(fileNameList.get(0).substring(0, 14), DateUtil.defaultFormat(new Date())) > fileRefreshTime) {
                writeFileName = DateUtil.defaultFormat(new Date()) + ".txt";
            } else {
                writeFileName = fileNameList.get(0);
            }

            // 获取写入文件
            File file = new File(pathName + "/" + writeFileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            // 读取文件最后一行
            int lineNum = readLastLine(file);
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
     * 解析文件内容
     * @param lineNum
     * @param lineData
     * @return
     */
    private static String parseLineData(int lineNum, String lineData) {
        try {
            String[] lineDateArray = lineData.split(";");
            int size = lineDateArray.length;
            // 预热中的数据不要
            if (size < 10 || "0".equals(lineDateArray[size - 5])) {
                return null;
            }
            StringBuilder parsedLineData = new StringBuilder();
            if (lineNum == 0) {
                // 解析成文件头 (例如：时间~SO2~NO~NO2~NH3~O3~HCHO~苯~甲苯~二甲苯~乙苯~GPS.x~GPS.y~单位~系统状态~GPS状态)
                parsedLineData.append("时间~");
                for (int index = 2; index < size - 7; index++) {
                    parsedLineData.append(lineDateArray[index].substring(0, lineDateArray[index].indexOf("(")) + "~");
                }
                parsedLineData.append("GPS.x~GPS.y~单位~系统状态~GPS状态~光源光强~光源已使用时间");
            } else {
                // 解析成数据 (例如：15:33:51~10~4~64~19~32~1~234~58~34~642~114.363922~36.381824~ug/m3~1~1)
                parsedLineData.append(DateUtil.formatTime(new Date()) + "~");
                for (int index = 2; index < size - 7; index++) {
                    parsedLineData.append(lineDateArray[index].substring(lineDateArray[index].indexOf("@") + 1) + "~");
                }
                // GPS.x
                parsedLineData.append(lineDateArray[size - 7] + "~");
                // GPS.y
                parsedLineData.append(lineDateArray[size - 6] + "~");
                // 单位
                String temp = lineDateArray[2];
                parsedLineData.append(temp.substring(temp.indexOf("(") + 1, temp.indexOf(")")) + "~");
                // 系统状态
                parsedLineData.append(lineDateArray[size - 5] + "~");
                // GPS状态
                parsedLineData.append(lineDateArray[size - 4] + "~");
                // 光源光强
                parsedLineData.append(lineDateArray[size - 3] + "~");
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
    public static int readLastLine(File file) {
        try (LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(file))){
            lineNumberReader.skip(Long.MAX_VALUE);
            int lineNumber = lineNumberReader.getLineNumber();
            return lineNumber;//实际上是读取换行符数量 , 所以需要+1
        } catch (IOException e) {
            return -1;
        }

    }
}

