package me.drex.advancedblockeditor.util;

import eu.pb4.placeholders.api.PlaceholderContext;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.message.api.LocalizedMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.text.DecimalFormat;

public class TextUtils {

    public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.####");
    
    public static MutableComponent gui(String path, EditingContext context) {
        return LocalizedMessage.localized("gui.advanced_block_editor." + path, context.placeholders(), PlaceholderContext.of(context.originDisplay));
    }

    public static MutableComponent text(String path, EditingContext context) {
        return LocalizedMessage.localized("text.advanced_block_editor." + path, context.placeholders(), PlaceholderContext.of(context.originDisplay));
    }

    public static MutableComponent integer(Integer integer) {
        return Component.literal(String.valueOf(integer));
    }

    public static MutableComponent floatF(Float f) {
        return Component.literal(DECIMAL_FORMAT.format(f));
    }

    public static MutableComponent doubleD(Double d) {
        return Component.literal(DECIMAL_FORMAT.format(d));
    }
}
