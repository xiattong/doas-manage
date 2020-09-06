package com.doas.controller;

import com.doas.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.text.DecimalFormat;
import java.util.*;

@Slf4j
@RestController()
public class DoasController {

    @Value("${excel.filePath}")
    private String excelFilePath;

    /**
     * @param param
     *  dataType "chart":图表数据  "map"：地图数据
     *  extractNum 抽取数据的数量
     * @param
     * @return
     */
    @PostMapping("/initData")
    public ResultObject initData(@RequestBody Map<String, String> param){
        log.info(DateUtil.format(new Date(),DateUtil.DATE_PATTERN)+ ":" +param.toString());
        String dataType = param.get("dataType");
        String extractNum = param.get("extractNum");

        if(StringUtils.isEmpty(extractNum)){
            extractNum = "0";
        }
        Map<Integer, List<Object>> dataMap;
        Map<String,Object> resultMap;
        try {
            //获取目录下最新的文件或指定文件
            File file = FileUtil.getLatestFile(excelFilePath,".xls",".xlsx");
            dataMap = ExcelUtil.readExtractContent(file,Integer.parseInt(extractNum));
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return ResultObject.error(e.getMessage());
        }
        if(dataMap.size() <= 1){
            return ResultObject.error("没有数据!");
        }
        try{
            if("chart".equals(dataType)){
                resultMap = dataParseChart(dataMap);
            }else{
                String red = param.get("red");
                if(StringUtils.isEmpty(red)){
                    red = "1000";
                }
                resultMap = dataParseMap(dataMap,Integer.parseInt(red));
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return ResultObject.error(e.getMessage());
        }
        ResultObject result = ResultObject.ok();
        result.put("result",resultMap);
        return result;
    }

    /**
     * 解析excel，前端图表展示数据解析
     * @param dataMap
     * @return
     */
    private Map<String,Object> dataParseChart(Map<Integer, List<Object>> dataMap){
        Map<String,Object> resultMap = new HashMap<>();
        //横坐标-时间
        List<String> xAxis = new ArrayList<>();
        //数值-曲线数据
        List<List<Object>> data = new ArrayList<>();
        //实时数据
        List<String> realTimeData = new ArrayList<>();
        //遍历解析数据
        dataMap.forEach((k,v)->{
            List<Object> cells = v.subList(1,v.size() - 3);
            if(k == 0){
                //存储因子
                resultMap.put("factors", cells.toArray());
                for(int i = 0 ; i< cells.size() ; i++){
                    data.add(new ArrayList<>());
                    realTimeData.add(cells.get(i)+"");
                }
            }else{
                //存储横坐标-时间
                xAxis.add(v.get(0).toString());
                //存储数值
                for(int i = 0 ; i < cells.size() ; i++){
                    data.get(i).add(cells.get(i));
                    // 最后一行数据
                    if(k == dataMap.size() - 1) {
                        realTimeData.set(i,
                                realTimeData.get(i)+" : "+cells.get(i)+"/"+v.get(v.size()-1).toString());
                    }
                }
            }
        });
        resultMap.put("xAxis", xAxis.toArray());
        resultMap.put("data", data.toArray());
        resultMap.put("latestTime",xAxis.get(xAxis.size()-1));
        resultMap.put("realTimeData",  String.join(" 、" , realTimeData));
        return resultMap;
    }

    /**
     * 解析excel，前端地图展示数据解析
     * @param dataMap
     * @return
     */
    private Map<String,Object> dataParseMap(Map<Integer, List<Object>> dataMap,int red){
        Map<String,Object> resultMap = new HashMap<>();
        //数值-高度
        List<List<Object>> data = new ArrayList<>();
        //颜色
        List<List<Object>> colors = new ArrayList<>();
        //坐标-地图数据
        List<List<Object>> coordinates = new ArrayList<>();
        //遍历解析数据
        dataMap.forEach((k,v)->{
            List<Object> cells = v.subList(1,v.size() - 3);
            if(k == 0){
                //存储因子
                resultMap.put("factors", cells.toArray());
                for(int i = 0 ; i< cells.size() ; i++){
                    data.add(new ArrayList<>());
                    colors.add(new ArrayList<>());
                }
            }else{
                //存储数值
                for(int i = 0 ; i < cells.size() ; i++){
                    data.get(i).add(cells.get(i));
                    colors.get(i).add(ColorUtil.convertVertexColors(
                            Double.parseDouble(cells.get(i).toString()),red));
                }
                //存储坐标
                coordinates.add(v.subList(v.size() - 3,v.size() - 1));
            }
        });
        resultMap.put("data", data.toArray());
        resultMap.put("colors", colors.toArray());
        resultMap.put("coordinates", coordinates.toArray());
        return resultMap;
    }
}
