package com.proxy;

import com.proxy.callback.MsgCallBack;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

@Slf4j
public class ClientStartup {


    public static void main(String[] args) throws ParseException {
        main0(args);
    }

    private static void main0(String[] args) throws ParseException {
        //-p_server_host 127.0.0.1 -p_server_port 18080 -client_id 0001  -client_secret 123456 -expose_server_host 127.0.0.1 -expose_server_port 8888 -inner_host 127.0.0.1 -inner_port 80

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
            if (pServerHost == null) {
                System.out.println("p_server_host cannot be null");
                return;
            }
            builder.pServerHost(pServerHost);
            String pServerPort = cmd.getOptionValue("p_server_port");
            if (pServerPort == null) {
                System.out.println("p_server_port cannot be null");
                return;
            }
            builder.pServerPort(Integer.parseInt(pServerPort));
            String clientId = cmd.getOptionValue("client_id");
            if (clientId == null) {
                System.out.println("client_id cannot be null");
                return;
            }
            builder.clientId(clientId);
            String clientSecret = cmd.getOptionValue("client_secret");
            if (clientSecret == null) {
                System.out.println("client_secret cannot be null");
                return;
            }
            builder.clientSecret(clientSecret);
            String exposeServerHost = cmd.getOptionValue("expose_server_host");
            if (exposeServerHost == null) {
                System.out.println("expose_server_host cannot be null");
                return;
            }
            builder.exposeServerHost(exposeServerHost);
            String exposeServerPort = cmd.getOptionValue("expose_server_port");
            if (exposeServerPort == null) {
                System.out.println("expose_server_port cannot be null");
                return;
            }
            builder.exposeServerPort(Integer.parseInt(exposeServerPort));

            String innerHost = cmd.getOptionValue("inner_host");
            if (innerHost == null) {
                System.out.println("inner_host cannot be null");
                return;
            }
            builder.innerHost(innerHost);
            String innerPort = cmd.getOptionValue("inner_port");
            if (innerPort == null) {
                System.out.println("inner_port cannot be null");
                return;
            }
            builder.innerPort(Integer.parseInt(innerPort));
            ClientController clientController = createServerController(builder.build());
            start(clientController);
       }
    }

    private static void start(ClientController clientController) {
        clientController.start();
    }

    private static ClientController createServerController(ClientInfo clientInfo) throws ParseException {
        ClientController serverController = new ClientController(new MsgCallBack(),clientInfo);
        return serverController;
    }
}
