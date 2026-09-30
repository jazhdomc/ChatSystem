package mc.jazhdo;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GlobalChatEvent extends Event {
    private final String server, prefix, player, message;

    public GlobalChatEvent(String server, String prefix, String player, String message) {
        this.server = server;
        this.prefix = prefix;
        this.player = player;
        this.message = message;
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

    public String getMessage() {
        return message;
    }

    @Override
    public HandlerList getHandlers() {
        return new HandlerList();
    }
}
