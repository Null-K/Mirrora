package com.puddingkc.Mirrora.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;


public class FurnitureMirrorLinkStorage {

    private final JavaPlugin plugin;
    private final File file;
    private final String sectionName;
    private final Map<String, String> links = new ConcurrentHashMap<>();

    public FurnitureMirrorLinkStorage(JavaPlugin plugin) {
        this(plugin, "furniture-mirror-links.yml", "links");
    }

    public FurnitureMirrorLinkStorage(JavaPlugin plugin, String fileName, String sectionName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
        this.sectionName = sectionName;
    }

    public void load() {
        links.clear();
        if (!file.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection(sectionName);
        if (root == null) {
            return;
        }
        for (String locationKey : root.getKeys(false)) {
            String regionId = root.getString(locationKey);
            if (regionId != null) {
                links.put(locationKey, regionId);
            }
        }
    }

    public String getRegionId(String locationKey) {
        return links.get(locationKey);
    }

    public void put(String locationKey, String regionId) {
        links.put(locationKey, regionId);
        save();
    }

    public String remove(String locationKey) {
        String removed = links.remove(locationKey);
        save();
        return removed;
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection(sectionName);
        for (Map.Entry<String, String> entry : links.entrySet()) {
            root.set(entry.getKey(), entry.getValue());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save mirror links.", e);
        }
    }
}
