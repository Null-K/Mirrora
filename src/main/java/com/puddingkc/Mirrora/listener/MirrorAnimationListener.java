package com.puddingkc.Mirrora.listener;

import com.puddingkc.Mirrora.manager.MirrorManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;


public record MirrorAnimationListener(MirrorManager mirrorManager) implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        boolean offHand = event.getAnimationType() == PlayerAnimationType.OFF_ARM_SWING;
        mirrorManager.sendSwingAnimation(event.getPlayer(), offHand);
    }
}
