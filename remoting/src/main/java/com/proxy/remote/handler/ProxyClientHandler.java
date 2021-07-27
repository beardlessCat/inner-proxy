package com.proxy.remote.handler;

import com.proxy.ClientInfo;
import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.callback.MsgCallBack;
import com.proxy.holder.ChannelHolder;
import com.proxy.remote.NettyRemotingClient;
import com.proxy.utils.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ProxyClientHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private ClientInfo clientInfo ;

    public ProxyClientHandler(ClientInfo clientInfo) {
        this.clientInfo = clientInfo ;
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ChannelHolder.removeChannel(clientInfo.getClientId());
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("proxy-client has connected,try to auth client !");
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.TYPE_AUTH);
        Map<String,Object> metaDate = new HashMap<>();
        metaDate.put("clientId",clientInfo.getClientId());
        metaDate.put("clientSecret",clientInfo.getClientSecret());
        metaDate.put("exposeServerPort",clientInfo.getExposeServerPort());
        metaDate.put("exposeServerHost",clientInfo.getExposeServerHost());
        String  metaStr= JsonUtil.objToStr(metaDate);
        proxyMessage.setMateData(metaStr);
        ctx.writeAndFlush(proxyMessage);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {
        log.debug("recieved proxy message, type is {}", proxyMessage.getType());
        switch (proxyMessage.getType()) {
            case ProxyMessage.TYPE_AUTH_RESULT:
                handleAuthResultMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_TRANSFER:
                handleTransferMessage(ctx, proxyMessage);
                break;
            default:
                break;
        }
    }

    /**
     * handler AuthResult Message
     * @param ctx
     * @param proxyMessage
     */
    private void handleAuthResultMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String authResult = new String(proxyMessage.getData());
        if(Constants.AUTH_RESULT_SUCCESS.equals(authResult)){
            log.info("客户端认证成功！");
            Channel channel = ctx.channel();
            channel.attr(Constants.INNER_PORT).set(clientInfo.getInnerPort());
            channel.attr(Constants.INNER_HOST).set(clientInfo.getInnerHost());
            channel.attr(Constants.ClIENT_ID).set(clientInfo.getClientId());
            ChannelHolder.addChannel(clientInfo.getClientId(),channel);
        }else {
            log.info("客户端认证失败，即将关闭连接！");
            ctx.close();
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        //数据发送完成后再关闭连接，解决http1.0数据传输问题
        ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        log.info("receive message {}",proxyMessage.getType());
        String host=ctx.channel().attr(Constants.INNER_HOST).get() ;
        int port =ctx.channel().attr(Constants.INNER_PORT).get() ;
        String serialNumber = proxyMessage.getSerialNumber();
        //连接server
        NettyRemotingClient nettyRemotingClient = new NettyRemotingClient(new ChannelInitializer() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(new InnerClientHandler(serialNumber));
            }
        }, new MsgCallBack(),host,port);
        ChannelFuture channelFuture = nettyRemotingClient.run();
        Channel channel = channelFuture.channel();
        channel.attr(Constants.ClIENT_ID).set(clientInfo.getClientId());
        ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
        buf.writeBytes(proxyMessage.getData());
        if(channel.isActive()){
            ChannelFuture channelFuture1 = channel.writeAndFlush(buf);
            channelFuture1.addListener(future -> {
                if (!future.isSuccess()){
                    log.error("message send fail !");
                }
            });
        }
    }

    private void handleConnectMessage(ChannelInboundInvoker ctx, ProxyMessage proxyMessage) {


    }
}
