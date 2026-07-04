package com.puddingkc.Mirrora.util;

import com.puddingkc.Mirrora.model.FurnitureMirrorConfig;
import com.puddingkc.Mirrora.model.MirrorRegion;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;


public final class MirrorRegionFactory {

    private MirrorRegionFactory() {
    }

    public static MirrorRegion build(String regionId, Location location, float yaw, FurnitureMirrorConfig mirrorConfig) {
        BlockFace face = ReflectionMath.yawToFace(yaw);
        BlockFace right = ReflectionMath.rightOf(face);

        double offsetX = face.getModX() * mirrorConfig.offsetForward() + right.getModX() * mirrorConfig.offsetRight();
        double offsetZ = face.getModZ() * mirrorConfig.offsetForward() + right.getModZ() * mirrorConfig.offsetRight();
        Location anchor = location.clone().add(offsetX, mirrorConfig.offsetY(), offsetZ);

        String worldName = anchor.getWorld().getName();

        double half = mirrorConfig.width() / 2.0;
        double a = (face == BlockFace.NORTH || face == BlockFace.SOUTH) ? anchor.getX() : anchor.getZ();
        double minA = a - half;
        double maxA = a + half;

        double minY = Math.floor(anchor.getY()) - mirrorConfig.extendDown();
        double maxY = Math.floor(anchor.getY()) + mirrorConfig.height();

        double planeCoordinate = switch (face) {
            case NORTH -> Math.floor(anchor.getZ());
            case SOUTH -> Math.floor(anchor.getZ()) + 1.0;
            case WEST -> Math.floor(anchor.getX());
            case EAST -> Math.floor(anchor.getX()) + 1.0;
            default -> 0;
        };

        return new MirrorRegion(regionId, worldName, face, planeCoordinate, minA, maxA, minY, maxY, mirrorConfig.depth());
    }

    public static String locationKey(Location location) {
        return location.getWorld().getName() + "_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ();
    }
}
