class SetCommand implements RedisCommand {

    @Override
    public void execute() {
        System.out.println("SET command executed");
    }
}