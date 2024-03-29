package com.proxy.remote.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.proxy.ClientInfo;
import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.callback.CallBack;
import com.proxy.holder.ChannelHolder;
import com.proxy.remote.NettyRemotingClient;
import com.proxy.utils.JsonUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProxyClientHandler extends SimpleChannelInboundHandler<ProxyMessage> {

    private ClientInfo clientInfo ;

    public ProxyClientHandler(ClientInfo clientInfo) {
        this.clientInfo = clientInfo ;
    }



    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //remove cached proxyClient
        ChannelHolder.removeChannel(clientInfo.getClientId());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.info("proxyClient has connected,try to auth !");
        Map<String,Object> metaDate = new HashMap<>();
        metaDate.put("clientId",clientInfo.getClientId());
        metaDate.put("clientSecret",clientInfo.getClientSecret());
        metaDate.put("exposeServerPort",clientInfo.getExposeServerPort());
        metaDate.put("exposeServerHost",clientInfo.getExposeServerHost());
        metaDate.put("innerHost",clientInfo.getInnerHost());
        metaDate.put("innerPort",clientInfo.getInnerPort());
        String  metaStr= JsonUtil.objToStr(metaDate);
        ProxyMessage proxyMessage = ProxyMessage.builder().type(ProxyMessage.TYPE_AUTH).mateData(metaStr).build();
        ctx.writeAndFlush(proxyMessage);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws Exception {
        log.debug("proxyClient  received proxy  message {} from proxyServer !", proxyMessage.getType());
        switch (proxyMessage.getType()) {
            case ProxyMessage.TYPE_AUTH_RESULT:
                handleAuthResultMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_DISCONNECT:
                handleDisconnectMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_TRANSFER:
                handleTransferMessage(ctx, proxyMessage);
                break;
            case ProxyMessage.TYPE_CONNECT:
                handleConnectMessage(ctx, proxyMessage);
                break;
            default:
                break;
        }
    }

    private void handleConnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String host=ctx.channel().attr(Constants.INNER_HOST).get() ;
        int port =ctx.channel().attr(Constants.INNER_PORT).get() ;
        String serialNumber = proxyMessage.getSerialNumber();
        //连接server
        ChannelInitializer ChannelInitializer = getChannelInitializer(serialNumber);
        CallBack callBack = getCallBack(ctx,serialNumber);
        //调整线程模型
        NettyRemotingClient nettyRemotingClient = new NettyRemotingClient(ChannelInitializer,callBack, host, port).group(ctx.channel().eventLoop()).init(false);
        // NettyRemotingClient nettyRemotingClient = new NettyRemotingClient(ChannelInitializer,callBack, host, port).group().init().ru;
        nettyRemotingClient.run();
    }

    /**
     * handler AuthResult Message
     * @param ctx
     * @param proxyMessage
     */
    private void handleAuthResultMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {
        String authResult = new String(proxyMessage.getData());
        if(Constants.AUTH_RESULT_SUCCESS.equals(authResult)){
            log.info("client auth success！");
            Channel channel = ctx.channel();
            channel.attr(Constants.INNER_PORT).set(clientInfo.getInnerPort());
            channel.attr(Constants.INNER_HOST).set(clientInfo.getInnerHost());
            channel.attr(Constants.ClIENT_ID).set(clientInfo.getClientId());
            ChannelHolder.addChannel(clientInfo.getClientId(),channel);
        }else {
            log.info("client auth fail，connection will close！");
            ctx.close();
        }
    }

    private void handleDisconnectMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) throws JsonProcessingException {
        Channel innerChannel = ChannelHolder.getIIdChannel(proxyMessage.getSerialNumber());
        //step 1:remove cached innerChannel
        ChannelHolder.removeIdChannel(proxyMessage.getSerialNumber());
        //step 2:close innerChannel
        //数据发送完成后再关闭连接，解决http1.0数据传输问题
        if(innerChannel!=null&&innerChannel.isActive()){
            innerChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleTransferMessage(ChannelHandlerContext ctx, ProxyMessage proxyMessage) {

        String serialNumber = proxyMessage.getSerialNumber();
        ByteBuf buf = ctx.alloc().buffer(proxyMessage.getData().length);
        buf.writeBytes(proxyMessage.getData());
        Channel innerChannel = ChannelHolder.getIIdChannel(serialNumber);
        //channel复用
        if(innerChannel!=null&&innerChannel.isActive()){
            innerChannel.writeAndFlush(buf).addListener(future -> {
                if ( future.isSuccess()) {
                    innerChannel.read();
                }
            });
        }
    }

    private CallBack getCallBack(ChannelHandlerContext ctx,String serialNumber) {
        return new CallBack() {
            @Override
            public void success(Channel channel) {
                log.debug("innerClient has be connected to server!");
                channel.attr(Constants.ClIENT_ID).set(clientInfo.getClientId());
                ChannelHolder.addIdChannel(serialNumber,channel);
                //send TYPE_CONNECT message to config clientChannel
                ProxyMessage message = ProxyMessage.builder().type(ProxyMessage.TYPE_CONNECT).serialNumber(serialNumber).build();
                ctx.writeAndFlush(message);
            }

            @Override
            public void error() {
                //连接出错，发送断开消息
                ctx.channel().writeAndFlush(ProxyMessage.disconnectedMessage());
                log.error("Exception occurred when innerClient is connecting to server!");
            }
        };
    }

    private ChannelInitializer getChannelInitializer(String serialNumber) {
        ChannelInitializer ChannelInitializer = new ChannelInitializer() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                //channel复用时如何传递serialNumber,解决方法每一个clientChannel对应一个innerChannel
                channel.pipeline().addLast("idle", new IdleStateHandler(0, 0, 120, TimeUnit.SECONDS));
                channel.pipeline().addLast("innerClientHandler", new InnerClientHandler(serialNumber));
            }
        };
        return ChannelInitializer;
    }
}
