package com.proxy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class ProxyMessageEncoder extends MessageToByteEncoder<ProxyMessage> {

    private static final int TYPE_SIZE = 1;

    private static final int SERIAL_NUMBER_SIZE = 1;

    private static final int META_DATA_LENGTH_SIZE = 4;

    @Override
    protected void encode(ChannelHandlerContext ctx, ProxyMessage msg, ByteBuf out) throws Exception {
        int bodyLength = TYPE_SIZE + SERIAL_NUMBER_SIZE + META_DATA_LENGTH_SIZE;
        byte[] metaDataBytes = null;
        byte[] snBytes = null;

        if (msg.getSerialNumber() != null) {
            snBytes = msg.getSerialNumber().getBytes();
            bodyLength += snBytes.length;
        }

        if (msg.getMateData() != null) {
            metaDataBytes = msg.getMateData().getBytes();
            bodyLength += metaDataBytes.length;
        }
        if (msg.getData() != null) {
            bodyLength += msg.getData().length;
        }
        ////|bodyLength(4)|msgType(1)|snLength(1)|Sn(8)|metaDataLength(4)|metaData(N)|Data(N)|
        // write the total packet length but without length field's length.
        out.writeInt(bodyLength);
        out.writeByte(msg.getType());
        if (snBytes != null) {
            out.writeByte((byte) snBytes.length);
            out.writeBytes(snBytes);
        } else {
            out.writeByte((byte) 0x00);
        }

        if (metaDataBytes != null) {
            out.writeInt(metaDataBytes.length);
            out.writeBytes(metaDataBytes);
        } else {
            out.writeInt(0);
        }

        if (msg.getData() != null) {
            out.writeBytes(msg.getData());
        }
    }
}