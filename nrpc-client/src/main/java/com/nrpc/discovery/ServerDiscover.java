package com.nrpc.discovery;

import com.google.common.collect.Lists;
import com.nrpc.common.config.Constant;
import com.nrpc.common.util.JSONUtil;
import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.common.zookeeper.ZookeeperClient;
import com.nrpc.connect.ConnectionManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 服务发现
 *
 * @Author: shican.sc
 * @Date: 2021/2/26 17:39
 * @see
 */
@Slf4j
public class ServerDiscover {

    private ZookeeperClient zookeeperClient;

    public ServerDiscover(String zookeeperAddress) {
        zookeeperClient = new ZookeeperClient(zookeeperAddress, 5000);
        discoverServer();
    }

    /**
     * 服务发现
     *    1）调用zookeeper 拿到所有节点信息（服务列表）
     *
     *    2）为每个服务列表信息 --> 创建netty客户端
     */
    private void discoverServer() {
        try {
            getServerAndUpdateServer();
            // Add watch listener
            zookeeperClient.watchPathChildrenNode(Constant.ZK_REGISTRY_PATH, new PathChildrenCacheListener() {
                @Override
                public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent pathChildrenCacheEvent) throws Exception {
                    PathChildrenCacheEvent.Type type = pathChildrenCacheEvent.getType();
                    switch (type) {
                        case CONNECTION_RECONNECTED:
                            log.info("Reconnected to zk, try to get latest service list");
                            getServerAndUpdateServer();
                            break;
                        case CHILD_ADDED:
                        case CHILD_UPDATED:
                        case CHILD_REMOVED:
                            log.info("Service info changed, try to get latest service list");
                            getServerAndUpdateServer();
                            break;
                    }
                }
            });
        }catch (Exception e){
            log.error("ServerDiscover#discoverServer error", e);
        }

    }

    private void getServerAndUpdateServer() {
        try {
            List<String> childrenPath = zookeeperClient.getChildrenPath(Constant.ZK_REGISTRY_PATH);
            List<RpcNodeInfo> rpcNodeInfos = Lists.newArrayList();

            for (String path : childrenPath) {
                byte[] pathData = zookeeperClient.getPathData(path);
                String data = new String(pathData);
                RpcNodeInfo rpcNodeInfo = JSONUtil.str2Object(data, RpcNodeInfo.class);
                rpcNodeInfos.add(rpcNodeInfo);
            }

            //connect server by nodeInfo
            updateConnectedServer(rpcNodeInfos);
        }catch (Exception e){
            log.error("ServerDiscover#getServerAndUpdateServer error", e);
        }
    }


    /**
     *
     * @param rpcNodeInfos
     */
    private void updateConnectedServer(List<RpcNodeInfo> rpcNodeInfos) {
        if (CollectionUtils.isEmpty(rpcNodeInfos)) {
            return;
        }

        ConnectionManager.getInstance().updateConnectedServer(rpcNodeInfos);
    }

    public void stop() {
        this.zookeeperClient.close();
    }
}
