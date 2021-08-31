package com.proxy.remote.handler;

import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.holder.ChannelHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

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
        // Channel channel = ctx.channel().attr(Constants.PROXY_CHANNEL).get();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        ProxyMessage proxyMessage = ProxyMessage.builder().type(ProxyMessage.TYPE_TRANSFER).data(bytes).build();
        String serialNumber = ctx.channel().attr(Constants.CHANNEL_ID).get();
        proxyMessage.setSerialNumber(serialNumber);
        this.channel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //设置为非可读状态,等待代理服务成功建立连接之后再改变为可读状态
        ctx.channel().config().setOption(ChannelOption.AUTO_READ, false);
        //绑定channel
        String serialNumber = UUID.randomUUID().toString().replace("-","");
        ChannelHolder.addIdChannel(serialNumber,ctx.channel());
        ctx.channel().attr(Constants.CHANNEL_ID).set(serialNumber);
        //发送连接消息
        ProxyMessage proxyMessage = ProxyMessage.builder().type(ProxyMessage.TYPE_CONNECT).serialNumber(serialNumber).build();
        this.channel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("client has disConnected and will notify proxy client!");
        String channelId = ctx.channel().attr(Constants.CHANNEL_ID).get();
        ChannelHolder.removeIdChannel(channelId);
        this.channel.writeAndFlush(ProxyMessage.disconnectedMessage());
        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
       log.info("AUTO_READ status changed!");
        super.channelWritabilityChanged(ctx);
    }
}