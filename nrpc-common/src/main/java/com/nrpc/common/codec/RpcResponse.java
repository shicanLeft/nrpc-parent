package com.nrpc.common.codec;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC 请求响应对象
 *
 * @Author: shican.sc
 * @Date: 2021/2/24 16:01
 * @see
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RpcResponse implements Serializable {

    private String requestId;
    private String error;
    private Object result;
    private int code;

}
