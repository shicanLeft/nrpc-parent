package com.nrpc.loadbalance.impl;


import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.handler.RpcClientHandler;
import com.nrpc.loadbalance.RpcLoadBalance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round robin load balance
 * Created by luxiaoxun on 2020-08-01.
 */
public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {
    private AtomicInteger roundRobin = new AtomicInteger(0);

    public RpcNodeInfo doRoute(List<RpcNodeInfo> addressList) {
        int size = addressList.size();
        // Round robin
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return addressList.get(index);
    }

    @Override
    public RpcNodeInfo route(String serviceKey, Map<RpcNodeInfo, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcNodeInfo>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcNodeInfo> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }
}
