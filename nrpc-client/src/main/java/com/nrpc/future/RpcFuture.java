package com.nrpc.future;

import com.nrpc.common.codec.RpcRequest;
import com.nrpc.common.codec.RpcResponse;
import com.sun.corba.se.impl.orbutil.concurrent.Sync;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * future模式
 *
 * @Author: shican.sc
 * @Date: 2021/2/26 16:33
 * @see
 */
@Slf4j
public class RpcFuture implements Future<Object> {


    private Sync sync;
    private RpcRequest rpcRequest;
    private RpcResponse rpcResponse;

    private long startTime;
    private long responseTimeout = 5000;

    public RpcFuture(RpcRequest request) {
        this.sync = new Sync();
        this.rpcRequest = request;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(1);
        if (this.rpcResponse != null) {
            return this.rpcResponse.getResult();
        } else {
            return null;
        }
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if (success) {
            if (this.rpcResponse != null) {
                return this.rpcResponse.getResult();
            } else {
                return null;
            }
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + this.rpcRequest.getRequestId()
                    + ". Request class name: " + this.rpcRequest.getClassName()
                    + ". Request method: " + this.rpcRequest.getMethodName());
        }
    }

    public void done(RpcResponse response) {
        this.rpcResponse = response;
        sync.release(1);
        // Threshold
        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > this.responseTimeout) {
            log.warn("Service response time is too slow. Request id = " + response.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }


    static class Sync extends AbstractQueuedSynchronizer {

        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        protected boolean isDone() {
            return getState() == done;
        }
    }
}
