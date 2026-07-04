package com.puddingkc.Mirrora;

import com.puddingkc.Mirrora.command.MirrorCommand;
import com.puddingkc.Mirrora.listener.CraftEngineBlockListener;
import com.puddingkc.Mirrora.listener.CraftEngineFurnitureListener;
import com.puddingkc.Mirrora.listener.MirrorAnimationListener;
import com.puddingkc.Mirrora.listener.MirrorWandListener;
import com.puddingkc.Mirrora.manager.BlockMirrorRegistry;
import com.puddingkc.Mirrora.manager.FurnitureMirrorRegistry;
import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.storage.FurnitureMirrorLinkStorage;
import com.puddingkc.Mirrora.util.Lang;
import com.puddingkc.Mirrora.util.WandItemFactory;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;


public class Mirrora extends JavaPlugin {

    private MirrorManager mirrorManager;
    private FurnitureMirrorRegistry furnitureMirrorRegistry;
    private BlockMirrorRegistry blockMirrorRegistry;

    @Override
    public void onEnable() {
        Lang.init(this);

        double defaultDepth = getConfig().getDouble("mirror.default-depth", 8.0);
        double maxDepth = getConfig().getDouble("mirror.max-depth", 32.0);
        long tickInterval = Math.max(1, getConfig().getLong("mirror.tick-interval", 1));

        boolean blockReflectionEnabled = getConfig().getBoolean("mirror.block-reflection.enabled", false);
        long blockTickInterval = Math.max(1, getConfig().getLong("mirror.block-reflection.tick-interval", 20));
        int maxReflectedBlocks = Math.max(1, getConfig().getInt("mirror.block-reflection.max-blocks", 2048));
        int blockReflectionExpand = Math.max(0, getConfig().getInt("mirror.block-reflection.expand", 1));

        Material wandMaterial = Material.matchMaterial(getConfig().getString("wand.material", "BLAZE_ROD"));
        if (wandMaterial == null) {
            getLogger().warning("The configured wand.material is invalid. Falling back to BLAZE_ROD");
            wandMaterial = Material.BLAZE_ROD;
        }

        mirrorManager = new MirrorManager(this, tickInterval, blockReflectionEnabled, blockTickInterval, maxReflectedBlocks, blockReflectionExpand);
        mirrorManager.start();

        SelectionManager selectionManager = new SelectionManager();
        WandItemFactory wandItemFactory = new WandItemFactory(this, wandMaterial);

        boolean craftEngineEnabled = getServer().getPluginManager().isPluginEnabled("CraftEngine");
        if (craftEngineEnabled) {
            furnitureMirrorRegistry = new FurnitureMirrorRegistry(this);
            furnitureMirrorRegistry.load();

            blockMirrorRegistry = new BlockMirrorRegistry(this);
            blockMirrorRegistry.load();
        }

        MirrorCommand mirrorCommand = new MirrorCommand(this, mirrorManager, selectionManager, wandItemFactory,
                defaultDepth, maxDepth, furnitureMirrorRegistry, blockMirrorRegistry);
        var command = getCommand("mirror");
        if (command != null) {
            command.setExecutor(mirrorCommand);
            command.setTabCompleter(mirrorCommand);
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(new MirrorAnimationListener(mirrorManager), this);
        getServer().getPluginManager().registerEvents(new MirrorWandListener(wandItemFactory, selectionManager), this);

        if (craftEngineEnabled) {
            FurnitureMirrorLinkStorage furnitureLinkStorage = new FurnitureMirrorLinkStorage(this);
            furnitureLinkStorage.load();
            getServer().getPluginManager().registerEvents(
                    new CraftEngineFurnitureListener(mirrorManager, furnitureMirrorRegistry, furnitureLinkStorage), this);

            FurnitureMirrorLinkStorage blockLinkStorage = new FurnitureMirrorLinkStorage(this, "block-mirror-links.yml", "links");
            blockLinkStorage.load();
            getServer().getPluginManager().registerEvents(
                    new CraftEngineBlockListener(mirrorManager, blockMirrorRegistry, blockLinkStorage), this);

            getLogger().info("Detected CraftEngine, mirror furniture/block support enabled.");
        }

        getLogger().info("Author: PuddingKC");
    }

    @Override
    public void onDisable() {
        if (mirrorManager != null) {
            mirrorManager.stop();
        }
    }
}
