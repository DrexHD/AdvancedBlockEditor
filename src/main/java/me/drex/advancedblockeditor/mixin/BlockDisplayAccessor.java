package me.drex.advancedblockeditor.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.BlockDisplay.class)
public interface BlockDisplayAccessor {

    @Invoker
    void invokeAddAdditionalSaveData(CompoundTag compoundTag);

    @Invoker
    BlockState invokeGetBlockState();

    @Invoker
    void invokeSetBlockState(BlockState blockState);

    @Invoker
    void invokeReadAdditionalSaveData(CompoundTag compoundTag);

}
