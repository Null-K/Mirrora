package com.puddingkc.Mirrora.manager;

import com.puddingkc.Mirrora.model.FurnitureMirrorConfig;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class BlockMirrorRegistry {

    private final JavaPlugin plugin;
    private final File file;
    private final Map<String, FurnitureMirrorConfig> configs = new ConcurrentHashMap<>();

    public BlockMirrorRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "block-mirrors.yml");
    }

    public void load() {
        configs.clear();

        if (!file.exists()) {
            plugin.saveResource("block-mirrors.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("block-mirrors");
        if (root == null) {
            return;
        }

        for (String blockId : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(blockId);
            if (section == null) {
                continue;
            }
            double width = section.getDouble("width", 1.0);
            double height = section.getDouble("height", 2.0);
            double depth = section.getDouble("depth", 8.0);
            double extendDown = section.getDouble("extend-down", 0.0);
            double offsetForward = section.getDouble("offset.forward", 0.0);
            double offsetRight = section.getDouble("offset.right", 0.0);
            double offsetY = section.getDouble("offset.y", 0.0);
            configs.put(blockId, new FurnitureMirrorConfig(blockId, width, height, depth,
                    extendDown, offsetForward, offsetRight, offsetY));
        }
    }

    public FurnitureMirrorConfig get(String blockId) {
        return configs.get(blockId);
    }
}
