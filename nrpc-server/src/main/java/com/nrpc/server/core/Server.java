package com.nrpc.server.core;

/**
 * server 服务顶层抽象类
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 11:39
 * @see
 */
public abstract class Server {

    /**
     * server start
     *
     * @throws Exception
     */
    public abstract void start() throws Exception;

    /**
     * server stop
     *
     * @throws Exception
     */
    public abstract void stop() throws Exception;
}
