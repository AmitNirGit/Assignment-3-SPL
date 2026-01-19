package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsManager<T> implements Connections<T> {

    private AtomicInteger idIndex = new AtomicInteger(0);
    // Connection ID -> Handler
    private HashMap<Integer, ConnectionHandler<T>> handlersById = new HashMap<>();
    
    // Topic -> <Connection ID -> subscriptionId>
    private HashMap<String, Map<Integer, String>> topicToConnectionIdAndSubId = new HashMap<>();

    // connectionId -> <topic -> subscriptionId>
    private HashMap<Integer, Map<String, String>> connectionIdToTopicAndSubId = new HashMap<>();

    // subscriptionId -> connectionId
    private HashMap<String, Integer> subscriptionIdToConnectionId = new HashMap<>();
    
    public int addConnection(ConnectionHandler<T> connectionHandler){
        int connectionId = idIndex.incrementAndGet();
        handlersById.put(connectionId, connectionHandler);
        return connectionId;
    }

    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = handlersById.get(connectionId);

        if (handler != null) {
            try {
                handler.send(msg);
                return true;
            } catch (Exception e) {
                System.out.println("Failed to send message " + e);
                return false;
            }
        }
        return false;
    }

    public void send(String channel, T msg) {
        Map<Integer, String> subscribers = topicToConnectionIdAndSubId.get(channel);
        if (subscribers != null) {
            // Send to each subscribed connection
            for (Integer connectionId : subscribers.keySet()) {
                send(connectionId, msg);
            }
        }
    }

    public synchronized void disconnect(int connectionId){
        ConnectionHandler<T> handler = handlersById.get(connectionId);
        if (handler != null) {
            try {
                handler.close();
            } catch (Exception e) {
                System.out.println("Failed to disconnect " + e);
            } finally {
                handlersById.remove(connectionId);
                
                // Remove all subscriptions for this connection
                Map<String, String> topicToSubId = connectionIdToTopicAndSubId.remove(connectionId);
                if (topicToSubId != null) {
                    for (Map.Entry<String, String> entry : topicToSubId.entrySet()) {
                        String topic = entry.getKey();
                        String subscriptionId = entry.getValue();
                        
                        // Remove from topic map
                        Map<Integer, String> subscribers = topicToConnectionIdAndSubId.get(topic);
                        if (subscribers != null) {
                            subscribers.remove(connectionId);
                            if (subscribers.isEmpty()) {
                                topicToConnectionIdAndSubId.remove(topic);
                            }
                        }
                        
                        // Remove from subscription ID lookup
                        subscriptionIdToConnectionId.remove(subscriptionId);
                    }
                }
            }
        }
    }

    public void disconnectAll(){
        ArrayList<Integer> connectionIds = new ArrayList<>(handlersById.keySet());
        for (Integer connectionId : connectionIds) {
            disconnect(connectionId);
        }
    }

    public synchronized void subscribe(int connectionId, String destination, String subscriptionId) {
        if (!handlersById.containsKey(connectionId)) {
            return; // Connection doesn't exist
        }
        
        // Add to topic -> connectionId map
        topicToConnectionIdAndSubId.putIfAbsent(destination, new HashMap<>());
        topicToConnectionIdAndSubId.get(destination).put(connectionId, subscriptionId);
        
        // Add to connectionId -> topic map
        connectionIdToTopicAndSubId.putIfAbsent(connectionId, new HashMap<>());
        connectionIdToTopicAndSubId.get(connectionId).put(destination, subscriptionId);
        
        // Add to subscription ID lookup
        subscriptionIdToConnectionId.put(subscriptionId, connectionId);
    }

    public synchronized void unsubscribe(String subscriptionId) {
        Integer connectionId = subscriptionIdToConnectionId.remove(subscriptionId);
        
        if (connectionId != null) {
            // Find the topic for this subscription
            Map<String, String> topicToSubId = connectionIdToTopicAndSubId.get(connectionId);
            if (topicToSubId != null) {
                String topicToRemove = null;
                
                // Find which topic has this subscription ID
                for (Map.Entry<String, String> entry : topicToSubId.entrySet()) {
                    if (entry.getValue().equals(subscriptionId)) {
                        topicToRemove = entry.getKey();
                        break;
                    }
                }
                
                if (topicToRemove != null) {
                    // Remove from connectionId -> topic map
                    topicToSubId.remove(topicToRemove);
                    if (topicToSubId.isEmpty()) {
                        connectionIdToTopicAndSubId.remove(connectionId);
                    }
                    
                    // Remove from topic -> connectionId map
                    Map<Integer, String> subscribers = topicToConnectionIdAndSubId.get(topicToRemove);
                    if (subscribers != null) {
                        subscribers.remove(connectionId);
                        if (subscribers.isEmpty()) {
                            topicToConnectionIdAndSubId.remove(topicToRemove);
                        }
                    }
                }
            }
        }
    }

    public boolean isSubscribedToTopic(int connectionId, String topic) {
        Map<String, String> topicToSubId = connectionIdToTopicAndSubId.get(connectionId);
        return topicToSubId != null && topicToSubId.containsKey(topic);
    }
}
