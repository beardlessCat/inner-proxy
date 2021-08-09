package com.proxy;


import com.proxy.callback.CallBack;
import com.proxy.config.NettyServerConfig;
import com.proxy.remote.AbstractNettyRemoting;
import com.proxy.remote.NettyRemotingServer;

public class ServerController {
    private AbstractNettyRemoting abstractNettyRemoting ;


    public ServerController(CallBack callBack) {
        this.abstractNettyRemoting  = new NettyRemotingServer(callBack);
    }

    public void start() {
        abstractNettyRemoting.run();
    }

    public void shutdown() {
        abstractNettyRemoting.shutdownGracefully();
    }
}
