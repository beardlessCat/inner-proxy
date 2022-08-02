package com.proxy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProxyMessage  implements Cloneable{

    /** 认证消息  */
    public static final byte TYPE_AUTH = 0x01;

    /** 认证结果消息 */
    public static final byte TYPE_AUTH_RESULT = 0x02;

    /** 连接断开消息 */
    public static final byte TYPE_DISCONNECT = 0x03;

    /** 数据传输消息 */
    public static final byte TYPE_TRANSFER = 0x04;

    /** 心跳消息 */
    public static final byte TYPE_HEARTBEAT = 0x05;

    /** 心跳消息 */
    public static final byte TYPE_CONNECT = 0x06;
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

    public static ProxyMessage heartbeatMessage(){
        return ProxyMessage.builder().type(TYPE_HEARTBEAT).build();
    }
}
