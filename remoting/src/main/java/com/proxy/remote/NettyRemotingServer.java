package com.proxy.remote;

import com.proxy.Constants;
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
        super.instanceName = Constants.PROXY_SERVER_NAME;
    }

    public NettyRemotingServer( ChannelInitializer channelInitializer , CallBack callBack ,InetSocketAddress inetSocketAddress ) {
        this(callBack,inetSocketAddress);
        this.channelInitializer = channelInitializer;
        super.instanceName = Constants.EXPOSE_SERVER_NAME;
    }
    public NettyRemotingServer init() {
        initEventLoopGroup();
        this.serverBootstrap.group(this.eventLoopGroupBoss, this.eventLoopGroupWorker)
                .channel(useEpoll()? EpollServerSocketChannel.class: NioServerSocketChannel.class)
                .childHandler(this.channelInitializer)
                .localAddress(this.inetSocketAddress);
        applyConnectionOptions();
        return this;
    }


    @Override
    public void run() {
        ChannelFuture channelFuture = null;
        try {
            channelFuture = serverBootstrap.bind().sync();
            callBack.success(channelFuture.channel());
        } catch (InterruptedException e) {
            callBack.error();
            throw new RuntimeException("this.serverBootstrap.bind().sync() InterruptedException", e);
        }
        log.info("proxyServer has started on port " +channelFuture.channel().localAddress());
        channelFuture.channel().closeFuture().addListener(future -> {
           shutdownGracefully();
        });
    }

    @Override
    public void shutdownGracefully() {
        eventLoopGroupBoss.shutdownGracefully();
        eventLoopGroupWorker.shutdownGracefully();
        log.info("{}-{}:{} has closed successful!",this.instanceName,inetSocketAddress.getAddress(),inetSocketAddress.getPort());
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
     */
    private void applyConnectionOptions() {
        SocketConfig config = NettyServerConfig.socketConfig;
        serverBootstrap.childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay());
        if (config.getTcpSendBufferSize() != -1) {
            serverBootstrap.childOption(ChannelOption.SO_SNDBUF, config.getTcpSendBufferSize());
        }
        if (config.getTcpReceiveBufferSize() != -1) {
            serverBootstrap.childOption(ChannelOption.SO_RCVBUF, config.getTcpReceiveBufferSize());
            serverBootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(config.getTcpReceiveBufferSize()));
        }
        serverBootstrap.childOption(ChannelOption.SO_KEEPALIVE, config.isTcpKeepAlive());
        serverBootstrap.childOption(ChannelOption.SO_LINGER, config.getSoLinger());
        serverBootstrap.option(ChannelOption.SO_REUSEADDR, config.isReuseAddress());
        serverBootstrap.option(ChannelOption.SO_BACKLOG, config.getAcceptBackLog());
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
