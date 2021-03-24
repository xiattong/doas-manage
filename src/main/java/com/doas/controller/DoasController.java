package com.doas.controller;

import com.alibaba.fastjson.JSON;
import com.doas.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * 请求处理控制类
 * @author xiattong
 */
@Slf4j
@RestController()
public class DoasController implements InitializingBean {

    @Value("${exe.setup}")
    private String setup;

    @Value("${exe.nginx}")
    private String nginx;

    @Value("${company-name}")
    private String companyName;

    @Value("${map-type}")
    private String mapType;

    @Autowired
    private DataReadThread dataReadThread;

    @Override
    public void afterPropertiesSet() throws Exception {
        //启动文件读取线程
        dataReadThread.start();
        log.info("File reader thread started successfully!");
    }

    /**
     * @param param
     * dataType "chart":图表数据  "map"：地图数据
     * extractNum 抽取数据的数量
     * @param
     * @return
     */
    @PostMapping("/initData")
    public ResultObject initData(@RequestBody Map<String, String> param) {
        log.info("param:"+ JSON.toJSONString(param));
        String dataType = param.get("dataType");
        String extractNum = param.get("extractNum");
        String currentFileName = param.get("currentFileName");
        dataReadThread.currentFileName = currentFileName;
        if (StringUtils.isEmpty(extractNum)) {
            extractNum = "0";
        }
        List<List<Object>> dataList = dataReadThread.dataList;
        Map<String, Object> resultMap = new HashMap<>();
        if (dataList.size() <= 1) {
            return ResultObject.error("没有数据!");
        }
        try {
            if ("chart".equals(dataType)) {
                resultMap = dataParseChart(dataList);
            } else if("map-line".equals(dataType) || "map-wall".equals(dataType)) {
                resultMap = dataParseMap(dataList);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return ResultObject.error(e.getMessage());
        }
        ResultObject result = ResultObject.ok();
        try {
            resultMap.put("companyName",new String(companyName.getBytes
                    ("iso-8859-1"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            resultMap.put("companyName",companyName);
        }
        resultMap.put("mapType",mapType);
        resultMap.put("fileNameList",dataReadThread.fileNameList);
        result.put("result", resultMap);
        return result;
    }

    /**
     * 解析excel，前端图表展示数据解析
     *
     * @param dataList
     * @return
     */
    private Map<String, Object> dataParseChart(List<List<Object>> dataList) {
        Map<String, Object> resultMap = new HashMap<>();
        //横坐标-时间
        List<String> xAxis = new ArrayList<>();
        //数值-曲线数据
        List<List<Object>> data = new ArrayList<>();
        //实时数据
        List<String> realTimeData = new ArrayList<>();
        //系统状态
        String[] systemState = new String[2];
        //遍历解析数据
        for(int k = 0 ; k < dataList.size() ; k ++){
            List<Object> v = dataList.get(k);
            if(k == 0) {
                //存储因子
                List<Object> cells = v.subList(1, v.size() - 5);
                resultMap.put("factors", cells.toArray());
                for (int i = 0; i < cells.size(); i++) {
                    data.add(new ArrayList<>());
                    realTimeData.add(cells.get(i) + "");
                }
            } else {
                //存储横坐标-时间
                xAxis.add(v.get(0).toString());
                //存储数值
                boolean lastRow = (k == dataList.size() - 1);
                List<Object> cells = v.subList(1, v.size() - 5);
                for (int i = 0; i < cells.size(); i++) {
                    data.get(i).add(cells.get(i));
                    // 最后一行数据,用于在面板上展示
                    if (lastRow) {
                        realTimeData.set(i, realTimeData.get(i) + " : "
                                + cells.get(i) + " " + v.get(v.size() - 3).toString());
                    }
                }
                if (lastRow) {
                    // 存储系统状态
                    systemState[0] =  v.get(v.size() - 2).toString();
                    systemState[1] =  v.get(v.size() - 1).toString();
                }
            }
        }
        resultMap.put("xAxis", xAxis.toArray());
        resultMap.put("data", data.toArray());
        resultMap.put("latestTime", xAxis.get(xAxis.size() - 1));
        resultMap.put("realTimeData", String.join(" 、", realTimeData));
        resultMap.put("systemState",systemState);
        return resultMap;
    }

    /**
     * 解析excel，前端地图展示数据解析
     *
     * @param dataList
     * @return
     */
    private Map<String, Object> dataParseMap(List<List<Object>> dataList) {
        Map<String, Object> resultMap = new HashMap<>();
        //数值-高度
        List<List<Object>> data = new ArrayList<>();
        //颜色
        List<List<Object>> colors = new ArrayList<>();
        //坐标-地图数据
        List<double[]> coordinates = new ArrayList<>();
        //系统状态
        String[] systemState = new String[2];
        // 保存各因子 red 值的列表
        List<Integer> redList = new ArrayList<>();
        //遍历解析数据
        for(int k = 0 ; k < dataList.size() ; k ++){
            List<Object> row = dataList.get(k);
            //保存数值的数据
            List<Object> cells = row.subList(1, row.size() - 5);
            if(k == 0) {
                //存储因子
                resultMap.put("factors", cells.toArray());
                for (int i = 0; i < cells.size(); i++) {
                    data.add(new ArrayList<>());
                    colors.add(new ArrayList<>());
                }
            } else {
                //存储坐标，地图舍弃坐标为 0 的数据
                List<Object> coordinate = row.subList(row.size() - 5, row.size() - 3);
                if (Double.valueOf(coordinate.get(0).toString()) == 0
                        || Double.valueOf(coordinate.get(1).toString()) == 0) {
                    continue;
                }
                //舍弃数值为0的数据
                if(CollectionUtils.isEmpty(redList)) {
                    int sumCellValue = 0;
                    for (int i = 0; i < cells.size(); i++) {
                        sumCellValue = sumCellValue + Integer.parseInt(cells.get(i).toString());
                    }
                    if(sumCellValue <= 0){
                        continue;
                    }
                    // 计算 redList
                    redList = redListComputer(cells);
                }

                coordinates.add(PositionUtil.convertList(coordinate));
                //存储数值
                for (int i = 0; i < cells.size(); i++) {
                    data.get(i).add(cells.get(i));
                    colors.get(i).add(ColorUtil.convertVertexColors(
                            Double.parseDouble(cells.get(i).toString()), redList.get(i)));
                }
                // 最后一行数据
                if (k == dataList.size() - 1) {
                    // 存储系统状态
                    systemState[0] = row.get(row.size() - 2).toString();
                    systemState[1] = row.get(row.size() - 1).toString();
                }
            }
        }
        resultMap.put("data", data.toArray());
        resultMap.put("colors", colors.toArray());
        resultMap.put("coordinates", coordinates.toArray());
        resultMap.put("systemState",systemState);
        resultMap.put("redList",redList.toArray());
        return resultMap;
    }

    /**
     * 根据 cells 的值，计算出每个因子合适的red
     * @param cells
     * @return
     */
    private List<Integer> redListComputer(List<Object> cells) {
        List<Integer> redList = new ArrayList<>();
        double scale = 0.75;
        cells.forEach(item -> {
            int red = (int)Math.round(Double.parseDouble(item.toString()) * scale);
            if(red <= 0){
                red = 1;
            }
            redList.add(red);
        });
        return redList;
    }
}