package com.nrpc.common.util;

import java.util.concurrent.*;

/**
 * threadPool Pool to do task
 *
 * @Author: chaoben.cb
 * @Date: 2021/2/25 16:58
 * @see
 */
public class ThreadPoolUtil {

    public static ThreadPoolExecutor makeServerThreadPool(final String serverName, int corePoolSize, int maxPoolSize){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(corePoolSize, maxPoolSize,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000),
                new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "netty-rpc-" + serverName + "-" + r.hashCode());
                    }
                },
                null);
        return threadPoolExecutor;
    }
}
