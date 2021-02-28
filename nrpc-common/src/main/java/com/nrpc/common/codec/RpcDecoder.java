package com.nrpc.common.codec;

import com.nrpc.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * netty rpc 解码器
 *
 * 将流出的二进制数据解码（序列化）为对象数据
 *
 * @Author: chaoben.cb
 * @Date: 2021/2/26 11:00
 * @see
 */
@Slf4j
public class RpcDecoder extends ByteToMessageDecoder {

    private Serializer serializer;

    private Class<?> genericClass;

    public RpcDecoder(Serializer serializer, Class<?> genericClass){
        this.serializer = serializer;
        this.genericClass = genericClass;
    }

    /**
     * 反序列化 ---> 容器（List<Object）
     *
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if (in.readableBytes() < 4){
            return;
        }

        //mark buffer
        in.markReaderIndex();

        //read int （每一次读取四个字节）
        int readInt = in.readInt();

        //下一次可读的与readInt比较
           //下一次可读如果不足以readInt的长度则 复位mark并返回
        if (in.readableBytes() < readInt) {
            in.resetReaderIndex();  //重置到上一次的mark位
            return;
        }

           //下一次可读的长度足以，则持续读取到byte[]
        byte[] data = new byte[readInt];
        in.readBytes(data);

        //序列化 -- add List
        Object obj;
        try {
            obj = serializer.deserialize(data, genericClass);
            out.add(obj);
        }catch (Exception e){
            log.error("RpcDecoder#decode error", e);
        }
    }
}
