package com.puddingkc.Mirrora.storage;

import com.puddingkc.Mirrora.model.MirrorRegion;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


public class MirrorStorage {

    private final JavaPlugin plugin;
    private final File file;

    public MirrorStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "mirrors.yml");
    }

    public List<MirrorRegion> load() {
        List<MirrorRegion> regions = new ArrayList<>();
        if (!file.exists()) {
            return regions;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = config.getConfigurationSection("mirrors");
        if (root == null) {
            return regions;
        }

        for (String id : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(id);
            if (section == null) {
                continue;
            }
            try {
                String worldName = section.getString("world");
                BlockFace face = BlockFace.valueOf(section.getString("face"));
                double planeCoordinate = section.getDouble("plane-coordinate");
                double minA = section.getDouble("min-a");
                double maxA = section.getDouble("max-a");
                double minY = section.getDouble("min-y");
                double maxY = section.getDouble("max-y");
                double depth = section.getDouble("depth");

                regions.add(new MirrorRegion(id, worldName, face, planeCoordinate, minA, maxA, minY, maxY, depth));
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load mirror configuration '" + id + "'. Skipping.", e);
            }
        }
        return regions;
    }

    public void save(List<MirrorRegion> regions) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection root = config.createSection("mirrors");

        for (MirrorRegion region : regions) {
            ConfigurationSection section = root.createSection(region.id());
            section.set("world", region.worldName());
            section.set("face", region.face().name());
            section.set("plane-coordinate", region.planeCoordinate());
            section.set("min-a", region.minA());
            section.set("max-a", region.maxA());
            section.set("min-y", region.minY());
            section.set("max-y", region.maxY());
            section.set("depth", region.depth());
        }

        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save mirror configuration.", e);
        }
    }
}
