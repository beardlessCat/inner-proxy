package com.proxy.remote;

import com.proxy.ClientInfo;
import com.proxy.ProxyMessageDecoder;
import com.proxy.ProxyMessageEncoder;
import com.proxy.callback.CallBack;
import com.proxy.config.NettyClientConfig;
import com.proxy.handler.IdleCheckHandler;
import com.proxy.remote.handler.ProxyClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
@Slf4j
public class NettyRemotingClient extends AbstractNettyRemoting{
    private final NioEventLoopGroup nioEventLoopGroup ;
    private final Bootstrap bootstrap = new Bootstrap();
    private final CallBack callBack ;
    private ClientInfo clientInfo ;
    private InetSocketAddress inetSocketAddress ;

    public NettyRemotingClient(CallBack callBack, ClientInfo clientInfo) {
        this.callBack =callBack ;
        this.clientInfo = clientInfo ;
        inetSocketAddress =  new InetSocketAddress(clientInfo.getPServerHost(),clientInfo.getPServerPort());
        this.nioEventLoopGroup = new NioEventLoopGroup(
            NettyClientConfig.clientWorkerThreads,
            new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
                }
            });
        this.bootstrap.group(this.nioEventLoopGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, NettyClientConfig.connectTimeoutMillis)
                .option(ChannelOption.SO_SNDBUF, NettyClientConfig.clientSocketSndBufSize)
                .option(ChannelOption.SO_RCVBUF, NettyClientConfig.clientSocketRcvBufSize)
                .handler(getChannelInitializer() );
    }

    public NettyRemotingClient( ChannelInitializer channelInitializer , CallBack callBack, String innerHost ,int innerPort) {
        this.callBack =callBack ;
        inetSocketAddress =  new InetSocketAddress(innerHost,innerPort);
        this.nioEventLoopGroup = new NioEventLoopGroup(
                NettyClientConfig.clientWorkerThreads,
                new ThreadFactory() {
                    private AtomicInteger threadIndex = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
                    }
                });
        this.bootstrap.group(this.nioEventLoopGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, NettyClientConfig.connectTimeoutMillis)
                .option(ChannelOption.SO_SNDBUF, NettyClientConfig.clientSocketSndBufSize)
                .option(ChannelOption.SO_RCVBUF, NettyClientConfig.clientSocketRcvBufSize)
                .handler(channelInitializer);
    }
    @Override
    public ChannelFuture run() {
        ChannelFuture channelFuture = null;
        try {
            channelFuture = this.bootstrap.connect(inetSocketAddress).sync();
            channelFuture.addListener(future -> {
                if(future.isSuccess()){
                    callBack.success();
                }else {
                    callBack.error();
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        future = channelFuture;
        return channelFuture ;
    }

    @Override
    public void shutdownGracefully() {
        nioEventLoopGroup.shutdownGracefully();
    }

    @Override
    public ChannelInitializer<Channel> getChannelInitializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                channel.pipeline().addLast(new ProxyMessageEncoder());
                channel.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME - 10, 0));

                channel.pipeline().addLast(new ProxyClientHandler(clientInfo));
            }
        };
    }
}
