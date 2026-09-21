import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class PubSubManager {

    // Channel -> subscribed clients
    private final ConcurrentHashMap<
            String,
            Set<ClientHandler>
            > subscribers =
            new ConcurrentHashMap<>();

    // =========================
    // SUBSCRIBE
    // =========================

    void subscribe(
            String channel,
            ClientHandler client) {

        subscribers
                .computeIfAbsent(
                        channel,
                        key -> ConcurrentHashMap.newKeySet()
                )
                .add(client);

        System.out.println(
                "Client subscribed to: " + channel
        );
    }

    // =========================
    // UNSUBSCRIBE
    // =========================

    void unsubscribe(
            String channel,
            ClientHandler client) {

        Set<ClientHandler> clients =
                subscribers.get(channel);

        if (clients == null) {
            return;
        }

        clients.remove(client);

        System.out.println(
                "Client unsubscribed from: " + channel
        );

        // Channel empty ho gaya
        if (clients.isEmpty()) {

            subscribers.remove(channel);
        }
    }

    // =========================
    // PUBLISH
    // =========================

    int publish(
            String channel,
            String message) {

        Set<ClientHandler> clients =
                subscribers.get(channel);

        if (clients == null ||
            clients.isEmpty()) {

            return 0;
        }

        int count = 0;

        for (ClientHandler client : clients) {

            client.sendMessage(
                    "Message [" + channel + "]: "
                    + message
            );

            count++;
        }

        return count;
    }

    // =========================
    // SUBSCRIBER COUNT
    // =========================

    int subscriberCount(
            String channel) {

        Set<ClientHandler> clients =
                subscribers.get(channel);

        if (clients == null) {
            return 0;
        }

        return clients.size();
    }
}