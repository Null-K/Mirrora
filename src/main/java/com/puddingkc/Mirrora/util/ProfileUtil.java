package com.puddingkc.Mirrora.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public final class ProfileUtil {

    private ProfileUtil() {
    }

    public static ItemProfile toItemProfile(Player player) {
        PlayerProfile profile = player.getPlayerProfile();

        List<ItemProfile.Property> properties = new ArrayList<>();
        for (ProfileProperty property : profile.getProperties()) {
            properties.add(new ItemProfile.Property(property.getName(), property.getValue(), property.getSignature()));
        }

        return new ItemProfile(profile.getName(), profile.getId(), properties);
    }
}
