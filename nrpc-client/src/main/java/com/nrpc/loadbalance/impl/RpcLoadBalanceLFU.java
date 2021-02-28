package com.nrpc.loadbalance.impl;

import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.handler.RpcClientHandler;
import com.nrpc.loadbalance.RpcLoadBalance;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LFU load balance;
 * Created by luxiaoxun on 2020-08-01.
 */
public class RpcLoadBalanceLFU extends RpcLoadBalance {

    private ConcurrentMap<String, HashMap<RpcNodeInfo, Integer>> jobLfuMap = new ConcurrentHashMap<String, HashMap<RpcNodeInfo, Integer>>();
    private long CACHE_VALID_TIME = 0;

    public RpcNodeInfo doRoute(String serviceKey, List<RpcNodeInfo> addressList) {
        // cache clear
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLfuMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }

        // lfu item init
        HashMap<RpcNodeInfo, Integer> lfuItemMap = jobLfuMap.get(serviceKey);
        if (lfuItemMap == null) {
            lfuItemMap = new HashMap<RpcNodeInfo, Integer>();
            jobLfuMap.putIfAbsent(serviceKey, lfuItemMap);   // 避免重复覆盖
        }

        // put new
        for (RpcNodeInfo address : addressList) {
            if (!lfuItemMap.containsKey(address) || lfuItemMap.get(address) > 1000000) {
                lfuItemMap.put(address, 0);
            }
        }

        // remove old
        List<RpcNodeInfo> delKeys = new ArrayList<>();
        for (RpcNodeInfo existKey : lfuItemMap.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (delKeys.size() > 0) {
            for (RpcNodeInfo delKey : delKeys) {
                lfuItemMap.remove(delKey);
            }
        }

        // load least used count address
        List<Map.Entry<RpcNodeInfo, Integer>> lfuItemList = new ArrayList<Map.Entry<RpcNodeInfo, Integer>>(lfuItemMap.entrySet());
        Collections.sort(lfuItemList, new Comparator<Map.Entry<RpcNodeInfo, Integer>>() {
            @Override
            public int compare(Map.Entry<RpcNodeInfo, Integer> o1, Map.Entry<RpcNodeInfo, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        Map.Entry<RpcNodeInfo, Integer> addressItem = lfuItemList.get(0);
        RpcNodeInfo minAddress = addressItem.getKey();
        addressItem.setValue(addressItem.getValue() + 1);

        return minAddress;
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
