package com.proxy;


import com.proxy.callback.CallBack;
import com.proxy.remote.AbstractNettyRemoting;
import com.proxy.remote.NettyRemotingClient;

public class ClientController {
    private AbstractNettyRemoting abstractNettyRemoting ;


    public ClientController(CallBack callBack, ClientInfo clientInfo) {
        this.abstractNettyRemoting  = new NettyRemotingClient(callBack,clientInfo);
    }

    public void start() {
        abstractNettyRemoting.run();
    }

    public void shutdown() {
        abstractNettyRemoting.shutdownGracefully();
    }
}
