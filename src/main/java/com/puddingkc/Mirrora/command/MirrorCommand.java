package com.puddingkc.Mirrora.command;

import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.model.MirrorRegion;
import com.puddingkc.Mirrora.model.MirrorSelection;
import com.puddingkc.Mirrora.util.Messages;
import com.puddingkc.Mirrora.util.WandItemFactory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


public record MirrorCommand(MirrorManager mirrorManager, SelectionManager selectionManager,
                            WandItemFactory wandItemFactory) implements CommandExecutor, TabCompleter {

    private static final double DEFAULT_DEPTH = 8.0;
    private static final double MAX_DEPTH = 32.0;
    private static final String USAGE = "正确命令: <#cee2f0>/mirror [wand|create|remove|list]";

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            Messages.warn(sender, USAGE);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> handleWand(sender);
            case "create" -> handleCreate(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            default -> Messages.warn(sender, USAGE);
        }
        return true;
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "该命令只能由玩家执行");
            return;
        }
        player.getInventory().addItem(wandItemFactory.create());
        selectionManager.clear(player.getUniqueId());
        Messages.success(player, "已获得镜子选区工具，<#cee2f0>左键</#cee2f0>选择点 1，<#cee2f0>右键</#cee2f0>选择点 2");
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.error(sender, "该命令只能由玩家执行");
            return;
        }
        if (args.length < 2) {
            Messages.warn(player, "正确命令: <#cee2f0>/mirror create <id> [深度]");
            return;
        }

        String id = args[1];
        if (mirrorManager.findRegion(id) != null) {
            Messages.error(player, "已存在同名镜子: <#cee2f0><arg>", id);
            return;
        }

        double depth = DEFAULT_DEPTH;
        if (args.length >= 3) {
            try {
                depth = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                Messages.error(player, "深度必须是一个正数");
                return;
            }
            if (depth <= 0 || depth > MAX_DEPTH) {
                Messages.error(player, "深度必须在 <#cee2f0>0</#cee2f0> 到 <#cee2f0><arg></#cee2f0> 之间", MAX_DEPTH);
                return;
            }
        }

        MirrorSelection selection = selectionManager.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            Messages.error(player, "请先用工具选取两个点 (<#cee2f0>/mirror wand</#cee2f0>)");
            return;
        }
        if (!selection.isFaceConsistent()) {
            Messages.error(player, "两个选点的朝向不一致，请重新选取");
            return;
        }

        MirrorRegion region = buildRegion(id, selection, depth);
        if (region == null) {
            Messages.error(player, "创建失败，两个选点必须在同一个世界");
            return;
        }

        if (mirrorManager.createRegion(region)) {
            Messages.success(player, "镜子 <#cee2f0><arg></#cee2f0> 创建成功", id);
            selectionManager.clear(player.getUniqueId());
        } else {
            Messages.error(player, "已存在同名镜子: <#cee2f0><arg>", id);
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
            Messages.warn(sender, "正确命令: <#cee2f0>/mirror remove <id>");
            return;
        }
        if (mirrorManager.removeRegion(args[1])) {
            Messages.success(sender, "镜子 <#cee2f0><arg></#cee2f0> 已移除", args[1]);
        } else {
            Messages.error(sender, "找不到镜子: <#cee2f0><arg>", args[1]);
        }
    }

    private void handleList(CommandSender sender) {
        List<MirrorRegion> regions = mirrorManager.getRegions();
        if (regions.isEmpty()) {
            Messages.warn(sender, "当前没有任何镜子");
            return;
        }
        Messages.info(sender, "共有 <#cee2f0><arg></#cee2f0> 面镜子:", regions.size());
        for (MirrorRegion region : regions) {
            Messages.info(sender, " - <#cee2f0><arg1></#cee2f0> @ <arg2> (<arg3>，深度 <arg4>)",
                    region.id(), region.worldName(), region.face(), region.depth());
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            for (String sub : new String[]{"wand", "create", "remove", "list"}) {
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
