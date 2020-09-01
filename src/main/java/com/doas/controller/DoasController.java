package com.doas.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.doas.common.utils.ExcelUtil;
import com.doas.common.utils.FileUtil;
import com.doas.common.utils.ResultObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@RestController()
public class DoasController {

    @Value("${excel.path}")
    private String excelPath;

    @Value("${excel.file}")
    private String excelFile;

    @PostMapping("/initData")
    public ResultObject initData(){
        Map<Integer, List<Object>> dataMap;
        Map<String,Object> resultMap;
        try {
            //获取目录下最新的文件或指定文件
            File file = FileUtil.getLatestFile(excelPath);
            dataMap = ExcelUtil.readExcelContent(file);
        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return ResultObject.error(e.getMessage());
        }
        if(dataMap.size() <= 1){
            return ResultObject.error("没有数据!");
        }
        try{
            //解析数据
            resultMap = dataParse(dataMap);
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
     * 解析excel数据，转化成前端可展示的格式
     * @param dataMap
     * @return
     */
    private Map<String,Object> dataParse(Map<Integer, List<Object>> dataMap){
        Map<String,Object> resultMap = new HashMap<>();
        //横坐标-时间
        List<String> xAxis = new ArrayList<>();
        //数值-曲线数据
        List<List<Object>> data = new ArrayList<>();
        //坐标-地图数据
        List<List<Object>> coordinates = new ArrayList<>();
        //单位
        List<String> units = new ArrayList<>();
        // 表格数据
        List<Map<String,Object>> tableDate = new ArrayList<>();
        //遍历解析数据
        dataMap.forEach((k,v)->{
            List<Object> values = v;
            if(k == 0){
                //存储因子
                List<Object> factors = values.subList(1,values.size() - 3);
                resultMap.put("factors", factors.toArray());
                for(int i = 0 ; i< factors.size() ; i++){
                    data.add(new ArrayList<>());
                    Map<String,Object> table = new HashMap<>();
                    table.put("采集因子",factors.get(i));
                    tableDate.add(table);
                }
            }else{
                //存储横坐标-时间
                xAxis.add(values.get(0).toString());
                //单位
                units.add(values.get(values.size()-1).toString());
                //存储数值
                List<Object> da = values.subList(1,values.size() - 3);
                for(int i = 0 ; i < da.size() ; i++){
                    data.get(i).add(da.get(i));
                    if(k == dataMap.size() - 1) {
                        tableDate.get(i).put("采集值", da.get(i));
                        tableDate.get(i).put("单位", values.get(values.size()-1).toString());
                        tableDate.get(i).put("TIME", values.get(0).toString());
                    }
                }
                //存储坐标
                coordinates.add(values.subList(values.size() - 3,values.size() - 1));

            }
        });
        resultMap.put("xAxis", xAxis.toArray());
        resultMap.put("data", data.toArray());
        resultMap.put("coordinates", coordinates.toArray());
        resultMap.put("units", units.toArray());
        resultMap.put("tableDate", JSONArray.parseArray(JSON.toJSONString(tableDate)));
        return resultMap;
    }
}
