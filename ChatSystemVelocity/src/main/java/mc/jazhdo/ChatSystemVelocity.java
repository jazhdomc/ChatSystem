package mc.jazhdo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.platform.PlayerAdapter;

@SuppressWarnings("CallToPrintStackTrace")
@Plugin(id = "chat-system-velocity", name = "ChatSystemVelocity", version = "0.3.1", dependencies = { @Dependency(id = "luckperms") })
public class ChatSystemVelocity {
    private final ProxyServer proxy;
    private final MinecraftChannelIdentifier global, single, refresh, update, announcement;
    private LuckPerms lp;
    private ContextManager cm;
    private Map<String, String> serverNames = new HashMap<>();
    private PlayerAdapter<Player> playerAdapter;
    private final Path dataDirectory;
    private final Logger logger;
    /**
     * A queue for plugin messages that have to be sent
     */
    private final List<QueueObject> pluginMsgQueue = new ArrayList<>();
    private Connection conn = null;
    // Forbidden word list from https://github.com/Hesham-Elbadawi/list-of-banned-words/blob/master/en
    private final List<String> unparsedForbiddenPhrases = List.of("2 girls 1 cup", "alabama hot pocket", "alaskan pipeline", "auto erotic", "baby batter", "baby juice", "ball gag", "ball gravy", "ball kicking", "ball licking", "ball sack", "ball sucking", "barely legal", "beaver cleaver", "beaver lips", "big black", "big breasts", "big knockers", "big tits", "black cock", "blonde action", "blonde on blonde action", "blow job", "blow your load", "blue waffle", "booty call", "brown showers", "brunette action", "bullet vibe", "bung hole", "camel toe", "carpet muncher", "chocolate rosebuds", "cleveland steamer", "clover clamps", "date rape", "deep throat", "dirty pillows", "dirty sanchez", "doggie style", "doggy style", "dog style", "donkey punch", "double dong", "double penetration", "dp action", "dry hump", "eat my ass", "female squirting", "foot fetish", "fuck buttons", "fudge packer", "gang bang", "gay sex", "giant cock", "girl on", "girl on top", "girls gone wild", "god damn", "golden shower", "goo girl", "group sex", "hand job", "hard core", "hot carl", "hot chick", "how to kill", "how to murder", "huge fat", "jack off", "jail bait", "jelly donut", "jerk off", "leather restraint", "leather straight jacket", "lemon party", "make me come", "male squirting", "menage a trois", "missionary position", "mound of venus", "mr hands", "muff diver", "nig nog", "nsfw images", "one cup two girls", "one guy one jar", "phone sex", "piece of shit", "piss pig", "pleasure chest", "pole smoker", "poop chute", "prince albert piercing", "raging boner", "reverse cowgirl", "rosy palm", "rosy palm and her 5 sisters", "rusty trombone", "shaved beaver", "shaved pussy", "splooge moose", "spread legs", "strap on", "strip club", "style doggy", "suicide girls", "sultry women", "tainted love", "taste my", "tea bagging", "tied up", "tight white", "tongue in a", "tub girl", "two girls one cup", "urethra play", "venus mound", "violet wand", "wet dream", "white power", "wrapping men", "wrinkled starfish", "yellow showers");
    private final List<String[]> parsedForbiddenPhrases = new ArrayList<>();
    private final Set<String> forbiddenWords = Set.of("2g1c", "acrotomophilia", "anal", "anilingus", "anus", "apeshit", "arsehole", "ass", "asshole", "assmunch", "autoerotic", "babeland", "bangbros", "bareback", "barenaked", "bastard", "bastardo", "bastinado", "bbw", "bdsm", "beaner", "beaners", "bestiality", "bimbos", "birdlock", "bitch", "bitches", "blowjob", "blumpkin", "bollocks", "bondage", "boner", "boob", "boobs", "bukkake", "bulldyke", "bullshit", "bunghole", "busty", "butt", "buttcheeks", "butthole", "camgirl", "camslut", "camwhore", "carpetmuncher", "circlejerk", "clit", "clitoris", "clusterfuck", "cock", "cocks", "coprolagnia", "coprophilia", "cornhole", "coon", "coons", "creampie", "cum", "cumming", "cunnilingus", "cunt", "darkie", "daterape", "deepthroat", "dendrophilia", "dick", "dildo", "dingleberry", "dingleberries", "doggiestyle", "doggystyle", "dolcett", "domination", "dominatrix", "dommes", "dvda", "ecchi", "ejaculation", "erotic", "erotism", "escort", "eunuch", "faggot", "fecal", "felch", "fellatio", "feltch", "femdom", "figging", "fingerbang", "fingering", "fisting", "footjob", "frotting", "fuck", "fuckin", "fucking", "fucktards", "fudgepacker", "futanari", "genitals", "goatcx", "goatse", "gokkun", "goodpoop", "goregasm", "grope", "g-spot", "guro", "handjob", "hardcore", "hentai", "homoerotic", "honkey", "hooker", "humping", "incest", "intercourse", "jailbait", "jigaboo", "jiggaboo", "jiggerboo", "jizz", "juggs", "kike", "kinbaku", "kinkster", "kinky", "knobbing", "lolita", "lovemaking", "masturbate", "milf", "motherfucker", "muffdiving", "nambla", "nawashi", "negro", "neonazi", "nigga", "nigger", "nimphomania", "nipple", "nipples", "nude", "nudity", "nympho", "nymphomania", "octopussy", "omorashi", "orgasm", "orgy", "paedophile", "paki", "panties", "panty", "pedobear", "pedophile", "pegging", "penis", "pissing", "pisspig", "playboy", "ponyplay", "poof", "poon", "poontang", "punany", "poopchute", "porn", "porno", "pornography", "pthc", "pubes", "pussy", "queaf", "queef", "quim", "raghead", "rape", "raping", "rapist", "rectum", "rimjob", "rimming", "sadism", "santorum", "scat", "schlong", "scissoring", "semen", "sex", "sexo", "sexy", "shemale", "shibari", "shit", "shitblimp", "shitty", "shota", "shrimping", "skeet", "slanteye", "slut", "s&m", "smut", "snatch", "snowballing", "sodomize", "sodomy", "spic", "splooge", "spooge", "spunk", "strapon", "strappado", "suck", "sucks", "swastika", "swinger", "threesome", "throating", "tit", "tits", "titties", "titty", "topless", "tosser", "towelhead", "tranny", "tribadism", "tubgirl", "tushy", "twat", "twink", "twinkie", "undressing", "upskirt", "urophilia", "vagina", "vibrator", "vorarephilia", "voyeur", "vulva", "wank", "wetback", "yaoi", "yiffy", "zoophilia");

