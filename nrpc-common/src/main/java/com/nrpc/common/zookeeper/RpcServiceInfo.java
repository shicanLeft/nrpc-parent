package com.nrpc.common.zookeeper;

import com.nrpc.common.util.JSONUtil;

import java.io.Serializable;
import java.util.Objects;

/**
 * @Author: shican.sc
 * @Date: 2021/2/24 16:49
 * @see
 */
public class RpcServiceInfo implements Serializable {

    private String serverName;

    private String version;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    /**
     * 重写equals hashcode toString
     */
    @Override
    public boolean equals(Object obj) {
        if (null == obj || getClass() != obj.getClass()) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        RpcServiceInfo rpcServiceInfo = (RpcServiceInfo) obj;
        return Objects.equals(serverName, rpcServiceInfo.serverName) &&
                Objects.equals(version, rpcServiceInfo.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serverName, version);
    }

    @Override
    public String toString() {
        return JSONUtil.Object2Str(this);
    }
}
