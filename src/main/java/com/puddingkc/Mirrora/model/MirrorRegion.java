package com.puddingkc.Mirrora.model;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;


public record MirrorRegion(String id, String worldName, BlockFace face, double planeCoordinate, double minA,
                           double maxA, double minY, double maxY, double depth) {

    public boolean isInRange(Player player) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null || !loc.getWorld().getName().equals(worldName)) {
            return false;
        }

        double a; // 沿矩形宽度方向的坐标
        double normalDistance; // 玩家到镜面平面的带符号距离（沿法线方向为正）

        switch (face) {
            case NORTH -> {
                a = loc.getX();
                // 法线指向 NORTH（-Z），玩家在镜面 -Z 一侧时 normalDistance 为正
                normalDistance = planeCoordinate - loc.getZ();
            }
            case SOUTH -> {
                a = loc.getX();
                normalDistance = loc.getZ() - planeCoordinate;
            }
            case EAST -> {
                a = loc.getZ();
                normalDistance = loc.getX() - planeCoordinate;
            }
            case WEST -> {
                a = loc.getZ();
                normalDistance = planeCoordinate - loc.getX();
            }
            default -> {
                return false;
            }
        }

        if (a < minA || a > maxA) {
            return false;
        }
        if (loc.getY() < minY || loc.getY() > maxY) {
            return false;
        }
        return normalDistance > 0 && normalDistance <= depth;
    }
}
