package me.drex.advancedblockeditor;

import me.drex.advancedblockeditor.command.AdvancedBlockEditorCommand;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.util.BlockDisplaySelector;
import me.drex.advancedblockeditor.util.interfaces.EditingPlayer;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class AdvancedBlockEditorMod implements DedicatedServerModInitializer {

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, commandSelection) -> {
            AdvancedBlockEditorCommand.register(dispatcher, commandBuildContext);
        });
        UseItemCallback.EVENT.register((Player player, Level level, InteractionHand hand) -> {
            if (level.isClientSide) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            ItemStack itemStack = player.getItemInHand(hand);
            if (player instanceof ServerPlayer serverPlayer) {
                if (((EditingPlayer) serverPlayer).isEditing())
                    return InteractionResultHolder.pass(player.getItemInHand(hand));
                if (itemStack.getItem() == Items.PRISMARINE_SHARD) {
                    Display.BlockDisplay closest = BlockDisplaySelector.getBlockDisplay(serverPlayer, level);
                    if (closest == null) return InteractionResultHolder.pass(player.getItemInHand(hand));
                    new MainGui(new EditingContext(serverPlayer, closest), 7);
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
                } else if (itemStack.getItem() == Items.PRISMARINE_CRYSTALS) {
                    ((EditingPlayer) player).setPos1(getSelectLocation(player));
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
                }
            }

            return InteractionResultHolder.pass(player.getItemInHand(hand));
        });
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide) {
                return InteractionResult.PASS;
            }
            if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;
            ItemStack itemStack = player.getItemInHand(hand);

            if (player instanceof ServerPlayer serverPlayer) {
                if (((EditingPlayer) serverPlayer).isEditing()
                        || ((EditingPlayer) serverPlayer).isSelecting()) return InteractionResult.PASS;
                if (itemStack.getItem() == Items.PRISMARINE_SHARD) {
                    BlockPos blockPos = hitResult.getBlockPos();
                    BlockState blockState = level.getBlockState(blockPos);
                    if (blockState.isAir()) return InteractionResult.PASS;
                    serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    Display.BlockDisplay blockDisplay = creatBlockDisplay(serverLevel, blockState, blockPos);
                    new MainGui(new EditingContext(serverPlayer, blockDisplay), 7);
                    return InteractionResult.SUCCESS;
                } else if (itemStack.getItem() == Items.PRISMARINE_CRYSTALS) {
                    ((EditingPlayer) player).setPos1(getSelectLocation(player));
                }
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player1, pos, state, blockEntity) -> {
            if (player1.getMainHandItem().getItem() == Items.PRISMARINE_CRYSTALS) {
                ((EditingPlayer) player1).setPos2(getSelectLocation(player1));
                return false;
            }
            return true;
        });
    }

    public static Vec3 getSelectLocation(Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1);
        Vec3 destinationVector = eyePosition.add(viewVector.x * 4.5, viewVector.y * 4.5, viewVector.z * 4.5);
        ClipContext clipContext = new ClipContext(eyePosition, destinationVector, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player);
        BlockHitResult blockHitResult = player.level().clip(clipContext);
        if (blockHitResult.getType() == HitResult.Type.MISS) {
            return destinationVector;
        }
        return blockHitResult.getLocation();
    }

    public static Display.BlockDisplay creatBlockDisplay(ServerLevel level, BlockState blockState, BlockPos pos) {
        return creatBlockDisplay(level, null, blockState, Vec3.atLowerCornerOf(pos));
    }


    public static Display.BlockDisplay creatBlockDisplay(ServerLevel level, CompoundTag tag, BlockPos pos) {
        return creatBlockDisplay(level, tag, null, Vec3.atLowerCornerOf(pos));
    }

    public static Display.BlockDisplay creatBlockDisplay(ServerLevel level, CompoundTag tag, Vec3 pos) {
        return creatBlockDisplay(level, tag, null, pos);
    }

    private static Display.BlockDisplay creatBlockDisplay(ServerLevel level, @Nullable CompoundTag tag, @Nullable BlockState blockState, Vec3 pos) {
        return EntityType.BLOCK_DISPLAY.spawn(level, null, blockDisplay -> {
            if (blockState != null) ((BlockDisplayAccessor)blockDisplay).invokeSetBlockState(blockState);
            blockDisplay.setYRot(0);
            blockDisplay.setXRot(0);
            blockDisplay.moveTo(pos);
            if (tag != null) {
                ((BlockDisplayAccessor) blockDisplay).invokeReadAdditionalSaveData(tag);
            }
        }, BlockPos.containing(pos), MobSpawnType.COMMAND, false, false);
    }


}
