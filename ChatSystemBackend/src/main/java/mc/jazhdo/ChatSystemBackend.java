package mc.jazhdo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;

public class ChatSystemBackend extends JavaPlugin implements Listener, PluginMessageListener {
    private final String global = "chatsystem:global", single = "chatsystem:single", refresh = "chatsystem:refresh", update = "chatsystem:update", announcement = "chatsystem:announcement";
    private final Map<String, String> prefixMap = new HashMap<>();
    private boolean firstPlayer = true, disableChatBroadcast;
    private PluginManager pluginManager;
    private LuckPerms lp;
    private Logger log;

    private class Commands implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (sender == null) return false;

            if (sender instanceof Player player) {
                // Calculate amount of rows to show
                User user = lp.getPlayerAdapter(Player.class).getUser(player);
                List<String> prefixes = new ArrayList<>(user.getCachedData().getMetaData().getPrefixes().values());
                int requiredRows = (int) Math.ceil(prefixes.size() / 9.0);
                if (requiredRows > 5) requiredRows = 6;
                else requiredRows++;
                
                // Create inventory view
                Inventory inventory = Bukkit.createInventory(null, requiredRows * 9, "Chat Prefix Manager");

                // Set exit button
                ItemStack exit = new ItemStack(Material.BARRIER);
                ItemMeta exitMeta = exit.getItemMeta();
                exitMeta.setDisplayName(ChatColor.RESET + "Exit Menu");
                exit.setItemMeta(exitMeta);
                inventory.setItem(0, exit);

                // Set the previous page button
                ItemStack previous = new ItemStack(Material.IRON_AXE);
                ItemMeta previousMeta = previous.getItemMeta();
                previousMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                previousMeta.setDisplayName(ChatColor.RESET + "Previous Page");
                previous.setItemMeta(previousMeta);
                inventory.setItem(3, previous);

                // Set page info slot
                ItemStack page = new ItemStack(Material.PAPER);
                ItemMeta pageMeta = page.getItemMeta();
                pageMeta.setDisplayName(ChatColor.RESET + "Page 1/" + Integer.toString((int) Math.ceil(prefixes.size() / 45.0)));
                page.setItemMeta(pageMeta);
                inventory.setItem(4, page);
                
                // Set the next page button
                ItemStack next = new ItemStack(Material.IRON_SWORD);
                ItemMeta nextMeta = next.getItemMeta();
                nextMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                nextMeta.setDisplayName(ChatColor.RESET + "Next Page");
                next.setItemMeta(nextMeta);
                inventory.setItem(5, next);

                // Set the current prefix info slot
                ItemStack current = new ItemStack(Material.STAINED_GLASS);
                ItemMeta currentMeta = current.getItemMeta();
                currentMeta.setDisplayName(ChatColor.RESET + "Current Prefix: " + ChatColor.translateAlternateColorCodes('&', prefixMap.getOrDefault(player.getName().toLowerCase(), lp.getGroupManager().getGroup("default").getCachedData().getMetaData().getPrefix())));
                current.setItemMeta(currentMeta);
                inventory.setItem(7, current);

                // Set all the prefix slots
                int i = 0;
                for (; i < prefixes.size() && i < 45; i++) {
                    ItemStack glass = new ItemStack(Material.STAINED_GLASS);
                    ItemMeta glassMeta = glass.getItemMeta();
                    glassMeta.setDisplayName(ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prefixes.get(i)));
                    glassMeta.setLore(List.of(ChatColor.RESET + "Left Click to set"));
                    glass.setItemMeta(glassMeta);
                    inventory.setItem(9 + i, glass);
                }

