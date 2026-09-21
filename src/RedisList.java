import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class RedisList {

    private final ConcurrentHashMap<String, LinkedList<String>> lists =
            new ConcurrentHashMap<>();

    synchronized int lpush(String key, String value) {

        lists.computeIfAbsent(
                key,
                k -> new LinkedList<>()
        );

        lists.get(key).addFirst(value);

        return lists.get(key).size();
    }

    synchronized int rpush(String key, String value) {

        lists.computeIfAbsent(
                key,
                k -> new LinkedList<>()
        );

        lists.get(key).addLast(value);

        return lists.get(key).size();
    }

    synchronized String lpop(String key) {

        LinkedList<String> list =
                lists.get(key);

        if (list == null || list.isEmpty()) {
            return "(nil)";
        }

        String value =
                list.removeFirst();

        if (list.isEmpty()) {
            lists.remove(key);
        }

        return value;
    }

    synchronized String rpop(String key) {

        LinkedList<String> list =
                lists.get(key);

        if (list == null || list.isEmpty()) {
            return "(nil)";
        }

        String value =
                list.removeLast();

        if (list.isEmpty()) {
            lists.remove(key);
        }

        return value;
    }

    synchronized String lrange(
            String key,
            int start,
            int end) {

        LinkedList<String> list =
                lists.get(key);

        if (list == null || list.isEmpty()) {
            return "(empty)";
        }

        int size = list.size();

        if (start < 0) {
            start = size + start;
        }

        if (end < 0) {
            end = size + end;
        }

        start = Math.max(start, 0);
        end = Math.min(end, size - 1);

        if (start > end || start >= size) {
            return "(empty)";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = start; i <= end; i++) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(list.get(i));
        }

        return result.toString();
    }
}