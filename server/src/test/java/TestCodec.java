import com.fasterxml.jackson.core.JsonProcessingException;
import com.proxy.ProxyMessage;
import com.proxy.utils.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class TestCodec {

    private static final byte HEADER_SIZE = 4;

    private static final int TYPE_SIZE = 1;

    private static final int SERIAL_NUMBER_SIZE = 1;

    private static final int META_DATA_LENGTH_SIZE = 4;
    @Test
    public void test()  {
        ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
        ProxyMessage msg = getHeartBeatMessage();
        ByteBuf encode = encode(out, msg);
        ProxyMessage proxyMessage = decode(encode);
        System.out.println(proxyMessage);
    }

    private static ProxyMessage decode(ByteBuf in ) {
        if (in == null) {
            return null;
        }

        if (in.readableBytes() < HEADER_SIZE) {
            return null;
        }
        /**
         * bodyLength
         */
        int frameLength = in.readInt();
        if (in.readableBytes() < frameLength) {
            return null;
        }
        ProxyMessage proxyMessage = new ProxyMessage();
        /**
         * type
         */
        byte type = in.readByte();
        proxyMessage.setType(type);
        /**
         * sn
         */
        byte snLength = in.readByte();
        byte[] snByte = new byte[snLength];
        in.readBytes(snByte);
        proxyMessage.setSerialNumber(new String(snByte));

        /**
         * metaData
         */
        int metaDataLength = in.readInt();
        if(metaDataLength>0){
            byte[] metaDataBytes = new byte[metaDataLength];
            in.readBytes(metaDataBytes);
            proxyMessage.setMateData(new String(metaDataBytes));
        }
        /**
         * data
         */
        byte[] data = new byte[frameLength - TYPE_SIZE - SERIAL_NUMBER_SIZE - META_DATA_LENGTH_SIZE - snLength - metaDataLength];
        in.readBytes(data);
        proxyMessage.setData(data);

        return proxyMessage;
    }

    private static ByteBuf encode(ByteBuf out, ProxyMessage msg) {
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
        return out;
    }

    private static ProxyMessage getAuthMessage() throws JsonProcessingException {
        Map<String,Object> metaDate = new HashMap<>();
        metaDate.put("clientId","123456");
        metaDate.put("clientSecret","123456");
        metaDate.put("exposeServerPort","127.0.0.1");
        metaDate.put("exposeServerHost",8080);
        metaDate.put("innerHost","127.0.0.1");
        metaDate.put("innerPort","9090");
        String  metaStr= JsonUtil.objToStr(metaDate);
        ProxyMessage proxyMessage = ProxyMessage.builder().type(ProxyMessage.TYPE_AUTH).mateData(metaStr).build();
        return proxyMessage;
    }

    private static ProxyMessage getHeartBeatMessage()  {
        return  ProxyMessage.heartbeatMessage();
    }
}