                // Show the player the inventory
                player.openInventory(inventory);
            } else sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }
    }

    @Override
    public void onEnable() {
        this.log = getLogger();
        log.log(Level.INFO, "Starting ChatSystemBackend...");

        // Configuration
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        disableChatBroadcast = config.getBoolean("disable-chat-broadcast");

        pluginManager = Bukkit.getPluginManager();

        // Register chat listener and plugin messenger
        Server server = getServer();
        Messenger messenger = server.getMessenger();
        messenger.registerIncomingPluginChannel(this, global, this);
        messenger.registerOutgoingPluginChannel(this, global);
        messenger.registerIncomingPluginChannel(this, single, this);
        messenger.registerIncomingPluginChannel(this, refresh, this);
        messenger.registerOutgoingPluginChannel(this, refresh);
        messenger.registerIncomingPluginChannel(this, update, this);
        messenger.registerOutgoingPluginChannel(this, update);
        server.getPluginManager().registerEvents(this, this);
        getCommand("chat").setExecutor(new Commands());

        // Load LuckPerms
        RegisteredServiceProvider<LuckPerms> rsp = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (rsp == null) log.log(Level.WARNING, "LuckPerms provider returned null. LuckPerms integration won't work.");
        else lp = rsp.getProvider();
    }

    @Override
    public void onDisable() {
        log.log(Level.INFO, "Shutting down ChatSystemBackend...");
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (!disableChatBroadcast) {
            // Make sure the unformatted chat doesn't go through
            event.setCancelled(true);

            // Build data stream
            ByteArrayDataOutput data = ByteStreams.newDataOutput();
            Player player = event.getPlayer();
            data.writeUTF(player.getName());
            data.writeUTF(event.getMessage());

            // Send plugin message
            player.sendPluginMessage(this, global, data.toByteArray());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Disable join messages for the better formatted one from the proxy
        event.setJoinMessage(null);

        // Request prefixes for /chat for the first player
        if (firstPlayer) {
            firstPlayer = false;
            event.getPlayer().sendPluginMessage(this, refresh, new byte[0]);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Disable quit messages for the better formatted one from the proxy
        event.setQuitMessage(null);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getClickedInventory();
        if (inv != null && inv.getName().equals("Chat Prefix Manager")) {
            // Prevent taking a item out
            event.setCancelled(true);

            // If click is to close
            int slot = event.getRawSlot();
            Player player = (Player) event.getWhoClicked();
            if (slot < 1) {
                player.closeInventory();
                return;
            }

            // If the slot is blank
            ItemStack slotItem = inv.getItem(slot);
            if (slotItem == null || slotItem.getType() == Material.AIR) return;

            // If the page or prefix is to be changed
            if (Math.abs(slot - 4) == 1) {
                // Make sure the page can be changed
                String name = inv.getItem(4).getItemMeta().getDisplayName();
                int page = Character.getNumericValue(name.charAt(name.length() - 3)), total = Character.getNumericValue(name.charAt(name.length() - 1)), newPage;
                if (slot == 3 && page > 1) newPage = page - 1;
                else if (slot == 5 && page < total) newPage = page + 1;
                else return;

                // Load new page's prefixes
                User user = lp.getPlayerAdapter(Player.class).getUser(player);
                SortedMap<Integer, String> prefixes = user.getCachedData().getMetaData().getPrefixes();
                int shownOnPage = page == total ? prefixes.size() - ((total - 1) * 45) : 45;
                int i = 0;
                for (; i < shownOnPage; i++) {
                    ItemStack glass = new ItemStack(Material.STAINED_GLASS);
                    ItemMeta glassMeta = glass.getItemMeta();
                    glassMeta.setDisplayName(ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prefixes.get(((newPage - 1) * 45) + i)));
                    glassMeta.setLore(List.of(ChatColor.RESET + "Left Click to set"));
                    glass.setItemMeta(glassMeta);
                    inv.setItem(9 + i, glass);
                }
                if (page == total) for (; i < 45; i++) inv.clear(9 + i);

                // Update page info
                ItemStack pageInfo = new ItemStack(Material.PAPER);
                ItemMeta pageInfoMeta = pageInfo.getItemMeta();
                pageInfoMeta.setDisplayName(ChatColor.RESET + "Page " + Integer.toString(newPage) + "/" + name.charAt(name.length() - 3));
                pageInfo.setItemMeta(pageInfoMeta);
                inv.setItem(4, pageInfo);
            } else if (slot / 9 > 0 && slot < inv.getSize()) {
                // Send update notification
                ByteArrayDataOutput output = ByteStreams.newDataOutput();
                String prefix = slotItem.getItemMeta().getDisplayName();
                output.writeUTF(player.getName().toLowerCase());
                output.writeUTF(prefix);
                player.sendPluginMessage(this, update, output.toByteArray());

                // Change current prefix display
                ItemStack current = new ItemStack(Material.STAINED_GLASS);
                ItemMeta currentMeta = current.getItemMeta();
                currentMeta.setDisplayName(ChatColor.RESET + "Current Prefix: " + prefix);
                current.setItemMeta(currentMeta);
                inv.setItem(7, current);
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player playerFrom, byte[] bytes) {
        // Figure out what method this is
        ByteArrayDataInput input = ByteStreams.newDataInput(bytes);
        switch (channel) {
            case global -> {
                // Whether the message is a player chat or server transfer
                if (input.readByte() == (byte) 0) {
                    String server = input.readUTF(), prefix = input.readUTF(), player = input.readUTF(), message = input.readUTF();
                    pluginManager.callEvent(new GlobalChatEvent(server, prefix, player, message));
                    if (!disableChatBroadcast) Bukkit.broadcastMessage(ChatColor.DARK_AQUA + "[" + server + "] " + prefix + ChatColor.WHITE + " <" + player + ">: " + message);
                } else {
                    String player = input.readUTF(), origin = input.readBoolean() ? input.readUTF() : null, destination = input.readBoolean() ? input.readUTF() : null;
                    pluginManager.callEvent(new ServerTransferEvent(player, origin, destination));
                    if (!disableChatBroadcast) {
                        if (destination == null) Bukkit.broadcastMessage(ChatColor.YELLOW + player + " has left the proxy from " + (origin == null ? "the login server" : origin) + ".");
                        else Bukkit.broadcastMessage(ChatColor.YELLOW + player + " has joined " + destination + " from " + (origin == null ? "the server list" : origin) + ".");
                    }
                }
            }
            case announcement -> {
                // Announce the message in plugin notification format
                Bukkit.broadcastMessage(ChatColor.GOLD + "[" + input.readUTF() + "] " + ChatColor.WHITE + input.readUTF());
            }
            case single -> {
                // Get player and send message with null checks
                String playerName = input.readUTF();
                Player p = Bukkit.getPlayer(playerName);
                if (p == null) log.log(Level.WARNING, "Player {0} does not exist in this server.", playerName);
                else p.sendMessage(ChatColor.translateAlternateColorCodes('&', input.readUTF()));
            }
            case update -> {
                // Get input and update prefix map
                prefixMap.put(input.readUTF(), input.readUTF());
            }
            case refresh -> {
                // Get input and put each username with its prefix
                prefixMap.clear();
                while (input.readBoolean()) prefixMap.put(input.readUTF(), input.readUTF());
            }
        }
    }
}