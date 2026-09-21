import java.util.LinkedHashMap;
import java.util.Map;

class LRUCache {

    private final int capacity;

    private final LinkedHashMap<String, String> cache;

    LRUCache(int capacity) {

        this.capacity = capacity;

        // accessOrder = true
        // GET karne par key recently used ban jayegi
        cache = new LinkedHashMap<>(
                capacity,
                0.75f,
                true
        ) {

            @Override
            protected boolean removeEldestEntry(
                    Map.Entry<String, String> eldest) {

                return size() > LRUCache.this.capacity;
            }
        };
    }

    // =========================
    // PUT
    // =========================

    void put(String key, String value) {

        cache.put(key, value);
    }

    // =========================
    // GET
    // =========================

    String get(String key) {

        return cache.get(key);
    }

    // =========================
    // REMOVE
    // =========================

    void remove(String key) {

        cache.remove(key);
    }

    // =========================
    // SIZE
    // =========================

    int size() {

        return cache.size();
    }

    // =========================
    // DISPLAY
    // =========================

    void display() {

        System.out.println(cache);
    }
}
