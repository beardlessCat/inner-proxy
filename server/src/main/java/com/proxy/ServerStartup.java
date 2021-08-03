package com.proxy;

import com.proxy.callback.CallBack;
import lombok.extern.slf4j.Slf4j;
import sun.tools.jar.CommandLine;
@Slf4j
public class ServerStartup {

    private static CommandLine commandLine = null;

    public static void main(String[] args) {
        main0(args);
    }

    private static void main0(String[] args) {
        ServerController serverController = createServerController(args);
        start(serverController);
    }

    private static void start(ServerController serverController) {
        serverController.start();
    }

    private static ServerController createServerController(String[] args) {
        //process serverConfig
        ServerController serverController = new ServerController(new CallBack() {
            @Override
            public void success() {
                log.info("proxyServer has started successfully!");
            }
            @Override
            public void error() {
                log.error("proxyServer startup  failed!");
            }
        });
        return serverController;
    }
}
