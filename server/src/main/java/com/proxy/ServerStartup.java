package com.proxy;

import com.proxy.callback.MsgCallBack;
import sun.tools.jar.CommandLine;

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
        ServerController serverController = new ServerController(new MsgCallBack());
        return serverController;
    }
}
