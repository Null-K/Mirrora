package com.puddingkc.Mirrora.model;

import java.util.UUID;


public class ReflectionState {

    private final int fakeEntityId;
    private final UUID fakeEntityUuid;

    private String lastPoseName;
    private String lastMainHandName;
    private byte lastSkinPartsRaw = -1;
    private byte lastHandStateRaw = -1;
    private org.bukkit.inventory.ItemStack[] lastEquipmentSnapshot;
    private boolean everSpawned = false;

    private double lastX = Double.NaN;
    private double lastY = Double.NaN;
    private double lastZ = Double.NaN;
    private float lastYaw = Float.NaN;
    private float lastPitch = Float.NaN;

    public ReflectionState(int fakeEntityId, UUID fakeEntityUuid) {
        this.fakeEntityId = fakeEntityId;
        this.fakeEntityUuid = fakeEntityUuid;
    }

    public int getFakeEntityId() {
        return fakeEntityId;
    }

    public UUID getFakeEntityUuid() {
        return fakeEntityUuid;
    }

    public String getLastPoseName() {
        return lastPoseName;
    }

    public void setLastPoseName(String lastPoseName) {
        this.lastPoseName = lastPoseName;
    }

    public String getLastMainHandName() {
        return lastMainHandName;
    }

    public void setLastMainHandName(String lastMainHandName) {
        this.lastMainHandName = lastMainHandName;
    }

    public byte getLastSkinPartsRaw() {
        return lastSkinPartsRaw;
    }

    public void setLastSkinPartsRaw(byte lastSkinPartsRaw) {
        this.lastSkinPartsRaw = lastSkinPartsRaw;
    }

    public byte getLastHandStateRaw() {
        return lastHandStateRaw;
    }

    public void setLastHandStateRaw(byte lastHandStateRaw) {
        this.lastHandStateRaw = lastHandStateRaw;
    }

    public org.bukkit.inventory.ItemStack[] getLastEquipmentSnapshot() {
        return lastEquipmentSnapshot;
    }

    public void setLastEquipmentSnapshot(org.bukkit.inventory.ItemStack[] lastEquipmentSnapshot) {
        this.lastEquipmentSnapshot = lastEquipmentSnapshot;
    }

    public boolean isEverSpawned() {
        return everSpawned;
    }

    public void markSpawned() {
        this.everSpawned = true;
    }

    public boolean isPositionKnown() {
        return !Double.isNaN(lastX);
    }

    public double getLastX() { return lastX; }
    public double getLastY() { return lastY; }
    public double getLastZ() { return lastZ; }
    public float getLastYaw() { return lastYaw; }
    public float getLastPitch() { return lastPitch; }

    public void updatePosition(double x, double y, double z, float yaw, float pitch) {
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.lastYaw = yaw;
        this.lastPitch = pitch;
    }
}
