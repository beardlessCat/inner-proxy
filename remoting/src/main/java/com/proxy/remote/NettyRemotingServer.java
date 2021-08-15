package com.proxy.remote;

import com.proxy.ProxyMessageDecoder;
import com.proxy.ProxyMessageEncoder;
import com.proxy.callback.CallBack;
import com.proxy.config.NettyServerConfig;
import com.proxy.config.SocketConfig;
import com.proxy.handler.IdleCheckHandler;
import com.proxy.remote.handler.ProxyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
@Slf4j
public class NettyRemotingServer extends AbstractNettyRemoting{
    private EventLoopGroup eventLoopGroupWorker;
    private EventLoopGroup eventLoopGroupBoss;
    private CallBack callBack;
    private ServerBootstrap serverBootstrap = new ServerBootstrap();
    private  InetSocketAddress inetSocketAddress ;
    private ChannelInitializer channelInitializer ;


    public NettyRemotingServer(CallBack callBack,InetSocketAddress inetSocketAddress) {
        this.callBack = callBack;
        this.inetSocketAddress = inetSocketAddress ;
        this.channelInitializer = getChannelInitializer();
    }

    public NettyRemotingServer( ChannelInitializer channelInitializer , CallBack callBack ,InetSocketAddress inetSocketAddress ) {
        this(callBack,inetSocketAddress);
        this.channelInitializer = channelInitializer;
    }
    @Override
    public NettyRemotingServer init() {
        initEventLoopGroup();
        this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupWorker)
                .channel(useEpoll()? EpollServerSocketChannel.class: NioServerSocketChannel.class)
                .childHandler(this.channelInitializer)
                .localAddress(this.inetSocketAddress);
        applyConnectionOptions(this.serverBootstrap);
        return this;
    }


    @Override
    public ChannelFuture run() {
        ChannelFuture channelFuture = null;
        try {
            channelFuture = serverBootstrap.bind().sync();
            future = channelFuture;
            callBack.success(channelFuture);
        } catch (InterruptedException e) {
            callBack.error();
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e);
        }
        log.info("proxyServer has started on port " +channelFuture.channel().localAddress());
        channelFuture.channel().closeFuture().addListener(future -> {
           shutdownGracefully();
        });
        return channelFuture;
    }

    @Override
    public void shutdownGracefully() {
        eventLoopGroupBoss.shutdownGracefully();
        eventLoopGroupWorker.shutdownGracefully();
    }

    @Override
    public ChannelInitializer<Channel> getChannelInitializer() {
        return new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                channel.pipeline().addLast(new ProxyMessageDecoder(MAX_FRAME_LENGTH, LENGTH_FIELD_OFFSET, LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP));
                channel.pipeline().addLast(new ProxyMessageEncoder());
                channel.pipeline().addLast(new IdleCheckHandler(IdleCheckHandler.READ_IDLE_TIME, IdleCheckHandler.WRITE_IDLE_TIME, 0));
                channel.pipeline().addLast(new ProxyServerHandler());
            }
        };
    }

    /**
     * 配置连接属性
     * @param bootstrap
     */
    private void applyConnectionOptions(ServerBootstrap bootstrap) {
        SocketConfig config = NettyServerConfig.socketConfig;
        bootstrap.childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay());
        if (config.getTcpSendBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, config.getTcpSendBufferSize());
        }
        if (config.getTcpReceiveBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, config.getTcpReceiveBufferSize());
            bootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(config.getTcpReceiveBufferSize()));
        }
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, config.isTcpKeepAlive());
        bootstrap.childOption(ChannelOption.SO_LINGER, config.getSoLinger());
        bootstrap.option(ChannelOption.SO_REUSEADDR, config.isReuseAddress());
        bootstrap.option(ChannelOption.SO_BACKLOG, config.getAcceptBackLog());
    }

    private void initEventLoopGroup() {
        if (useEpoll()) {
            this.eventLoopGroupWorker = new EpollEventLoopGroup(NettyServerConfig.bossThreads, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyEPOLLBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });
            eventLoopGroupBoss = new EpollEventLoopGroup(NettyServerConfig.workerThreads, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = NettyServerConfig.workerThreads;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerEPOLLSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
        } else {
            this.eventLoopGroupWorker = new NioEventLoopGroup(NettyServerConfig.bossThreads, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyNIOBoss_%d", this.threadIndex.incrementAndGet()));
                }
            });
            this.eventLoopGroupBoss = new NioEventLoopGroup(NettyServerConfig.workerThreads, new ThreadFactory() {
                private AtomicInteger threadIndex = new AtomicInteger(0);
                private int threadTotal = NettyServerConfig.workerThreads;

                @Override
                public Thread newThread(Runnable r) {
                    return new Thread(r, String.format("NettyServerNIOSelector_%d_%d", threadTotal, this.threadIndex.incrementAndGet()));
                }
            });
        }
    }
}
