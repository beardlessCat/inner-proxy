package com.proxy.remote.handler;

import com.proxy.Constants;
import com.proxy.ProxyMessage;
import com.proxy.holder.ChannelHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 处理服务端 channel.
 */
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
        proxyMessage.setType(ProxyMessage.P_TYPE_TRANSFER);
        proxyMessage.setData(bytes);
        long serialNumber = snProducer.getAndIncrement();
        proxyMessage.setSerialNumber(serialNumber);
        ChannelHolder.addIdChannel(serialNumber,ctx.channel());
        this.channel.writeAndFlush(proxyMessage);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(this);
        //绑定channel
        Channel channel = ctx.channel();
        String channelId = ctx.channel().id().asLongText();

        this.channel.attr(Constants.EXPOSE_CHANNEL).set(channel);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("客户端连接断开！");
        // // 通知代理客户端
        // Channel userChannel = ctx.channel();
        // InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        // Channel cmdChannel = ProxyChannelManager.getCmdChannel(sa.getPort());
        // if (cmdChannel == null) {
        //
        //     // 该端口还没有代理客户端
        //     ctx.channel().close();
        // } else {
        //
        //     // 用户连接断开，从控制连接中移除
        //     String userId = ProxyChannelManager.getUserChannelUserId(userChannel);
        //     ProxyChannelManager.removeUserChannelFromCmdChannel(cmdChannel, userId);
        //     Channel proxyChannel = userChannel.attr(Constants.NEXT_CHANNEL).get();
        //     if (proxyChannel != null && proxyChannel.isActive()) {
        //         proxyChannel.attr(Constants.NEXT_CHANNEL).remove();
        //         proxyChannel.attr(Constants.CLIENT_KEY).remove();
        //         proxyChannel.attr(Constants.USER_ID).remove();
        //
        //         proxyChannel.config().setOption(ChannelOption.AUTO_READ, true);
        //         // 通知客户端，用户连接已经断开
        //         ProxyMessage proxyMessage = new ProxyMessage();
        //         proxyMessage.setType(ProxyMessage.TYPE_DISCONNECT);
        //         proxyMessage.setUri(userId);
        //         proxyChannel.writeAndFlush(proxyMessage);
        //     }
        // }

        super.channelInactive(ctx);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {

        // // 通知代理客户端
        // Channel userChannel = ctx.channel();
        // InetSocketAddress sa = (InetSocketAddress) userChannel.localAddress();
        // Channel cmdChannel = ChannelHolder.getChannel(sa.getPort());
        // if (cmdChannel == null) {
        //
        //     // 该端口还没有代理客户端
        //     ctx.channel().close();
        // } else {
        //     Channel proxyChannel = userChannel.attr(Constants.NEXT_CHANNEL).get();
        //     if (proxyChannel != null) {
        //         proxyChannel.config().setOption(ChannelOption.AUTO_READ, userChannel.isWritable());
        //     }
        // }
        //
        // super.channelWritabilityChanged(ctx);
    }

}