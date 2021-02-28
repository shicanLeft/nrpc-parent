package com.nrpc.server.registry;

import com.google.common.collect.Lists;
import com.nrpc.common.config.Constant;
import com.nrpc.common.util.JSONUtil;
import com.nrpc.common.util.ServerUtil;
import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.common.zookeeper.RpcServiceInfo;
import com.nrpc.common.zookeeper.ZookeeperClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author: shican.sc
 * @Date: 2021/2/24 11:53
 * @see
 */
@Slf4j
public class ServerRegistry {

    private ZookeeperClient zookeeperClient;
    private List<String> pathList = new ArrayList<>();

    public ServerRegistry(String registryAddress) {
        this.zookeeperClient = new ZookeeperClient(registryAddress, 5000);
    }

    /**
     * 注册服务接口信息
     *
     *    1）服务信息 --> 二进制 --> zookeeper
     *
     * @param serverIp
     * @param port
     * @param serverMap
     */
    public void registryServer(final String serverIp, final int port, final Map<String, Object> serverMap){

        //封装server信息
        List<RpcServiceInfo> rpcServiceInfoList = Lists.newArrayList();
        for (String serverkey : serverMap.keySet()) {
            String[] serverArray = serverkey.split(ServerUtil.SERVER_CONTACT_SIGN);
            if (serverArray.length > 0) {
                RpcServiceInfo rpcServiceInfo = new RpcServiceInfo();
                String serverName = serverArray[0];
                rpcServiceInfo.setServerName(serverName);

                if (serverArray.length == 2) {
                    rpcServiceInfo.setVersion(serverArray[1]);
                }else {
                    rpcServiceInfo.setVersion("");
                }
                rpcServiceInfoList.add(rpcServiceInfo);
            }
        }

        //封装zookeeper节点信息
        RpcNodeInfo rpcNodeInfo = new RpcNodeInfo();
        rpcNodeInfo.setHost(serverIp);
        rpcNodeInfo.setPort(port);
        rpcNodeInfo.setRpcServiceInfoList(rpcServiceInfoList);

        //暴露节点信息至zookeeper
        try {
            String nodeStr = JSONUtil.Object2Str(rpcNodeInfo);
            byte[] data = nodeStr.getBytes();

            String path = Constant.ZK_DATA_PATH + "-" + rpcNodeInfo.hashCode();
            zookeeperClient.createPathData(path, data);
            pathList.add(path);
            log.info("ServerRegistry#registryServer is success, the resitryed path={}", path);
        }catch (Exception e){
            log.error("ServerRegistry#registryServer is fail", e);
        }

        //添加链接状态监听
        zookeeperClient.addConnectStateListen(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                if (newState == ConnectionState.RECONNECTED) {
                    log.info("ConnectState is {}, registry this", newState);
                    registryServer(serverIp, port, serverMap);
                }

            }
        });
    }


    /**
     * 服务取消注册
     */
    public void unRegistryServer(){
        for (String path : pathList) {
            try {
                zookeeperClient.deletePath(path);
            }catch (Exception e){
                log.error("ServerRegistry#unRegistryServer is fail");
            }
        }

        log.info("ServerRegistry#unRegistryServer is successfully, this unRegistryServer paths is {}", pathList);
        zookeeperClient.close();
    }
}
