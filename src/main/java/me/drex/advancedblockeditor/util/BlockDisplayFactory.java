package me.drex.advancedblockeditor.util;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;

public class BlockDisplayFactory {

    public static void createFromWorld(double scale, BlockPos from, BlockPos to, ServerLevel serverLevel, Vec3 origin) {
        createFromWorld(scale, from, to, serverLevel, origin, false, null);
    }

    public static void createFromWorld(double scale, BlockPos from, BlockPos to, ServerLevel serverLevel, Vec3 origin, ServerPlayer editorUser) {
        createFromWorld(scale, from, to, serverLevel, origin, true, editorUser);
    }

    private static void createFromWorld(double scale, BlockPos from, BlockPos to, ServerLevel level, Vec3 origin, boolean openEditor, @Nullable ServerPlayer editorUser) {
        BoundingBox boundingBox = BoundingBox.fromCorners(from, to);
        List<Display.BlockDisplay> blockDisplays = new LinkedList<>();
        for (BlockPos blockPos : BlockPos.betweenClosed(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), boundingBox.maxZ())) {
            double dx = (boundingBox.minX() - blockPos.getX()) * scale;
            double dy = (boundingBox.minY() - blockPos.getY()) * scale;
            double dz = (boundingBox.minZ() - blockPos.getZ()) * scale;
            BlockState blockState = level.getBlockState(blockPos);
            // Check if all surrounding blocks are solid
            if (level.getBlockState(blockPos.above()).isRedstoneConductor(level, blockPos.above()) &&
                level.getBlockState(blockPos.below()).isRedstoneConductor(level, blockPos.below()) &&
                level.getBlockState(blockPos.north()).isRedstoneConductor(level, blockPos.north()) &&
                level.getBlockState(blockPos.east()).isRedstoneConductor(level, blockPos.east()) &&
                level.getBlockState(blockPos.south()).isRedstoneConductor(level, blockPos.south()) &&
                level.getBlockState(blockPos.west()).isRedstoneConductor(level, blockPos.west())
            ) continue;
            if (blockState.isAir()) continue;
            blockDisplays.add(create(
                    level, null, blockState,
                    new Transformation(null, null, new Vector3f((float) scale, (float) scale, (float) scale), null),
                    new Vec3(origin.x - dx, origin.y - dy, origin.z - dz)
                )
            );
        }
        if (openEditor) new MainGui(new EditingContext(editorUser, blockDisplays));
    }

    public static Display.BlockDisplay create(ServerLevel level, BlockState blockState, BlockPos pos) {
        return create(level, null, blockState, null, Vec3.atLowerCornerOf(pos));
    }

    public static Display.BlockDisplay create(ServerLevel level, CompoundTag tag, Vec3 pos) {
        return create(level, tag, null, null, pos);
    }

    private static Display.BlockDisplay create(ServerLevel level, @Nullable CompoundTag tag, @Nullable BlockState blockState, @Nullable Transformation transformation, Vec3 pos) {
        return EntityType.BLOCK_DISPLAY.spawn(level, null, blockDisplay -> {
            if (blockState != null) ((BlockDisplayAccessor) blockDisplay).invokeSetBlockState(blockState);
            blockDisplay.setYRot(0);
            blockDisplay.setXRot(0);
            blockDisplay.moveTo(pos);
            if (tag != null) {
                ((BlockDisplayAccessor) blockDisplay).invokeReadAdditionalSaveData(tag);
            }
        }, BlockPos.containing(pos), MobSpawnType.COMMAND, false, false);
    }
}
