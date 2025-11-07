package com.bebopze.tdx.quant.common.config.convert;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.bebopze.tdx.quant.service.impl.TopBlockServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.*;


/**
 * bug : 002550 -> 2550               fastjson2 bug（以后项目中   严禁使用  🐶💩国产）  ->   已修复
 *
 * @author: bebopze
 * @date: 2025/7/20
 */
@Deprecated
public class StringSetDeserializer implements ObjectReader<Set<String>> {


//    static {
//        JSONFactory.getDefaultObjectReaderProvider()
//                   .register(Set.class, new StringSetDeserializer());
//    }


    @Override
    public Set<String> readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {


        Set<String> set = new HashSet<>();
        reader.startArray();
        while (!reader.nextIfArrayEnd()) {
            // 无论是数字还是字符串，统统转为字符串处理
            String val = reader.readAny().toString();
            set.add(String.format("%06d", Integer.parseInt(val))); // 保证6位补零
        }
        reader.endArray();
        return set;


//        // 读到原始 "002550,600000,000001"
//        String raw = jsonReader.readString();
//        if (raw == null || raw.isEmpty()) {
//            return Collections.emptySet();
//        }
//
//
//        // [300357,002755,000650,603207,002437,600812,300158]
//        // [300357,2755,650,603207,2437,600812,300158]
//
//        return Arrays.stream(raw.split(",")).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    // --------------------------


    public static void main(String[] args) {


        String result = "{\n" +
                "  \"blockId\": 1,\n" +
                "  \"blockCode\": \"BK001\",\n" +
                "  \"blockName\": \"区块链\",\n" +
                "  \"stockCodeSet\": [300357,002755,000650,603207,002437,600812,300158]\n" +
                "}\n";


        List<TopBlockServiceImpl.BlockTopInfoDTO> infoList = JSON.parseArray(result, TopBlockServiceImpl.BlockTopInfoDTO.class);


        String result2 = "[{\"blockId\":1,\"blockCode\":\"BK001\",\"blockName\":\"新能源\",\"stockCodeSet\":[\"002755\",\"000001\"]}]";

        List<TopBlockServiceImpl.BlockTopInfoDTO> dtoList = new Gson().fromJson(result, new TypeToken<List<TopBlockServiceImpl.BlockTopInfoDTO>>() {
        }.getType());


        System.out.println(dtoList);
    }

}