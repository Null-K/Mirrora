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
}
