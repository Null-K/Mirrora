package com.puddingkc.Mirrora.command;

import com.puddingkc.Mirrora.manager.MirrorManager;
import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.model.MirrorRegion;
import com.puddingkc.Mirrora.model.MirrorSelection;
import com.puddingkc.Mirrora.util.WandItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("正确命令: /mirror <wand|create|remove|list>", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "wand" -> handleWand(sender);
            case "create" -> handleCreate(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            default -> sender.sendMessage(Component.text("正确命令: /mirror <wand|create|remove|list>", NamedTextColor.YELLOW));
        }
        return true;
    }

    private void handleWand(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行", NamedTextColor.RED));
            return;
        }
        player.getInventory().addItem(wandItemFactory.create());
        selectionManager.clear(player.getUniqueId());
        player.sendMessage(Component.text("已获得镜子选区工具，左键选择点 1，右键选择点 2", NamedTextColor.GREEN));
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("该命令只能由玩家执行", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Component.text("正确命令: /mirror create <id> [深度]", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1];
        if (mirrorManager.findRegion(id) != null) {
            player.sendMessage(Component.text("已存在同名镜子: " + id, NamedTextColor.RED));
            return;
        }

        double depth = DEFAULT_DEPTH;
        if (args.length >= 3) {
            try {
                depth = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("深度必须是一个正数", NamedTextColor.RED));
                return;
            }
            if (depth <= 0 || depth > MAX_DEPTH) {
                player.sendMessage(Component.text("深度必须在 0 到 " + MAX_DEPTH + " 之间", NamedTextColor.RED));
                return;
            }
        }

        MirrorSelection selection = selectionManager.get(player.getUniqueId());
        if (selection == null || !selection.isComplete()) {
            player.sendMessage(Component.text("请先用工具选取两个点 (/mirror wand)", NamedTextColor.RED));
            return;
        }
        if (!selection.isFaceConsistent()) {
            player.sendMessage(Component.text("两个选点的朝向不一致，请重新选取", NamedTextColor.RED));
            return;
        }

        MirrorRegion region = buildRegion(id, selection, depth);
        if (region == null) {
            player.sendMessage(Component.text("创建失败，两个选点必须在同一个世界", NamedTextColor.RED));
            return;
        }

        if (mirrorManager.createRegion(region)) {
            player.sendMessage(Component.text("镜子 '" + id + "' 创建成功", NamedTextColor.GREEN));
            selectionManager.clear(player.getUniqueId());
        } else {
            player.sendMessage(Component.text("已存在同名镜子: " + id, NamedTextColor.RED));
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
            sender.sendMessage(Component.text("正确命令: /mirror remove <id>", NamedTextColor.YELLOW));
            return;
        }
        if (mirrorManager.removeRegion(args[1])) {
            sender.sendMessage(Component.text("镜子 '" + args[1] + "' 已移除", NamedTextColor.GREEN));
        } else {
            sender.sendMessage(Component.text("找不到镜子: " + args[1], NamedTextColor.RED));
        }
    }

    private void handleList(CommandSender sender) {
        List<MirrorRegion> regions = mirrorManager.getRegions();
        if (regions.isEmpty()) {
            sender.sendMessage(Component.text("当前没有任何镜子", NamedTextColor.YELLOW));
            return;
        }
        sender.sendMessage(Component.text("共有 " + regions.size() + " 面镜子:", NamedTextColor.AQUA));
        for (MirrorRegion region : regions) {
            sender.sendMessage(Component.text(" - " + region.id() + " @ " + region.worldName()
                    + " (" + region.face() + "，深度 " + region.depth() + ")", NamedTextColor.GRAY));
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
