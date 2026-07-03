package com.puddingkc.Mirrora.command;

import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.model.MirrorRegion;
import com.puddingkc.Mirrora.model.MirrorSelection;
import com.puddingkc.Mirrora.util.Lang;
import com.puddingkc.Mirrora.util.Messages;
import com.puddingkc.Mirrora.util.WandItemFactory;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public final class MirrorCommand implements CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final MirrorManager mirrorManager;
    private final SelectionManager selectionManager;
    private final WandItemFactory wandItemFactory;

    private double defaultDepth;
    private double maxDepth;

    public MirrorCommand(JavaPlugin plugin, MirrorManager mirrorManager, SelectionManager selectionManager,
                          WandItemFactory wandItemFactory, double defaultDepth, double maxDepth) {
        this.plugin = plugin;
        this.mirrorManager = mirrorManager;
        this.selectionManager = selectionManager;
        this.wandItemFactory = wandItemFactory;
        this.defaultDepth = defaultDepth;
        this.maxDepth = maxDepth;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("mirrora.admin")) {
            return false;
        }

        if (args.length == 0) {
            Messages.warn(sender, Lang.get("command.usage"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> handleWand(sender);
            case "create" -> handleCreate(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            default -> Messages.warn(sender, Lang.get("command.usage"));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        Lang.init(plugin);

        defaultDepth = plugin.getConfig().getDouble("mirror.default-depth", 8.0);
        maxDepth = plugin.getConfig().getDouble("mirror.max-depth", 32.0);
        long tickInterval = Math.max(1, plugin.getConfig().getLong("mirror.tick-interval", 1));
        mirrorManager.setTickInterval(tickInterval);

        boolean blockReflectionEnabled = plugin.getConfig().getBoolean("mirror.block-reflection.enabled", false);
        long blockTickInterval = Math.max(1, plugin.getConfig().getLong("mirror.block-reflection.tick-interval", 20));
        int maxReflectedBlocks = Math.max(1, plugin.getConfig().getInt("mirror.block-reflection.max-blocks", 2048));
        int blockReflectionExpand = Math.max(0, plugin.getConfig().getInt("mirror.block-reflection.expand", 1));
        mirrorManager.setBlockReflectionSettings(blockReflectionEnabled, blockTickInterval, maxReflectedBlocks, blockReflectionExpand);

        Material wandMaterial = Material.matchMaterial(plugin.getConfig().getString("wand.material", "BLAZE_ROD"));
        if (wandMaterial == null) {
            plugin.getLogger().warning("The configured wand.material is invalid. Falling back to BLAZE_ROD");
            wandMaterial = Material.BLAZE_ROD;
        }
        wandItemFactory.setWandMaterial(wandMaterial);

        Messages.success(sender, Lang.get("command.reload.success"));
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, Lang.get("command.player-only"));
            return;
        }
        player.getInventory().addItem(wandItemFactory.create());
        selectionManager.clear(player.getUniqueId());
        Messages.success(player, Lang.get("command.wand.success"));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, Lang.get("command.player-only"));
            return;
        }
        if (args.length < 2) {
            Messages.warn(player, Lang.get("command.create.usage"));
            return;
        }

        String id = args[1];
        if (mirrorManager.findRegion(id) != null) {
            Messages.error(player, Lang.get("command.create.duplicate"), id);
            return;
        }

        double depth = defaultDepth;
        if (args.length >= 3) {
            try {
                depth = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                Messages.error(player, Lang.get("command.create.invalid-depth"));
                return;
            }
            if (depth <= 0 || depth > maxDepth) {
                Messages.error(player, Lang.get("command.create.depth-range"), maxDepth);
                return;
            }
        }

        MirrorSelection selection = selectionManager.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            Messages.error(player, Lang.get("command.create.no-selection"));
            return;
        }
        if (!selection.isFaceConsistent()) {
            Messages.error(player, Lang.get("command.create.face-mismatch"));
            return;
        }

        MirrorRegion region = buildRegion(id, selection, depth);
        if (region == null) {
            Messages.error(player, Lang.get("command.create.different-world"));
            return;
        }

        if (mirrorManager.createRegion(region)) {
            Messages.success(player, Lang.get("command.create.success"), id);
            selectionManager.clear(player.getUniqueId());
        } else {
            Messages.error(player, Lang.get("command.create.duplicate"), id);
        }
    }

    private MirrorRegion buildRegion(String id, MirrorSelection selection, double depth) {
        Block b1 = selection.getPos1();
        Block b2 = selection.getPos2();
        if (!b1.getWorld().equals(b2.getWorld())) {
            return null;
        }

        BlockFace face = selection.getFace1();
        String worldName = b1.getWorld().getName();

        double planeCoordinate = switch (face) {
            case NORTH -> Math.min(b1.getZ(), b2.getZ());
            case SOUTH -> Math.max(b1.getZ(), b2.getZ()) + 1.0;
            case WEST -> Math.min(b1.getX(), b2.getX());
            case EAST -> Math.max(b1.getX(), b2.getX()) + 1.0;
            default -> 0;
        };

        double minA;
        double maxA;
        if (face == BlockFace.NORTH || face == BlockFace.SOUTH) {
            minA = Math.min(b1.getX(), b2.getX());
            maxA = Math.max(b1.getX(), b2.getX()) + 1.0;
        } else {
            minA = Math.min(b1.getZ(), b2.getZ());
            maxA = Math.max(b1.getZ(), b2.getZ()) + 1.0;
        }

        double minY = Math.min(b1.getY(), b2.getY());
        double maxY = Math.max(b1.getY(), b2.getY()) + 1.0;

        return new MirrorRegion(id, worldName, face, planeCoordinate, minA, maxA, minY, maxY, depth);
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            Messages.warn(sender, Lang.get("command.remove.usage"));
            return;
        }
        if (mirrorManager.removeRegion(args[1])) {
            Messages.success(sender, Lang.get("command.remove.success"), args[1]);
        } else {
            Messages.error(sender, Lang.get("command.remove.not-found"), args[1]);
        }
    }

    private void handleList(CommandSender sender) {
        List<MirrorRegion> regions = mirrorManager.getRegions();
        if (regions.isEmpty()) {
            Messages.warn(sender, Lang.get("command.list.empty"));
            return;
        }
        Messages.info(sender, Lang.get("command.list.header"), regions.size());
        for (MirrorRegion region : regions) {
            Messages.info(sender, Lang.get("command.list.entry"),
                    region.id(), region.worldName(), region.face(), region.depth());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : new String[]{"wand", "create", "remove", "list", "reload"}) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    options.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            for (MirrorRegion region : mirrorManager.getRegions()) {
                if (region.id().toLowerCase().startsWith(args[1].toLowerCase())) {
                    options.add(region.id());
                }
            }
        }
        return options;
    }
}
