package com.nrpc.common.util;

import org.springframework.util.StringUtils;

/**
 * 服务端工具类
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 11:31
 * @see
 */
public class ServerUtil {

    public final static String SERVER_CONTACT_SIGN = "#";

    public static String buildServerKey(String serverName, String version){
        if (StringUtils.isEmpty(serverName)) {
            throw new RuntimeException("服务名不合法");
        }

        if (StringUtils.isEmpty(version)) {
            return serverName;
        }

        return serverName + SERVER_CONTACT_SIGN + version;
    }

}
