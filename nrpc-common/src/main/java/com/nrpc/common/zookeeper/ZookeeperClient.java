package com.nrpc.common.zookeeper;

import com.nrpc.common.config.Constant;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;

import java.util.List;

/**
 *  zookeeper 客户端
 *
 * @Author: shican.sc
 * @Date: 2021/2/25 13:57
 * @see
 */
public class ZookeeperClient {

    private CuratorFramework client;

    public ZookeeperClient() {
    }

    public ZookeeperClient(String connectAddress, int timeout) {
        this(connectAddress, Constant.ZOOKEEPER_NAMESPACE, timeout, timeout);
    }

    public ZookeeperClient(String connectAddress, String nameSpace, int sessionTimeout, int connectTimeout){

        client = CuratorFrameworkFactory.builder().namespace(nameSpace)
                .connectString(connectAddress).sessionTimeoutMs(sessionTimeout)
                .connectionTimeoutMs(connectTimeout)
                .retryPolicy(new ExponentialBackoffRetry(1000, 10))
                .build();

        client.start();
    }

    public CuratorFramework getZkClient() {
        return client;
    }


    /**
     * 客户端链接状态监听
     *
     * @param connectionStateListener
     */
    public void addConnectStateListen(ConnectionStateListener connectionStateListener){
        client.getConnectionStateListenable().addListener(connectionStateListener);
    }


    /**
     * create node by path and data
     *
     * @param path
     * @param data
     * @throws Exception
     */
    public void createPathData(String path, byte[] data) throws Exception{
        client.create().creatingParentsIfNeeded()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(path, data);
    }

    /**
     * delete node by path
     *
     * @param path
     * @throws Exception
     */
    public void deletePath(String path) throws Exception {
        client.delete().forPath(path);
    }

    /**
     * get Data by path
     *
     * @param path
     * @throws Exception
     */
    public byte[] getPathData(String path) throws Exception {
        return client.getData().forPath(path);
    }

    /**
     *  get childrenPath by parentPath
     *
     * @param path
     * @return
     * @throws Exception
     */
    public List<String> getChildrenPath(String path) throws Exception {
        return client.getChildren().forPath(path);
    }

    /**
     *  update pathData by this path
     *
     * @param path
     * @param data
     * @throws Exception
     */
    public void updatePathData(String path, byte[] data) throws Exception {
        client.setData().forPath(path, data);
    }


    /**
     * watch PathDate by this path
     * using interface
     * @see  org.apache.zookeeper.Watcher
     *
     * @param path
     * @param watcher
     * @throws Exception
     */
    public void watchPath(String path, Watcher watcher) throws Exception {
        client.getData().usingWatcher(watcher).forPath(path);
    }

    public void watchPathChildrenNode(String path, PathChildrenCacheListener listener) throws Exception {
        PathChildrenCache pathChildrenCache = new PathChildrenCache(client, path, true);
        //BUILD_INITIAL_CACHE 代表使用同步的方式进行缓存初始化。
        pathChildrenCache.start(PathChildrenCache.StartMode.BUILD_INITIAL_CACHE);
        pathChildrenCache.getListenable().addListener(listener);
    }

    /**
     * stop this client
     */
    public void close() {
        client.close();
    }

}
