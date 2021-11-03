package com.proxy;


import com.proxy.callback.CallBack;
import com.proxy.config.NettyServerConfig;
import com.proxy.remote.AbstractNettyRemoting;
import com.proxy.remote.NettyRemotingServer;

import java.net.InetSocketAddress;

public class ServerController {
    private AbstractNettyRemoting abstractNettyRemoting ;


    public ServerController(CallBack callBack,int port) {
        this.abstractNettyRemoting  = new NettyRemotingServer(callBack,new InetSocketAddress(port)).init();
    }

    public void start() {
        abstractNettyRemoting.run();
    }

    public void shutdown() {
        abstractNettyRemoting.shutdownGracefully();
    }
}
