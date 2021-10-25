package com.proxy;

import com.proxy.callback.CallBack;
import com.proxy.thread.ShutdownHookThread;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.springframework.util.Assert;

import java.util.concurrent.Callable;

@Slf4j
public class ClientStartup {


    public static void main(String[] args) throws ParseException {
        main0(args);
    }

    private static void main0(String[] args) throws ParseException {
        // -p_server_host 127.0.0.1
        // -p_server_port 18080
        // -client_id 0001
        // -client_secret 123456
        // -expose_server_host 127.0.0.1
        // -expose_server_port 8888
        // -inner_host 127.0.0.1
        // -inner_port 80

        Options options = new Options();
        options.addOption("h", false, "Help");
        options.addOption("p_server_host", true, "P-Server host");
        options.addOption("p_server_port", true, "P-Server port");
        options.addOption("client_id", true, "client_id to auth");
        options.addOption("client_secret", true, "client_secret to auth");
        options.addOption("expose_server_host", true, "Expose-Server host");
        options.addOption("expose_server_port", true, "Expose-Server port");
        options.addOption("inner_host", true, "inner host");
        options.addOption("inner_port", true, "inner port");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("h")) {
            // print help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("options", options);
        } else {
            ClientInfo.ClientInfoBuilder builder = ClientInfo.builder();
            String pServerHost = cmd.getOptionValue("p_server_host");
            Assert.notNull(pServerHost,"p_server_host must not be null!");
            builder.pServerHost(pServerHost);

            String pServerPort = cmd.getOptionValue("p_server_port");
            Assert.notNull(pServerPort,"p_server_port must not be null!");
            builder.pServerPort(Integer.parseInt(pServerPort));

            String clientId = cmd.getOptionValue("client_id");
            Assert.notNull(clientId,"client_id must not be null!");
            builder.clientId(clientId);

            String clientSecret = cmd.getOptionValue("client_secret");
            Assert.notNull(clientSecret,"client_secret must not be null!");
            builder.clientSecret(clientSecret);

            String exposeServerHost = cmd.getOptionValue("expose_server_host");
            Assert.notNull(exposeServerHost,"expose_server_host must not be null!");
            builder.exposeServerHost(exposeServerHost);

            String exposeServerPort = cmd.getOptionValue("expose_server_port");
            Assert.notNull(exposeServerPort,"expose_server_port must not be null!");
            builder.exposeServerPort(Integer.parseInt(exposeServerPort));

            String innerHost = cmd.getOptionValue("inner_host");
            Assert.notNull(innerHost,"inner_host must not be null!");
            builder.innerHost(innerHost);

            String innerPort = cmd.getOptionValue("inner_port");
            Assert.notNull(innerPort,"inner_port must not be null!");
            builder.innerPort(Integer.parseInt(innerPort));

            ClientController clientController = createServerController(builder.build());
            start(clientController);
       }
    }

    private static void start(ClientController clientController) {
        //register serverShutdownHook
        Runtime.getRuntime().addShutdownHook(new ShutdownHookThread(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                clientController.shutdown();
                return null;
            }
        }));
        clientController.start();
    }

    private static ClientController createServerController(ClientInfo clientInfo) throws ParseException {
        ClientController serverController = new ClientController(new CallBack() {
            @Override
            public void success(Channel channel) {
                log.info("\n----------------------------------------------------------   \n\t" +
                        "proxyClient({}) has started successfully! Access URLs:         \n\t" +
                        "ExposeUrl: \t\thttp://{}:{}/                                    \n\t" +
                        "InnerUrl: \t\thttp://{}:{}/                                     \n\t" +
                        "For example, the user access address is http://{}:{}/index,     \n\t" +
                        "             and the actual access address is http://{}:{}/index        \n\t" +
                        "----------------------------------------------------------",clientInfo.getClientId(),clientInfo.getExposeServerHost(),clientInfo.getExposeServerPort(),clientInfo.getInnerHost(),clientInfo.getInnerPort(),clientInfo.getExposeServerHost(),clientInfo.getExposeServerPort(),clientInfo.getInnerHost(),clientInfo.getInnerPort());
            }
            @Override
            public void error() {
                log.error("Exception occurred when proxyClient is connecting to proxyServer!");
            }
        }, clientInfo);
        return serverController;
    }
}
