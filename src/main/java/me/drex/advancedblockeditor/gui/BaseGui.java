package me.drex.advancedblockeditor.gui;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementInterface;
import eu.pb4.sgui.api.gui.HotbarGui;
import me.drex.advancedblockeditor.gui.util.Displayable;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.gui.util.Setting;
import me.drex.advancedblockeditor.util.TextUtils;
import me.drex.message.api.LocalizedMessage;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static me.drex.message.api.LocalizedMessage.localized;

public abstract class BaseGui extends HotbarGui {

    protected EditingContext context;

    public BaseGui(EditingContext context, int slot) {
        super(context.player);
        this.setSelectedSlot(slot);
        this.context = context;
    }

    protected void rebuildUi() {
        for (int i = 0; i < this.size; i++) {
            this.clearSlot(i);
        }
        this.buildUi();
        this.setSlot(8, new GuiElementBuilder(Items.SPRUCE_DOOR)
            .setName(TextUtils.gui(context.interfaceList.isEmpty() ? "close" : "back", context))
            .hideFlags()
            .setCallback((x, y, z, c) -> {
                this.playClickSound();
                if (this.context == null || this.context.interfaceList.isEmpty()) {
                    this.close();
                } else {
                    this.switchUi(this.context.interfaceList.remove(0), false);
                }
            })
        );

        this.setSlot(37, this.player.getItemBySlot(EquipmentSlot.HEAD).copy());
        this.setSlot(38, this.player.getItemBySlot(EquipmentSlot.CHEST).copy());
        this.setSlot(39, this.player.getItemBySlot(EquipmentSlot.LEGS).copy());
        this.setSlot(40, this.player.getItemBySlot(EquipmentSlot.FEET).copy());
    }

    protected abstract void buildUi();

    protected abstract SwitchEntry asSwitchableUi();

    protected GuiElementBuilder baseElement(Item item, MutableComponent component) {
        return new GuiElementBuilder(item)
            .setName(component)
            .hideFlags();
    }

    protected GuiElementBuilder switchElement(Item item, String name, SwitchableUi ui) {
        return new GuiElementBuilder(item)
            // TODO:
            .setName(TextUtils.gui("entry." + name, context))
            .hideFlags()
            .setCallback(switchCallback(ui));
    }

    protected <T extends Displayable> GuiElementBuilder enumElement(T[] values, Supplier<T> supplier, Consumer<T> consumer) {
        return baseElement(supplier.get().getDisplayItem(), localized(supplier.get().getDisplayName())).setCallback(() -> {
            int i = ArrayUtils.indexOf(values, supplier.get());
            i += values.length;
            if (this.player.isShiftKeyDown()) {
                i -= 1;
            } else {
                i += 1;
            }
            i %= values.length;
            this.playClickSound();
            consumer.accept(values[i]);
            this.buildUi();
        });
    }

    @Override
    public boolean onSelectedSlotChange(int slot) {
        Setting setting = getSetting();
        if (this.player.isShiftKeyDown() && setting != null) {
            var current = this.getSelectedSlot();
            var delta = slot - current;
            setting.updateValue(delta);
            this.playSound(SoundEvents.NOTE_BLOCK_HAT, 0.5f, 1f);
            this.setSelectedSlot(4);
            this.buildUi();
            return false;
        }

        return super.onSelectedSlotChange(slot);
    }

    @Nullable
    protected Setting getSetting() {
        return null;
    }

    protected GuiElementInterface.ClickCallback switchCallback(SwitchableUi ui) {
        return (x, y, z, c) -> {
            this.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1f);
            this.switchUi(new SwitchEntry(ui, 0), true);
        };
    }

    public void switchUi(SwitchEntry uiOpener, boolean addSelf) {
        var context = this.context;
        if (addSelf) {
            context.interfaceList.add(0, this.asSwitchableUi());
        }
        this.context = null;
        uiOpener.open(context);
    }

    protected void playClickSound() {
        this.playSound(SoundEvents.UI_BUTTON_CLICK, 0.5f, 1f);
    }

    protected void playFailSound() {
        this.playSound(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.VILLAGER_NO), 0.5f, 1f);
    }

    @Override
    public void onClose() {
        if (this.context != null) {
            this.context.close();
        }
    }

    protected void playSound(Holder<SoundEvent> sound, float volume, float pitch) {
        this.player.connection.send(new ClientboundSoundPacket(sound, SoundSource.MASTER, this.player.getX(), this.player.getY(), this.player.getZ(), volume, pitch, 0));
    }

    @Override
    public void onTick() {
        super.onTick();
        this.context.onTick();
    }

    @FunctionalInterface
    public interface SwitchableUi {
        void openUi(EditingContext context, int selectedSlot);
    }

    public record SwitchEntry(SwitchableUi ui, int currentSlot) {
        public void open(EditingContext context) {
            ui.openUi(context, currentSlot);
        }
    }

}
