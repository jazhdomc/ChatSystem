package mc.jazhdo;

import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

public class QueueObject {
    private final RegisteredServer server;
    private final byte[] pluginMsg;
    private final MinecraftChannelIdentifier id;

    public QueueObject(RegisteredServer server, byte[] pluginMsg, MinecraftChannelIdentifier id) {
        this.server = server;
        this.pluginMsg = pluginMsg;
        this.id = id;
    }

    public RegisteredServer getServer() {
        return server;
    }

    public byte[] getPluginMsg() {
        return pluginMsg;
    }

    public MinecraftChannelIdentifier getId() {
        return id;
    }
}
