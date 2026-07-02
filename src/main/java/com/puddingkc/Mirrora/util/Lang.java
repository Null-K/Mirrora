package com.puddingkc.Mirrora.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;


public final class Lang {

    private static final String DEFAULT_LANGUAGE = "zh_cn";

    private static YamlConfiguration messages;

    private Lang() {
    }

    public static void init(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.saveResource("lang/zh_cn.yml", false);
        plugin.saveResource("lang/en_us.yml", false);

        String language = plugin.getConfig().getString("language", DEFAULT_LANGUAGE);

        File langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("Language file '" + language + "' not found. Falling back to " + DEFAULT_LANGUAGE);
            langFile = new File(plugin.getDataFolder(), "lang/" + DEFAULT_LANGUAGE + ".yml");
        }

        messages = YamlConfiguration.loadConfiguration(langFile);

        try (InputStream defaultStream = plugin.getResource("lang/" + DEFAULT_LANGUAGE + ".yml")) {
            if (defaultStream != null) {
                messages.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaultStream, StandardCharsets.UTF_8)));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load the default language file.", e);
        }
    }

    public static String get(String path) {
        if (messages == null) {
            return path;
        }
        return messages.getString(path, path);
    }
}
