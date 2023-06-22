package me.drex.advancedblockeditor.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.advancedblockeditor.AdvancedBlockEditorMod;
import me.drex.advancedblockeditor.gui.transformation.RelativeScaleGui;
import me.drex.advancedblockeditor.gui.transformation.RotationGui;
import me.drex.advancedblockeditor.gui.transformation.ScaleGui;
import me.drex.advancedblockeditor.gui.transformation.YPRRotationGui;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.util.TextUtils;
import me.drex.advancedblockeditor.util.interfaces.RotatingDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;

import static me.drex.advancedblockeditor.util.TextUtils.text;

public class MainGui extends BaseGui {

    public MainGui(EditingContext context, int slot) {
        super(context, slot);
        this.rebuildUi();
        this.open();
    }

    @Override
    public void onTick() {
        super.onTick();
        context.player.sendSystemMessage(text("actionbar.main", context), true);
    }

    @Override
    protected void buildUi() {

        // TODO: add permission check
        this.addSlot(switchElement(Items.PAINTING, "scale", context.hasMultipleDisplays() ? RelativeScaleGui::new : ScaleGui::new));
        this.addSlot(switchElement(Items.ARMOR_STAND, "rotation", context.useYPRRotation() ? YPRRotationGui::new : RotationGui::new));
        this.addSlot(switchElement(Items.MINECART, "move", MoveGui::new));
        this.addSlot(switchElement(Items.LIGHT, "brightness", LightGui::new));
        if (!context.hasMultipleDisplays()) {
            this.addSlot(new GuiElementBuilder(Items.BIRCH_SIGN)
                    .setName(TextUtils.gui("entry.nbt", context))
                    .setCallback(() -> {
                        EntityDataAccessor dataAccessor = new EntityDataAccessor(context.originDisplay);
                        context.player.sendSystemMessage(dataAccessor.getPrintSuccess(dataAccessor.getData()));
                    }));
        }
        /*if (!context.hasMultipleDisplays()) {
            this.addSlot(new GuiElementBuilder(Items.PAPER)
                    .setName(TextUtils.gui("entry.template", context))
                    .setCallback(() -> {
                        Block block = context.originDisplay.getBlockState().getBlock();
                        ItemStack itemStack = new ItemStack(block.asItem());
                        itemStack.setHoverName(
                                TextUtils.text("item.template", new HashMap<>() {{
                                    put("block", Component.translatable(block.getDescriptionId()));
                                }})
                        );
                        CompoundTag tag = new CompoundTag();
                        ((BlockDisplayAccessor) context.originDisplay).invokeAddAdditionalSaveData(tag);
                        itemStack.addTagElement("advanced_block_editor", tag);
                        context.player.getInventory().add(itemStack);
                        close();
                    })
            );
        }*/
        this.addSlot(new GuiElementBuilder(Items.COMMAND_BLOCK)
                .setName(TextUtils.gui("entry.clone", context))
                .setCallback(() -> {
                    for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
                        CompoundTag tag = new CompoundTag();
                        ((BlockDisplayAccessor) blockDisplay).invokeAddAdditionalSaveData(tag);
                        AdvancedBlockEditorMod.creatBlockDisplay(player.serverLevel(), tag, blockDisplay.position());
                    }
                })
        );

        this.addSlot(new GuiElementBuilder(Items.TNT)
                .setName(TextUtils.gui("entry.destroy", context))
                .setCallback(() -> {
                    context.blockDisplays.forEach(Entity::kill);
                    close();
                }));
    }


    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(MainGui::new, this.getSelectedSlot());
    }
}
