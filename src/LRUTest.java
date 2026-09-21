public class LRUTest {

    public static void main(String[] args) {

        LRUCache cache = new LRUCache(3);

        cache.put("A", "Apple");
        cache.put("B", "Banana");
        cache.put("C", "Cat");

        System.out.println("Initial Cache:");
        cache.display();

        // A ko recently used bana diya
        cache.get("A");

        // Capacity full hai
        // B least recently used hai, so B remove hoga
        cache.put("D", "Dog");

        System.out.println("After adding D:");
        cache.display();

        System.out.println("Get B: " + cache.get("B"));
        System.out.println("Get A: " + cache.get("A"));
        System.out.println("Cache Size: " + cache.size());
    }
}