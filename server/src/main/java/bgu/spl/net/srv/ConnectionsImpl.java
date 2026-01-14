package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.HashMap;

public class ConnectionsImpl<T> implements Connections<T> {

    public HashMap<Integer, BlockingConnectionHandler<T>> handlersById = new HashMap<>();
    public HashMap<String, ArrayList<Integer>> handlersIdsByChannels = new HashMap<>();

    public boolean send(int connectionId, T msg) {
        BlockingConnectionHandler<T> handler = handlersById.get(connectionId);

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
        BlockingConnectionHandler<T> handler = handlersById.get(connectionId);
        if (handler != null) {
            try {
                handler.close();
            } catch (Exception e) {
                System.out.println("Failed to disconnect " + e);
            } finally {
                handlersById.remove(connectionId);
                handlersIdsByChannels.remove(connectionId);
                for (String channel : handlersIdsByChannels.keySet()) {
                    if (handlersIdsByChannels.get(channel).contains(connectionId)) {
                        handlersIdsByChannels.get(channel).remove(connectionId);
                    }
                }
            }
        }
    }
}
