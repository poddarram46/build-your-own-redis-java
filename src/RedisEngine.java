import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RedisEngine {

    private final ConcurrentHashMap<String, Object> database =
            new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> expiryTimes =
            new ConcurrentHashMap<>();

    private final LRUCache cache =
            new LRUCache(100);

    private final RedisList redisList =
            new RedisList();

    private final RedisHash redisHash =
            new RedisHash();

    private final RedisSet redisSet =
            new RedisSet();

    private AOFManager aof;
    private SnapshotManager snapshot;

    RedisEngine() {

        aof = new AOFManager("redis.aof");
        snapshot = new SnapshotManager("redis.snapshot");

        snapshot.load(this);
        aof.load(this);
    }

    // =========================
    // STRING COMMANDS
    // =========================

    synchronized String set(
            String key,
            String value) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (value == null || value.isEmpty()) {
            return "ERROR: value cannot be empty";
        }

        database.put(key, value);
        cache.put(key, value);
        expiryTimes.remove(key);

        aof.append(
                "SET " + key + " " + value
        );

        return "OK";
    }

    synchronized void setFromAOF(
            String key,
            String value) {

        database.put(key, value);
        cache.put(key, value);
    }

    synchronized void setFromSnapshot(
            String key,
            String value) {

        database.put(key, value);
        cache.put(key, value);
    }

    synchronized void restoreExpiry(
            String key,
            long expiryTime) {

        if (!database.containsKey(key)) {
            return;
        }

        if (System.currentTimeMillis() >= expiryTime) {

            database.remove(key);
            cache.remove(key);
            expiryTimes.remove(key);

            return;
        }

        expiryTimes.put(
                key,
                expiryTime
        );
    }

    synchronized String get(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        checkExpiry(key);

        String cachedValue =
                cache.get(key);

        if (cachedValue != null) {
            return cachedValue;
        }

        Object value =
                database.get(key);

        if (value == null) {
            return "(nil)";
        }

        String result =
                value.toString();

        cache.put(
                key,
                result
        );

        return result;
    }

    synchronized String del(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        Object removed =
                database.remove(key);

        if (removed != null) {

            expiryTimes.remove(key);
            cache.remove(key);

            aof.append(
                    "DEL " + key
            );

            return "1";
        }

        return "0";
    }

    synchronized void delFromAOF(
            String key) {

        database.remove(key);
        expiryTimes.remove(key);
        cache.remove(key);
    }

    synchronized String exists(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        checkExpiry(key);

        return database.containsKey(key)
                ? "1"
                : "0";
    }

    synchronized String expire(
            String key,
            long seconds) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (seconds <= 0) {
            return "ERROR: seconds must be greater than 0";
        }

        if (!database.containsKey(key)) {
            return "0";
        }

        long expiryTime =
                System.currentTimeMillis()
                + (seconds * 1000);

        expiryTimes.put(
                key,
                expiryTime
        );

        aof.append(
                "EXPIREAT "
                + key
                + " "
                + expiryTime
        );

        return "1";
    }

    synchronized String ttl(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        checkExpiry(key);

        if (!database.containsKey(key)) {
            return "-2";
        }

        Long expiryTime =
                expiryTimes.get(key);

        if (expiryTime == null) {
            return "-1";
        }

        long remaining =
                (
                    expiryTime
                    - System.currentTimeMillis()
                ) / 1000;

        if (remaining < 0) {

            database.remove(key);
            expiryTimes.remove(key);
            cache.remove(key);

            return "-2";
        }

        return String.valueOf(
                remaining
        );
    }

    private void checkExpiry(
            String key) {

        Long expiryTime =
                expiryTimes.get(key);

        if (
                expiryTime != null
                &&
                System.currentTimeMillis()
                >= expiryTime
        ) {

            database.remove(key);
            expiryTimes.remove(key);
            cache.remove(key);
        }
    }

    // =========================
    // LIST COMMANDS
    // =========================

    synchronized String lpush(
            String key,
            String value) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (value == null || value.isEmpty()) {
            return "ERROR: value cannot be empty";
        }

        return String.valueOf(
                redisList.lpush(
                        key,
                        value
                )
        );
    }

    synchronized String rpush(
            String key,
            String value) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (value == null || value.isEmpty()) {
            return "ERROR: value cannot be empty";
        }

        return String.valueOf(
                redisList.rpush(
                        key,
                        value
                )
        );
    }

    synchronized String lpop(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        return redisList.lpop(key);
    }

    synchronized String rpop(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        return redisList.rpop(key);
    }

    synchronized String lrange(
            String key,
            int start,
            int end) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        return redisList.lrange(
                key,
                start,
                end
        );
    }

    // =========================
    // HASH COMMANDS
    // =========================

    synchronized String hset(
            String key,
            String field,
            String value) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (field == null || field.isEmpty()) {
            return "ERROR: field cannot be empty";
        }

        if (value == null || value.isEmpty()) {
            return "ERROR: value cannot be empty";
        }

        return String.valueOf(
                redisHash.hset(
                        key,
                        field,
                        value
                )
        );
    }

    synchronized String hget(
            String key,
            String field) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (field == null || field.isEmpty()) {
            return "ERROR: field cannot be empty";
        }

        return redisHash.hget(
                key,
                field
        );
    }

    synchronized String hdel(
            String key,
            String field) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (field == null || field.isEmpty()) {
            return "ERROR: field cannot be empty";
        }

        return redisHash.hdel(
                key,
                field
        );
    }

    synchronized String hgetall(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        return redisHash.hgetall(
                key
        );
    }

    // =========================
    // SET DATATYPE COMMANDS
    // =========================

    synchronized String sadd(
            String key,
            String value) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (value == null || value.isEmpty()) {
            return "ERROR: value cannot be empty";
        }

        return String.valueOf(
                redisSet.sadd(
                        key,
                        value
                )
        );
    }

    synchronized String srem(
            String key,
            String value) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (value == null || value.isEmpty()) {
            return "ERROR: value cannot be empty";
        }

        return String.valueOf(
                redisSet.srem(
                        key,
                        value
                )
        );
    }

    synchronized String sismember(
            String key,
            String value) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        if (value == null || value.isEmpty()) {
            return "ERROR: value cannot be empty";
        }

        return String.valueOf(
                redisSet.sismember(
                        key,
                        value
                )
        );
    }

    synchronized String smembers(
            String key) {

        if (key == null || key.isEmpty()) {
            return "ERROR: key cannot be empty";
        }

        return redisSet.smembers(
                key
        );
    }

    // =========================
    // DATABASE / SNAPSHOT
    // =========================

    Map<String, Object> getDatabase() {
        return database;
    }

    Map<String, Long> getExpiryTimes() {
        return expiryTimes;
    }

    synchronized String saveSnapshot() {

        snapshot.save(
                database,
                expiryTimes
        );

        return "OK";
    }

    // =========================
    // COMMAND PARSER
    // =========================

    synchronized String executeCommand(
            String input) {

        if (input == null) {
            return "ERROR: command cannot be null";
        }

        input = input.trim();

        if (input.isEmpty()) {
            return "ERROR: empty command";
        }

        String[] parts =
                input.split("\\s+");

        String command =
                parts[0].toUpperCase();

        switch (command) {

            case "SET":

                if (parts.length != 3) {
                    return "ERROR: SET key value";
                }

                return set(
                        parts[1],
                        parts[2]
                );

            case "GET":

                if (parts.length != 2) {
                    return "ERROR: GET key";
                }

                return get(
                        parts[1]
                );

            case "DEL":

                if (parts.length != 2) {
                    return "ERROR: DEL key";
                }

                return del(
                        parts[1]
                );

            case "EXISTS":

                if (parts.length != 2) {
                    return "ERROR: EXISTS key";
                }

                return exists(
                        parts[1]
                );

            case "EXPIRE":

                if (parts.length != 3) {
                    return "ERROR: EXPIRE key seconds";
                }

                try {

                    long seconds =
                            Long.parseLong(
                                    parts[2]
                            );

                    return expire(
                            parts[1],
                            seconds
                    );

                } catch (
                        NumberFormatException e) {

                    return "ERROR: seconds must be a number";
                }

            case "TTL":

                if (parts.length != 2) {
                    return "ERROR: TTL key";
                }

                return ttl(
                        parts[1]
                );

            // =====================
            // LIST
            // =====================

            case "LPUSH":

                if (parts.length != 3) {
                    return "ERROR: LPUSH key value";
                }

                return lpush(
                        parts[1],
                        parts[2]
                );

            case "RPUSH":

                if (parts.length != 3) {
                    return "ERROR: RPUSH key value";
                }

                return rpush(
                        parts[1],
                        parts[2]
                );

            case "LPOP":

                if (parts.length != 2) {
                    return "ERROR: LPOP key";
                }

                return lpop(
                        parts[1]
                );

            case "RPOP":

                if (parts.length != 2) {
                    return "ERROR: RPOP key";
                }

                return rpop(
                        parts[1]
                );

            case "LRANGE":

                if (parts.length != 4) {
                    return "ERROR: LRANGE key start end";
                }

                try {

                    int start =
                            Integer.parseInt(
                                    parts[2]
                            );

                    int end =
                            Integer.parseInt(
                                    parts[3]
                            );

                    return lrange(
                            parts[1],
                            start,
                            end
                    );

                } catch (
                        NumberFormatException e) {

                    return "ERROR: start and end must be numbers";
                }

            // =====================
            // HASH
            // =====================

            case "HSET":

                if (parts.length != 4) {
                    return "ERROR: HSET key field value";
                }

                return hset(
                        parts[1],
                        parts[2],
                        parts[3]
                );

            case "HGET":

                if (parts.length != 3) {
                    return "ERROR: HGET key field";
                }

                return hget(
                        parts[1],
                        parts[2]
                );

            case "HDEL":

                if (parts.length != 3) {
                    return "ERROR: HDEL key field";
                }

                return hdel(
                        parts[1],
                        parts[2]
                );

            case "HGETALL":

                if (parts.length != 2) {
                    return "ERROR: HGETALL key";
                }

                return hgetall(
                        parts[1]
                );

            // =====================
            // SET DATATYPE
            // =====================

            case "SADD":

                if (parts.length != 3) {
                    return "ERROR: SADD key value";
                }

                return sadd(
                        parts[1],
                        parts[2]
                );

            case "SREM":

                if (parts.length != 3) {
                    return "ERROR: SREM key value";
                }

                return srem(
                        parts[1],
                        parts[2]
                );

            case "SISMEMBER":

                if (parts.length != 3) {
                    return "ERROR: SISMEMBER key value";
                }

                return sismember(
                        parts[1],
                        parts[2]
                );

            case "SMEMBERS":

                if (parts.length != 2) {
                    return "ERROR: SMEMBERS key";
                }

                return smembers(
                        parts[1]
                );

            // =====================
            // SNAPSHOT
            // =====================

            case "SAVE":

                if (parts.length != 1) {
                    return "ERROR: SAVE";
                }

                return saveSnapshot();

            default:

                return "ERROR: Unknown command '"
                        + command
                        + "'";
        }
    }
}