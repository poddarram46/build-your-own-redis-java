import java.io.*;
import java.net.*;
import java.util.List;

class ClientHandler implements Runnable {

    private Socket clientSocket;
    private RedisEngine redis;
    private TransactionManager transaction;
    private PubSubManager pubSub;

    private PrintWriter output;

    ClientHandler(
            Socket clientSocket,
            RedisEngine redis,
            PubSubManager pubSub) {

        this.clientSocket = clientSocket;
        this.redis = redis;
        this.pubSub = pubSub;

        this.transaction =
                new TransactionManager();
    }

    // Pub/Sub ke liye client ko message bhejna
    void sendMessage(String message) {

        if (output != null) {
            output.println(message);
        }
    }

    @Override
    public void run() {

        try (
            BufferedReader input =
                    new BufferedReader(
                            new InputStreamReader(
                                    clientSocket.getInputStream()
                            )
                    );

            PrintWriter writer =
                    new PrintWriter(
                            clientSocket.getOutputStream(),
                            true
                    )
        ) {

            output = writer;

            output.println("Welcome to Mini Redis!");

            String command;

            while ((command = input.readLine()) != null) {

                command = command.trim();

                if (command.isEmpty()) {
                    continue;
                }

                // EXIT
                if (command.equalsIgnoreCase("exit")) {

                    output.println("Bye!");
                    break;
                }

                // =========================
                // PUB/SUB
                // =========================

                String[] parts =
                        command.split("\\s+", 3);

                String firstCommand =
                        parts[0].toUpperCase();

                // SUBSCRIBE channel
                if (firstCommand.equals("SUBSCRIBE")) {

                    if (parts.length != 2) {

                        output.println(
                                "ERROR: SUBSCRIBE channel"
                        );

                        continue;
                    }

                    pubSub.subscribe(
                            parts[1],
                            this
                    );

                    output.println(
                            "Subscribed to "
                            + parts[1]
                    );

                    continue;
                }

                // UNSUBSCRIBE channel
                if (firstCommand.equals("UNSUBSCRIBE")) {

                    if (parts.length != 2) {

                        output.println(
                                "ERROR: UNSUBSCRIBE channel"
                        );

                        continue;
                    }

                    pubSub.unsubscribe(
                            parts[1],
                            this
                    );

                    output.println(
                            "Unsubscribed from "
                            + parts[1]
                    );

                    continue;
                }

                // PUBLISH channel message
                if (firstCommand.equals("PUBLISH")) {

                    if (parts.length != 3) {

                        output.println(
                                "ERROR: PUBLISH channel message"
                        );

                        continue;
                    }

                    int count =
                            pubSub.publish(
                                    parts[1],
                                    parts[2]
                            );

                    output.println(count);

                    continue;
                }

                // =========================
                // TRANSACTIONS
                // =========================

                if (command.equalsIgnoreCase("MULTI")) {

                    output.println(
                            transaction.multi()
                    );

                    continue;
                }

                if (command.equalsIgnoreCase("EXEC")) {

                    List<String> commands =
                            transaction.exec();

                    if (commands == null) {

                        output.println(
                                "ERROR: MULTI required"
                        );

                        continue;
                    }

                    for (String queuedCommand : commands) {

                        String result =
                                redis.executeCommand(
                                        queuedCommand
                                );

                        output.println(result);
                    }

                    continue;
                }

                if (command.equalsIgnoreCase("DISCARD")) {

                    output.println(
                            transaction.discard()
                    );

                    continue;
                }

                // Transaction ke andar command queue
                if (transaction.isActive()) {

                    output.println(
                            transaction.queueCommand(
                                    command
                            )
                    );

                    continue;
                }

                // Normal Redis command
                String result =
                        redis.executeCommand(command);

                output.println(result);
            }

        } catch (IOException e) {

            System.out.println(
                    "Client disconnected: "
                    + e.getMessage()
            );

        } finally {

            try {
                clientSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }
}