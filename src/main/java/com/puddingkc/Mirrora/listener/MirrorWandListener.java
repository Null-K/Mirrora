package com.puddingkc.Mirrora.listener;

import com.puddingkc.Mirrora.manager.SelectionManager;
import com.puddingkc.Mirrora.model.MirrorSelection;
import com.puddingkc.Mirrora.util.Lang;
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
            Messages.error(event.getPlayer(), Lang.get("wand-listener.invalid-face"));
            return;
        }

        MirrorSelection selection = selectionManager.getOrCreate(event.getPlayer().getUniqueId());
        if (action == Action.LEFT_CLICK_BLOCK) {
            selection.setPos1(block, face);
            Messages.success(event.getPlayer(), Lang.get("wand-listener.point1"), describe(block), face);
        } else {
            selection.setPos2(block, face);
            Messages.success(event.getPlayer(), Lang.get("wand-listener.point2"), describe(block), face);
        }

        if (selection.isComplete() && !selection.isFaceConsistent()) {
            Messages.error(event.getPlayer(), Lang.get("wand-listener.face-mismatch"));
        }
    }

    private String describe(Block block) {
        return block.getX() + ", " + block.getY() + ", " + block.getZ();
    }
}
