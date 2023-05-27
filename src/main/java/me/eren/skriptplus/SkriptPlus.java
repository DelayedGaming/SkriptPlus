package me.eren.skriptplus;

import ch.njol.skript.Skript;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public final class SkriptPlus extends JavaPlugin {
    private static SkriptPlus instance;
    private static Properties addonProperties;

    @Override
    public void onEnable() {
        instance = this;
        getLogger().info("Enabled SkriptPlus v" + getDescription().getVersion());
        Skript.registerAddon(this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabled SkriptPlus v" + getDescription().getVersion());
    }

    public static SkriptPlus getInstance() {
        return instance;
    }

    public static Properties getAddonProperties() throws IOException {
        if (addonProperties == null) {
            Properties properties = new Properties();
            File file = new File("./plugins/SkriptPlus/addon.properties");
            FileInputStream stream = new FileInputStream(file);
            properties.load(stream);
            stream.close();
            addonProperties = properties;
        }
        return addonProperties;
    }
}
