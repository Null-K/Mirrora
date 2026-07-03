package com.puddingkc.Mirrora.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;


public final class ReflectionMath {

    private ReflectionMath() {
    }

    public static boolean isHorizontal(BlockFace face) {
        return face == BlockFace.NORTH || face == BlockFace.SOUTH
                || face == BlockFace.EAST || face == BlockFace.WEST;
    }

    private static final double MIRROR_DEPTH_OFFSET = 1.0;

    /**
     * 计算玩家位置相对镜面的镜像位置
     *
     * @param mirrorFace      镜子所贴的墙面朝向
     * @param planeCoordinate 镜面所在的平面坐标：NORTH/SOUTH 时为 Z 坐标，EAST/WEST 时为 X 坐标
     * @param playerLoc       玩家当前位置
     * @return 镜像后的位置
     */
    public static Location reflectPosition(BlockFace mirrorFace, double planeCoordinate, Location playerLoc) {
        double x = playerLoc.getX();
        double y = playerLoc.getY();
        double z = playerLoc.getZ();

        if (mirrorFace == BlockFace.NORTH || mirrorFace == BlockFace.SOUTH) {
            z = 2 * planeCoordinate - z - mirrorFace.getModZ() * MIRROR_DEPTH_OFFSET;
        } else if (mirrorFace == BlockFace.EAST || mirrorFace == BlockFace.WEST) {
            x = 2 * planeCoordinate - x - mirrorFace.getModX() * MIRROR_DEPTH_OFFSET;
        }

        Location result = playerLoc.clone();
        result.setX(x);
        result.setY(y);
        result.setZ(z);
        return result;
    }

    public static float reflectYaw(BlockFace mirrorFace, float yaw) {
        float result;
        if (mirrorFace == BlockFace.NORTH || mirrorFace == BlockFace.SOUTH) {
            result = 180f - yaw;
        } else {
            result = -yaw;
        }

        result = result % 360f;
        if (result >= 180f) {
            result -= 360f;
        } else if (result < -180f) {
            result += 360f;
        }
        return result;
    }

    public static float reflectPitch(float pitch) {
        return pitch;
    }

    /**
     * 计算方块相对镜面的镜像方块坐标
     *
     * @param mirrorFace      镜子所贴的墙面朝向
     * @param planeCoordinate 镜面所在的平面坐标：NORTH/SOUTH 时为 Z 坐标，EAST/WEST 时为 X 坐标
     * @param blockX          源方块 X
     * @param blockY          源方块 Y
     * @param blockZ          源方块 Z
     * @return 镜像后的方块坐标 [x, y, z]
     */
    public static int[] reflectBlockPosition(BlockFace mirrorFace, double planeCoordinate, int blockX, int blockY, int blockZ) {
        int plane = (int) Math.floor(planeCoordinate);
        int x = blockX;
        int z = blockZ;

        if (mirrorFace == BlockFace.NORTH || mirrorFace == BlockFace.SOUTH) {
            z = 2 * plane - blockZ - 1 - mirrorFace.getModZ();
        } else if (mirrorFace == BlockFace.EAST || mirrorFace == BlockFace.WEST) {
            x = 2 * plane - blockX - 1 - mirrorFace.getModX();
        }

        return new int[]{x, blockY, z};
    }

    /**
     * 镜像一个有朝向的方块
     * NORTH/SOUTH 镜面翻转 Z 分量，EAST/WEST 镜面翻转 X 分量；UP/DOWN 及与镜面轴向垂直的分量不变
     *
     * @param mirrorFace 镜子所贴的墙面朝向
     * @param direction  源方块的朝向
     * @return 镜像后的朝向，若无法匹配到合法的 BlockFace 则返回原朝向
     */
    public static BlockFace reflectBlockFace(BlockFace mirrorFace, BlockFace direction) {
        if (direction == BlockFace.UP || direction == BlockFace.DOWN || direction == BlockFace.SELF) {
            return direction;
        }

        int modX = direction.getModX();
        int modY = direction.getModY();
        int modZ = direction.getModZ();

        if (mirrorFace == BlockFace.NORTH || mirrorFace == BlockFace.SOUTH) {
            modZ = -modZ;
        } else if (mirrorFace == BlockFace.EAST || mirrorFace == BlockFace.WEST) {
            modX = -modX;
        } else {
            return direction;
        }

        for (BlockFace candidate : BlockFace.values()) {
            if (candidate.getModX() == modX && candidate.getModY() == modY && candidate.getModZ() == modZ) {
                return candidate;
            }
        }
        return direction;
    }
}
