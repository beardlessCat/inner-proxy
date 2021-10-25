package com.proxy.callback;

import io.netty.channel.Channel;

public interface CallBack {

    void success(Channel channel);

    void error();

}
