package me.eren.skriptplus.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class CommandListener implements Listener {
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.getMessage().startsWith("/skript") || event.getMessage().startsWith("/sk")) {
            String[] command = event.getMessage().split(" ");
            command[0] = "/skp";
            event.setMessage(String.join(" ", command));
        }
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent event) {
        if (event.getCommand().startsWith("skript") || event.getCommand().startsWith("sk")) {
            String[] command = event.getCommand().split(" ");
            command[0] = "skp";
            event.setCommand(String.join(" ", command));
        }
    }
}

