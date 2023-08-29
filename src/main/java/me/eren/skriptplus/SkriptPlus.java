package me.eren.skriptplus;

import ch.njol.skript.Skript;
import me.eren.skriptplus.listeners.CommandListener;
import me.eren.skriptplus.utils.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class SkriptPlus extends JavaPlugin {
    private static SkriptPlus instance;
    public static final String PREFIX = "<white>[<gold>Skript<yellow>+<white>] ";
    private static final Properties ADDON_PROPERTIES = new Properties();

    @Override
    public void onEnable() {
        instance = this;
        new Metrics(this, 19422);

        getLogger().info("Enabled SkriptPlus v" + getDescription().getVersion());
        Skript.registerAddon(this);

        this.getCommand("skriptplus").setExecutor(new SkriptCommand());

        this.saveDefaultConfig();
        if (this.getConfig().getBoolean("overwrite-command")) {
            getServer().getPluginManager().registerEvents(new CommandListener(), this);
        }

        // create the addon.properties file if it doesn't exist.
        final File properties = new File(getDataFolder(), "addon.properties");
        try {
            if (!properties.exists() && !properties.createNewFile())
                throw new RuntimeException("addon.properties doesn't exist and can't be created.");
            Files.copy(getResource("addon.properties"), properties.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled SkriptPlus v" + getDescription().getVersion());
    }

    public static SkriptPlus getInstance() {
        return instance;
    }


    public static Properties getAddonProperties() {
        if (!ADDON_PROPERTIES.containsKey("skript")) {
            File file = new File(SkriptPlus.getInstance().getDataFolder(), "/SkriptPlus/addon.properties");
            try (FileInputStream stream = new FileInputStream(file)) {
                ADDON_PROPERTIES.load(stream);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load addon.properties file.", e);
            }
        }
        return ADDON_PROPERTIES;
    }
}
