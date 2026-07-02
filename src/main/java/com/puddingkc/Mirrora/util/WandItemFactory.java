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

    private final Material wandMaterial;
    private final NamespacedKey wandKey;

    public WandItemFactory(Plugin plugin, Material wandMaterial) {
        this.wandMaterial = wandMaterial;
        this.wandKey = new NamespacedKey(plugin, "mirror_wand");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(wandMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(Lang.get("wand-item.name"), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.empty(),
                Component.text(Lang.get("wand-item.lore1"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text(Lang.get("wand-item.lore2"), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text(Lang.get("wand-item.lore3"), NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
                Component.empty()
        ));
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isWand(ItemStack item) {
        if (item == null || item.getType() != wandMaterial || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta.getPersistentDataContainer().has(wandKey, PersistentDataType.BOOLEAN);
    }
}
