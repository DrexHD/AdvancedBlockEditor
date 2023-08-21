package me.drex.advancedblockeditor.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.util.BlockDisplayFactory;
import me.drex.advancedblockeditor.util.TextUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.commands.data.EntityDataAccessor;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Items;

import static me.drex.advancedblockeditor.util.TextUtils.text;

public class MainGui extends BaseGui {

    public MainGui(EditingContext context) {
        super(context, 7);
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
        this.addSlot(switchElement(Items.PAINTING, context.blockDisplays.size() > 1 ? "relative_scale" : "scale", context.blockDisplays.size() > 1 ? RelativeScaleGui::new : ScaleGui::new));
        this.addSlot(switchElement(Items.ARMOR_STAND, "rotation", RotationGui::new));
        this.addSlot(switchElement(Items.MINECART, "move", MoveGui::new));
        this.addSlot(switchElement(Items.REDSTONE, "misc", MiscGui::new));
        this.addSlot(new GuiElementBuilder(Items.TNT)
            .setName(TextUtils.gui("entry.destroy", context))
            .setCallback(() -> {
                context.blockDisplays.forEach(Entity::kill);
                close();
            }));
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry((context1, slot) -> new MainGui(context1), this.getSelectedSlot());
    }
}
