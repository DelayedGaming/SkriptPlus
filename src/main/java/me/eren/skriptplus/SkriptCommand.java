package me.eren.skriptplus;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.util.Version;
import com.google.gson.JsonObject;
import me.eren.skriptplus.utils.FileUtils;
import me.eren.skriptplus.utils.HttpUtils;
import me.eren.skriptplus.utils.SkriptUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class SkriptCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendRichMessage("help message");
            return true;
        }

        switch (args[0]) {

            case "backup-scripts":
                try {
                    FileUtils.copyDirectory("./plugins/Skript/scripts", "./plugins/Skript/scripts-backup-" + getCurrentDate());
                    sender.sendRichMessage(SkriptPlus.PREFIX + "Created a backup in <yellow>\"plugins/Skript/scripts-backup-" + getCurrentDate() + "\"");
                } catch (IOException e) {
                    sender.sendRichMessage(SkriptPlus.PREFIX + "<red>BAD stuff happened, check console for more info.");
                    throw new RuntimeException("Error while creating a backup of the scripts folder.", e);
                }
                break;

            case "info":
                sender.sendRichMessage(SkriptPlus.PREFIX + "Please wait...");
                Properties properties = SkriptPlus.getAddonProperties();
                List<Component> addons = new ArrayList<>();
                List<Component> dependencies = new ArrayList<>();
                List<String> plugins = SkriptUtils.getEnabledDependencies();
                plugins.addAll(SkriptUtils.getEnabledAddons());

                for (String plugin : plugins) {
                    if (properties.containsKey(plugin.toLowerCase())) {
                        try {
                            String repo = properties.getProperty(plugin.toLowerCase());
                            URL url = new URL("https://api.github.com/repos/" + repo + "/releases/latest");
                            CompletableFuture<HttpResponse<String>> future = HttpUtils.sendGetRequest(url);
                            future.join();

                            future.thenAccept(response -> {
                                if (response.statusCode() != 200) {
                                    throw new RuntimeException("Got code " + response.statusCode() + " while trying to get the version of " + plugin + ".");
                                }
                            });

                            future.exceptionally(ex -> {
                                throw new RuntimeException("Error while getting the version of " + plugin + ".", ex);
                            });

                            future.thenApply(HttpResponse::body).thenAccept(stringResponse -> {
                                JsonObject response = HttpUtils.parseResponse(stringResponse);

                                Version latestVer = new Version(response.get("tag_name").getAsString());
                                Version currentVer = new Version(Bukkit.getPluginManager().getPlugin(plugin).getDescription().getVersion());

                                if (latestVer.isLargerThan(currentVer)) { // version is outdated
                                    Component message = MiniMessage.miniMessage().deserialize("<gray>[<red>⬤<gray>] <white>" + plugin + " <gray>(" + currentVer + " -> " + latestVer + ")");
                                    if (Skript.getAddon(plugin) != null) { addons.add(message); }
                                    else { dependencies.add(message); }
                                } else { // version is up-to-date
                                    Component message = MiniMessage.miniMessage().deserialize("<gray>[<green>⬤<gray>] <white>" + plugin + " <gray>(" + currentVer + ")");
                                    if (Skript.getAddon(plugin) != null) { addons.add(message); }
                                    else { dependencies.add(message); }
                                }
                            });
                        } catch (URISyntaxException | MalformedURLException e) {
                            throw new RuntimeException("Error while getting the version of " + plugin + ".", e);
                        }
                    } else { // version is unknown
                        Component message = MiniMessage.miniMessage().deserialize("<gray>[<gold>⬤<gray>] <white>" + plugin + " <gray>(" + plugin + ")");
                        if (Skript.getAddon(plugin) != null) { addons.add(message); }
                        else { dependencies.add(message); }
                    }
                }
                sender.sendRichMessage("<gray>==============[ <yellow>Skript<gold>+ <white>Info <gray>]==============");
                sender.sendRichMessage("Skript Version: " + Skript.getVersion());
                sender.sendRichMessage("Server Version: " + Bukkit.getServer().getVersion());
                sender.sendRichMessage(""); // newlines look very ugly in console, send an empty message instead
                sender.sendRichMessage("Addons [" + addons.size() + "]");
                addons.forEach(sender::sendMessage);
                sender.sendRichMessage("");
                sender.sendRichMessage("Dependencies [" + dependencies.size() + "]");
                dependencies.forEach(sender::sendMessage);
                break;
        }

        return true;
    }

    private String getCurrentDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy---HH:mm");
        Date currentDate = new Date();
        return dateFormat.format(currentDate);
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }
}
