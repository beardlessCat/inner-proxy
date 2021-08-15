package com.proxy.callback;

import io.netty.channel.ChannelFuture;

public interface CallBack {

    void success(ChannelFuture channelFuture);

    void error();

}
