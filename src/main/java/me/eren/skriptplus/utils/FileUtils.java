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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FileUtils {
    public static void downloadFile(URL url, File file) {
        try (ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream());
             FileOutputStream fileOutputStream = new FileOutputStream(file)) {
             fileOutputStream.getChannel().transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static File getFileOfPlugin(Plugin plugin) {
        try {
            Method method = (JavaPlugin.class).getDeclaredMethod("getFile");
            method.setAccessible(true);
            return (File) method.invoke(plugin);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Error while getting the file of a plugin.", e);
        }
    }

    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) {
        try (Stream<Path> pathStream = Files.walk(Paths.get(sourceDirectoryLocation), FileVisitOption.FOLLOW_LINKS)) {
            pathStream.forEach(source -> {
                Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                                                                        .substring(sourceDirectoryLocation.length()));
                try {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            throw new RuntimeException("Error while copying a directory.", e);
        }
    }

    public static List<String> getFileTree(String sourceDirectoryLocation) {
        List<String> tree = new ArrayList<>();
        Path path = Paths.get(sourceDirectoryLocation);
        int sourceCount = path.getNameCount();
        try (Stream<Path> pathStream = Files.walk(path, FileVisitOption.FOLLOW_LINKS)) {
            pathStream.forEach(file -> {
                if (file.getNameCount() == sourceCount) return;
                String indentation = " ".repeat((file.getNameCount() - sourceCount - 1) * 4);
                String fileColor;
                if (file.getFileName().toString().endsWith(".sk")) {
                    fileColor = file.getFileName().toString().startsWith("-") ? "<red>" : "<green>";
                } else if (Files.isDirectory(file)) {
                    fileColor = "<gold>";
                } else {
                    fileColor = "<#8f8f8f>";
                }
                tree.add(indentation + (indentation.equals("") ? "" : "<gray>└ ") + fileColor + file.getFileName());
            });
        } catch (IOException e) {
            throw new RuntimeException("Error while getting a file tree.", e);
        }
        return tree;
    }
}
