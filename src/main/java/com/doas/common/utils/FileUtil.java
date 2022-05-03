package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
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
    public static List<String>


    getSortedFileNameList(String excelFilePath, String... acceptSuffix){
        List<String> fileNameList = new ArrayList<>();
        File file = new File(excelFilePath);
        if (file != null) {
            if (file.isDirectory()) {
                // 倒序，最新的文件在最前面
                File[] files = getSortedDescFiles(file,acceptSuffix);
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
     *
     * @param serialCommName  "COMM-DATA" - 因子采集数据   "COMM-GEO" - 坐标采集数据
     * @param pathName
     * @param lineData
     * @param fileRefreshTime 文件更新时间
     */
    public static void writeData(String serialCommName, String pathName, String lineData, Integer fileRefreshTime) {

        if (Objects.isNull(pathName) || Objects.isNull(lineData)) {
            return;
        }
        try {
            String writeFileName = "";
            // 查询已存在的文件
            List<String> fileNameList = FileUtil.getSortedFileNameList(pathName,".txt");
            if (CollectionUtils.isEmpty(fileNameList) || DateUtil.diffSeconds(fileNameList.get(0).substring(0, 14), DateUtil.defaultFormat(new Date())) > fileRefreshTime) {
                writeFileName = DateUtil.defaultFormat(new Date()) + ".txt";
            } else {
                writeFileName = fileNameList.get(0);
            }
            // 获取写入文件
            File file = new File(pathName + "/" + writeFileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            // 写入数据预处理
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(lineData);
            bw.close();
        } catch (Exception e) {
            log.error("Write lineData error!:{}", e.getStackTrace());
        }
    }
}

