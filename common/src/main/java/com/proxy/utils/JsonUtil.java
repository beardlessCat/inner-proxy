package com.proxy.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    private static ObjectMapper objectMapper = new ObjectMapper();

    // 对象转json字符串
    public static String objToStr(Object o) throws JsonProcessingException {
        return objectMapper.writeValueAsString(o);
    }

    // json字符串转对象
    public static <T> T strToObj(String str, Class<T> valueType) throws JsonProcessingException {
        return objectMapper.readValue(str, valueType);
    }

    // map转对象
    public static <T> T mapToObj(Map map, Class<T> valueType) {
        return objectMapper.convertValue(map, valueType);
    }

    // json字符串转对象的List
    public static <T> List<T> strToObjList(String jsonStr, Class<T> valueType)
            throws JsonProcessingException {
        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, valueType);
        List<T> list = objectMapper.readValue(jsonStr, javaType);
        return list;
    }

    // json字符串转对象相关Type，例如List,Set
    public static <T> T strToObjTypeReference(String jsonStr, TypeReference<T> valueTypeRef)
            throws JsonProcessingException {
        return objectMapper.readValue(jsonStr, valueTypeRef);
    }

}

