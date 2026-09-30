package mc.jazhdo;

public class SendAsPlayerEvent {
    private final String server, prefix, player, msg;

    public SendAsPlayerEvent(String server, String prefix, String player, String msg) {
        this.server = server;
        this.prefix = prefix;
        this.player = player;
        this.msg = msg;
    }

    public String getServer() {
        return server;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getPlayer() {
        return player;
    }

    public String getMsg() {
        return msg;
    }
}
