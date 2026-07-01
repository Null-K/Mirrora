package com.puddingkc.Mirrora.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;


public final class WandItemFactory {

    private static final Material WAND_MATERIAL = Material.BLAZE_ROD;

    private final NamespacedKey wandKey;

    public WandItemFactory(Plugin plugin) {
        this.wandKey = new NamespacedKey(plugin, "mirror_wand");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(WAND_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("镜子选区工具", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(" 左键: 选取第一个点", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(" 右键: 选取第二个点", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(" 两点必须点在同一朝向的墙面上", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
        ));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != WAND_MATERIAL || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BOOLEAN);
    }
}
