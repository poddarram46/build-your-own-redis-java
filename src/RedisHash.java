import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RedisHash {

    private final ConcurrentHashMap<
            String,
            ConcurrentHashMap<String, String>
            > hashes =
            new ConcurrentHashMap<>();

    synchronized int hset(
            String key,
            String field,
            String value) {

        hashes.computeIfAbsent(
                key,
                k -> new ConcurrentHashMap<>()
        );

        boolean isNewField =
                !hashes.get(key).containsKey(field);

        hashes.get(key).put(
                field,
                value
        );

        return isNewField ? 1 : 0;
    }

    synchronized String hget(
            String key,
            String field) {

        Map<String, String> hash =
                hashes.get(key);

        if (hash == null) {
            return "(nil)";
        }

        String value =
                hash.get(field);

        if (value == null) {
            return "(nil)";
        }

        return value;
    }

    synchronized String hdel(
            String key,
            String field) {

        Map<String, String> hash =
                hashes.get(key);

        if (hash == null) {
            return "0";
        }

        String removed =
                hash.remove(field);

        if (hash.isEmpty()) {
            hashes.remove(key);
        }

        return removed != null
                ? "1"
                : "0";
    }

    synchronized String hgetall(
            String key) {

        Map<String, String> hash =
                hashes.get(key);

        if (hash == null || hash.isEmpty()) {
            return "(empty)";
        }

        StringBuilder result =
                new StringBuilder();

        for (
                Map.Entry<String, String> entry
                : hash.entrySet()
        ) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(entry.getKey())
                  .append(" ")
                  .append(entry.getValue());
        }

        return result.toString();
    }
}