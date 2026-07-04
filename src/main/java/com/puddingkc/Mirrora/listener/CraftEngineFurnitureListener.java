package com.puddingkc.Mirrora.listener;

import com.puddingkc.Mirrora.manager.FurnitureMirrorRegistry;
import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.model.FurnitureMirrorConfig;
import com.puddingkc.Mirrora.model.MirrorRegion;
import com.puddingkc.Mirrora.storage.FurnitureMirrorLinkStorage;
import com.puddingkc.Mirrora.util.MirrorRegionFactory;
import net.momirealms.craftengine.bukkit.api.event.FurnitureBreakEvent;
import net.momirealms.craftengine.bukkit.api.event.FurniturePlaceEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;


public record CraftEngineFurnitureListener(MirrorManager mirrorManager, FurnitureMirrorRegistry registry,
                                           FurnitureMirrorLinkStorage linkStorage) implements Listener {

    public static final String AUTO_CREATE_PERMISSION = "mirrora.autoCreate";

    @EventHandler(ignoreCancelled = true)
    public void onFurniturePlace(FurniturePlaceEvent event) {
        String furnitureId = event.furniture().id().asString();
        FurnitureMirrorConfig mirrorConfig = registry.get(furnitureId);
        if (mirrorConfig == null) {
            return;
        }

        if (!event.player().hasPermission(AUTO_CREATE_PERMISSION)) {
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

        String regionId = "furniture_" + locationKey;
        MirrorRegion region = MirrorRegionFactory.build(regionId, location, location.getYaw(), mirrorConfig);
        if (mirrorManager.createRegion(region)) {
            linkStorage.put(locationKey, region.id());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFurnitureBreak(FurnitureBreakEvent event) {
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
