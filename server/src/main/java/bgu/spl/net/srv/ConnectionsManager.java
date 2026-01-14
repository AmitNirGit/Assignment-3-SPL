package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionsManager<T> implements Connections<T> {

    private AtomicInteger idIndex = new AtomicInteger(0);
    public HashMap<Integer, ConnectionHandler<T>> handlersById = new HashMap<>();
    public HashMap<String, ArrayList<Integer>> handlersIdsByChannels = new HashMap<>();

    public int addConnection(ConnectionHandler<T> connection){
        int connectionId = idIndex.incrementAndGet();
        handlersById.put(connectionId, connection);
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
        ArrayList<Integer> handlerIds = handlersIdsByChannels.get(channel);
        if (handlerIds != null) {
            for (int index = 0; index < handlerIds.size(); index++) {
                send(handlerIds.get(index), msg);
            }
        }
    }

    // TODO: When moving to SQL change the logic
    public void disconnect(int connectionId){
        ConnectionHandler<T> handler = handlersById.get(connectionId);
        if (handler != null) {
            try {
                handler.close();
            } catch (Exception e) {
                System.out.println("Failed to disconnect " + e);
            } finally {
                handlersById.remove(connectionId);
                for (String channel : handlersIdsByChannels.keySet()) {
                    if (handlersIdsByChannels.get(channel).contains(connectionId)) {
                        handlersIdsByChannels.get(channel).remove((Integer) connectionId);
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
}
