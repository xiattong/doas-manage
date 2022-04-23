package com.doas.controller;

import com.alibaba.fastjson.JSON;
import com.doas.common.utils.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

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

    @Value("${red-list}")
    private String defaultRedList;

    @Value("${red-scale}")
    private String redScale;

    @Resource
    private DataReadThread dataReadThread;

    @Resource
    private DataWriteThread dataWriteThread;

    @Override
    public void afterPropertiesSet() throws Exception {
        //启动文件读取线程
        dataReadThread.start();
        Thread.sleep(3000);
        dataWriteThread.run();
    }

    /**
     * @param param
     * dataType "chart":图表数据  "map"：地图数据
     * extractNum 抽取数据的数量
     * @param
     * @return
     */
    @PostMapping("/initData")
    public ResultObject initData(@RequestBody Map<String, Object> param) {
        log.info("param:"+ JSON.toJSONString(param));

        //请求的数据模型 chart 曲线; map-line 地图（柱线）; map-wall 地图（柱面）
        String dataType = param.get("dataType").toString();
        String extractNum = param.get("extractNum").toString();
        if (StringUtils.isEmpty(extractNum)) {
            extractNum = "0";
        }
        /** 时间段*/
        String timeRange = param.get("timeRange").toString();
        dataReadThread.setTimeRange(timeRange);

        /** 当前选择的文件*/
        List<String> selectedFiles = Objects.isNull(param.get("selectedFiles")) ? null : (List)param.get("selectedFiles");
        dataReadThread.setSelectedFiles(selectedFiles);

        String redList = Objects.isNull(param.get("redList")) ? null : param.get("redList").toString();
        if(StringUtils.isEmpty(redList)){
            redList = defaultRedList;
        }
        List<List<String>> dataList = dataReadThread.getDataList();

        ResultObject result = ResultObject.basic();
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("mapType",mapType);
        resultMap.put("fileNameList",dataReadThread.getFileNameList());
        try {
            resultMap.put("companyName",new String(companyName.getBytes("iso-8859-1"), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            resultMap.put("companyName",companyName);
        }
        // 有数据时
        if (!CollectionUtils.isEmpty(dataList)) {
            try {
                if ("chart".equals(dataType)) {
                    dataParseChart(dataList, resultMap);
                } else if ("map-line".equals(dataType) || "map-wall".equals(dataType)) {
                    dataParseMap(dataList, redList, resultMap);
                }
                ResultObject.setOk(result);
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
                return ResultObject.error(e.getMessage());
            }
        }
        result.put("result", resultMap);
        return result;
    }

    /**
     * 解析excel，前端图表展示数据解析
     *
     * @param dataList
     * @return
     */
    private void dataParseChart(List<List<String>> dataList, Map<String, Object> resultMap) {
        //横坐标-时间
        List<String> xAxis = new ArrayList<>();
        //数值-曲线数据
        List<List<String>> data = new ArrayList<>();
        //实时数据
        List<String> realTimeData = new ArrayList<>();
        //平均值
        List<String> averageData = new ArrayList<>();
        //最大值
        List<String> maxData = new ArrayList<>();
        //最小值
        List<String> minData = new ArrayList<>();
        //系统状态
        String[] systemState = new String[2];
        //遍历解析数据
        for(int k = 0 ; k < dataList.size() ; k ++){
            List<String> v = dataList.get(k);
            //存储因子
            List<String> cells = v.subList(1, v.size() - 5);
            if(k == 0) {
                resultMap.put("factors", cells.toArray());
                resultMap.put("factorColors", ColorUtil.getVariantColors(cells.toArray().length));
                for (int i = 0; i < cells.size(); i++) {
                    data.add(new ArrayList<>());
                    realTimeData.add(cells.get(i) + "");
                }
            } else {
                //舍弃数值为0的数据
                double sumCellValue = 0;
                for (int i = 0; i < cells.size(); i++) {
                    sumCellValue = sumCellValue + Double.parseDouble(cells.get(i));
                }
                if(sumCellValue <= 0){
                    continue;
                }
                //存储横坐标-时间
                xAxis.add(v.get(0));
                //存储数值
                boolean lastRow = (k == dataList.size() - 1);
                for (int i = 0; i < cells.size(); i++) {
                    data.get(i).add(cells.get(i));
                    // 最后一行数据,用于在面板上展示
                    if (lastRow) {
                        realTimeData.set(i, cells.get(i));
                    }
                }
                if (lastRow) {
                    // 存储系统状态
                    systemState[0] =  v.get(v.size() - 2);
                    systemState[1] =  v.get(v.size() - 1);
                }
            }
        }
        // 求平均值、最大值、最小值
        if (!CollectionUtils.isEmpty(data)) {
            for (List<String> itemData : data ) {
                if (CollectionUtils.isEmpty(itemData)) {
                    // 如果有一项没有值，就不统计
                    break;
                }
                DoubleSummaryStatistics statistics = itemData.stream().mapToDouble(x -> Double.parseDouble(x)).summaryStatistics();
                // 平均值
                averageData.add(MathUtil.roundHalfUp(statistics.getAverage()) + "");
                // 最大值
                maxData.add(MathUtil.roundHalfUp(statistics.getMax()) + "");
                // 最小值
                minData.add(MathUtil.roundHalfUp(statistics.getMin()) + "");
            }
        }

        // 判断，当data中只有一行标题的时候，给一个默认值
        if (data.get(0).size() == 0) {
            List<String> list = new ArrayList<>();
            for (int i = 0 ; i < data.size() ; i++) {
                list.add("0");
                realTimeData.set(i, "0");
                averageData.add("0");
                maxData.add("0");
                minData.add("0");
            }
            data.add(list);
            xAxis.add("00:00:00");
        }

        resultMap.put("xAxis", xAxis.toArray());
        resultMap.put("data", data.toArray());
        resultMap.put("latestTime", xAxis.get(xAxis.size() - 1));
        resultMap.put("realTimeData", realTimeData);
        resultMap.put("averageData", averageData);
        resultMap.put("maxData", maxData);
        resultMap.put("minData", minData);
        resultMap.put("systemState",systemState);
    }

    /**
     * 解析excel，前端地图展示数据解析
     *
     * @param dataList
     * @param specifiedRedList 指定的
     * @return
     */
    private void dataParseMap(List<List<String>> dataList,String specifiedRedList, Map<String, Object> resultMap) {
        //数值-高度
        List<List<Object>> data = new ArrayList<>();
        List<List<Object>> dataHigh = new ArrayList<>();
        //颜色
        List<List<Object>> colors = new ArrayList<>();
        //坐标-地图数据
        List<double[]> coordinates = new ArrayList<>();
        //系统状态
        String[] systemState = new String[2];
        //保存各因子 red 值的列表
        List<Integer> redList = new ArrayList<>();
        //计算redList
        for (int k = 0 ; k < dataList.size() ; k ++) {
            List<String> row = dataList.get(k);
            //保存数值的数据
            List<String> cells = row.subList(1, row.size() - 5);
            //解析指定的色等值
            if (!StringUtils.isEmpty(specifiedRedList) && specifiedRedList.split(",").length == cells.size()) {
                List<String> redListStr = Arrays.asList(specifiedRedList.split(","));
                redList = redListStr.stream().map(item -> (int)Double.parseDouble(item)).collect(Collectors.toList());
                break;
            }
            //色等值未指定，需要计算
            if (k == 0) {
                //初始化
                for (int i = 0; i < cells.size(); i++) {
                    redList.add(0);
                }
            } else {
                for (int i = 0; i < cells.size(); i++) {
                    int cellValue = (int)Double.parseDouble(cells.get(i));
                    //修改 redList,使 redList 中的值保持最大
                    if (redList.get(i) < cellValue) {
                        redList.set(i, cellValue);
                    }
                }
            }
        }

        //遍历解析数据
        if(StringUtils.isEmpty(redScale)){
            redScale = "1";
        }
        redList = redList.stream().map(item -> item * (int)Double.parseDouble(redScale)).collect(Collectors.toList());
        for (int k = 0 ; k < dataList.size() ; k ++) {
            List<String> row = dataList.get(k);
            //保存数值的数据
            List<String> cells = row.subList(1, row.size() - 5);
            if (k == 0) {
                //存储因子
                resultMap.put("factors", cells.toArray());
                //初始化
                for (int i = 0; i < cells.size(); i++) {
                    data.add(new ArrayList<>());
                    dataHigh.add(new ArrayList<>());
                    colors.add(new ArrayList<>());
                }
            } else {
                //舍弃地图坐标为0的数据
                List<String> coordinate = row.subList(row.size() - 5, row.size() - 3);
                if (Double.valueOf(coordinate.get(0)) == 0
                        || Double.valueOf(coordinate.get(1)) == 0) {
                    continue;
                }
                //舍弃数值为0的数据
                double sumCellValue = 0;
                for (int i = 0; i < cells.size(); i++) {
                    sumCellValue = sumCellValue + Double.parseDouble(cells.get(i));
                }
                if(sumCellValue <= 0){
                    continue;
                }
                //存储坐标
                coordinates.add(PositionUtil.convertList(coordinate));
                //存储数值
                for (int i = 0; i < cells.size(); i++) {
                    data.get(i).add(cells.get(i));
                    double cellValue = Double.parseDouble(cells.get(i));
                    //计算颜色，并保存
                    colors.get(i).add(ColorUtil.convertVertexColors(
                            Double.parseDouble(cells.get(i)), redList.get(i)));
                    //计算线条高度，并保存,
                    DoubleSummaryStatistics statistics = redList.stream().mapToDouble((x) -> x).summaryStatistics();
                    dataHigh.get(i).add(cellValue * (statistics.getAverage() / redList.get(i)));
                }
                // 最后一行数据
                if (k == dataList.size() - 1) {
                    // 存储系统状态
                    systemState[0] = row.get(row.size() - 2);
                    systemState[1] = row.get(row.size() - 1);
                }
            }
        }
        resultMap.put("data", data.toArray());
        resultMap.put("dataHigh", dataHigh.toArray());
        resultMap.put("colors", colors.toArray());
        resultMap.put("coordinates", coordinates.toArray());
        resultMap.put("systemState",systemState);
        resultMap.put("redList",redList);
        resultMap.put("redListStr",listToString(redList,','));
    }

    // 方法四
    public static String listToString(List list, char separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

}