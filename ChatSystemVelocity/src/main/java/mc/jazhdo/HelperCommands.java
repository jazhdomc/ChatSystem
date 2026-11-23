package mc.jazhdo;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class HelperCommands {
    public static void init(ProxyServer proxy, CommandManager cm, Logger logger) {
        // Register server transfer commands
        List<String> cmds = List.of(
            "lobby", "base", "hub",
            "1block", "island", "one",
            "creative", "create", "build",
            "survival", "live", "1.12",
            "minigames", "games", "fun",
            "new-survival", "survive", "new"
        );
        List<String> servers = List.of("lobby", "oneblock", "creative", "survival", "minigames", "new-survival");
        for (int i = 0; i < 6; i++)
            for (String command : cmds.subList(i * 3, (i + 1) * 3))
                cm.register(cm.metaBuilder(command).build(), new ServerCommand(proxy, servers.get(i), logger));

        // Register discord and website link commands
        for (String command : List.of("discord", "dc")) cm.register(cm.metaBuilder(command).build(), new MessageCommand("Discord", "discord.gg/X6Ab2B35n4"));
        for (String command : List.of("website", "www", "web", "site")) cm.register(cm.metaBuilder(command).build(), new MessageCommand("Website", "mc.itsjaz.com"));

        // Register other help commands
        cm.register(cm.metaBuilder("vote").build(), new VoteCommand());
        cm.register(cm.metaBuilder("rules").build(), new RulesCommand());
    }

    private static class RulesCommand implements SimpleCommand {
        private static final Component message = Component.empty()
            .append(Component.text("The rules are on the website: ").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
            .append(
                Component.text("Rules Link")
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.UNDERLINED, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.openUrl("https://mc.itsjaz.com/rules"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open").color(NamedTextColor.BLUE)))
            );

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (source == null) return;

            if (source instanceof Player player) player.sendMessage(message);
            else source.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
        }
    }

    private static class VoteCommand implements SimpleCommand {
        private final Component message = Component.empty()
            .append(Component.text("You can vote for JazhdoMC on:").color(NamedTextColor.GOLD))
            .append(Component.text("\n - Eagler Server List: ").color(NamedTextColor.GREEN))
            .append(
                Component.text("ESL Link")
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.UNDERLINED, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.openUrl("https://servers.eaglercraft.com/servers/ba29cf5a-c7f6-4b7c-a566-c80dca72390e"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open").color(NamedTextColor.BLUE)))
            )
            .append(Component.text("\n - Top Eagler Servers: ").color(NamedTextColor.GREEN))
            .append(
                Component.text("TES Link")
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.UNDERLINED, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.openUrl("https://topeaglerservers.com/server/954b7682/JazhdoMC"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open").color(NamedTextColor.BLUE)))
            )
            .append(Component.text("\n - MinecraftServers.org: ").color(NamedTextColor.GREEN))
            .append(
                Component.text("MS Link")
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.UNDERLINED, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.openUrl("https://minecraftservers.org/server/691538"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open").color(NamedTextColor.BLUE)))
            )
            .append(Component.text("\n - Eaglercraft Hub: ").color(NamedTextColor.GREEN))
            .append(
                Component.text("EGH Link")
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.UNDERLINED, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.openUrl("https://eaglercrafthub.com/servers/jazhdomc"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open").color(NamedTextColor.BLUE)))
            )
            .append(Component.text("\n - MC-Servers.com: ").color(NamedTextColor.GREEN))
            .append(
                Component.text("MCS Link")
                .color(NamedTextColor.BLUE)
                .decorate(TextDecoration.UNDERLINED, TextDecoration.ITALIC)
                .clickEvent(ClickEvent.openUrl("https://mc-servers.com/server/6886"))
                .hoverEvent(HoverEvent.showText(Component.text("Click to open").color(NamedTextColor.BLUE)))
            )
            .append(
                Component.text("\nWARNING: Voting on Eaglercraft Hub won't give you a voter role.")
                    .color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD)
            );

        @Override
        public void execute(Invocation invocation) {
            CommandSource source = invocation.source();
            if (source == null) return;

            if (source instanceof Player player) player.sendMessage(message);
            else source.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
        }
    }
    
    private static class ServerCommand implements SimpleCommand {
        private static final Component otherSourceError = Component.text("Only players can use this!").color(NamedTextColor.RED);
        private final Component teleportationMessage;
        private final RegisteredServer target;

        public ServerCommand(ProxyServer server, String serverName, Logger logger) {
            // Show normal teleportation messages or server not found errors
            Optional<RegisteredServer> serverObj = server.getServer(serverName);
            if (serverObj.isPresent()) {
                target = serverObj.get();
                teleportationMessage = Component.text("Sending you to " + serverName + " server...").color(NamedTextColor.YELLOW);
            } else {
                logger.log(Level.WARNING, "Server {0} was not found. All teleportation requests to that server will not work.", serverName);
                target = null;
                teleportationMessage = Component.text("Server " + serverName + " was not found. Contact staff for help.").color(NamedTextColor.RED);
            }
        }

        @Override
        public void execute(Invocation invocation) {
            // Make sure source exists
            CommandSource source = invocation.source();
            if (source == null) return;

            // Check that a player and not console is sending the command
            if (source instanceof Player player) {
                // Teleport if server exists and send cooresponding message
                if (target != null) player.createConnectionRequest(target).fireAndForget();
                player.sendMessage(teleportationMessage);
            } else source.sendMessage(otherSourceError);
        }
    }

    private static class MessageCommand implements SimpleCommand {
        private final Component message;

        public MessageCommand(String name, String link) {
            message = Component.empty()
                .append(Component.text("JazhdoMC's Official " + name).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .append(Component.text(": "))
                .append(
                    Component.text("https://" + link)
                        .color(NamedTextColor.BLUE)
                        .decorate(TextDecoration.UNDERLINED)
                        .decorate(TextDecoration.ITALIC)
                        .clickEvent(ClickEvent.openUrl("https://" + link))
                        .hoverEvent(HoverEvent.showText(Component.text("Click to open").color(NamedTextColor.BLUE)))
                );
        }

        @Override
        public void execute(Invocation invocation) {
            // Send link message
            invocation.source().sendMessage(message);
        }
    }
}
