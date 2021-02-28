package com.nrpc;

import com.nrpc.common.annotation.NetttyRpcServer;
import com.nrpc.server.core.NettyServer;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import java.util.Map;

/**
 * netty service bootstrap
 */
public class NrpcServerStart extends NettyServer implements ApplicationContextAware, InitializingBean, DisposableBean {

    public NrpcServerStart(String nettyServerAddress, String registryAddress){
        super(nettyServerAddress, registryAddress);
    }

    /**
     * 在spring-ioc容器中找出需要暴露为nprc服务的接口，
     * 并add 到 serverMap
     *
     * @param applicationContext
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

        Map<String, Object> beansWithAnnotationMap = applicationContext.getBeansWithAnnotation(NetttyRpcServer.class);

        if (MapUtils.isNotEmpty(beansWithAnnotationMap)) {
            for (Object serverBean : beansWithAnnotationMap.values()) {
                NetttyRpcServer annotation = serverBean.getClass().getAnnotation(NetttyRpcServer.class);
                String serverName = annotation.value().getName();
                String version = annotation.version();
                super.addServer(serverName, version, serverBean);
            }
        }
    }

    /**
     * 加载完成之后启动服务
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        super.start();
    }

    /**
     * 销毁容器并停止服务
     *
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        super.stop();
    }

}
