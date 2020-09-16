package com.doas;

import com.alibaba.fastjson.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class MapGroup {
    public static void main(String[] args) {
        List<List<String>> groups = new ArrayList<>();

        List<String> data = new ArrayList<>();
        data.add("a-c");
        data.add("8-9");
        data.add("6-8");
        data.add("a-n");
        data.add("1-2");
        //data.add("1-c");
        data.add("m-n");

        data.forEach(item->{
            joinGroup(groups,item);
        });

        System.out.println(JSONArray.toJSONString(groups));
    }

    /**
     * 解析由 “两点关系” 导致的：
     * 新组的产生 或 已有组之间合并
     * @param groups
     * @param relation
     */
    private static void joinGroup(List<List<String>> groups,String relation){

        List<Integer> relationalGroup = new ArrayList<>();
        for(int g = 0 ; g < groups.size() ; g++){
            // 拿到一个组
            List<String> group = groups.get(g);
            // 判断 这个“两点关系” 和 当前组是否有关系
            boolean isRelational = isRelational(group,relation);
            if(isRelational){
                // 如果有关系，记录当前组的索引
                relationalGroup.add(g);
            }
        }
        // 检查 relationalGroup 有多少个记录，全部合并
        if(relationalGroup.size() == 0){
            // 说明这个“两点关系” 和目前所有的组都没有关系，则自成一组
            List<String> newGroup = new ArrayList<>();
            newGroup.add(relation);
            groups.add(newGroup);
        } else {
            // 说明这个“两点关系” 其他组有关系，要加入并合并
            // 拿到第一个组，先把“两点关系”加入进去
            List<String> one = groups.get(relationalGroup.get(0));
            one.add(relation);
            // 再把其他组和第一个组合并
            for(int i = 1 ; i < relationalGroup.size() ; i ++){
                // 合并到第一个各
                one.addAll(groups.get(relationalGroup.get(i)));
            }
            // 合并后再移除其他组
            for(int i = 1 ; i < relationalGroup.size() ; i ++){
                groups.remove(relationalGroup.get(i).intValue());
            }
        }
    }

    /**
     * 判断 某个“两点关系” 和 当前组是否有关系
     * @param group  当前组
     * @param relation 两点关系
     * @return
     */
    private static boolean isRelational(List<String> group,String relation){
        String both[] = relation.split("-");
        for(int i = 0 ; i < group.size(); i++){
            String[] gp = group.get(i).split("-");
            if(gp[0].equals(both[0])
                            || gp[0].equals(both[1])
                            || gp[1].equals(both[0])
                            || gp[1].equals(both[1])){
                return true;
            }
        }
        return false;
    }
}
