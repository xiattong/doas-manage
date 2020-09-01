package com.doas.common.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FileUtil {
    /**
     * 获取目录下最新的文件或指定文件
     * @param filePath
     *     filePath为目录时，返回目录下最新的文件
     *     filePath为文件名时，返回该文件
     * @return File
     * TODO 当目录下的文件太多后，可能会影响文件读取速度，
     *  应当需要使用一个线程来控制目录下文件的数量？？？？
     */
    public static File getLatestFile(String filePath){
        File file =  new File(filePath);
        if(file != null) {
            if (file.isDirectory()){
                File[] files = file.listFiles();
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
            }else{
                return file;
            }
        }else{
            throw new RuntimeException("未找到文件目录！");
        }
    }
}
