package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Server;
import bgu.spl.net.impl.stomp.StompProtocol;

public class StompServer {

    public static void main(String[] args) {
       // you can use any server... 
       System.out.println("Stomp Server started");
       Server.threadPerClient(
               7777, //port
               () -> new StompProtocol(), //protocol factory
               StompEncoderDecoder::new //message encoder decoder factory
       ).serve();
    }
}
