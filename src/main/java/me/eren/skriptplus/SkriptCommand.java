package me.eren.skriptplus;

import ch.njol.skript.Skript;
import com.google.gson.JsonObject;
import me.eren.skriptplus.utils.FileUtils;
import me.eren.skriptplus.utils.HttpUtils;
import me.eren.skriptplus.utils.SkriptUtils;
import me.eren.skriptplus.utils.Version;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SkriptCommand implements TabExecutor {
    private final String GITHUB_API = "https://api.github.com/repos/%s/releases/latest";
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy---HH:mm");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, "help message");
            return true;
        }

        switch (args[0]) {

            case "backup-scripts":
                FileUtils.copyDirectory(SkriptPlus.getInstance().getDataFolder() + "/Skript/scripts",
                        SkriptPlus.getInstance().getDataFolder() + "/Skript/scripts-backup-" + getCurrentDate());
                send(sender, "Created a backup in <yellow>\"plugins/Skript/scripts-backup-" + getCurrentDate() + "\"", true);
                break;

            case "info":
                send(sender, "Please wait...", true);
                Properties properties = SkriptPlus.getAddonProperties();
                List<Component> addons = new ArrayList<>();
                List<Component> dependencies = new ArrayList<>();
                List<String> plugins = SkriptUtils.getEnabledDependencies();
                plugins.addAll(SkriptUtils.getEnabledAddons());

                for (String plugin : plugins) {
                    Version currentVer = new Version(Bukkit.getPluginManager().getPlugin(plugin).getDescription().getVersion());
                    if (!properties.containsKey(plugin.toLowerCase())) { // version is unknown
                        Component message = MiniMessage.miniMessage().deserialize("<gray>[<gold>⬤<gray>] <white>" + plugin + " <gray>(" + currentVer + ")");
                        if (Skript.getAddon(plugin) != null) { addons.add(message); }
                        else { dependencies.add(message); }
                        continue;
                    }
                    try {
                        String repo = properties.getProperty(plugin.toLowerCase());
                        URL url = new URI(String.format(GITHUB_API, repo)).toURL();
                        CompletableFuture<HttpResponse<String>> future = HttpUtils.sendGetRequest(url);
                        future.join();

                        future.exceptionally(e -> {
                            throw new RuntimeException("Error while getting the latest version of " + plugin + ".", e);
                        });

                        future.thenAccept(request -> {
                            if (request.statusCode() != 200) {
                                throw new RuntimeException("Got code " + request.statusCode() + " while trying to get the latest version of " + args[2] + ".");
                            }
                            String stringResponse = request.body();
                            JsonObject response = HttpUtils.parseResponse(stringResponse);

                            if (!response.has("tag_name")) { // version is unknown
                                Component message = MiniMessage.miniMessage().deserialize("<gray>[<gold>⬤<gray>] <white>" + plugin + " <gray>(" + currentVer + ")");
                                if (Skript.getAddon(plugin) != null) { addons.add(message); }
                                else { dependencies.add(message); }
                                return;
                            }

                            Version latestVer = new Version(response.get("tag_name").getAsString());
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
                }
                AtomicReference<String> skVerColor = new AtomicReference<>();
                try {
                    URL url = new URI(String.format(GITHUB_API, "SkriptLang/Skript")).toURL();
                    CompletableFuture<HttpResponse<String>> future = HttpUtils.sendGetRequest(url);
                    future.join();

                    future.thenApply(HttpResponse::body).thenAccept(stringResponse -> {
                       JsonObject response = HttpUtils.parseResponse(stringResponse);
                       Version latestVer = new Version(response.get("tag_name").getAsString());
                       Version currentVer = new Version(Skript.getVersion().toString());
                       skVerColor.set(latestVer.isLargerThan(currentVer) ? "<red>" : "<green>");
                    });
                } catch (URISyntaxException | MalformedURLException ignored) {}
                send(sender, "<gray>==============[ <gold>Skript<yellow>+ <white>Info <gray>]==============");
                send(sender, "Skript Version: " + skVerColor + Skript.getVersion());
                send(sender, "Server Version: <yellow>" + Bukkit.getServer().getVersion());
                send(sender, ""); // newlines look very ugly in console, send an empty message instead
                send(sender, "Addons [" + addons.size() + "]");
                addons.forEach(sender::sendMessage);
                send(sender, "");
                send(sender, "Dependencies [" + dependencies.size() + "]");
                dependencies.forEach(sender::sendMessage);
                break;

            case "addon":
                if (args.length < 3) {
                    send(sender, "Please enter an addon name.", true);
                    break;
                }
                if (args[1].equals("delete") || args[1].equals("update")) {
                    if (!Bukkit.getPluginManager().isPluginEnabled(args[2])) {
                        send(sender, "This addon doesn't exist.", true);
                        break;
                    }
                    File plugin = FileUtils.getFileOfPlugin(Bukkit.getPluginManager().getPlugin(args[2]));
                    plugin.delete();
                    send(sender, "Deleted <yellow>" + args[2] + "<white>.", true);
                }
                if (args[1].equals("download") || args[1].equals("update")) {
                    if (FileUtils.getFileOfPlugin(Bukkit.getPluginManager().getPlugin(args[2])).exists()) {
                        send(sender, "This addon is already installed.", true);
                        break;
                    }
                    if (!SkriptPlus.getAddonProperties().containsKey(args[2].toLowerCase())) {
                        send(sender, "Couldn't find an addon with that name.", true);
                        break;
                    }
                    send(sender, "Downloading...", true);
                    try {
                        String repo = SkriptPlus.getAddonProperties().getProperty(args[2].toLowerCase());
                        URL url = new URI(String.format(GITHUB_API, repo)).toURL();
                        CompletableFuture<HttpResponse<String>> future = HttpUtils.sendGetRequest(url);
                        future.join();

                        future.exceptionally(ex -> {
                            throw new RuntimeException("Error while getting the latest version of " + args[2] + ".", ex);
                        })

                        .thenAccept(request -> {
                            if (request.statusCode() != 200) {
                                throw new RuntimeException("Got code " + request.statusCode() + " while trying to get the latest version of " + args[2] + ".");
                            }
                            String stringResponse = request.body();
                            JsonObject response = HttpUtils.parseResponse(stringResponse);
                            String name = response.getAsJsonArray("assets").get(0).getAsJsonObject().get("name").getAsString();
                            String link = response.getAsJsonArray("assets").get(0).getAsJsonObject().get("browser_download_url").getAsString();
                            try {
                                FileUtils.downloadFile(new URI(link).toURL(), new File(SkriptPlus.getInstance().getDataFolder() + "/" + name));
                                send(sender, "Downloaded <yellow>" + args[2] + "<white>. Please restart your server.", true);
                            } catch (MalformedURLException | URISyntaxException e) {
                                throw new RuntimeException("Error while downloading an addon.", e);
                            }
                        });
                    } catch (URISyntaxException | MalformedURLException e) {
                        throw new RuntimeException("Error while downloading an addon.", e);
                    }
                }
                break;
        }
        return true;
    }

    // used in backup-scripts command
    private String getCurrentDate() {
        Date currentDate = new Date();
        return DATE_FORMAT.format(currentDate);
    }

    private void send(CommandSender sender, String message) {
        send(sender, message, false);
    }
    private void send(CommandSender sender, String message, Boolean showPrefix) {
        if (showPrefix) {
            message = SkriptPlus.PREFIX + message;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }
}
