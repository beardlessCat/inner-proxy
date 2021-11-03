package com.proxy;

import com.proxy.callback.CallBack;
import com.proxy.metrics.Flow;
import com.proxy.metrics.FlowManager;
import com.proxy.thread.ShutdownHookThread;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.util.Assert;
import sun.tools.jar.CommandLine;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ServerStartup {

    private static CommandLine commandLine = null;

    public static void main(String[] args) throws ParseException {
        main0(args);
    }

    private static void main0(String[] args) throws ParseException {
        // -p_server_port 18080
        Options options = new Options();
        options.addOption("h", false, "Help");
        options.addOption("p_server_port", true, "P-Server port");

        CommandLineParser parser = new DefaultParser();
        org.apache.commons.cli.CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("h")) {
            // print help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {
            String pServerPort = cmd.getOptionValue("p_server_port");
            Assert.notNull(pServerPort, "p_server_port must not be null!");
            ServerController serverController = createServerController(Integer.parseInt(pServerPort));
            start(serverController);
        }
    }

    private static void start(ServerController serverController) {
        //register serverShutdownHook
        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                serverController.shutdown();
                return null;
            }
        }));
        serverController.start();
    }

    private static ServerController createServerController(int port) {
        //process serverConfig
        ServerController serverController = new ServerController(new CallBack() {
            @Override
            public void success(Channel channel) {
                log.info("proxyServer has started successfully!");
                //打印流量信息
                new Thread(()->{
                    while (true){
                        List<Flow> allMetrics = FlowManager.getAllMetrics();
                        allMetrics.stream().forEach(flow -> {
                            log.debug("exposeServer on {}, total read:{},total write:{}",flow.getPort(),(flow.getReadBytes()/1024) +"KB",(flow.getWroteBytes()>>10) +"KB");
                        });
                        log.debug("=======================================================");
                        try {
                            TimeUnit.SECONDS.sleep(60);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            @Override
            public void error() {
                log.error("proxyServer startup  failed!");
            }
        },port);
        return serverController;
    }
}
