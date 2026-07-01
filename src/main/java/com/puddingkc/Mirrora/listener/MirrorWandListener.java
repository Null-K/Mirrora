package com.puddingkc.Mirrora.listener;

import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.model.MirrorSelection;
import com.puddingkc.Mirrora.util.ReflectionMath;
import com.puddingkc.Mirrora.util.WandItemFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;


public record MirrorWandListener(WandItemFactory wandItemFactory,
                                 SelectionManager selectionManager) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!wandItemFactory.isWand(event.getItem())) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_BLOCK && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        BlockFace face = event.getBlockFace();
        if (block == null) {
            return;
        }

        event.setCancelled(true);

        if (!ReflectionMath.isHorizontal(face)) {
            event.getPlayer().sendMessage(Component.text("请点击垂直的墙面 (不能是地面或天花板)", NamedTextColor.RED));
            return;
        }

        MirrorSelection selection = selectionManager.getOrCreate(event.getPlayer().getUniqueId());
        if (action == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(block, face);
            event.getPlayer().sendMessage(Component.text(
                    "已选取第一个点: " + describe(block) + "，朝向: " + face,
                    NamedTextColor.GREEN));
        } else {
            selection.setPos2(block, face);
            event.getPlayer().sendMessage(Component.text(
                    "已选取第二个点: " + describe(block) + "，朝向: " + face,
                    NamedTextColor.GREEN));
        }

        if (selection.isComplete() && !selection.isFaceConsistent()) {
            event.getPlayer().sendMessage(Component.text(
                    "两个选点的朝向不一致，请确保两点都在同一面墙上", NamedTextColor.RED));
        }
    }

    private String describe(Block block) {
        return block.getX() + ", " + block.getY() + ", " + block.getZ();
    }
}
