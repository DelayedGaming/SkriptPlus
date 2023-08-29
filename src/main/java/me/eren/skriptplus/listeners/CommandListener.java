package me.eren.skriptplus.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandListener implements Listener {
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message.startsWith("/skript") || message.startsWith("/sk") && !message.startsWith("/skript:")) {
            String[] command = event.getMessage().split(" ");
            command[0] = "/skp";
            event.setMessage(String.join(" ", command));
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        String cmd = event.getCommand();
        if (cmd.startsWith("skript") || cmd.startsWith("sk") && !cmd.startsWith("skript:")) {
            String[] command = event.getCommand().split(" ");
            command[0] = "skp";
            event.setCommand(String.join(" ", command));
        }
    }
}

