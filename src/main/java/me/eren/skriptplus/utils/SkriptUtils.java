package me.eren.skriptplus.utils;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.SkriptConfig;
import ch.njol.skript.lang.Statement;
import org.bukkit.Bukkit;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SkriptUtils {
    public static long getParseTime(String statement) {
        long start = System.currentTimeMillis();
        Statement.parse(statement, null);
        long end = System.currentTimeMillis();
        return end - start;
    }

    public static List<String> getEnabledDependencies() {
        List<String> dependencies = new ArrayList<>();
        for(String plugin : Bukkit.getPluginManager().getPlugin("Skript").getDescription().getSoftDepend()) {
            if (Bukkit.getPluginManager().isPluginEnabled(plugin)) {
                dependencies.add(plugin);
            }
        }
        return dependencies;
    }

    public static List<String> getEnabledAddons() {
        return Skript.getAddons().stream()
                .map(SkriptAddon::getName)
                .collect(Collectors.toList());
    }

    public static Object executeMethod(Class<?> clazz, String method, Object[] params) {
        try {
            Method m = clazz.getDeclaredMethod(method);
            m.setAccessible(true);
            return m.invoke(params);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error while running '" + method + "' from '" + clazz + "'.", e);
        }
    }
}
