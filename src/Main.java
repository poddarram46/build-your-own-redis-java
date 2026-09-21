public class Main {

    public static void main(String[] args) {

        RedisServer server = new RedisServer(6379);

        server.start();
    }
}