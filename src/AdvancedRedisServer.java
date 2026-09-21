class AdvancedRedisServer extends RedisServer {

    AdvancedRedisServer(int port) {
        super(port);
    }

    @Override
    void start() {
        System.out.println("Advanced Redis Server Started");
    }
}