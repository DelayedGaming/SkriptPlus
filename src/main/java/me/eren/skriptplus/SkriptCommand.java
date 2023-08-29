package me.eren.skriptplus;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.aliases.Aliases;
import ch.njol.skript.config.Config;
import ch.njol.skript.log.LogHandler;
import ch.njol.skript.log.RedirectingLogHandler;
import ch.njol.skript.log.RetainingLogHandler;
import ch.njol.skript.log.TimingLogHandler;
import ch.njol.util.OpenCloseable;
import com.google.gson.JsonObject;
import me.eren.skriptplus.listeners.CommandListener;
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
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

public class SkriptCommand implements TabExecutor {
    private final String GITHUB_API = "https://api.github.com/repos/%s/releases/latest";
    private final String HASTEBIN_API = "https://ptero.co/%s";
    private final DateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy---HH:mm");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 0) {
            send(sender, "help message");
            return true;
        }
        File script;

        switch (args[0].toLowerCase()) {
            case "backup-scripts" -> {
                FileUtils.copyDirectory("./plugins/Skript/scripts", ".plugins/Skript/scripts-backup-" + getCurrentDate());
                send(sender, "Created a backup in <yellow>\"plugins/Skript/scripts-backup-" + getCurrentDate() + "\"", true);
            }

            case "info" -> {
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
                        if (Skript.getAddon(plugin) != null)
                            addons.add(message);
                        else
                            dependencies.add(message);

                        continue;
                    }
                    try {
                        String repo = properties.getProperty(plugin.toLowerCase());
                        URL url = new URI(String.format(GITHUB_API, repo)).toURL();
                        CompletableFuture<HttpResponse<String>> future = HttpUtils.sendGetRequest(url);
                        future.join();

                        future.exceptionally(ex -> {
                            throw new RuntimeException("Error while getting the latest version of " + plugin + ".", ex);
                        });

                        future.thenAccept(request -> {
                            if (request.statusCode() != 200)
                                throw new RuntimeException("Got code " + request.statusCode() + " while trying to get the latest version of " + args[2] + ".");

                            String stringResponse = request.body();
                            JsonObject response = HttpUtils.parseResponse(stringResponse);

                            if (!response.has("tag_name")) { // version is unknown
                                Component message = MiniMessage.miniMessage().deserialize("<gray>[<gold>⬤<gray>] <white>" + plugin + " <gray>(" + currentVer + ")");
                                if (Skript.getAddon(plugin) != null)
                                    addons.add(message);
                                else
                                    dependencies.add(message);
                                return;
                            }

                            Version latestVer = new Version(response.get("tag_name").getAsString());
                            if (latestVer.isLargerThan(currentVer)) { // version is outdated
                                Component message = MiniMessage.miniMessage().deserialize("<gray>[<red>⬤<gray>] <white>" + plugin + " <gray>(" + currentVer + " -> " + latestVer + ")");
                                if (Skript.getAddon(plugin) != null)
                                    addons.add(message);
                                else
                                    dependencies.add(message);
                            } else { // version is up-to-date
                                Component message = MiniMessage.miniMessage().deserialize("<gray>[<green>⬤<gray>] <white>" + plugin + " <gray>(" + currentVer + ")");
                                if (Skript.getAddon(plugin) != null) {
                                    addons.add(message);
                                } else {
                                    dependencies.add(message);
                                }
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
                } catch (URISyntaxException | MalformedURLException ignored) {
                }
                send(sender, "<gray>==============[ <gold>Skript<yellow>+ <white>Info <gray>]==============");
                send(sender, "Skript Version: " + skVerColor + Skript.getVersion());
                send(sender, "Server Version: <yellow>" + Bukkit.getServer().getVersion());
                send(sender, ""); // newlines look very ugly in console, send an empty message instead
                send(sender, "Addons [" + addons.size() + "]");
                addons.forEach(sender::sendMessage);
                send(sender, "");
                send(sender, "Dependencies [" + dependencies.size() + "]");
                dependencies.forEach(sender::sendMessage);
            }

            case "addon" -> {
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
                                    if (request.statusCode() != 200)
                                        throw new RuntimeException("Got code " + request.statusCode() + " while trying to get the latest version of " + args[2] + ".");

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
            }

            // TODO needs improvements
            case "analyse" -> {
                if (args.length < 2) {
                    send(sender, "Please enter a script name.", true);
                    break;
                }
                script = new File("./plugins/Skript/scripts/", args[1]);
                if (!script.exists()) {
                    send(sender, "This script doesn't exist.", true);
                    break;
                }
                final Pattern regex = Pattern.compile("^\\s*(?:#.*)?$"); // checks if a line is empty or is a comment
                List<String> lines = new ArrayList<>();
                lines.add("Analysed by SkriptPlus.\n\n");
                try {
                    AtomicLong totalParseTime = new AtomicLong();
                    Files.readAllLines(Paths.get(script.getPath())).forEach(line -> {
                        if (regex.matcher(line).matches()) {
                            lines.add(line);
                            return;
                        }
                        long parseTime = SkriptUtils.getParseTime(line);
                        totalParseTime.addAndGet(parseTime);
                        lines.add("(" + parseTime + "ms) " + line);
                    });
                    String data = String.join("\n", lines);
                    CompletableFuture<HttpResponse<String>> future = HttpUtils.sendPostRequest(new URI(String.format(HASTEBIN_API, "documents")).toURL(), data);
                    future.join();

                    future.exceptionally(ex -> {
                        throw new RuntimeException("Error while analysing a script.", ex);
                    })
                    .thenAccept(request -> {
                        if (request.statusCode() != 200)
                            throw new RuntimeException("Got code " + request.statusCode() + " while trying to analyse a script.");

                        JsonObject response = HttpUtils.parseResponse(request.body());
                        String key = response.get("key").getAsString();
                        send(sender, "Analysed in <yellow>" + totalParseTime + "ms<white>. Click <underlined><yellow><click:open_url:" + String.format(HASTEBIN_API, key) + ">here<reset> to see the results.", true);
                    });

                } catch (IOException | URISyntaxException e) {
                    throw new RuntimeException("Error while analysing a script.", e);
                }
            }

            case "reload-config" -> {
                HandlerList.unregisterAll(new CommandListener());
                if (SkriptPlus.getInstance().getConfig().getBoolean("overwrite-command")) {
                    Bukkit.getPluginManager().registerEvents(new CommandListener(), SkriptPlus.getInstance());
                }
                send(sender, "Reloaded SkriptPlus config.", true);
            }

            case "enable", "disable", "reload" -> {
                if (args.length < 2) {
                    send(sender, "Please enter a script name.", true);
                    break;
                }
                script = ch.njol.skript.SkriptCommand.getScriptFromName(args[1]);
                if (script == null || !script.exists()) {
                    send(sender, "This script doesn't exist.", true);
                    break;
                }
                boolean isCustomReload = SkriptPlus.getInstance().getConfig().getBoolean("custom-errors");
                try (LogHandler logHandler = (isCustomReload ? new RetainingLogHandler() : new RedirectingLogHandler(sender, "")).start();
                     TimingLogHandler timingHandler = new TimingLogHandler().start()) {
                    if (args[0].equalsIgnoreCase("enable")) {
                        if (!script.getName().startsWith("-")) {
                           send(sender, "This script is already enabled.", true);
                          break;
                        }
                        try {
                            // remove the prefix
                            script = ch.njol.skript.util.FileUtils.move(
                                    script,
                                    new File(script.getParentFile(), script.getName().substring(1)),
                                    false
                            );
                        } catch (IOException e) {
                            throw new RuntimeException("Error while enabling a script", e);
                        }
                        Config config = ScriptLoader.loadStructure(script);
                        if (config == null) {
                            send(sender, "This script cannot be enabled.", true);
                            break;
                        }
                        ScriptLoader.loadScripts(Collections.singletonList(config), OpenCloseable.EMPTY);
                    } else if (args[0].equalsIgnoreCase("reload")) {
                        if (args[1].equalsIgnoreCase("config") || args[1].equalsIgnoreCase("all")) {
                            try {
                                Method method = (SkriptConfig.class).getDeclaredMethod("load");
                                method.setAccessible(true);
                                method.invoke(null);
                            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException("Error while reloading the Skript config.", e);
                            }
                        }
                        if (args[1].equalsIgnoreCase("aliases") || args[1].equalsIgnoreCase("all")) {
                            Aliases.clear();
                            Aliases.load();
                            }
                        if (script.getName().startsWith("-")) {
                            send(sender, "This script is disabled.", true);
                            break;
                        }
                        ScriptLoader.reloadScript(script, OpenCloseable.EMPTY);
                    } else if (args[0].equalsIgnoreCase("disable")) {
                        if (script.getName().startsWith("-")) {
                            send(sender, "This script is already disabled.", true);
                            break;
                        }
                        ScriptLoader.unloadScript(script);
                        try {
                            // add prefix
                            ch.njol.skript.util.FileUtils.move(
                                    script,
                                    new File(script.getParentFile(), "-" + script.getName()),
                                    false
                            );
                        } catch (IOException e) {
                            throw new RuntimeException("Error while disabling script file: " + args[0] + ".", e);
                        }
                    }
                }
            }
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
