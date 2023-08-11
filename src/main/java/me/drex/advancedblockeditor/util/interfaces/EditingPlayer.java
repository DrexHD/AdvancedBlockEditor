package me.drex.advancedblockeditor.util.interfaces;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public interface EditingPlayer {

    void advancedBlockEditor$setEditing(boolean editing);

    boolean advancedBlockEditor$isEditing();

    boolean advancedBlockEditor$isSelecting();

    void advancedBlockEditor$setEntityPos1(Vec3 entityPos1);

    void advancedBlockEditor$setEntityPos2(Vec3 entityPos2);

    void advancedBlockEditor$setBlockPos1(BlockPos blockPos1);

    void advancedBlockEditor$setBlockPos2(BlockPos blockPos2);

}
