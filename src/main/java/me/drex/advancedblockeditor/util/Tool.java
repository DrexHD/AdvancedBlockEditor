package me.drex.advancedblockeditor.util;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static me.drex.message.api.LocalizedMessage.localized;

public enum Tool {

    SINGLE_ENTITY_SELECTOR(Items.PRISMARINE_SHARD, "single_entity_selector"),
    ENTITY_SELECTOR(Items.PRISMARINE_CRYSTALS, "entity_selector"),
    BLOCK_SELECTOR(Items.ECHO_SHARD,"block_selector");

    private final Item item;
    private final String localization;

    Tool(Item item, String localization) {
        this.item = item;
        this.localization = localization;
    }

    public void give(Player player) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.getOrCreateTag().putBoolean("AdvancedBlockEditor_Tool", true);
        itemStack.setHoverName(localized("text.advanced_block_editor.tools." + localization + ".name"));
        ListTag loreTag = new ListTag();
        loreTag.add(StringTag.valueOf(Component.Serializer.toJson(
            localized("text.advanced_block_editor.tools." + localization + ".lore")
        )));
        itemStack.getOrCreateTagElement("display").put("Lore", loreTag);
        player.getInventory().placeItemBackInInventory(itemStack);
    }

    public boolean isActive(Player player) {
        return isTool(player.getMainHandItem());
    }

    private boolean isTool(ItemStack itemStack) {
        return itemStack.is(item) && itemStack.getOrCreateTag().getBoolean("AdvancedBlockEditor_Tool");
    }

    public boolean isPassiveActive(Player player) {
        ItemStack mainHandItem = player.getMainHandItem();
        return isActive(player) || isTool(player.getOffhandItem());
    }

}
