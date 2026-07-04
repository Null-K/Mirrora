package com.puddingkc.Mirrora.listener;

import com.puddingkc.Mirrora.manager.BlockMirrorRegistry;
import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.model.FurnitureMirrorConfig;
import com.puddingkc.Mirrora.model.MirrorRegion;
import com.puddingkc.Mirrora.storage.FurnitureMirrorLinkStorage;
import com.puddingkc.Mirrora.util.MirrorRegionFactory;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.CustomBlockPlaceEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public record CraftEngineBlockListener(MirrorManager mirrorManager, BlockMirrorRegistry registry,
                                       FurnitureMirrorLinkStorage linkStorage) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onCustomBlockPlace(CustomBlockPlaceEvent event) {
        String blockId = event.customBlock().id().asString();
        FurnitureMirrorConfig mirrorConfig = registry.get(blockId);
        if (mirrorConfig == null) {
            return;
        }

        if (!event.player().hasPermission(CraftEngineFurnitureListener.AUTO_CREATE_PERMISSION)) {
            return;
        }

        Location location = event.location();
        if (location.getWorld() == null) {
            return;
        }

        String locationKey = MirrorRegionFactory.locationKey(location);
        if (linkStorage.getRegionId(locationKey) != null) {
            return;
        }

        String regionId = "block_" + locationKey;
        MirrorRegion region = MirrorRegionFactory.build(regionId, location, event.player().getLocation().getYaw(), mirrorConfig);
        if (mirrorManager.createRegion(region)) {
            linkStorage.put(locationKey, region.id());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCustomBlockBreak(CustomBlockBreakEvent event) {
        Location location = event.location();
        if (location.getWorld() == null) {
            return;
        }

        String locationKey = MirrorRegionFactory.locationKey(location);
        String regionId = linkStorage.remove(locationKey);
        if (regionId != null) {
            mirrorManager.removeRegion(regionId);
        }
    }
}
