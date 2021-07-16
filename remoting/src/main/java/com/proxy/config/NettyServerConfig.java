package com.proxy.config;

import lombok.Data;

@Data
public class NettyServerConfig {

    public static SocketConfig socketConfig = new SocketConfig();

    public static int bossThreads = 1;

    public static int workerThreads = 0;

    public static int port = 18080;
}
