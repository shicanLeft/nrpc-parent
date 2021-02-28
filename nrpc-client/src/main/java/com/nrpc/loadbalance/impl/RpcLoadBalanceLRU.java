package com.nrpc.loadbalance.impl;

import com.nrpc.common.zookeeper.RpcNodeInfo;
import com.nrpc.handler.RpcClientHandler;
import com.nrpc.loadbalance.RpcLoadBalance;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LRU load balance
 * 、LRU == Least Recently User
 * Created by luxiaoxun on 2020-08-01.
 */
public class RpcLoadBalanceLRU extends RpcLoadBalance {
    private ConcurrentMap<String, LinkedHashMap<RpcNodeInfo, RpcNodeInfo>> jobLRUMap =
            new ConcurrentHashMap<String, LinkedHashMap<RpcNodeInfo, RpcNodeInfo>>();
    private long CACHE_VALID_TIME = 0;

    public RpcNodeInfo doRoute(String serviceKey, List<RpcNodeInfo> addressList) {
        // cache clear
        if (System.currentTimeMillis() > CACHE_VALID_TIME) {
            jobLRUMap.clear();
            CACHE_VALID_TIME = System.currentTimeMillis() + 1000 * 60 * 60 * 24;
        }

        // init lru
        LinkedHashMap<RpcNodeInfo, RpcNodeInfo> lruHashMap = jobLRUMap.get(serviceKey);
        if (lruHashMap == null) {
            /**
             * LinkedHashMap
             * a、accessOrder：ture=访问顺序排序（get/put时排序）/ACCESS-LAST；false=插入顺序排期/FIFO；
             * b、removeEldestEntry：新增元素时将会调用，返回true时会删除最老元素；
             *      可封装LinkedHashMap并重写该方法，比如定义最大容量，超出是返回true即可实现固定长度的LRU算法；
             */
            lruHashMap = new LinkedHashMap<RpcNodeInfo, RpcNodeInfo>(16, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<RpcNodeInfo, RpcNodeInfo> eldest) {
                    if (super.size() > 1000) {
                        return true;
                    } else {
                        return false;
                    }
                }
            };
            jobLRUMap.putIfAbsent(serviceKey, lruHashMap);
        }

        // put new
        for (RpcNodeInfo address : addressList) {
            if (!lruHashMap.containsKey(address)) {
                lruHashMap.put(address, address);
            }
        }
        // remove old
        List<RpcNodeInfo> delKeys = new ArrayList<>();
        for (RpcNodeInfo existKey : lruHashMap.keySet()) {
            if (!addressList.contains(existKey)) {
                delKeys.add(existKey);
            }
        }
        if (delKeys.size() > 0) {
            for (RpcNodeInfo delKey : delKeys) {
                lruHashMap.remove(delKey);
            }
        }

        // load
        RpcNodeInfo eldestKey = lruHashMap.entrySet().iterator().next().getKey();
        RpcNodeInfo eldestValue = lruHashMap.get(eldestKey);
        return eldestValue;
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
