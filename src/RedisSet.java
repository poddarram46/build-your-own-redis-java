import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

class RedisSet {

    private final ConcurrentHashMap<
            String,
            Set<String>
            > sets =
            new ConcurrentHashMap<>();

    // Add value to SET
    synchronized int sadd(
            String key,
            String value) {

        sets.computeIfAbsent(
                key,
                k -> ConcurrentHashMap.newKeySet()
        );

        boolean added =
                sets.get(key).add(value);

        return added ? 1 : 0;
    }

    // Remove value from SET
    synchronized int srem(
            String key,
            String value) {

        Set<String> set =
                sets.get(key);

        if (set == null) {
            return 0;
        }

        boolean removed =
                set.remove(value);

        if (set.isEmpty()) {
            sets.remove(key);
        }

        return removed ? 1 : 0;
    }

    // Check whether value exists
    synchronized int sismember(
            String key,
            String value) {

        Set<String> set =
                sets.get(key);

        if (set == null) {
            return 0;
        }

        return set.contains(value)
                ? 1
                : 0;
    }

    // Return all values
    synchronized String smembers(
            String key) {

        Set<String> set =
                sets.get(key);

        if (set == null || set.isEmpty()) {
            return "(empty)";
        }

        return String.join(
                " ",
                set
        );
    }
}