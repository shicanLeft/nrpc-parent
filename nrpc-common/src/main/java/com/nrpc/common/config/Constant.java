package com.nrpc.common.config;

/**
 * @Author: shican.sc
 * @Date: 2021/2/25 14:06
 * @see
 */
public interface Constant {

    //zookeeper namespace
    String ZOOKEEPER_NAMESPACE = "netty-rpc";

    //zookeeper node info
    String ZK_REGISTRY_PATH = "/registry";
    String ZK_DATA_PATH = ZK_REGISTRY_PATH + "/data";

}
