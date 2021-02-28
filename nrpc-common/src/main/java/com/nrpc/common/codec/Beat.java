package com.nrpc.common.codec;

/**
 * 客户端维持心跳的类
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 16:42
 * @see
 */
public final class Beat {

    public static final int BEAT_INTERVAL = 30;
    public static final int BEAT_TIMEOUT = 3 * BEAT_INTERVAL;
    public static final String BEAT_REQUEST_ID = "BEAT_PING_PONG";
    public static RpcRequest BEAT_REQUEST;

    static {
        BEAT_REQUEST = new RpcRequest();
        BEAT_REQUEST.setRequestId(BEAT_REQUEST_ID);
    }
}
