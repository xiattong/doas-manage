package com.doas.controller;

import com.doas.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 请求处理控制类
 * @author xiattong
 */
@Slf4j
@RestController()
public class DoasController {

    /**
     * 数据读取线程
     * @param dataReadThread
     */
    @Autowired
    public DoasController(DataReadThread dataReadThread){
        dataReadThread.start();
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
        log.info(DateUtil.format(new Date(), DateUtil.DATE_PATTERN) + ":" + param.toString());
        String dataType = param.get("dataType");
        String extractNum = param.get("extractNum");

        if (StringUtils.isEmpty(extractNum)) {
            extractNum = "0";
        }
        List<List<Object>> dataList = DataReadThread.dataList;;
        Map<String, Object> resultMap;
        if (dataList.size() <= 1) {
            return ResultObject.error("没有数据!");
        }
        try {
            if ("chart".equals(dataType)) {
                resultMap = dataParseChart(dataList);
            } else {
                String red = param.get("red");
                if (StringUtils.isEmpty(red)) {
                    red = "1000";
                }
                resultMap = dataParseMap(dataList, Integer.parseInt(red));
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return ResultObject.error(e.getMessage());
        }
        ResultObject result = ResultObject.ok();
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
        //判断数据的最后一行是否完整，不完整便舍弃
        boolean isFull = dataList.get(0).size() == dataList.get(dataList.size() - 1).size();
        if(!isFull){
            dataList.remove(dataList.size() - 1);
        }
        //遍历解析数据
        for(int k = 0 ; k < dataList.size() ; k ++){
            List<Object> v = dataList.get(k);
            if (k == 0) {
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
    private Map<String, Object> dataParseMap(List<List<Object>> dataList, int red) {
        Map<String, Object> resultMap = new HashMap<>();
        //数值-高度
        List<List<Object>> data = new ArrayList<>();
        //颜色
        List<List<Object>> colors = new ArrayList<>();
        //坐标-地图数据
        List<List<Object>> coordinates = new ArrayList<>();
        //系统状态
        String[] systemState = new String[2];
        //判断数据的最后一行是否完整，不完整便舍弃
        boolean isFull = dataList.get(0).size() == dataList.get(dataList.size() - 1).size();
        if(!isFull){
            dataList.remove(dataList.size() - 1);
        }
        //遍历解析数据
        for(int k = 0 ; k < dataList.size() ; k ++){
            List<Object> v = dataList.get(k);
            if (k == 0) {
                //存储因子
                List<Object> cells = v.subList(1, v.size() - 5);
                resultMap.put("factors", cells.toArray());
                for (int i = 0; i < cells.size(); i++) {
                    data.add(new ArrayList<>());
                    colors.add(new ArrayList<>());
                }
            } else {
                //存储数值
                List<Object> cells = v.subList(1, v.size() - 5);
                for (int i = 0; i < cells.size(); i++) {
                    data.get(i).add(cells.get(i));
                    colors.get(i).add(ColorUtil.convertVertexColors(
                            Double.parseDouble(cells.get(i).toString()), red));
                }
                //存储坐标
                coordinates.add(v.subList(v.size() - 5, v.size() - 3));
                // 最后一行数据
                if (k == dataList.size() - 1) {
                    // 存储系统状态
                    systemState[0] =  v.get(v.size() - 2).toString();
                    systemState[1] =  v.get(v.size() - 1).toString();
                }
            }
        }
        resultMap.put("data", data.toArray());
        resultMap.put("colors", colors.toArray());
        resultMap.put("coordinates", coordinates.toArray());
        resultMap.put("systemState",systemState);
        return resultMap;
    }
}