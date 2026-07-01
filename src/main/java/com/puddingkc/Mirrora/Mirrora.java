package com.puddingkc.Mirrora;

import com.puddingkc.Mirrora.command.MirrorCommand;
import com.puddingkc.Mirrora.listener.MirrorAnimationListener;
import com.puddingkc.Mirrora.listener.MirrorWandListener;
import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.util.WandItemFactory;
import org.bukkit.plugin.java.JavaPlugin;


public class Mirrora extends JavaPlugin {

    private MirrorManager mirrorManager;

    @Override
    public void onEnable() {
        mirrorManager = new MirrorManager(this);
        mirrorManager.start();

        SelectionManager selectionManager = new SelectionManager();
        WandItemFactory wandItemFactory = new WandItemFactory(this);

        MirrorCommand mirrorCommand = new MirrorCommand(mirrorManager, selectionManager, wandItemFactory);
        var command = getCommand("mirror");
        if (command != null) {
            command.setExecutor(mirrorCommand);
            command.setTabCompleter(mirrorCommand);
        } else {
            getServer().getPluginManager().disablePlugin(this);
        }

        getServer().getPluginManager().registerEvents(new MirrorAnimationListener(mirrorManager), this);
        getServer().getPluginManager().registerEvents(new MirrorWandListener(wandItemFactory, selectionManager), this);
    }

    @Override
    public void onDisable() {
        if (mirrorManager != null) {
            mirrorManager.stop();
        }
    }
}
