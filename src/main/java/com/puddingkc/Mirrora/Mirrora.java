package com.puddingkc.Mirrora;

import com.puddingkc.Mirrora.command.MirrorCommand;
import com.puddingkc.Mirrora.listener.MirrorAnimationListener;
import com.puddingkc.Mirrora.listener.MirrorWandListener;
import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.util.Lang;
import com.puddingkc.Mirrora.util.WandItemFactory;
import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;


public class Mirrora extends JavaPlugin {

    private MirrorManager mirrorManager;

    @Override
    public void onEnable() {
        Lang.init(this);

        double defaultDepth = getConfig().getDouble("mirror.default-depth", 8.0);
        double maxDepth = getConfig().getDouble("mirror.max-depth", 32.0);
        long tickInterval = Math.max(1, getConfig().getLong("mirror.tick-interval", 1));

        Material wandMaterial = Material.matchMaterial(getConfig().getString("wand.material", "BLAZE_ROD"));
        if (wandMaterial == null) {
            getLogger().warning("The configured wand.material is invalid. Falling back to BLAZE_ROD");
            wandMaterial = Material.BLAZE_ROD;
        }

        mirrorManager = new MirrorManager(this, tickInterval);
        mirrorManager.start();

        SelectionManager selectionManager = new SelectionManager();
        WandItemFactory wandItemFactory = new WandItemFactory(this, wandMaterial);

        MirrorCommand mirrorCommand = new MirrorCommand(this, mirrorManager, selectionManager, wandItemFactory, defaultDepth, maxDepth);
        var command = getCommand("mirror");
        if (command != null) {
            command.setExecutor(mirrorCommand);
            command.setTabCompleter(mirrorCommand);
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(new MirrorAnimationListener(mirrorManager), this);
        getServer().getPluginManager().registerEvents(new MirrorWandListener(wandItemFactory, selectionManager), this);

        getLogger().info("Author: PuddingKC");
    }

    @Override
    public void onDisable() {
        if (mirrorManager != null) {
            mirrorManager.stop();
        }
    }
}
