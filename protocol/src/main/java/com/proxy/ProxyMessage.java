package com.proxy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 代理客户端与代理服务器消息交换协议
 *
 *
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProxyMessage  implements Cloneable{

    /** 认证消息  */
    public static final byte TYPE_AUTH = 0x01;

    /** 认证结果消息 */
    public static final byte TYPE_AUTH_RESULT = 0x02;

    /** 连接建立消息 */
    public static final byte TYPE_CONNECT = 0x03;

    /** 连接断开消息 */
    public static final byte TYPE_DISCONNECT = 0x04;

    /** 数据传输消息 */
    public static final byte TYPE_TRANSFER = 0x05;

    /** 心跳消息 */
    public static final byte TYPE_HEARTBEAT = 0x06;
    /** 类型 */
    private byte type;

    /** 流水号 */
    private String serialNumber;

    /** 额外元数据信息 */
    private String mateData;

    /** 数据实体 */
    private byte[] data;

    public static ProxyMessage disconnectedMessage(){
        return ProxyMessage.builder().type(TYPE_DISCONNECT).build();
    }
}
