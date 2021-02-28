package com.nrpc.connect;

import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.handler.RpcClientHandler;
import com.nrpc.handler.RpcClientInitializer;
import com.nrpc.loadbalance.RpcLoadBalance;
import com.nrpc.loadbalance.impl.RpcLoadBalanceRoundRobin;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author: shican.sc
 * @Date: 2021/2/26 17:19
 * @see
 */
@Slf4j
public class ConnectionManager {

    // default route
    private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();

    //客户端节点信息集合
    private CopyOnWriteArraySet<RpcNodeInfo> serverNodeSet = new CopyOnWriteArraySet();

    //netty客户端线程池处理
    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
            2 * Runtime.getRuntime().availableProcessors(), 600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    //客户端对象维护netty - nioeventLoopGroup
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    //节点 -- handler (Map)
    private Map<RpcNodeInfo, RpcClientHandler> handlerNodeMap = new ConcurrentHashMap<>();

    //server lock
    private ReentrantLock lock;
    private Condition condition = lock.newCondition();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private volatile boolean isRunning = true;


    //单例模式
    private ConnectionManager(){}

    private static class SingletonHolder {
        private final static ConnectionManager instance = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    //节点信息连接
    public void updateConnectedServer(List<RpcNodeInfo> rpcNodeInfos) {
        if (CollectionUtils.isEmpty(rpcNodeInfos)) {
            return;
        }

        //去重
        Set<RpcNodeInfo> setNodes = new HashSet<>();
        for (RpcNodeInfo rpcNodeInfo : rpcNodeInfos) {
            setNodes.add(rpcNodeInfo);
        }

        //and add new server node
        for (RpcNodeInfo setNode : setNodes) {
            if (!serverNodeSet.contains(setNode)) {
                //serverNodeSet.add(setNode);
                connectServer(setNode);
            }
        }

        //CLOSE AND REMOVE INVALID SERVER
        for (RpcNodeInfo rpcNodeInfo : serverNodeSet) {
            if (!setNodes.contains(rpcNodeInfo)) {
                RpcClientHandler rpcClientHandler = handlerNodeMap.get(rpcNodeInfo);
                if (null != rpcClientHandler) {
                    rpcClientHandler.close();
                }
                handlerNodeMap.remove(rpcNodeInfo);
                serverNodeSet.remove(rpcNodeInfo);
            }
        }
    }

    /**
     * 为节点创建连接（netty-client）
     *
     * @param setNode
     */
    private void connectServer(final RpcNodeInfo setNode) {
        if (null == setNode || StringUtils.isEmpty(setNode.getHost())
                || CollectionUtils.isEmpty(setNode.getRpcServiceInfoList())) {
            log.warn("ConnectionManager#connectServer setnode={} is no valid", setNode);
        }

        serverNodeSet.add(setNode);

        //一个节点绑定一个网络信息
        String host = setNode.getHost();
        int port = setNode.getPort();
        final InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);

        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture connect = bootstrap.connect(inetSocketAddress);

                connect.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            log.info("Successfully connect to remote server, remote peer = {}", future);

                            //保存节点与对应的业务逻辑hander
                            RpcClientHandler rpcClientHandler = future.channel().pipeline().get(RpcClientHandler.class);
                            handlerNodeMap.put(setNode, rpcClientHandler);
                            rpcClientHandler.setRpcNodeInfo(setNode);

                            //可用server
                            signalAvailableHandler();
                        }else {
                            log.info("Can not connect to remote server, remote peer = {}", future);
                        }
                    }
                });
            }
        });

    }

    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = handlerNodeMap.values().size();
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = handlerNodeMap.values().size();
            } catch (InterruptedException e) {
                log.error("Waiting for available service is interrupted!", e);
            }
        }
        RpcNodeInfo rpcProtocol = loadBalance.route(serviceKey, handlerNodeMap);
        RpcClientHandler handler = handlerNodeMap.get(rpcProtocol);
        if (handler != null) {
            return handler;
        } else {
            throw new Exception("Can not get available connection");
        }
    }

    private void signalAvailableHandler(){
        lock.lock();
        try {
            condition.signalAll();
        }finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            log.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }


    public void removeHandler(RpcNodeInfo rpcProtocol) {
        serverNodeSet.remove(rpcProtocol);
        handlerNodeMap.remove(rpcProtocol);
        log.info("Remove one connection, host: {}, port: {}", rpcProtocol.getHost(), rpcProtocol.getPort());
    }


    public void stop() {
        isRunning = false;
        for (RpcNodeInfo rpcProtocol : serverNodeSet) {
            RpcClientHandler handler = handlerNodeMap.get(rpcProtocol);
            if (handler != null) {
                handler.close();
            }
            handlerNodeMap.remove(rpcProtocol);
            serverNodeSet.remove(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}
