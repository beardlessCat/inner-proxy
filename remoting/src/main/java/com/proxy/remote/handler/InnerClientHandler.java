package com.proxy.remote.handler;

import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.holder.ChannelHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.Promise;
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
        ProxyMessage proxyMessage = ProxyMessage.builder()
                .type(ProxyMessage.TYPE_TRANSFER)
                .data(bytes)
                .serialNumber(serialNumber).build();
        Channel channel = ChannelHolder.getChannel(clientId);
        channel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ChannelHolder.addIdChannel(serialNumber,ctx.channel());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ChannelHolder.removeIdChannel(serialNumber);
        //不进行消息通知（不关闭client到exposeServer的连接），当有新的消息进入时，发现连接已经断开，将会重新启动客户端连接server。
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        try {
            if (evt instanceof IdleStateEvent) {
                log.debug("innerClient got idle event");
                ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }
        } finally {
            super.userEventTriggered(ctx, evt);
        }
    }
}
