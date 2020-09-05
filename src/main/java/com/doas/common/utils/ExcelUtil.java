package com.doas.common.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

public class ExcelUtil {

    /**
     *
     * @param file
     * @param extractNum  抽取后的最大值，0表示不抽取
     * @return
     * @throws Exception
     */
    public static Map<Integer, List<Object>> readExtractContent(File file,int extractNum)
            throws Exception {
        Map<Integer, List<Object>> content = new LinkedHashMap<>(50);
        // 上传文件名
        Workbook wb = getWb(file);
        if (wb == null) {
            throw new Exception("Workbook对象为空！");
        }
        Sheet sheet = wb.getSheetAt(0);
        // 得到总行数
        int rowNum = sheet.getLastRowNum();
        if(rowNum <= 0){
            throw new Exception("数据文件中没有可用数据！");
        }
        //计算间隔数
        int interval = extractNum == 0 ? 0 : rowNum / extractNum;
        Row row = sheet.getRow(0);
        int colNum = row.getPhysicalNumberOfCells();
        for (int i = 0; i <= rowNum; i++) {
            row = sheet.getRow(i);
            int j = 0;
            List<Object> cellValue = new ArrayList<>();
            while (j < colNum) {
                Object obj = getCellFormatValue(row.getCell(j));
                cellValue.add(obj);
                j++;
            }
            content.put(i, cellValue);
            i = i + interval;
            //保证能取到最后一条
            if(i - interval < rowNum && i >= rowNum){
                i = rowNum - 1;
            }
        }
        return content;
    }

    /**
     * 根据Cell类型设置数据
     */
    private static Object getCellFormatValue(Cell cell) {
        Object cellValue = "";
        if (cell != null) {
            switch (cell.getCellType()) {
                case NUMERIC:
                case FORMULA: {
                    if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        cellValue = DateUtil.formatTime(date);
                    } else {
                        //DecimalFormat df = new DecimalFormat("0.00");
                        //cellValue = df.format(cell.getNumericCellValue());
                        cellValue = cell.getNumericCellValue();
                    }
                    break;
                }
                case STRING:
                    cellValue = cell.getRichStringCellValue()
                            .getString().trim().replaceAll("[ \n]+","-");
                    break;
                default:
                    cellValue = "";
            }
        } else {
            cellValue = "";
        }
        return cellValue;
    }

    private static Workbook getWb(File file) {
        String filepath = file.getName();
        assert filepath != null;
        String ext = filepath.substring(filepath.lastIndexOf("."));
        Workbook wb = null;
        try {
            FileInputStream is = new FileInputStream(file);
            if (".xls".equals(ext)) {
                wb = new HSSFWorkbook(is);
            } else if (".xlsx".equals(ext)) {
                wb = new XSSFWorkbook(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return wb;
    }

}
