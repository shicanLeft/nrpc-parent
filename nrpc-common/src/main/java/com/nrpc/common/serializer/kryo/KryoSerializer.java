package com.nrpc.common.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.pool.KryoPool;
import com.nrpc.common.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/**
 * kryo 序列化
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 15:45
 * @see
 */
public class KryoSerializer implements Serializer {

    private KryoPool kryoPool = KryoPoolFactory.getKryoPoolInstance();

    @Override
    public <T> byte[] serialize(T obj) throws Exception {
        Kryo borrow = kryoPool.borrow();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        try {
            borrow.writeObject(output, output);
            output.close();
            return byteArrayOutputStream.toByteArray();
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            if (null != byteArrayOutputStream) {
                try {
                    byteArrayOutputStream.close();
                } catch (IOException ioex){
                    throw new RuntimeException(ioex);
                }
            }
            kryoPool.release(borrow);
        }
    }

    @Override
    public <T> Object deserialize(byte[] bytes, Class<T> clazz) throws Exception {
        Kryo borrow = kryoPool.borrow();
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream);
        try {
            Object obj = borrow.readObject(input, clazz);
            input.close();
            return obj;
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            if (null != byteArrayInputStream) {
                try {
                    byteArrayInputStream.close();
                }catch (IOException r){
                    throw new RuntimeException(r);
                }
            }
            kryoPool.release(borrow);
        }
    }
}
