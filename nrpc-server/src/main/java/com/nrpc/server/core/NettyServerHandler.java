package com.nrpc.server.core;

import com.nrpc.common.codec.Beat;
import com.nrpc.common.codec.RpcRequest;
import com.nrpc.common.codec.RpcResponse;
import com.nrpc.common.util.ServerUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.reflect.FastClass;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: chaoben.cb
 * @Date: 2021/2/26 11:45
 * @see
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<RpcRequest> {


    private Map<String, Object> serverMap;

    private ThreadPoolExecutor threadPoolExecutor;

    public NettyServerHandler(Map<String, Object> serverMap, ThreadPoolExecutor threadPoolExecutor){
        this.serverMap = serverMap;
        this.threadPoolExecutor = threadPoolExecutor;
    }


    /**
     * 服务端inboundHandler --> dealwith the request from netty client
     *
     * @param ctx
     * @param request
     * @throws Exception
     */
    protected void channelRead0(final ChannelHandlerContext ctx, final RpcRequest request) throws Exception {
        //处理心跳
        if (Beat.BEAT_REQUEST_ID == request.getRequestId()) {
            log.info("nettyServerHandler#channelRead0 beat tcp");
            return;
        }

        log.info("nettyServerHandler#channelRead0 request={}", request);
        //处理业务请求
        //拿到本地接口信息，method信息,cglib反射调用
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                RpcResponse rpcResponse = new RpcResponse();
                rpcResponse.setRequestId(request.getRequestId());
                try {
                    Object result = NettyServerHandler.this.handle(request);
                    rpcResponse.setResult(result);
                }catch (Throwable e){
                    log.error("nettyServerHandler#channelRead0 error", e);
                }

                //ChannelHandlerContext write and flush
                ctx.writeAndFlush(rpcResponse).addListener(new GenericFutureListener<Future<? super Void>>() {
                    @Override
                    public void operationComplete(Future<? super Void> future) throws Exception {
                        log.info("Send response for requestId is {}", request.getRequestId());
                    }
                });
            }
        });
    }

    /**
     * 业务逻辑处理接口
     *
     *    服务端接口反射调用
     *
     * @param request
     * @return
     */
    private Object handle(RpcRequest request) throws Exception {
        String className = request.getClassName();
        String version = request.getVersion();
        String serverKey = ServerUtil.buildServerKey(className, version);

        Object serverBean = serverMap.get(serverKey);
        if (null == serverBean) {
            log.error("Can not find service implement with interface name: {} and version: {}", className, version);
            return null;
        }

        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        //cglib reflect by Fastclass
        FastClass fastClass = FastClass.create(serverBean.getClass());
        int methodIndex = fastClass.getIndex(methodName, parameterTypes);
        Object invokeResult = fastClass.invoke(methodIndex, serverBean, parameters);
        return invokeResult;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            ctx.channel().close();
            log.warn("Channel idle in last {} seconds, close it", Beat.BEAT_TIMEOUT);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.warn("nettyServerHandler#exceptionCaught is {}", cause.getCause());
        ctx.close();
    }
}
