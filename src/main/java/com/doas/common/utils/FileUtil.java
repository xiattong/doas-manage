package com.doas.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;

/**
 * 文件读取工具类
 *  @author xiattong
 */
@Slf4j
public class FileUtil implements FilenameFilter {

    // 接受的文件类型，小写
    private String[] acceptSuffix;

    private FileUtil(String[] acceptSuffix){
        this.acceptSuffix = acceptSuffix;
    }

    /**
     * 获取目录下最新的文件或指定文件
     * @param filePath
     *     filePath为目录时，返回目录下最新的文件
     *     filePath为文件名时，返回该文件
     *  @param acceptSuffix
     *      接受的文件类型后缀，使用小写
     * @return File
     */
    public static File getLatestFile(String filePath,String... acceptSuffix) {
        File file = new File(filePath);
        if (file != null) {
            if (file.isDirectory()) {
               File[] files = getSortedDescFiles(file,acceptSuffix);
               if(files != null)
                   return files[0];
            } else {
                // 直接读取文件
                return file;
            }
        }
        return null;
    }

    public static List<String> getSortedFileNameList(String excelFilePath, String... acceptSuffix){
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
                        String file2Name = null;
                        String file1Name = null;
                        if (file2.getName().lastIndexOf(".") > 0) {
                            file2Name = file2.getName().substring(0, file2.getName().lastIndexOf("."));
                        } else {
                            file2Name = file2.getName();
                        }
                        if (file1.getName().lastIndexOf(".") > 0) {
                            file1Name = file1.getName().substring(0, file1.getName().lastIndexOf("."));
                        } else {
                            file1Name = file1.getName();
                        }
                        if (StringUtils.isEmpty(file2Name) || StringUtils.isEmpty(file1Name)) {
                            return 1;
                        }
                        return (int) (Long.parseLong(file2Name) - Long.parseLong(file1Name));
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
                    String file2Name = null;
                    String file1Name = null;
                    if (file2.lastIndexOf(".") > 0) {
                        file2Name = file2.substring(0, file2.lastIndexOf("."));
                    } else {
                        file2Name = file2;
                    }
                    if (file1.lastIndexOf(".") > 0) {
                        file1Name = file1.substring(0, file1.lastIndexOf("."));
                    } else {
                        file1Name = file1;
                    }
                    if (StringUtils.isEmpty(file2Name) || StringUtils.isEmpty(file1Name)) {
                        return 1;
                    }
                    return (int) (Long.parseLong(file1Name) - Long.parseLong(file2Name));
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Error ! FileUtil#getSortedFiles: {}", e.getMessage());
                    return 1;
                }
            }
        });
        return fileNames;
    }


    /***
     * 指定查询哪些文件
     * @param dir
     * @param name
     * @return
     */
    @Override
    public boolean accept(File dir, String name) {
        return isAccept(name);
    }

    public boolean isAccept(String filename){
        if(this.acceptSuffix == null || this.acceptSuffix.length <= 0) {
            return true;
        }
        for(String suffix : acceptSuffix){
            if (filename.toLowerCase().endsWith(suffix)){
                return true;
            }
        }
        return false;
    }
}
