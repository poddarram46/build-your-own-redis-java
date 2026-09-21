import java.io.*;
import java.net.*;
import java.util.Scanner;

class RedisClient {

    public static void main(String[] args) {

        try {

            Socket socket =
                    new Socket("localhost", 6379);

            BufferedReader input =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            PrintWriter output =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            Scanner scanner =
                    new Scanner(System.in);

            // Server se messages continuously receive karne ke liye
            Thread readerThread = new Thread(() -> {

                try {

                    String response;

                    while ((response = input.readLine()) != null) {

                        System.out.println();
                        System.out.println(response);
                        System.out.print("redis> ");
                    }

                } catch (IOException e) {

                    System.out.println(
                            "Connection closed."
                    );
                }
            });

            readerThread.setDaemon(true);
            readerThread.start();

            // User commands
            while (true) {

                System.out.print("redis> ");

                String command =
                        scanner.nextLine();

                output.println(command);

                if (command.equalsIgnoreCase("exit")) {

                    break;
                }
            }

            scanner.close();
            socket.close();

        } catch (IOException e) {

            System.out.println(
                    "Client error: "
                    + e.getMessage()
            );
        }
    }
}