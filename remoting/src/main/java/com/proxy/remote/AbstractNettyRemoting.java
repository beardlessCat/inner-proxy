package com.proxy.remote;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.epoll.Epoll;

public abstract class AbstractNettyRemoting {
    protected static final int MAX_FRAME_LENGTH = 2 * 1024 * 1024;

    protected static final int LENGTH_FIELD_OFFSET = 0;

    protected static final int LENGTH_FIELD_LENGTH = 4;

    protected static final int INITIAL_BYTES_TO_STRIP = 0;

    protected static final int LENGTH_ADJUSTMENT = 0;

    protected ChannelFuture future ;

    public abstract ChannelFuture run();

    public void close(){
        try {
            future.channel().close().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            shutdownGracefully();
        }
    }

    public abstract void shutdownGracefully();

    public abstract ChannelInitializer<Channel> getChannelInitializer();
    /**
     * 是否使用epoll
     * @return
     */
    protected boolean useEpoll() {
        return  Epoll.isAvailable();
    }
}
