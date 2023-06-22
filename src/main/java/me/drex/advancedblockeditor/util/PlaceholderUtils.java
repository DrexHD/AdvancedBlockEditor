package me.drex.advancedblockeditor.util;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Brightness;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

import static me.drex.advancedblockeditor.util.TextUtils.floatF;
import static me.drex.advancedblockeditor.util.TextUtils.integer;

public class PlaceholderUtils {

    public static Map<String, Component> quaternionF(String prefix, Quaternionf quaternionF) {
        return new HashMap<>() {{
            put(prefix + "_x", floatF(quaternionF.x));
            put(prefix + "_y", floatF(quaternionF.y));
            put(prefix + "_z", floatF(quaternionF.z));
            put(prefix + "_w", floatF(quaternionF.w));
        }};
    }

    public static Map<String, Component> vector3f(String prefix, Vector3f vector3f) {
        return new HashMap<>() {{
            put(prefix + "_x", floatF(vector3f.x));
            put(prefix + "_y", floatF(vector3f.y));
            put(prefix + "_z", floatF(vector3f.z));
        }};
    }

    public static Map<String, Component> brightness(String prefix, @Nullable Brightness brightness) {
        return new HashMap<>() {{
            put(prefix + "_block", brightness != null ? integer(brightness.block()) : TextUtils.text("brightness.not_set"));
            put(prefix + "_sky", brightness != null ? integer(brightness.sky()) : TextUtils.text("brightness.not_set"));
        }};
    }

    @SafeVarargs
    public static Map<String, Component> mergePlaceholders(Map<String, Component>... maps) {
        Map<String, Component> result = new HashMap<>();
        for (Map<String, Component> map : maps) {
            if (map == null) continue;
            result.putAll(map);
        }
        return result;
    }

}
