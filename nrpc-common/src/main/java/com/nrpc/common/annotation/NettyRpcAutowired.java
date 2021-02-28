package com.nrpc.common.annotation;

/**
 * nrpc autowired interface will be proxy by ObjectProxy(class)
 *
 * @Author: shican.sc
 * @Date: 2021/2/26 15:41
 * @see
 */
public @interface NettyRpcAutowired {

    String version() default "";
}
