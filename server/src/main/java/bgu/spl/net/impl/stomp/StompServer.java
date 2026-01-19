package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7777;
        String serverType = args.length > 1 ? args[1].toLowerCase() : "reactor";

        System.out.println("Stomp Server starting on port " + port + " with " + serverType + " pattern");

        if ("tpc".equals(serverType)) {
            Server.threadPerClient(port,
                    () -> new StompProtocol(),
                    () -> new StompEncoderDecoder())
                    .serve();
        } else {
            Server.reactor(
                    Runtime.getRuntime().availableProcessors(),
                    port,
                    () -> new StompProtocol(),
                    () -> new StompEncoderDecoder())
                    .serve();
        }
    }

}
