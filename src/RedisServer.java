import java.io.*;
import java.net.*;
import java.util.concurrent.*;

class RedisServer {

    private int port;
    private RedisEngine redis;
    private ExecutorService threadPool;

    // Shared Pub/Sub manager
    private PubSubManager pubSub;

    RedisServer(int port) {

        this.port = port;

        // Shared Redis database
        this.redis = new RedisEngine();

        // Shared Pub/Sub system
        this.pubSub = new PubSubManager();

        // Maximum 10 clients simultaneously
        this.threadPool =
                Executors.newFixedThreadPool(10);
    }

    void start() {

        try (ServerSocket serverSocket =
                     new ServerSocket(port)) {

            System.out.println(
                    "Mini Redis Server Started"
            );

            System.out.println(
                    "Port: " + port
            );

            System.out.println(
                    "Waiting for clients..."
            );

            while (true) {

                Socket clientSocket =
                        serverSocket.accept();

                System.out.println(
                        "Client connected!"
                );

                threadPool.execute(
                        new ClientHandler(
                                clientSocket,
                                redis,
                                pubSub
                        )
                );
            }

        } catch (IOException e) {

            System.out.println(
                    "Server error: "
                    + e.getMessage()
            );

        } finally {

            threadPool.shutdown();
        }
    }
}