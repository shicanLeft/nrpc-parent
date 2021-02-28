package com.nrpc.common.util;

import com.alibaba.fastjson.JSON;
import org.springframework.util.StringUtils;

/**
 * json工具
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 16:53
 * @see
 */
public class JSONUtil {


    /**
     * 对象转JSON_STR
     *
     * @param obj
     * @return
     */
    public static String Object2Str(Object obj){
        if (null == obj) {
            return null;
        }


        return JSON.toJSONString(obj);
    }


    /**
     * JSON字符串转对象
     *
     * @param str
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T str2Object(String str, Class<T> clazz){
        if (StringUtils.isEmpty(str)) {
            return null;
        }

        return JSON.parseObject(str, clazz);
    }
}
