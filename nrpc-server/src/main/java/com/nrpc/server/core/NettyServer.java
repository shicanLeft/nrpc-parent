package com.nrpc.server.core;

import com.nrpc.common.util.ServerUtil;
import com.nrpc.common.util.ThreadPoolUtil;
import com.nrpc.server.registry.ServerRegistry;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @Author: shican.sc
 * @Date: 2021/2/24 11:05
 * @see
 */
@Slf4j
public class NettyServer extends Server{

    //服务接口存储
    private final static ConcurrentHashMap<String, Object> serverMap = new ConcurrentHashMap<>();

    //netty服务地址
    private String nettyServerAddress;

    //zookeeper注册地址
    private ServerRegistry serverRegistry;

    //启动线程
    private Thread thread;

    public NettyServer(String nettyServerAddress, String registryAddress) {
        this.nettyServerAddress = nettyServerAddress;
        this.serverRegistry = new ServerRegistry(registryAddress);
    }


    /**
     * 需暴露的服务接口，添加至内存MAP中
     *
     * @param serverName
     * @param version
     * @param Server
     */
    public void addServer(String serverName, String version, Object Server){
        String serverKey = ServerUtil.buildServerKey(serverName, version);
        serverMap.put(serverKey, Server);
    }

    /**
     * netty服务启动
     *
     * zookeeper服务节点注册
     *
     * @throws Exception
     */
    @Override
    public void start() throws Exception {
        thread = new Thread(new Runnable() {

            ThreadPoolExecutor serverThreadPool = ThreadPoolUtil.makeServerThreadPool(NettyServer.class.getSimpleName(),
                    Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2);
            @Override
            public void run() {
                //启动netty服务
                NioEventLoopGroup bossThread = new NioEventLoopGroup(1);
                NioEventLoopGroup workThread = new NioEventLoopGroup();
                try {
                    ServerBootstrap bootstrap = new ServerBootstrap();
                    bootstrap.group(bossThread, workThread).channel(NioServerSocketChannel.class)
                            .childHandler(new NettyServerInitizlizer(serverMap, serverThreadPool))
                            .option(ChannelOption.SO_BACKLOG, 128)
                            .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE);

                    String[] split = nettyServerAddress.split(":");
                    String nettyHost = split[0];
                    int nettyPort = Integer.parseInt(split[1]);
                    ChannelFuture channelFuture = bootstrap.bind(nettyHost, nettyPort).sync();

                    //注册zookeeper
                    if (null != serverRegistry) {
                        serverRegistry.registryServer(nettyHost, nettyPort, serverMap);
                    }

                    log.info("NettyServer@start is successful, start ip is {} and port is {}", nettyHost, nettyPort);
                    channelFuture.channel().closeFuture().sync();
                }catch (Exception e){
                    log.error("NettyServer@start is fail", e);
                }finally {
                    try {
                        serverRegistry.unRegistryServer();
                        bossThread.shutdownGracefully();
                        workThread.shutdownGracefully();
                    }catch (Exception e){
                        log.error(e.getMessage(), e);
                    }
                }
            }
        });
      thread.start();
    }

    @Override
    public void stop() throws Exception {
        if (null != thread && thread.isAlive()) {
            thread.interrupt();
        }
    }
}
