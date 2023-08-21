package me.drex.advancedblockeditor.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.advancedblockeditor.gui.util.Displayable;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.util.BlockDisplayFactory;
import me.drex.advancedblockeditor.util.TextUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public class MiscGui extends BaseGui {

    private BillboardType billboardType = BillboardType.FIXED;

    public MiscGui(EditingContext context, int slot) {
        super(context, slot);
        this.rebuildUi();
        this.open();
    }

    @Override
    protected void buildUi() {
        this.setSlot(0, switchElement(Items.LIGHT, "brightness", LightGui::new));


        this.setSlot(1, enumElement(BillboardType.values(), () -> billboardType, updatedBillboardType -> {
            billboardType = updatedBillboardType;
            for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
                ((DisplayAccessor) blockDisplay).invokeSetBillboardConstraints(updatedBillboardType.billboard);
            }
        }));
        this.setSlot(2, new GuiElementBuilder(Items.COMMAND_BLOCK)
            .setName(TextUtils.gui("entry.clone", context))
            .setCallback(() -> {
                for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
                    CompoundTag tag = new CompoundTag();
                    ((BlockDisplayAccessor) blockDisplay).invokeAddAdditionalSaveData(tag);
                    BlockDisplayFactory.create(player.serverLevel(), tag, blockDisplay.position());
                }
                playClickSound();
            })
        );
        if (!context.hasMultipleDisplays()) {
            this.setSlot(3, new GuiElementBuilder(Items.BIRCH_SIGN)
                .setName(TextUtils.gui("entry.nbt", context))
                .setCallback(() -> {
                    EntityDataAccessor dataAccessor = new EntityDataAccessor(context.originDisplay);
                    context.player.sendSystemMessage(dataAccessor.getPrintSuccess(dataAccessor.getData()));
                }));
        }
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(MiscGui::new, this.getSelectedSlot());
    }

    enum BillboardType implements Displayable {

        FIXED(Display.BillboardConstraints.FIXED, Items.ANVIL),
        VERTICAL(Display.BillboardConstraints.VERTICAL, Items.GLASS_PANE),
        HORIZONTAL(Display.BillboardConstraints.HORIZONTAL, Items.SNOW),
        CENTER(Display.BillboardConstraints.CENTER, Items.ENDER_PEARL);

        private final Display.BillboardConstraints billboard;
        private final Item item;

        BillboardType(Display.BillboardConstraints billboard, Item item) {
            this.billboard = billboard;
            this.item = item;
        }

        @Override
        public Item getDisplayItem() {
            return item;
        }

        @Override
        public String getDisplayName() {
            return "gui.advanced_block_editor.entry.misc.billboard." + billboard.getSerializedName();
        }
    }

}
