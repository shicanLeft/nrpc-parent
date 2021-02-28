package com.nrpc;

import com.nrpc.common.annotation.NettyRpcAutowired;
import com.nrpc.connect.ConnectionManager;
import com.nrpc.discovery.ServerDiscover;
import com.nrpc.proxy.ObjectProxy;
import javafx.beans.property.ObjectProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 客户端启动，自动注入需要远程调用的接口信息
 *
 * @Author: chaoben.cb
 * @Date: 2021/2/26 14:55
 * @see
 */
@Slf4j
public class NrpcClientStart implements ApplicationContextAware, DisposableBean {


    private ServerDiscover serviceDiscovery;

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));


    public NrpcClientStart(String address) {
        this.serviceDiscovery = new ServerDiscover(address);

    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();

        for (String beanDefinitionName : beanDefinitionNames) {
            Object bean = applicationContext.getBean(beanDefinitionName);
            Field[] fields = bean.getClass().getFields();
            try {
                for (Field field : fields) {
                    NettyRpcAutowired annotation = field.getAnnotation(NettyRpcAutowired.class);
                    if (null != annotation) {
                        String version = annotation.version();
                        field.setAccessible(Boolean.TRUE);
                        field.set(bean, this.createServer(field.getType(), version));
                    }
                }
            }catch (Exception e){
                log.error("NrpcClientStart@setApplicationContext error", e);
            }
        }
    }

    /**
     * create proxy Object
     *
     * @param interfaceClass
     * @param version
     * @return
     */
    private Object createServer(Class<?> interfaceClass, String version) {
        Object proxy = Proxy.newProxyInstance(
                interfaceClass.getClassLoader(), new Class<?>[] {interfaceClass}, new ObjectProxy(interfaceClass, version)
        );
        return proxy;
    }


    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscovery.stop();
        ConnectionManager.getInstance().stop();
    }

    @Override
    public void destroy() throws Exception {
        this.stop();
    }
}
