package com.puddingkc.Mirrora.manager;

import com.destroystokyo.paper.ClientOption;
import com.destroystokyo.paper.SkinParts;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.entity.pose.EntityPose;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.HumanoidArm;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBlockChange;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityAnimation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityHeadLook;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityTeleport;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.puddingkc.Mirrora.model.MirrorRegion;
import com.puddingkc.Mirrora.model.ReflectionState;
import com.puddingkc.Mirrora.storage.MirrorStorage;
import com.puddingkc.Mirrora.util.FakeEntityIdAllocator;
import com.puddingkc.Mirrora.util.ProfileUtil;
import com.puddingkc.Mirrora.util.ReflectionMath;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Rotatable;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MainHand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class MirrorManager {

    private final JavaPlugin plugin;
    private final MirrorStorage storage;
    private final List<MirrorRegion> regions = new CopyOnWriteArrayList<>();

    private final Map<String, Set<UUID>> regionOccupants = new ConcurrentHashMap<>();

    private final Map<String, Map<UUID, ReflectionState>> reflectionStates = new ConcurrentHashMap<>();

    private final Map<String, Map<Long, WrappedBlockState>> blockSnapshots = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> blockSyncedObservers = new ConcurrentHashMap<>();

    private long tickInterval;

    private boolean blockReflectionEnabled;
    private long blockTickInterval;
    private int maxReflectedBlocks;
    private int blockReflectionExpand;

    private BukkitTask tickTask;
    private BukkitTask blockTickTask;

    public MirrorManager(JavaPlugin plugin, long tickInterval, boolean blockReflectionEnabled,
                          long blockTickInterval, int maxReflectedBlocks, int blockReflectionExpand) {
        this.plugin = plugin;
        this.storage = new MirrorStorage(plugin);
        this.tickInterval = tickInterval;
        this.blockReflectionEnabled = blockReflectionEnabled;
        this.blockTickInterval = blockTickInterval;
        this.maxReflectedBlocks = maxReflectedBlocks;
        this.blockReflectionExpand = blockReflectionExpand;
    }

    public void start() {
        regions.addAll(storage.load());
        tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, tickInterval, tickInterval);
        restartBlockTickTask();
    }

    public void setTickInterval(long tickInterval) {
        this.tickInterval = tickInterval;
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, tickInterval, tickInterval);
        }
    }

    public void setBlockReflectionSettings(boolean enabled, long blockTickInterval, int maxReflectedBlocks, int blockReflectionExpand) {
        this.blockReflectionEnabled = enabled;
        this.blockTickInterval = blockTickInterval;
        this.maxReflectedBlocks = maxReflectedBlocks;
        this.blockReflectionExpand = blockReflectionExpand;
        if (!enabled) {
            revertAllBlockReflections();
        }
        restartBlockTickTask();
    }

    private void restartBlockTickTask() {
        if (blockTickTask != null) {
            blockTickTask.cancel();
            blockTickTask = null;
        }
        if (blockReflectionEnabled) {
            blockTickTask = plugin.getServer().getScheduler().runTaskTimer(
                    plugin, this::blockTick, blockTickInterval, blockTickInterval);
        }
    }

    public void stop() {
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        if (blockTickTask != null) {
            blockTickTask.cancel();
            blockTickTask = null;
        }

        for (MirrorRegion region : regions) {
            Set<UUID> occupants = regionOccupants.getOrDefault(region.id(), Set.of());
            for (UUID observerId : occupants) {
                Player observer = plugin.getServer().getPlayer(observerId);
                if (observer != null && observer.isOnline()) {
                    for (UUID reflectedId : occupants) {
                        destroyReflectionFor(observer, region, reflectedId);
                    }
                }
            }
        }
        regionOccupants.clear();
        reflectionStates.clear();

        revertAllBlockReflections();
    }

    private void revertAllBlockReflections() {
        for (MirrorRegion region : regions) {
            revertBlockReflectionsForRegion(region);
        }
        blockSnapshots.clear();
        blockSyncedObservers.clear();
    }

    private void revertBlockReflectionsForRegion(MirrorRegion region) {
        Set<UUID> synced = blockSyncedObservers.remove(region.id());
        Map<Long, WrappedBlockState> snapshot = blockSnapshots.remove(region.id());
        if (synced == null || snapshot == null || snapshot.isEmpty()) {
            return;
        }
        for (UUID observerId : synced) {
            Player observer = plugin.getServer().getPlayer(observerId);
            if (observer != null && observer.isOnline()) {
                sendRealBlocks(observer, region, snapshot.keySet());
            }
        }
    }

    public List<MirrorRegion> getRegions() {
        return regions;
    }

    public MirrorRegion findRegion(String id) {
        for (MirrorRegion region : regions) {
            if (region.id().equalsIgnoreCase(id)) {
                return region;
            }
        }
        return null;
    }

    public boolean createRegion(MirrorRegion region) {
        if (findRegion(region.id()) != null) {
            return false;
        }
        regions.add(region);
        storage.save(regions);
        return true;
    }

    public boolean removeRegion(String id) {
        MirrorRegion region = findRegion(id);
        if (region == null) {
            return false;
        }

        Set<UUID> occupants = regionOccupants.remove(region.id());
        if (occupants != null) {
            for (UUID observerId : occupants) {
                Player observer = plugin.getServer().getPlayer(observerId);
                if (observer != null && observer.isOnline()) {
                    for (UUID reflectedId : occupants) {
                        destroyReflectionFor(observer, region, reflectedId);
                    }
                }
            }
        }
        reflectionStates.remove(region.id());

        revertBlockReflectionsForRegion(region);

        regions.remove(region);
        storage.save(regions);
        return true;
    }

    private void tick() {
        if (regions.isEmpty()) {
            return;
        }
        for (MirrorRegion region : regions) {
            tickRegion(region);
        }
    }

    private void tickRegion(MirrorRegion region) {
        Set<UUID> current = new HashSet<>();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (isClientSupported(player) && region.isInRange(player)) {
                current.add(player.getUniqueId());
            }
        }

        Set<UUID> previous = regionOccupants.getOrDefault(region.id(), Set.of());

        Set<UUID> allInvolved = new HashSet<>(previous);
        allInvolved.addAll(current);

        Map<UUID, ReflectionState> states = reflectionStates.computeIfAbsent(region.id(), k -> new ConcurrentHashMap<>());

        for (UUID observerId : allInvolved) {
            Player observer = plugin.getServer().getPlayer(observerId);
            if (observer == null || !observer.isOnline()) {
                continue;
            }

            boolean observerStaying = current.contains(observerId);
            boolean observerWasStaying = previous.contains(observerId);

            Set<UUID> shouldSee = observerStaying ? current : Set.of();
            Set<UUID> wasSeeing = observerWasStaying ? previous : Set.of();

            for (UUID reflectedId : current) {
                ReflectionState state = states.computeIfAbsent(reflectedId, id -> newReflectionState());
                if (wasSeeing.contains(reflectedId)) {
                    if (shouldSee.contains(reflectedId)) {
                        updateReflection(observer, region, reflectedId, state);
                    }
                } else if (shouldSee.contains(reflectedId)) {
                    spawnReflection(observer, region, reflectedId, state);
                }
            }

            for (UUID reflectedId : wasSeeing) {
                if (!shouldSee.contains(reflectedId)) {
                    destroyReflectionFor(observer, region, reflectedId);
                }
            }
        }

        states.keySet().removeIf(id -> !current.contains(id));

        regionOccupants.put(region.id(), current);
    }

    private ReflectionState newReflectionState() {
        int fakeId = FakeEntityIdAllocator.next();
        return new ReflectionState(fakeId, UUID.randomUUID());
    }

    private boolean isClientSupported(Player player) {
        User user = PacketEvents.getAPI().getPlayerManager().getUser(player);
        return user != null && user.getClientVersion().isNewerThanOrEquals(ClientVersion.V_1_21_9);
    }

    private void spawnReflection(Player observer, MirrorRegion region, UUID reflectedId, ReflectionState state) {
        Player reflected = plugin.getServer().getPlayer(reflectedId);
        if (reflected == null || !reflected.isOnline()) {
            return;
        }

        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        ReflectedTransform transform = computeReflectedTransform(region, reflected.getLocation());

        WrapperPlayServerSpawnEntity spawnPacket = new WrapperPlayServerSpawnEntity(
                state.getFakeEntityId(),
                java.util.Optional.of(state.getFakeEntityUuid()),
                EntityTypes.MANNEQUIN,
                transform.position,
                transform.pitch,
                transform.yaw,
                transform.yaw,
                0,
                java.util.Optional.empty()
        );
        playerManager.sendPacket(observer, spawnPacket);
        state.markSpawned();

        sendFullMetadata(observer, reflected, state);
        sendEquipment(observer, reflected, state, true);
    }

    private static final double POS_EPSILON = 1e-4;
    private static final float ROT_EPSILON = 0.1f;

    private void updateReflection(Player observer, MirrorRegion region, UUID reflectedId, ReflectionState state) {
        Player reflected = plugin.getServer().getPlayer(reflectedId);
        if (reflected == null || !reflected.isOnline()) {
            return;
        }

        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        ReflectedTransform transform = computeReflectedTransform(region, reflected.getLocation());

        boolean moved = !state.isPositionKnown()
                || Math.abs(transform.position().getX() - state.getLastX()) > POS_EPSILON
                || Math.abs(transform.position().getY() - state.getLastY()) > POS_EPSILON
                || Math.abs(transform.position().getZ() - state.getLastZ()) > POS_EPSILON
                || Math.abs(transform.yaw() - state.getLastYaw()) > ROT_EPSILON
                || Math.abs(transform.pitch() - state.getLastPitch()) > ROT_EPSILON;

        if (moved) {
            playerManager.sendPacket(observer, new WrapperPlayServerEntityTeleport(
                    state.getFakeEntityId(), transform.position(), transform.yaw(), transform.pitch(), true
            ));
            playerManager.sendPacket(observer, new WrapperPlayServerEntityHeadLook(
                    state.getFakeEntityId(), transform.yaw()
            ));
            state.updatePosition(
                    transform.position().getX(), transform.position().getY(), transform.position().getZ(),
                    transform.yaw(), transform.pitch()
            );
        }

        sendChangedMetadata(observer, reflected, state);
        sendEquipment(observer, reflected, state, false);
    }

    private void destroyReflectionFor(Player observer, MirrorRegion region, UUID reflectedId) {
        Map<UUID, ReflectionState> states = reflectionStates.get(region.id());
        if (states == null) {
            return;
        }
        ReflectionState state = states.get(reflectedId);
        if (state == null || !state.isEverSpawned()) {
            return;
        }
        PacketEvents.getAPI().getPlayerManager().sendPacket(observer,
                new WrapperPlayServerDestroyEntities(state.getFakeEntityId()));
    }

    private ReflectedTransform computeReflectedTransform(MirrorRegion region, Location playerLoc) {
        Location reflectedPos = ReflectionMath.reflectPosition(
                region.face(), region.planeCoordinate(), playerLoc
        );
        float yaw = ReflectionMath.reflectYaw(region.face(), playerLoc.getYaw());
        float pitch = ReflectionMath.reflectPitch(playerLoc.getPitch());
        Vector3d position = new Vector3d(reflectedPos.getX(), reflectedPos.getY(), reflectedPos.getZ());
        return new ReflectedTransform(position, yaw, pitch);
    }

    private void sendFullMetadata(Player observer, Player reflected, ReflectionState state) {
        List<EntityData<?>> data = new ArrayList<>();

        byte handState = resolveHandState(reflected);
        data.add(new EntityData<>(8, EntityDataTypes.BYTE, handState));

        EntityPose pose = resolvePose(reflected);
        data.add(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, pose));

        HumanoidArm mainHand = resolveReflectedMainHand(reflected);
        data.add(new EntityData<>(15, EntityDataTypes.HUMANOID_ARM, mainHand));

        byte skinParts = resolveSkinParts(reflected);
        data.add(new EntityData<>(16, EntityDataTypes.BYTE, skinParts));

        data.add(new EntityData<>(17, EntityDataTypes.RESOLVABLE_PROFILE, ProfileUtil.toItemProfile(reflected)));
        data.add(new EntityData<>(18, EntityDataTypes.BOOLEAN, Boolean.TRUE));

        PacketEvents.getAPI().getPlayerManager().sendPacket(observer,
                new WrapperPlayServerEntityMetadata(state.getFakeEntityId(), data));

        state.setLastHandStateRaw(handState);
        state.setLastPoseName(pose.name());
        state.setLastMainHandName(mainHand.name());
        state.setLastSkinPartsRaw(skinParts);
    }

    private void sendChangedMetadata(Player observer, Player reflected, ReflectionState state) {
        List<EntityData<?>> changed = new ArrayList<>();

        byte handState = resolveHandState(reflected);
        if (handState != state.getLastHandStateRaw()) {
            changed.add(new EntityData<>(8, EntityDataTypes.BYTE, handState));
            state.setLastHandStateRaw(handState);
        }

        EntityPose pose = resolvePose(reflected);
        if (!pose.name().equals(state.getLastPoseName())) {
            changed.add(new EntityData<>(6, EntityDataTypes.ENTITY_POSE, pose));
            state.setLastPoseName(pose.name());
        }

        HumanoidArm mainHand = resolveReflectedMainHand(reflected);
        if (!mainHand.name().equals(state.getLastMainHandName())) {
            changed.add(new EntityData<>(15, EntityDataTypes.HUMANOID_ARM, mainHand));
            state.setLastMainHandName(mainHand.name());
        }

        byte skinParts = resolveSkinParts(reflected);
        if (skinParts != state.getLastSkinPartsRaw()) {
            changed.add(new EntityData<>(16, EntityDataTypes.BYTE, skinParts));
            state.setLastSkinPartsRaw(skinParts);
        }

        if (!changed.isEmpty()) {
            PacketEvents.getAPI().getPlayerManager().sendPacket(observer,
                    new WrapperPlayServerEntityMetadata(state.getFakeEntityId(), changed));
        }
    }

    private void sendEquipment(Player observer, Player reflected, ReflectionState state, boolean force) {
        EntityEquipment equipment = reflected.getEquipment();

        org.bukkit.inventory.ItemStack[] snapshot = new org.bukkit.inventory.ItemStack[]{
                equipment.getItemInMainHand(),
                equipment.getItemInOffHand(),
                equipment.getHelmet(),
                equipment.getChestplate(),
                equipment.getLeggings(),
                equipment.getBoots()
        };

        if (!force && equipmentEquals(snapshot, state.getLastEquipmentSnapshot())) {
            return;
        }

        List<Equipment> list = new ArrayList<>();
        list.add(new Equipment(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.MAIN_HAND, toPacketItem(snapshot[0])));
        list.add(new Equipment(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.OFF_HAND, toPacketItem(snapshot[1])));
        list.add(new Equipment(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.HELMET, toPacketItem(snapshot[2])));
        list.add(new Equipment(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.CHEST_PLATE, toPacketItem(snapshot[3])));
        list.add(new Equipment(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.LEGGINGS, toPacketItem(snapshot[4])));
        list.add(new Equipment(com.github.retrooper.packetevents.protocol.player.EquipmentSlot.BOOTS, toPacketItem(snapshot[5])));

        PacketEvents.getAPI().getPlayerManager().sendPacket(observer,
                new WrapperPlayServerEntityEquipment(state.getFakeEntityId(), list));

        org.bukkit.inventory.ItemStack[] clonedSnapshot = new org.bukkit.inventory.ItemStack[snapshot.length];
        for (int i = 0; i < snapshot.length; i++) {
            clonedSnapshot[i] = snapshot[i] == null ? null : snapshot[i].clone();
        }
        state.setLastEquipmentSnapshot(clonedSnapshot);
    }

    private boolean equipmentEquals(org.bukkit.inventory.ItemStack[] a, org.bukkit.inventory.ItemStack[] b) {
        if (b == null) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            org.bukkit.inventory.ItemStack itemA = a[i];
            org.bukkit.inventory.ItemStack itemB = b[i];
            boolean aEmpty = itemA == null || itemA.getType().isAir();
            boolean bEmpty = itemB == null || itemB.getType().isAir();
            if (aEmpty && bEmpty) {
                continue;
            }
            if (aEmpty != bEmpty) {
                return false;
            }
            if (!itemA.equals(itemB)) {
                return false;
            }
        }
        return true;
    }

    private ItemStack toPacketItem(org.bukkit.inventory.ItemStack stack) {
        if (stack == null) {
            return ItemStack.EMPTY;
        }
        return SpigotConversionUtil.fromBukkitItemStack(stack);
    }

    private EntityPose resolvePose(Player player) {
        if (!Mannequin.validPoses().contains(player.getPose())) {
            return EntityPose.STANDING;
        }
        return SpigotConversionUtil.fromBukkitPose(player.getPose());
    }

    private HumanoidArm resolveReflectedMainHand(Player player) {
        MainHand hand = player.getMainHand();
        return hand == MainHand.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
    }

    private byte resolveSkinParts(Player player) {
        SkinParts skinParts = player.getClientOption(ClientOption.SKIN_PARTS);
        return (byte) skinParts.getRaw();
    }

    private byte resolveHandState(Player player) {
        byte state = 0;
        if (player.isHandRaised()) {
            state |= 0x01;
            if (player.getHandRaised() == EquipmentSlot.OFF_HAND) {
                state |= 0x02;
            }
        }
        if (player.isRiptiding()) {
            state |= 0x04;
        }
        return state;
    }

    public void sendSwingAnimation(Player reflected, boolean offHand) {
        if (regions.isEmpty()) {
            return;
        }
        WrapperPlayServerEntityAnimation.EntityAnimationType type = offHand
                ? WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_OFF_HAND
                : WrapperPlayServerEntityAnimation.EntityAnimationType.SWING_MAIN_ARM;

        UUID reflectedId = reflected.getUniqueId();
        for (MirrorRegion region : regions) {
            Set<UUID> occupants = regionOccupants.get(region.id());
            if (occupants == null || !occupants.contains(reflectedId)) {
                continue;
            }
            Map<UUID, ReflectionState> states = reflectionStates.get(region.id());
            if (states == null) {
                continue;
            }
            ReflectionState state = states.get(reflectedId);
            if (state == null || !state.isEverSpawned()) {
                continue;
            }

            WrapperPlayServerEntityAnimation packet = new WrapperPlayServerEntityAnimation(state.getFakeEntityId(), type);
            for (UUID observerId : occupants) {
                Player observer = plugin.getServer().getPlayer(observerId);
                if (observer != null && observer.isOnline()) {
                    PacketEvents.getAPI().getPlayerManager().sendPacket(observer, packet);
                }
            }
        }
    }

    private record ReflectedTransform(Vector3d position, float yaw, float pitch) { }

    // 方块反射

    private static long packBlockKey(int x, int y, int z) {
        return (((long) x & 0x3FFFFFF) << 38) | (((long) y & 0xFFF) << 26) | ((long) z & 0x3FFFFFF);
    }

    private static Vector3i unpackBlockKey(long key) {
        int x = (int) (key >> 38);
        int y = (int) ((key >> 26) & 0xFFF);
        if (y > 0x7FF) {
            y -= 0x1000;
        }
        int z = (int) (key << 38 >> 38);
        return new Vector3i(x, y, z);
    }

    private void blockTick() {
        if (regions.isEmpty()) {
            return;
        }
        for (MirrorRegion region : regions) {
            blockTickRegion(region);
        }
    }

    private void blockTickRegion(MirrorRegion region) {
        Set<UUID> occupants = regionOccupants.getOrDefault(region.id(), Set.of());

        if (occupants.isEmpty()) {
            revertBlockReflectionsForRegion(region);
            return;
        }

        MirrorRegion.BlockBounds bounds = region.blockReflectionBounds(blockReflectionExpand);
        if (bounds.blockCount() > maxReflectedBlocks) {
            return;
        }

        World world = plugin.getServer().getWorld(region.worldName());
        if (world == null) {
            return;
        }

        Map<Long, WrappedBlockState> newSnapshot = buildBlockSnapshot(region, world, bounds);
        Map<Long, WrappedBlockState> oldSnapshot = blockSnapshots.getOrDefault(region.id(), Map.of());
        Set<UUID> synced = blockSyncedObservers.computeIfAbsent(region.id(), k -> new HashSet<>());

        Map<Long, WrappedBlockState> diff = new HashMap<>();
        for (Map.Entry<Long, WrappedBlockState> entry : newSnapshot.entrySet()) {
            WrappedBlockState old = oldSnapshot.get(entry.getKey());
            if (old == null || old.getGlobalId() != entry.getValue().getGlobalId()) {
                diff.put(entry.getKey(), entry.getValue());
            }
        }

        for (UUID observerId : occupants) {
            Player observer = plugin.getServer().getPlayer(observerId);
            if (observer == null || !observer.isOnline() || !isClientSupported(observer)) {
                continue;
            }
            if (synced.contains(observerId)) {
                sendBlockSnapshot(observer, diff);
            } else {
                sendBlockSnapshot(observer, newSnapshot);
                synced.add(observerId);
            }
        }

        synced.removeIf(observerId -> {
            if (occupants.contains(observerId)) {
                return false;
            }
            Player observer = plugin.getServer().getPlayer(observerId);
            if (observer != null && observer.isOnline()) {
                sendRealBlocks(observer, region, oldSnapshot.keySet());
            }
            return true;
        });

        blockSnapshots.put(region.id(), newSnapshot);
    }

    private Map<Long, WrappedBlockState> buildBlockSnapshot(MirrorRegion region, World world, MirrorRegion.BlockBounds bounds) {
        Map<Long, WrappedBlockState> snapshot = new HashMap<>();
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    Block source = world.getBlockAt(x, y, z);
                    int[] target = ReflectionMath.reflectBlockPosition(region.face(), region.planeCoordinate(), x, y, z);
                    long key = packBlockKey(target[0], target[1], target[2]);
                    BlockData reflectedData = reflectBlockData(region.face(), source.getBlockData());
                    snapshot.put(key, SpigotConversionUtil.fromBukkitBlockData(reflectedData));
                }
            }
        }
        return snapshot;
    }

    private BlockData reflectBlockData(BlockFace mirrorFace, BlockData source) {
        BlockData reflected = source.clone();
        if (reflected instanceof Directional directional) {
            BlockFace reflectedFacing = ReflectionMath.reflectBlockFace(mirrorFace, directional.getFacing());
            if (directional.getFaces().contains(reflectedFacing)) {
                directional.setFacing(reflectedFacing);
            }
        }
        if (reflected instanceof Rotatable rotatable) {
            rotatable.setRotation(ReflectionMath.reflectBlockFace(mirrorFace, rotatable.getRotation()));
        }
        return reflected;
    }

    private void sendBlockSnapshot(Player observer, Map<Long, WrappedBlockState> blocks) {
        if (blocks.isEmpty()) {
            return;
        }
        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        for (Map.Entry<Long, WrappedBlockState> entry : blocks.entrySet()) {
            Vector3i pos = unpackBlockKey(entry.getKey());
            playerManager.sendPacket(observer, new WrapperPlayServerBlockChange(pos, entry.getValue()));
        }
    }

    private void sendRealBlocks(Player observer, MirrorRegion region, Set<Long> keys) {
        if (keys.isEmpty()) {
            return;
        }
        World world = plugin.getServer().getWorld(region.worldName());
        if (world == null) {
            return;
        }
        PlayerManager playerManager = PacketEvents.getAPI().getPlayerManager();
        for (long key : keys) {
            Vector3i pos = unpackBlockKey(key);
            Block realBlock = world.getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            WrappedBlockState realState = SpigotConversionUtil.fromBukkitBlockData(realBlock.getBlockData());
            playerManager.sendPacket(observer, new WrapperPlayServerBlockChange(pos, realState));
        }
    }
}
