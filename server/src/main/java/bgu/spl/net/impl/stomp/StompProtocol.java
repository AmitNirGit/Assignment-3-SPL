package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.ConnectionsManager;
import java.util.HashMap;
import java.util.Map;

public class StompProtocol implements StompMessagingProtocol<String> {
    
    private boolean shouldTerminate = false;
    private int connectionId = -1;
    private ConnectionsManager<String> connectionsManager;
    @Override
    public void process(String msg) {
        System.out.println("Processing msg: " + msg);
        msg = msg.trim();
        String[] lines = msg.split("\n");
        String command = lines[0];
        System.out.println("Processing command: " + command);
        switch (command) {
            case "CONNECT":
                handleConnect(lines);
                break;
            case "DISCONNECT":
                handleDisconnect(lines);
                break;
            case "SUBSCRIBE":
                handleSubscribe(lines);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(lines);
                break;
            case "SEND":
                handleSend(lines);
                break;
            default:
                sendErrorAndDisconnect("Unknown command", "Command not recognized: " + command);
                break;
        }


    }

    @Override
        public boolean shouldTerminate() {
            return shouldTerminate;
    }

    @Override
    public void start(int connectionId, Connections<String> connections) {
        this.connectionId = connectionId;
        this.connectionsManager = (ConnectionsManager<String>) connections;
    }

    private Map<String, String> parseHeaders(String[] lines) {
        Map<String, String> headers = new HashMap<>();
        
        // skip command line at index 0
        boolean inBody = false;
        String Body = "";
        for (int i = 1; i < lines.length; i++) {

            if (lines[i].isEmpty()) {
                inBody = true;
                continue;
            }
            if (lines[i].contains("\0")) {
                break;
            }
            if (inBody) {
                Body += lines[i];
            }
            // Split on first colon only (value might contain colons)
            String[] headerParts = lines[i].split(":", 2);
            if (headerParts.length == 2) {
                String headerName = headerParts[0].trim();
                String headerValue = headerParts[1].trim();
                headers.put(headerName, headerValue);
            }
        }
        headers.put("body", Body);
        return headers;
    }

    private void sendErrorAndDisconnect(String message, String details) {
        String error = "ERROR\nmessage:" + message + "\n\n" + details + "\0";
        connectionsManager.send(connectionId, error);
        shouldTerminate = true;
        System.out.println("Connection " + connectionId + " will disconnect due to error: " + message);
    }

    private void handleConnect(String[] lines) {
        Map<String, String> headers = parseHeaders(lines);
        
        String version = headers.get("accept-version");
        String login = headers.get("login");
        String passcode = headers.get("passcode");
        
        // TODO: add login and passcode to database
        System.out.println("add to database: Login: " + login + " Passcode: " + passcode);
        
        String msgToSend = "CONNECTED\nversion:" + version + "\n\n\0";
        connectionsManager.send(connectionId, msgToSend);
    }   

    private void handleDisconnect(String[] lines) {
        Map<String, String> headers = parseHeaders(lines);
        String receiptId = headers.get("receipt");
        
        // Send receipt before disconnecting
        if (receiptId != null) {
            String receipt = "RECEIPT\nreceipt-id:" + receiptId + "\n\n\0";
            connectionsManager.send(connectionId, receipt);
        }
        
        // Disconnect the client
        connectionsManager.disconnect(connectionId);
        shouldTerminate = true;
        System.out.println("Connection " + connectionId + " disconnected");
    }

    private void handleSubscribe(String[] lines) {
        Map<String, String> headers = parseHeaders(lines);
        
        String destination = headers.get("destination");
        String subscriptionId = headers.get("id");
        String receiptId = headers.get("receipt");
        
        if (destination != null && subscriptionId != null) {
            connectionsManager.subscribe(connectionId, destination,subscriptionId);
            System.out.println("Connection " + connectionId + " subscribed to " + destination + " with id " + subscriptionId);
            
            // Send receipt if requested
            if (receiptId != null) {
                String receipt = "RECEIPT\nreceipt-id:" + receiptId + "\n\n\0";
                connectionsManager.send(connectionId, receipt);
            }
        } else {
            // Send ERROR frame and disconnect
            sendErrorAndDisconnect("Missing required headers", "Missing destination or id header");
        }
    }

    private void handleUnsubscribe(String[] lines) {
        Map<String, String> headers = parseHeaders(lines);
        
        String subscriptionId = headers.get("id");
        String receiptId = headers.get("receipt");
        
        if (subscriptionId != null) {
            // Unregister the subscription (only needs subscription ID)
            connectionsManager.unsubscribe(subscriptionId);
            System.out.println("Connection " + connectionId + " unsubscribed with subscription id " + subscriptionId);
            
            // Send receipt if requested
            if (receiptId != null) {
                String receipt = "RECEIPT\nreceipt-id:" + receiptId + "\n\n\0";
                connectionsManager.send(connectionId, receipt);
            }
        } else {
            // Send ERROR frame and disconnect
            sendErrorAndDisconnect("Missing required headers", "Missing id header");
        }
    }

    private void handleSend(String[] lines) {
        Map<String, String> headers = parseHeaders(lines);
        
        String destination = headers.get("destination");
        String receiptId = headers.get("receipt");
        String body = headers.get("body");

        // Check if user is subscribed to the destination
        if (destination == null) {
            sendErrorAndDisconnect("Missing destination", "SEND frame must include a destination header");
            return;
        }

        if (!connectionsManager.isSubscribedToTopic(connectionId, destination)) {
            sendErrorAndDisconnect("Not subscribed", "You must be subscribed to " + destination + " to send messages");
            return;
        }

        System.out.println("Connection " + connectionId + " sending to destination: " + destination);
        
        // Forward message to all subscribers
        connectionsManager.send(destination, body);
        
        if (receiptId != null) {
            String receipt = "RECEIPT\nreceipt-id:" + receiptId + "\n\n\0";
            connectionsManager.send(connectionId, receipt);
        }
    }

    public int getConnectionId() {
        return connectionId;
    }
}
