package com.nrpc.common.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.pool.KryoCallback;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.nrpc.common.codec.RpcRequest;
import com.nrpc.common.codec.RpcResponse;
import org.objenesis.strategy.StdInstantiatorStrategy;

/**
 * kryo序列化对象池
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 15:49
 * @see
 */
public class KryoPoolFactory {

    private volatile static KryoPoolFactory kryoPoolFactory = null;

    private KryoPoolFactory() {
    }

    private KryoFactory kryoFactory = new KryoFactory() {
        @Override
        public Kryo create() {
            Kryo kryo = new Kryo();
            kryo.setReferences(false);
            kryo.register(RpcRequest.class);
            kryo.register(RpcResponse.class);
            Kryo.DefaultInstantiatorStrategy strategy = (Kryo.DefaultInstantiatorStrategy) kryo.getInstantiatorStrategy();
            strategy.setFallbackInstantiatorStrategy(new StdInstantiatorStrategy());
            return kryo;
        }
    };

    private KryoPool kryoPool = new KryoPool.Builder(kryoFactory).build();

    public static KryoPool getKryoPoolInstance(){
        if (null == kryoPoolFactory) {
            synchronized (KryoPoolFactory.class) {
                if (null == kryoPoolFactory) {
                    kryoPoolFactory = new KryoPoolFactory();
                }
            }
        }
        return kryoPoolFactory.getKryoPool();
    }

    public KryoPool getKryoPool(){
        return kryoPool;
    }

}
