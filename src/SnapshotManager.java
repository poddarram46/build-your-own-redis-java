import java.io.*;
import java.util.Map;

class SnapshotManager {

    private String fileName;

    SnapshotManager(String fileName) {
        this.fileName = fileName;
    }

    // =========================
    // SAVE SNAPSHOT
    // =========================

    void save(
            Map<String, Object> database,
            Map<String, Long> expiryTimes) {

        try (
            BufferedWriter writer =
                    new BufferedWriter(
                            new FileWriter(fileName)
                    )
        ) {

            for (Map.Entry<String, Object> entry
                    : database.entrySet()) {

                String key =
                        entry.getKey();

                Object value =
                        entry.getValue();

                Long expiry =
                        expiryTimes.get(key);

                if (expiry != null) {

                    writer.write(
                            "DATA "
                            + key
                            + " "
                            + value
                            + " "
                            + expiry
                    );

                } else {

                    writer.write(
                            "DATA "
                            + key
                            + " "
                            + value
                    );
                }

                writer.newLine();
            }

            System.out.println(
                    "Snapshot saved!"
            );

        } catch (IOException e) {

            System.out.println(
                    "Snapshot save error: "
                    + e.getMessage()
            );
        }
    }

    // =========================
    // LOAD SNAPSHOT
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

            String line;

            while ((line = reader.readLine())
                    != null) {

                String[] parts =
                        line.trim()
                            .split("\\s+");

                if (parts.length >= 3 &&
                    parts[0].equals("DATA")) {

                    String key =
                            parts[1];

                    String value =
                            parts[2];

                    redis.setFromSnapshot(
                            key,
                            value
                    );

                    // TTL restore
                    if (parts.length == 4) {

                        try {

                            long expiryTime =
                                    Long.parseLong(
                                            parts[3]
                                    );

                            redis.restoreExpiry(
                                    key,
                                    expiryTime
                            );

                        } catch (
                                NumberFormatException e) {

                            System.out.println(
                                    "Invalid expiry time for "
                                    + key
                            );
                        }
                    }
                }
            }

            System.out.println(
                    "Snapshot loaded!"
            );

        } catch (IOException e) {

            System.out.println(
                    "Snapshot load error: "
                    + e.getMessage()
            );
        }
    }
}