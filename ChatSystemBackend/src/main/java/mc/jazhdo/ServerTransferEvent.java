package mc.jazhdo;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ServerTransferEvent extends Event {
    private final String player, origin, destination;

    public ServerTransferEvent(String player, String origin, String destination) {
        this.player = player;
        this.origin = origin;
        this.destination = destination;
    }

    public String getPlayer() {
        return player;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    @Override
    public HandlerList getHandlers() {
        return new HandlerList();
    }
}
