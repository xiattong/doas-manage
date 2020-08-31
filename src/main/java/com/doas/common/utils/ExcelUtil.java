package com.doas.common.utils;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.*;

public class ExcelUtil {
    public static Map<Integer, List<Object>> readExcelContent(File file) throws Exception {
        Map<Integer, List<Object>> content = new LinkedHashMap<>(50);
        // 上传文件名
        Workbook wb = getWb(file);
        if (wb == null) {
            throw new Exception("Workbook对象为空！");
        }
        Sheet sheet = wb.getSheetAt(0);
        // 得到总行数
        int rowNum = sheet.getLastRowNum();
        Row row = sheet.getRow(0);
        int colNum = row.getPhysicalNumberOfCells();
        // 正文内容应该从第二行开始,第一行为表头的标题
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
                    if (DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        cellValue = DateUtils.formatTime(date);
                    } else {
                        DecimalFormat df = new DecimalFormat("0.00");
                        cellValue = df.format(cell.getNumericCellValue());
                        //int idx = value.indexOf(".00");
                        //value = idx > 0 ? value.substring(0,idx) : value ;
                        //cellValue = value;
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


    public static void main(String[] args) {
        String a = "安徽\n省 蚌埠市 方阵房地产开发有限公司中央城\n" +
                "小区 10KV变配电工程HXGN-15高压开关柜技术\n";
        System.out.println(a.trim().replaceAll("[ \n]+","-"));
    }
}
