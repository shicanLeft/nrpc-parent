package com.nrpc.common.zookeeper;

import com.nrpc.common.util.JSONUtil;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * @Author: shican.sc
 * @Date: 2021/2/24 17:08
 * @see
 */
public class RpcNodeInfo implements Serializable {

    private String host;

    private int port;

    private List<RpcServiceInfo> rpcServiceInfoList;

    public RpcNodeInfo() {
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public List<RpcServiceInfo> getRpcServiceInfoList() {
        return rpcServiceInfoList;
    }

    public void setRpcServiceInfoList(List<RpcServiceInfo> rpcServiceInfoList) {
        this.rpcServiceInfoList = rpcServiceInfoList;
    }


    /**
     * 重写equals hashcode toString方法
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        RpcNodeInfo that= (RpcNodeInfo) obj;

        return Objects.equals(host, that.host) &&
                Objects.equals(port, that.port) &&
                isListEquals(rpcServiceInfoList, that.rpcServiceInfoList);
    }

    //判断list是否相等
    private boolean isListEquals(List<RpcServiceInfo> thisList, List<RpcServiceInfo> thatList) {
        if (null == thisList && null == thatList) {
            return true;
        }

        if (null == thisList && null != thatList) {
            return false;
        }

        if (null != thisList && null == thatList) {
            return false;
        }

        if (null != thisList && null != thatList
                && thisList.size() != thatList.size()) {
            return false;
        }
        return thisList.containsAll(thatList) && thatList.containsAll(thisList);
    }

    @Override
    public int hashCode() {
       return Objects.hash(host, port, rpcServiceInfoList.hashCode());
    }

    @Override
    public String toString() {
        return JSONUtil.Object2Str(this);
    }
}
