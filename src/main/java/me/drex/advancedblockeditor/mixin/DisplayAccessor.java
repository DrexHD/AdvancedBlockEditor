package me.drex.advancedblockeditor.mixin;

import com.mojang.math.Transformation;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {

    @Invoker
    void invokeSetBrightnessOverride(@Nullable Brightness brightness);

    @Invoker
    void invokeSetTransformation(Transformation transformation);

    @Invoker
    int invokeGetPackedBrightnessOverride();

    @Invoker
    static Transformation invokeCreateTransformation(SynchedEntityData synchedEntityData) {
        throw new AssertionError();
    }

}