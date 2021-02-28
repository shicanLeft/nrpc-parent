package com.nrpc.common.codec;

import com.nrpc.common.serializer.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

/**
 *  RPC encode messagee to byte
 *
 * @Author: shican.sc
 * @Date: 2021/2/26 11:30
 * @see
 */
@Slf4j
public class RpcEncoder extends MessageToByteEncoder {

    private Class<?> genericClass;

    private Serializer serializer;

    public RpcEncoder(Serializer serializer, Class<?> genericClass){
        this.genericClass = genericClass;
        this.serializer = serializer;
    }


    /**
     * 序列化 --> 容器（byteBuf）
     *
     * @param ctx
     * @param in
     * @param out
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, Object in, ByteBuf out) throws Exception {
        if (genericClass.isInstance(in)) {
            try {
                byte[] datas = serializer.serialize(in);
                out.writeInt(datas.length);
                out.writeBytes(datas);
            }catch (Exception e){
                log.error("RpcEncoder@encode error", e);
            }

        }
    }
}
