package com.nrpc.common.serializer;

/**
 * 序列化接口
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 15:41
 * @see
 */
public interface Serializer {

    public <T> byte[]  serialize(T obj) throws Exception;

    public <T> Object deserialize(byte[] bytes, Class<T> clazz) throws Exception;

}
