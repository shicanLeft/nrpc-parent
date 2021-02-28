package com.nrpc.loadbalance.impl;

import com.google.common.hash.Hashing;
import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.handler.RpcClientHandler;
import com.nrpc.loadbalance.RpcLoadBalance;
import java.util.List;
import java.util.Map;


/**
 * 一致性hash算法
 */
public class RpcLoadBalanceConsistentHash extends RpcLoadBalance {

    public RpcNodeInfo doRoute(String serviceKey, List<RpcNodeInfo> addressList) {
        int index = Hashing.consistentHash(serviceKey.hashCode(), addressList.size());
        return addressList.get(index);
    }

    @Override
    public RpcNodeInfo route(String serviceKey, Map<RpcNodeInfo, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcNodeInfo>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcNodeInfo> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(serviceKey, addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
