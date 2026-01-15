package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;

public class StompProtocol implements StompMessagingProtocol<String> {
    
    private boolean shouldTerminate = false;

    @Override
    public void process(String msg) {
        // TODO: Implement STOMP protocol processing
        System.out.println("Received message: " + msg);
    }

    @Override
        public boolean shouldTerminate() {
            return shouldTerminate;
    }

    @Override
    public void start(int connectionId, Connections<String> connections) {
        // TODO: Implement start logic
    }
}
