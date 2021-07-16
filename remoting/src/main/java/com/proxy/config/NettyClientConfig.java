package com.proxy.config;

import lombok.Data;

@Data
public  class NettyClientConfig {
   public static int clientWorkerThreads = 4;
   public static int clientCallbackExecutorThreads = Runtime.getRuntime().availableProcessors();
   public static int clientOnewaySemaphoreValue ;
   public static int clientAsyncSemaphoreValue ;
   public static int connectTimeoutMillis = 3000;
   public static long channelNotActiveInterval = 1000 * 60;
   public static int clientSocketSndBufSize =1024;
   public static int clientSocketRcvBufSize =1024;
}
