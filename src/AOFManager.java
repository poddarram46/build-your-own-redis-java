import java.io.*;

class AOFManager {

    private String fileName;

    AOFManager(String fileName) {
        this.fileName = fileName;
    }

    // =========================
    // APPEND COMMAND
    // =========================

    synchronized void append(String command) {

        try (
            FileWriter writer =
                    new FileWriter(
                            fileName,
                            true
                    )
        ) {

            writer.write(command);
            writer.write(
                    System.lineSeparator()
            );

        } catch (IOException e) {

            System.out.println(
                    "AOF write error: "
                    + e.getMessage()
            );
        }
    }

    // =========================
    // LOAD AOF
    // =========================

    void load(RedisEngine redis) {

        File file =
                new File(fileName);

        if (!file.exists()) {
            return;
        }

        try (
            BufferedReader reader =
                    new BufferedReader(
                            new FileReader(file)
                    )
        ) {

            String command;

            while (
                    (command =
                            reader.readLine())
                            != null
            ) {

                replay(
                        command,
                        redis
                );
            }

        } catch (IOException e) {

            System.out.println(
                    "AOF load error: "
                    + e.getMessage()
            );
        }
    }

    // =========================
    // REPLAY AOF COMMAND
    // =========================

    private void replay(
            String command,
            RedisEngine redis) {

        String[] parts =
                command.trim()
                        .split("\\s+");

        if (parts.length == 0) {
            return;
        }

        String type =
                parts[0].toUpperCase();

        // =========================
        // SET
        // =========================

        if (
                type.equals("SET")
                && parts.length == 3
        ) {

            redis.setFromAOF(
                    parts[1],
                    parts[2]
            );
        }

        // =========================
        // DEL
        // =========================

        else if (
                type.equals("DEL")
                && parts.length == 2
        ) {

            redis.delFromAOF(
                    parts[1]
            );
        }

        // =========================
        // EXPIRE
        // =========================

        else if (
                type.equals("EXPIRE")
                && parts.length == 3
        ) {

            try {

                long seconds =
                        Long.parseLong(
                                parts[2]
                        );

                long expiryTime =
                        System.currentTimeMillis()
                        + (seconds * 1000);

                redis.restoreExpiry(
                        parts[1],
                        expiryTime
                );

            } catch (
                    NumberFormatException e) {

                System.out.println(
                        "Invalid EXPIRE command: "
                        + command
                );
            }
        }

        // =========================
        // EXPIREAT
        // =========================

        else if (
                type.equals("EXPIREAT")
                && parts.length == 3
        ) {

            try {

                long expiryTime =
                        Long.parseLong(
                                parts[2]
                        );

                redis.restoreExpiry(
                        parts[1],
                        expiryTime
                );

            } catch (
                    NumberFormatException e) {

                System.out.println(
                        "Invalid EXPIREAT command: "
                        + command
                );
            }
        }
    }
}