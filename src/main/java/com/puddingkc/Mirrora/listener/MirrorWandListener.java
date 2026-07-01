package com.puddingkc.Mirrora.listener;

import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.model.MirrorSelection;
import com.puddingkc.Mirrora.util.Messages;
import com.puddingkc.Mirrora.util.ReflectionMath;
import com.puddingkc.Mirrora.util.WandItemFactory;
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
            Messages.error(event.getPlayer(), "请点击垂直的墙面 (不能是地面或天花板)");
            return;
        }

        MirrorSelection selection = selectionManager.getOrCreate(event.getPlayer().getUniqueId());
        if (action == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(block, face);
            Messages.success(event.getPlayer(), "已选取第一个点: <#cee2f0><arg1></#cee2f0>，朝向: <#cee2f0><arg2></#cee2f0>", describe(block), face);
        } else {
            selection.setPos2(block, face);
            Messages.success(event.getPlayer(), "已选取第二个点: <#cee2f0><arg1></#cee2f0>，朝向: <#cee2f0><arg2></#cee2f0>", describe(block), face);
        }

        if (selection.isComplete() && !selection.isFaceConsistent()) {
            Messages.error(event.getPlayer(), "两个选点的朝向不一致，请确保两点都在同一面墙上");
        }
    }

    private String describe(Block block) {
        return block.getX() + ", " + block.getY() + ", " + block.getZ();
    }
}
