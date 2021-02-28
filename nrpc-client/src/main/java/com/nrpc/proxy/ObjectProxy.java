package com.nrpc.proxy;

import com.nrpc.common.codec.RpcRequest;
import com.nrpc.common.util.ServerUtil;
import com.nrpc.connect.ConnectionManager;
import com.nrpc.future.RpcFuture;
import com.nrpc.handler.RpcClientHandler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * @Author: chaoben.cb
 * @Date: 2021/2/26 15:42
 * @see
 */
public class ObjectProxy implements InvocationHandler {

    private Class<?> interfaceClass;
    private String version;

    public ObjectProxy(Class<?> interfaceClass, String version){
        this.interfaceClass = interfaceClass;
        this.version = version;
    }


    /**
     * 客户端代理出口
     *
     * 远程调用
     *
     * 1-封装request
     *
     * 2-choose handler(服务器<ip：端口>)
     *
     * 3-netty channer writeAndFlush
     *
     * @param proxy
     * @param method
     * @param args
     * @return
     * @throws Throwable
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        if (Object.class == method.getDeclaringClass()) {
            if ("equals".equals(method.getName())) {
                return proxy == args[0];
            }else if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }else if ("toString".equals(method.getName())){
                return proxy.getClass().getName() + "@" +
                        Integer.toHexString(System.identityHashCode(proxy)) +
                        ", with InvocationHandler " + this;
            }else {
                throw new IllegalStateException(String.valueOf(method));
            }
        }

        //封装RpcRequest
        RpcRequest request = createRequest(interfaceClass.getName(), method.getName(), args);

        //通过serverKey选择handler(服务处理)
        String serverKey = ServerUtil.buildServerKey(method.getDeclaringClass().getName(), version);

        //发送request
        String serviceKey = ServerUtil.buildServerKey(method.getDeclaringClass().getName(), version);
        RpcClientHandler handler = ConnectionManager.getInstance().chooseHandler(serviceKey);
        RpcFuture rpcFuture = handler.sendRequest(request);
        return ((RpcFuture) rpcFuture).get();
    }


    public RpcRequest createRequest(String className, String methodName, Object[] args) {
        RpcRequest rpcRequest = new RpcRequest();
        //todo: 分布式唯一ID
        rpcRequest.setRequestId(UUID.randomUUID().toString());

        Class<?>[] parameterTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }

        rpcRequest.setClassName(className);
        rpcRequest.setMethodName(methodName);
        rpcRequest.setParameters(args);
        rpcRequest.setParameterTypes(parameterTypes);
        rpcRequest.setVersion(version);
        return rpcRequest;
    }
}
