package me.drex.advancedblockeditor.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LightBlock;

import static me.drex.advancedblockeditor.util.TextUtils.gui;
import static me.drex.advancedblockeditor.util.TextUtils.text;

public class LightGui extends BaseGui {

    public LightGui(EditingContext context, int slot) {
        super(context, slot);
        this.rebuildUi();
        this.open();
    }

    @Override
    protected void buildUi() {
        int brightnessOverride = ((DisplayAccessor)context.originDisplay).invokeGetPackedBrightnessOverride();
        // TODO: what if it's not set / you want to reset it
        if (brightnessOverride != -1) {
            Brightness brightness = Brightness.unpack(brightnessOverride);
            ItemStack block = new ItemStack(Items.LIGHT);
            LightBlock.setLightOnStack(block, brightness.block());
            ItemStack sky = new ItemStack(Items.LIGHT);
            LightBlock.setLightOnStack(sky, brightness.sky());
            this.setSlot(3, GuiElementBuilder.from(block)
                    .setName(gui("action.light.block", context))
                    .hideFlags()
                    .setCallback(() -> {
                        int blockBrightness = Mth.clamp(0, brightness.block() + (context.player.isShiftKeyDown() ? -1 : 1), 15);
                        context.blockDisplays.forEach(blockDisplay -> ((DisplayAccessor) blockDisplay).invokeSetBrightnessOverride(new Brightness(blockBrightness, brightness.sky())));
                        buildUi();
                    }));
            this.setSlot(4, new GuiElementBuilder(Items.WITHER_ROSE)
                    .setName(gui("action.light.reset", context))
                    .hideFlags().setCallback(() -> {
                        context.blockDisplays.forEach(blockDisplay -> ((DisplayAccessor) blockDisplay).invokeSetBrightnessOverride(null));
                        buildUi();
                    })
            );
            this.setSlot(5, GuiElementBuilder.from(sky)
                    .setName(gui("action.light.sky", context))
                    .hideFlags()
                    .setCallback(() -> {
                        int skyBrightness = Mth.clamp(0, brightness.sky() + (context.player.isShiftKeyDown() ? -1 : 1), 15);
                        context.blockDisplays.forEach(blockDisplay -> ((DisplayAccessor) blockDisplay).invokeSetBrightnessOverride(new Brightness(brightness.block(), skyBrightness)));
                        buildUi();
                    }));
        } else {
            this.setSlot(3, new GuiElementBuilder(Items.GLASS_BOTTLE)
                    .setName(gui("action.light.block", context))
                    .hideFlags());
            this.setSlot(4, new GuiElementBuilder(Items.TNT)
                    .setName(gui("action.light.override", context))
                    .hideFlags().setCallback(() -> {
                        context.blockDisplays.forEach(blockDisplay -> ((DisplayAccessor) blockDisplay).invokeSetBrightnessOverride(Brightness.FULL_BRIGHT));
                        buildUi();
                    })
            );
            this.setSlot(5, new GuiElementBuilder(Items.GLASS_BOTTLE)
                    .setName(gui("action.light.block", context))
                    .hideFlags());
        }
    }

    @Override
    public void onTick() {
        super.onTick();
        context.player.sendSystemMessage(text("actionbar.light", context), true);
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(LightGui::new, this.getSelectedSlot());
    }
}
