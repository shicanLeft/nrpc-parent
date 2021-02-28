package com.nrpc.server.core;

import com.nrpc.common.codec.RpcDecoder;
import com.nrpc.common.codec.RpcEncoder;
import com.nrpc.common.codec.RpcRequest;
import com.nrpc.common.codec.RpcResponse;
import com.nrpc.common.serializer.Serializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * netty childhander init
 *
 * @Author: chaoben.cb
 * @Date: 2021/2/25 17:36
 * @see
 */
public class NettyServerInitizlizer extends ChannelInitializer<SocketChannel> {

    private ThreadPoolExecutor threadPoolExecutor;
    private Map<String, Object> serverMap;


    public NettyServerInitizlizer(Map<String, Object> serverMap, ThreadPoolExecutor threadPoolExecutor){
        this.threadPoolExecutor = threadPoolExecutor;
        this.serverMap = serverMap;
    }


    /**
     * 初始化channelPipeline
     * pipeline中会顺序增加inboundHandler & outboundHandler
     *
     * 执行顺序： 顺序执行inboundhandler
     *          逆向执行outboundhandler
     *
     *
     * @param ch
     * @throws Exception
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {

        //pipeline
        ChannelPipeline pipeline = ch.pipeline();

        Serializer serializer = Serializer.class.newInstance();

        //向pipeline中插入handler
        pipeline.addLast(new IdleStateHandler(0 ,0, 3 * 30, TimeUnit.SECONDS));
        pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0));
        pipeline.addLast(new RpcDecoder(serializer, RpcRequest.class));
        pipeline.addLast(new RpcEncoder(serializer, RpcResponse.class));
        //自定义handler处理器
        pipeline.addLast(new NettyServerHandler(serverMap, threadPoolExecutor));
    }
}
