package com.nrpc.handler;

import com.nrpc.common.codec.Beat;
import com.nrpc.common.codec.RpcRequest;
import com.nrpc.common.codec.RpcResponse;
import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.connect.ConnectionManager;
import com.nrpc.future.RpcFuture;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: chaoben.cb
 * @Date: 2021/2/28 14:56
 * @see
 */
@Slf4j
public class RpcClientHandler extends SimpleChannelInboundHandler<RpcResponse> {

    //维护节点信息
    private RpcNodeInfo rpcNodeInfo;
    private ConcurrentHashMap<String, RpcFuture> pendingRPC = new ConcurrentHashMap<>();


    public RpcNodeInfo getRpcNodeInfo() {
        return rpcNodeInfo;
    }

    public void setRpcNodeInfo(RpcNodeInfo rpcNodeInfo) {
        this.rpcNodeInfo = rpcNodeInfo;
    }

    private volatile Channel channel;
    private SocketAddress socketAddress;

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        return super.acceptInboundMessage(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        this.socketAddress = this.channel.remoteAddress();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
        this.channel = ctx.channel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close();
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcResponse response) throws Exception {
        String requestId = response.getRequestId();
        log.debug("Receive response: " + requestId);
        RpcFuture rpcFuture = pendingRPC.get(requestId);
        if (rpcFuture != null) {
            pendingRPC.remove(requestId);
            rpcFuture.done(response);
        } else {
            log.warn("Can not get pending response for request id: " + requestId);
        }
    }

    public void close() {
        channel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        ConnectionManager.getInstance().removeHandler(rpcNodeInfo);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            //Send ping
            sendRequest(Beat.BEAT_REQUEST);
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    public RpcFuture sendRequest(RpcRequest request) {
        RpcFuture rpcFuture = new RpcFuture(request);
        pendingRPC.put(request.getRequestId(), rpcFuture);
        try {
            ChannelFuture channelFuture = channel.writeAndFlush(request).sync();
            if (!channelFuture.isSuccess()) {
                log.error("Send request {} error", request.getRequestId());
            }
        } catch (InterruptedException e) {
            log.error("Send request exception: " + e.getMessage());
        }

        return rpcFuture;
    }

}
