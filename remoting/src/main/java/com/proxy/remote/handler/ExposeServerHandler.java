package com.proxy.remote.handler;

import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.holder.ChannelHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * 处理服务端 channel.
 */
@Slf4j
public class ExposeServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Channel channel ;
    public ExposeServerHandler(Channel channel){
        this.channel =channel ;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 当出现异常就关闭连接
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        byte[] bytes = ByteBufUtil.getBytes(buf);
        ProxyMessage proxyMessage = ProxyMessage.builder().type(ProxyMessage.TYPE_TRANSFER).data(bytes).build();
        String serialNumber = ctx.channel().attr(Constants.CHANNEL_ID).get();
        proxyMessage.setSerialNumber(serialNumber);
        this.channel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //绑定channel
        String serialNumber = UUID.randomUUID().toString().replace("-","");
        ChannelHolder.addIdChannel(serialNumber,ctx.channel());
        ctx.channel().attr(Constants.CHANNEL_ID).set(serialNumber);
        //config channel AUTO_READ false
        ctx.channel().config().setOption(ChannelOption.AUTO_READ, false);
        //send TYPE_CONNECT message
        ProxyMessage message = ProxyMessage.builder().type(ProxyMessage.TYPE_CONNECT).serialNumber(serialNumber).build();
        this.channel.writeAndFlush(message);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.error("client has disConnected and will notify proxy client!");
        String channelId = ctx.channel().attr(Constants.CHANNEL_ID).get();
        ChannelHolder.removeIdChannel(channelId);
        this.channel.writeAndFlush(ProxyMessage.disconnectedMessage());
        super.channelInactive(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        try {
            if (evt instanceof IdleStateEvent) {
                log.debug("exposeServer got idle event");
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } finally {
            super.userEventTriggered(ctx, evt);
        }
    }
    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        log.debug("config channel auto_read {}" ,ctx.channel().isWritable());
        super.channelWritabilityChanged(ctx);
    }

}