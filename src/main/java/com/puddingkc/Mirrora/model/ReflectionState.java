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
}
