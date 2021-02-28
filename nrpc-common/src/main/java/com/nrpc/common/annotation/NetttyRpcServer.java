package com.nrpc.common.annotation;

/**
 * nrpc interface server flag:
 * one such interface that haved this annotation, that meaning its the nrpc service
 * that will expose service for client,
 * In addition, will registry interface information to zookeeper
 *
 * @Author: shican.sc
 * @Date: 2021/2/25 15:05
 * @see
 */
public @interface NetttyRpcServer {

    Class<?> value();

    String version() default "";

}
