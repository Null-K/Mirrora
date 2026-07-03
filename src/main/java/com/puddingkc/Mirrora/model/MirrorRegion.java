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

    /**
     * 镜面前用于方块反射探测的方块坐标范围
     *
     * @param expand 在镜子矩形宽度/高度方向各自向外膨胀的格数
     */
    public BlockBounds blockReflectionBounds(int expand) {
        int minABlock = (int) Math.floor(minA) - expand;
        int maxABlock = (int) Math.ceil(maxA) - 1 + expand;
        int minYBlock = (int) Math.floor(minY) - expand;
        int maxYBlock = (int) Math.ceil(maxY) - 1 + expand;
        int plane = (int) Math.floor(planeCoordinate);
        int depthBlocks = (int) Math.ceil(depth);

        int minX, maxX, minZ, maxZ;
        switch (face) {
            case NORTH -> {
                minX = minABlock;
                maxX = maxABlock;
                minZ = plane - depthBlocks;
                maxZ = plane - 1;
            }
            case SOUTH -> {
                minX = minABlock;
                maxX = maxABlock;
                minZ = plane;
                maxZ = plane + depthBlocks - 1;
            }
            case EAST -> {
                minZ = minABlock;
                maxZ = maxABlock;
                minX = plane;
                maxX = plane + depthBlocks - 1;
            }
            case WEST -> {
                minZ = minABlock;
                maxZ = maxABlock;
                minX = plane - depthBlocks;
                maxX = plane - 1;
            }
            default -> throw new IllegalStateException("Unsupported mirror face: " + face);
        }

        return new BlockBounds(minX, maxX, minYBlock, maxYBlock, minZ, maxZ);
    }

    public record BlockBounds(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
        public long blockCount() {
            return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        }
    }
}
