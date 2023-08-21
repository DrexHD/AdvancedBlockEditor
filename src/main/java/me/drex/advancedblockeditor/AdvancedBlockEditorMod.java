package me.drex.advancedblockeditor;

import me.drex.advancedblockeditor.command.AdvancedBlockEditorCommand;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.util.BlockDisplayFactory;
import me.drex.advancedblockeditor.util.BlockDisplaySelector;
import me.drex.advancedblockeditor.util.Tool;
import me.drex.advancedblockeditor.util.interfaces.EditingPlayer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AdvancedBlockEditorMod implements ModInitializer {

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, commandSelection) -> {
            AdvancedBlockEditorCommand.register(dispatcher);
        });
        UseItemCallback.EVENT.register((Player player, Level level, InteractionHand hand) -> {
            if (level.isClientSide) {
                return InteractionResultHolder.pass(player.getItemInHand(hand));
            }

            if (player instanceof ServerPlayer serverPlayer) {
                if (((EditingPlayer) serverPlayer).advancedBlockEditor$isEditing())
                    return InteractionResultHolder.pass(player.getItemInHand(hand));
                if (Tool.SINGLE_ENTITY_SELECTOR.isActive(player)) {
                    Display.BlockDisplay closest = BlockDisplaySelector.getBlockDisplay(serverPlayer, level);
                    if (closest == null) return InteractionResultHolder.pass(player.getItemInHand(hand));
                    new MainGui(new EditingContext(serverPlayer, closest));
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
                } else if (Tool.ENTITY_SELECTOR.isActive(player)) {
                    ((EditingPlayer) player).advancedBlockEditor$setEntityPos2(getSelectLocation(player));
                    return InteractionResultHolder.consume(player.getItemInHand(hand));
                } else if (Tool.BLOCK_SELECTOR.isActive(player)) {
                    ((EditingPlayer) player).advancedBlockEditor$setBlockPos2(BlockPos.containing(getSelectLocation(player)));
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

            if (player instanceof ServerPlayer serverPlayer) {
                if (((EditingPlayer) serverPlayer).advancedBlockEditor$isEditing()
                        || ((EditingPlayer) serverPlayer).advancedBlockEditor$isSelecting()) return InteractionResult.PASS;
                if (Tool.SINGLE_ENTITY_SELECTOR.isActive(player)) {
                    BlockPos blockPos = hitResult.getBlockPos();
                    BlockState blockState = level.getBlockState(blockPos);
                    if (blockState.isAir()) return InteractionResult.PASS;
                    serverLevel.setBlock(blockPos, Blocks.AIR.defaultBlockState(), 2);
                    Display.BlockDisplay blockDisplay = BlockDisplayFactory.create(serverLevel, blockState, blockPos);
                    new MainGui(new EditingContext(serverPlayer, blockDisplay));
                    return InteractionResult.SUCCESS;
                } else if (Tool.ENTITY_SELECTOR.isActive(player)) {
                    ((EditingPlayer) player).advancedBlockEditor$setEntityPos2(getSelectLocation(player));
                } else if (Tool.BLOCK_SELECTOR.isActive(player)) {
                    ((EditingPlayer) player).advancedBlockEditor$setBlockPos2(hitResult.getBlockPos());
                }
            }
            return InteractionResult.PASS;
        });
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
            if (Tool.ENTITY_SELECTOR.isActive(player)) {
                ((EditingPlayer) player).advancedBlockEditor$setEntityPos1(getSelectLocation(player));
                return false;
            } else if (Tool.BLOCK_SELECTOR.isActive(player)) {
                ((EditingPlayer) player).advancedBlockEditor$setBlockPos1(pos);
                return false;
            }
            return true;
        });
    }

    public static BlockHitResult getBlockHitResult(Player player) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1);
        Vec3 destinationVector = eyePosition.add(viewVector.x * 4.5, viewVector.y * 4.5, viewVector.z * 4.5);
        ClipContext clipContext = new ClipContext(eyePosition, destinationVector, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player);
        return player.level().clip(clipContext);

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


}