    @Inject
    public ChatSystemVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.global = MinecraftChannelIdentifier.from("chatsystem:global");
        this.single = MinecraftChannelIdentifier.from("chatsystem:single");
        this.refresh = MinecraftChannelIdentifier.from("chatsystem:refresh");
        this.update = MinecraftChannelIdentifier.from("chatsystem:update");
        this.announcement = MinecraftChannelIdentifier.from("chatsystem:announcement");
        this.logger = logger;
        this.dataDirectory = dataDirectory;

        // Preparse forbidden phrases for the profanity filter
        for (String item : unparsedForbiddenPhrases) parsedForbiddenPhrases.add(item.split(" "));
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        ChannelRegistrar registrar = proxy.getChannelRegistrar();
        registrar.register(global);
        registrar.register(single);
        registrar.register(refresh);
        registrar.register(update);
        lp = LuckPermsProvider.get();
        cm = lp.getContextManager();
        playerAdapter = lp.getPlayerAdapter(Player.class);

        // Create plugin's folder if doesn't exist
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "There was an error creating missing directories. Error message: {0}", e.getMessage());
            return;
        }

        // Get JDBC here before using it
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "There was an error getting the database driver. Error message: {0}", e.getMessage());
            return;
        }

        // Connect to the databse or create it and connect
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + dataDirectory.resolve("database.db").toAbsolutePath());
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "There was an error getting a connection to the database. Error message: {0}", e.getMessage());
            return;
        }

        // Make sure table exits
        try (Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS prefixes (username TEXT NOT NULL UNIQUE COLLATE NOCASE, prefix TEXT NOT NULL COLLATE NOCASE, PRIMARY KEY(username))");
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "There was an error creating a table if it didn't exist. Error message: {0}", e.getMessage());
            return;
        }

        // Send refresh message to all backends
        try (PreparedStatement ps = conn.prepareStatement("SELECT username, prefix FROM prefixes")) {
            ResultSet resultList = ps.executeQuery();
            ByteArrayDataOutput output = ByteStreams.newDataOutput();
            while (resultList.next()) {
                output.writeUTF(resultList.getString("username"));
                output.writeUTF(resultList.getString("prefix"));
            }
            byte[] outputBytes = output.toByteArray();
            for (RegisteredServer server : proxy.getAllServers()) pluginMsgQueue.add(new QueueObject(server, outputBytes, refresh));
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "There was an error querying the prefixes to send to all the backend servers. Error message: {0}", e.getMessage());
        }
    }

    // Server Transfer Listeners
    @Subscribe
    public void onPlayerJoin(ServerConnectedEvent event) {
        // Send join/transfer message to all players on the proxy
        Optional<RegisteredServer> previousServer = event.getPreviousServer();
        broadcastServerTransferOnGlobal(event.getPlayer().getUsername(), (previousServer.isPresent() ? previousServer.get().getServerInfo().getName() : null), event.getServer().getServerInfo().getName());
        if (!pluginMsgQueue.isEmpty()) {
            RegisteredServer current = event.getServer();
            Player player = event.getPlayer();
            pluginMsgQueue.removeIf(pluginMsg -> {
                if (pluginMsg.getServer() == current) {
                    player.sendPluginMessage(pluginMsg.getId(), pluginMsg.getPluginMsg());
                    return true;
                }
                return false;
            });
        }
    }
    @Subscribe
    public void onPlayerLeave(DisconnectEvent event) {
        // Send leaving message to all players on the proxy
        Player player = event.getPlayer();
        Optional<ServerConnection> originServer = player.getCurrentServer();
        broadcastServerTransferOnGlobal(player.getUsername(), originServer.isPresent() ? originServer.get().getServerInfo().getName() : null, null);
    }
    private void broadcastServerTransferOnGlobal(@NotNull String playerName, String origin, String destination) {
        /** 
         * Build output bytes
         * 
         * Format:
         * 1. Type byte (1 is server transfer, 0 is player chat)
         * 2. Type String playername
         * 3. Type String origin server
         * 4. Type String destination server
         */
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeByte((byte) 1);
        output.writeUTF(playerName);
        if (origin == null) output.writeBoolean(false);
        else {
            output.writeBoolean(true);
            output.writeUTF(origin);
        }
        if (destination == null) output.writeBoolean(false);
        else {
            output.writeBoolean(true);
            output.writeUTF(destination);
        }
        byte[] bytes = output.toByteArray();

        // Broadcast to all servers with players
        for (RegisteredServer server : proxy.getAllServers()) if (!server.getPlayersConnected().isEmpty()) server.sendPluginMessage(global, bytes);
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // Only continue if its the right plugin messaging channel
        if (event.getIdentifier() == global) {
            // Get and extract the data
            ByteArrayDataInput dataStream = event.dataAsDataStream();
            String playerName, msg;
            try {
                playerName = dataStream.readUTF();
                msg = dataStream.readUTF();
            } catch (IllegalStateException e) {
                logger.log(Level.WARNING, "Data stream returned a Illegal State Exception: {0}", e.getMessage());
                return;
            }

            // Get the player to format it with their prefix
            proxy.getPlayer(playerName).ifPresentOrElse(p -> {
                // Do profanity check early to avoid unnecessary prefix queries
                if (!hasProfanity(p, msg)) {
                    ChannelMessageSource source = event.getSource();
                    if (source instanceof ServerConnection connection) {
                        // Get better formatted server name
                        String serverName = connection.getServerInfo().getName();
                        if (serverNames.containsKey(serverName)) serverName = serverNames.get(serverName);
                        else {
                            String tempServerName = capitalize(serverName.replaceAll("-", " "));
                            serverNames.put(serverName, tempServerName);
                            serverName = tempServerName;
                        }

                        // Get prefix
                        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM prefixes WHERE username = ?")) {
                            ps.setString(1, playerName.toLowerCase());
                            ResultSet result = ps.executeQuery();
                            sendOnGlobal(serverName, result.next() ? result.getString("prefix") : playerAdapter.getUser(p).getCachedData().getMetaData(cm.getQueryOptions(p)).getPrefix(), playerName, msg);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            logger.log(Level.WARNING, "There was an error finding the prefix for username \"{0}\". Error message: {1}", new String[]{playerName.toLowerCase(), e.getMessage()});
                        }
                    } else logger.log(Level.WARNING, "PluginMessageEvent source {0} was not a ServerConnection.", source.toString());
                }
            }, () -> logger.log(Level.WARNING, "Player {0} could not be found by the proxy. Message \"{1}\" will not be sent to global chat.", new String[]{playerName, msg}));
        } else if (event.getIdentifier() == refresh) {
            // Send all the prefixes
            try (PreparedStatement ps = conn.prepareStatement("SELECT username, prefix FROM prefixes")) {
                ResultSet resultList = ps.executeQuery();
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                while (resultList.next()) {
                    output.writeBoolean(true);
                    output.writeUTF(resultList.getString("username"));
                    output.writeUTF(resultList.getString("prefix"));
                }
                output.writeBoolean(false);
                if (event.getSource() instanceof ServerConnection conn) pluginMsgQueue.add(new QueueObject(conn.getServer(), output.toByteArray(), refresh));
                else logger.log(Level.WARNING, "A event source is not a ServerConnection from which a refresh request was sent.");
            } catch (SQLException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "There was an error querying the prefixes to send to all the backend servers. Error message: {0}", e.getMessage());
            }
        } else if (event.getIdentifier() == update) {
            // Get username and prefix to set
            ByteArrayDataInput input = event.dataAsDataStream();
            String username, prefix;
            try {
                username = input.readUTF();
                prefix = input.readUTF();
            } catch (IllegalStateException e) {
                logger.log(Level.WARNING, "Data stream returned a Illegal State Exception: {0}", e.getMessage());
                return;
            }

            // Set new username and prefix
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR REPLACE INTO prefixes (username, prefix) VALUES (?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, prefix);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "There was an error setting the new prefix \"{0}\" for username \"{1}\". Error message: {2}", new String[]{username, prefix, e.getMessage()});
                return;
            }

            // Report the update to all other servers
            if (event.getSource() instanceof ServerConnection conn) {
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                output.writeUTF(username);
                output.writeUTF(prefix);
                byte[] outputBytes = output.toByteArray();
                List<RegisteredServer> servers = new ArrayList<>(proxy.getAllServers());
                servers.remove(conn.getServer());
                for (RegisteredServer server : servers) pluginMsgQueue.add(new QueueObject(server, outputBytes, update));
            } else logger.log(Level.WARNING, "Updating username \"{0}\"'s prefix to \"{1}\" was unsuccessful because the source of the request was a instance of ServerConnection.", new String[]{username, prefix});
        } else return;

        // Prevent packet bouncing
        event.setResult(PluginMessageEvent.ForwardResult.handled());
    }

    // Event system for custom broadcasts
    @Subscribe
    public void onBroadcastEvent(BroadcastEvent event) {
        announceMessage(event.getName(), event.getMsg());
    }
    @Subscribe
    public void onSendAsPlayer(SendAsPlayerEvent event) {
        sendOnGlobal(event.getServer(), event.getPrefix(), event.getPlayer(), event.getMsg());
    }

    private String capitalize(String input) {
        char[] inputChars = input.toCharArray();
        StringBuilder output = new StringBuilder();
        char previousChar = ' ';
        for (int i = 0; i < inputChars.length; i++) {
            char currentChar = inputChars[i];
            if (previousChar == ' ') output.append(Character.toUpperCase(currentChar));
            else output.append(currentChar);
            previousChar = currentChar;
        }
        return output.toString();
    }

    private void announceMessage(String announcer, String announcement) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(announcer);
        output.writeUTF(announcement);

        byte[] bytes = output.toByteArray();
        for (RegisteredServer server : proxy.getAllServers()) if (!server.getPlayersConnected().isEmpty()) server.sendPluginMessage(this.announcement, bytes);
    }

    private void sendOnGlobal(String serverName, String prefix, String playerName, String msg) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeByte((byte) 0); // Byte 0 for player chat
        output.writeUTF(serverName);
        output.writeUTF(prefix);
        output.writeUTF(playerName);
        output.writeUTF(msg);
        byte[] bytes = output.toByteArray();
        for (RegisteredServer server : proxy.getAllServers()) if (!server.getPlayersConnected().isEmpty()) server.sendPluginMessage(global, bytes);
    }

    private void sendSingle(Player player, String msg) {
        // Build data stream
        ByteArrayDataOutput data = ByteStreams.newDataOutput();
        data.writeUTF(player.getUsername());
        data.writeUTF(msg);

        // Send message
        player.getCurrentServer().ifPresentOrElse(server -> server.sendPluginMessage(single, data.toByteArray()), () -> logger.log(Level.WARNING, "Player {0} is not in a server, unable to send message to them.", player.getUsername()));
    }

    private boolean has(String[] list, String[] searchList) {
        int searchLength = searchList.length;
        outer:
        for (int i = 0; i <= list.length - searchLength; i++) {
            for (int j = 0; j < searchLength; j++) if (!list[i + j].equals(searchList[j])) continue outer;
            return true;
        }
        return false;
    }

    private boolean hasProfanity(Player player, String msg) {
        // Iterate through all the words
        String[] msgWords = msg.toLowerCase().replaceAll("@", "a").replaceAll("\\$", "s").replaceAll("[^a-z0-9\\s]", "").trim().split(" ");
        for (String msgWord : msgWords) {
            if (forbiddenWords.contains(msgWord)) {
                sendSingle(player, "&cYour message contained the forbidden word \"" + msgWord + "\". It has not been sent.");
                return true;
            }
        }
        for (String[] phrase : parsedForbiddenPhrases) {
            if (has(msgWords, phrase)) {
                sendSingle(player, "&cYour message contained the forbidden phrase \"" + String.join(" ", phrase) + "\". It has not been sent.");
                return true;
            }
        }
        return false;
    }
}