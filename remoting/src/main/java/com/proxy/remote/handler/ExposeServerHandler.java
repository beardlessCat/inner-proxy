package com.proxy.remote.handler;

import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.holder.ChannelHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 处理服务端 channel.
 */
@Slf4j
public class ExposeServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    private Channel channel ;
    public ExposeServerHandler(Channel channel){
        this.channel =channel ;
    }

    private static AtomicLong snProducer = new AtomicLong(0);

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 当出现异常就关闭连接
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf buf) throws Exception {
        // Channel channel = ctx.channel().attr(Constants.PROXY_CHANNEL).get();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.TYPE_TRANSFER);
        proxyMessage.setData(bytes);
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
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("client has disConnected and will notify proxy client!");
        String channelId = ctx.channel().attr(Constants.CHANNEL_ID).get();
        ChannelHolder.removeIdChannel(channelId);
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
        this.channel.writeAndFlush(proxyMessage);
        super.channelInactive(ctx);
    }

}