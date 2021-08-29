package com.proxy.metrics.handler;

import com.proxy.metrics.FlowManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.net.InetSocketAddress;

public class BytesFlowHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        FlowManager flowManager = FlowManager.getCollector(sa.getPort());
        flowManager.incrementReadBytes(((ByteBuf) msg).readableBytes());
        flowManager.incrementReadMsgs(1);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        FlowManager flowManager = FlowManager.getCollector(sa.getPort());
        flowManager.incrementWroteBytes(((ByteBuf) msg).readableBytes());
        flowManager.incrementWroteMsgs(1);
        super.write(ctx, msg, promise);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        FlowManager.getCollector(sa.getPort()).getChannels().incrementAndGet();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress sa = (InetSocketAddress) ctx.channel().localAddress();
        FlowManager.getCollector(sa.getPort()).getChannels().decrementAndGet();
        super.channelInactive(ctx);
    }
}
