package me.eren.skriptplus;

import ch.njol.skript.Skript;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class SkriptPlus extends JavaPlugin {
    private static SkriptPlus INSTANCE;
    private static Properties addonProperties;
    public static final String PREFIX = "<white>[<gold>Skript<yellow>+<white>] ";

    @Override
    public void onEnable() {
        INSTANCE = this;
        getLogger().info("Enabled SkriptPlus v" + getDescription().getVersion());
        Skript.registerAddon(this);

        this.getCommand("skriptplus").setExecutor(new SkriptCommand());

        if (!getDataFolder().exists() && !getDataFolder().mkdir())
            throw new RuntimeException("Data directory doesn't exist and can't be created.");

        final File properties = new File(getDataFolder(), "addon.properties");
        try {
            if (!properties.exists() && !properties.createNewFile())
                throw new RuntimeException("addon.properties doesn't exist but can't be created.");
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
        return INSTANCE;
    }

    public static Properties getAddonProperties() {
        if (addonProperties == null) {
            try {
                Properties properties = new Properties();
                File file = new File("./plugins/SkriptPlus/addon.properties");
                FileInputStream stream = new FileInputStream(file);
                properties.load(stream);
                stream.close();
                addonProperties = properties;
            } catch (IOException e) {
                throw new RuntimeException("Failed to load addon.properties file.", e);
            }
        }
        return addonProperties;
    }
}
