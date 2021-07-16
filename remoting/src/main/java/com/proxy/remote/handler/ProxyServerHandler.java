package com.proxy.remote.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.callback.MsgCallBack;
import com.proxy.holder.ChannelHolder;
import com.proxy.remote.NettyRemotingServer;
import com.proxy.utils.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public class ProxyServerHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("tcp客户端连接!");
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        int port = socketAddress.getPort();
        ChannelHolder.addChannel(port,ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {
        log.debug("Proxy-Sever received  message {} from proxy-client !", proxyMessage.getType());
        switch (proxyMessage.getType()) {
            case ProxyMessage.TYPE_HEARTBEAT:
                handleHeartbeatMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.C_TYPE_AUTH:
                handleAuthMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.P_TYPE_TRANSFER:
                handleTransferMessage(ctx, proxyMessage);
                break;
            default:
                break;
        }
    }

    /**
     * handler auth message
     * @param ctx
     * @param proxyMessage
     */
    private void handleAuthMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws JsonProcessingException {
        String mateData = proxyMessage.getMateData();
        Map map = JsonUtil.strToObj(mateData, Map.class);
        String clientKey  = (String) map.get("clientId");
        String clientSecret =(String) map.get("clientSecret");
        int exposeServerPort = (int) map.get("exposeServerPort");
        String exposeServerHost =(String) map.get("exposeServerHost");
        //fixme validate clientKey and  clientSecret.
        boolean authSuccess = this.validateClient(clientKey,clientSecret) ;
        String authResultMsg = Constants.AUTH_RESULT_FAIL;
        if(authSuccess){
            //保存channel
            authResultMsg = Constants.AUTH_RESULT_SUCCESS;
            //启动客户端，用于接受客户端的http消息。
            NettyRemotingServer nettyRemotingServer = new NettyRemotingServer( new ChannelInitializer() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline().addLast(new ExposeServerHandler(ctx.channel()));
                }
            }, new MsgCallBack(),exposeServerHost,exposeServerPort);
            ChannelFuture channelFuture = nettyRemotingServer.run();

        }
        ProxyMessage authResultMessage = new ProxyMessage();
        authResultMessage.setType(ProxyMessage.TYPE_AUTH_RESULT);
        authResultMessage.setData(authResultMsg.getBytes(StandardCharsets.UTF_8));
        ctx.writeAndFlush(authResultMessage);
        if(!authSuccess){
            ctx.channel();
        }
    }

    private boolean validateClient(String clientKey, String clientSecret) {
        return true;
    }

    // handle connect  message
    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {

    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {

    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        //将response消息写会客户端
        log.info("receive Message ",proxyMessage.getType());
        long serialNumber = proxyMessage.getSerialNumber();
        ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
        buf.writeBytes(proxyMessage.getData());
        //获取客户端连接channel信息
        System.out.println(serialNumber);
        Channel userChannel = ChannelHolder.getIIdChannel(serialNumber);
        userChannel.writeAndFlush(buf);
    }

    private void handleHeartbeatMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        ProxyMessage heartbeatMessage = new ProxyMessage();
        heartbeatMessage.setSerialNumber(heartbeatMessage.getSerialNumber());
        heartbeatMessage.setType(ProxyMessage.TYPE_HEARTBEAT);
        // log.debug("response heartbeat message {}", ctx.channel());
        ctx.channel().writeAndFlush(heartbeatMessage);
    }
}
