package com.nrpc.loadbalance;


import com.nrpc.common.util.ServerUtil;
import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.common.zookeeper.RpcServiceInfo;
import com.nrpc.handler.RpcClientHandler;
import org.apache.commons.collections4.map.HashedMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by luxiaoxun on 2020-08-01.
 */
public abstract class RpcLoadBalance {

    // Service map: group by service name
    protected Map<String, List<RpcNodeInfo>> getServiceMap(Map<RpcNodeInfo, RpcClientHandler> connectedServerNodes) {
        Map<String, List<RpcNodeInfo>> serviceMap = new HashedMap<>();
        if (connectedServerNodes != null && connectedServerNodes.size() > 0) {
            for (RpcNodeInfo rpcProtocol : connectedServerNodes.keySet()) {
                for (RpcServiceInfo serviceInfo : rpcProtocol.getRpcServiceInfoList()) {
                    String serviceKey = ServerUtil.buildServerKey(serviceInfo.getServerName(), serviceInfo.getVersion());
                    List<RpcNodeInfo> rpcProtocolList = serviceMap.get(serviceKey);
                    if (rpcProtocolList == null) {
                        rpcProtocolList = new ArrayList<>();
                    }
                    rpcProtocolList.add(rpcProtocol);
                    serviceMap.put(serviceKey, rpcProtocolList);
                }
            }
        }
        return serviceMap;
    }

    // Route the connection for service key
    public abstract RpcNodeInfo route(String serviceKey, Map<RpcNodeInfo, RpcClientHandler> connectedServerNodes) throws Exception;
}
