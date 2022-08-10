package com.proxy.remote;

import com.proxy.ClientInfo;
import com.proxy.ProxyMessageDecoder;
import com.proxy.ProxyMessageEncoder;
import com.proxy.callback.CallBack;
import com.proxy.config.NettyClientConfig;
import com.proxy.handler.IdleCheckHandler;
import com.proxy.remote.handler.ProxyClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
@Slf4j
public class NettyRemotingClient extends AbstractNettyRemoting{
    private  NioEventLoopGroup nioEventLoopGroup ;
    private  Bootstrap bootstrap = new Bootstrap();
    private  CallBack callBack ;
    private ClientInfo clientInfo ;
    private InetSocketAddress inetSocketAddress ;
    private ChannelInitializer channelInitializer;

    public NettyRemotingClient(CallBack callBack, ClientInfo clientInfo) {
        this.callBack =callBack ;
        this.clientInfo = clientInfo ;
        this.inetSocketAddress =  new InetSocketAddress(clientInfo.getPServerHost(),clientInfo.getPServerPort());
        this.channelInitializer = getChannelInitializer();
    }

    public NettyRemotingClient( ChannelInitializer channelInitializer , CallBack callBack, String innerHost ,int innerPort) {
        this.callBack =callBack ;
        inetSocketAddress =  new InetSocketAddress(innerHost,innerPort);
        this.channelInitializer = channelInitializer;
    }

    public NettyRemotingClient group(){
        this.nioEventLoopGroup = new NioEventLoopGroup(
                NettyClientConfig.clientWorkerThreads,
                new ThreadFactory() {
                    private AtomicInteger threadIndex = new AtomicInteger(0);
                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "NettyClientWorkerThread_" + this.threadIndex.incrementAndGet());
                    }
                });
        this.bootstrap.group(this.nioEventLoopGroup);
        return this;
    }

    public NettyRemotingClient group(EventLoop eventLoop){
        this.bootstrap.group(eventLoop);
        return this;
    }

    public NettyRemotingClient init(boolean autoRead){
        this.bootstrap.channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.AUTO_READ, autoRead)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, NettyClientConfig.connectTimeoutMillis)
                .option(ChannelOption.SO_SNDBUF, NettyClientConfig.clientSocketSndBufSize)
                .option(ChannelOption.SO_RCVBUF, NettyClientConfig.clientSocketRcvBufSize)
                .handler(this.channelInitializer);
        return this;
    }

    @Override
    public void run() {
        try {
            this.bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener)future -> {
                if(future.isSuccess()){
                    callBack.success(future.channel());
                }else {
                    callBack.error();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            callBack.error();
        }
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
