package me.eren.skriptplus.utils;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;

public class FileUtils {
    public static void downloadFile(URL url, File file) {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
             fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getFileOfPlugin(Plugin plugin) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method method = (JavaPlugin.class).getDeclaredMethod("getFile");
        method.setAccessible(true);
        return (File) method.invoke(plugin);
    }

    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) throws IOException {
        Files.walk(Paths.get(sourceDirectoryLocation))
            .forEach(source -> {
                Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                                                                        .substring(sourceDirectoryLocation.length()));
                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
    }
}
