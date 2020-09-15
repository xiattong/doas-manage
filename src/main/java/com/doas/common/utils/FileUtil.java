package com.doas.common.utils;

import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 文件读取工具类
 *  @author xiattong
 */
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
                File[] files = file.listFiles(new FileUtil(acceptSuffix));
                if (files.length > 0) {
                    Arrays.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File file1, File file2) {
                            return (int) (file2.lastModified() - file1.lastModified());
                        }
                    });
                    return files[0];
                } else {
                    throw new RuntimeException("未找到文件！");
                }
            } else {
                return file;
            }
        } else {
            throw new RuntimeException("未找到文件目录！");
        }
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
