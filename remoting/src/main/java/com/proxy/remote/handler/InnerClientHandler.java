package com.proxy.remote.handler;

import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.holder.ChannelHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InnerClientHandler  extends SimpleChannelInboundHandler<ByteBuf> {
    private String serialNumber ;
    public InnerClientHandler(String serialNumber){
        this.serialNumber = serialNumber ;
    }
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, ByteBuf byteBuf) throws Exception {
        String clientId = channelHandlerContext.channel().attr(Constants.ClIENT_ID).get();
        byte[] bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        ProxyMessage proxyMessage = new ProxyMessage();
        proxyMessage.setType(ProxyMessage.TYPE_TRANSFER);
        proxyMessage.setData(bytes);
        proxyMessage.setSerialNumber(serialNumber);
        Channel channel = ChannelHolder.getChannel(clientId);
        channel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }
}
