package me.drex.advancedblockeditor.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.AdvancedBlockEditorMod;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.util.BlockDisplayFactory;
import me.drex.advancedblockeditor.util.BlockDisplaySelector;
import me.drex.advancedblockeditor.util.Tool;
import me.drex.advancedblockeditor.util.interfaces.EditingPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CubeVoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

import static me.drex.advancedblockeditor.AdvancedBlockEditorMod.getSelectLocation;
import static me.drex.message.api.LocalizedMessage.localized;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements EditingPlayer {

    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Shadow
    public abstract ServerLevel serverLevel();

    @Shadow
    public abstract void sendSystemMessage(Component component, boolean bl);

    @Shadow
    public abstract void sendSystemMessage(Component component);

    @Unique
    private final List<UUID> selectedBlockDisplays = new ArrayList<>();

    @Unique
    private boolean editing = false;

    @Unique
    private boolean selecting = false;

    @Nullable
    @Unique
    private Vec3 entityPos1 = null;

    @Nullable
    @Unique
    private Vec3 entityPos2 = null;

    @Nullable
    @Unique
    private BlockPos blockPos1 = null;

    @Nullable
    @Unique
    private BlockPos blockPos2 = null;

    public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }

    @Unique
    private List<Display.BlockDisplay> getSelectedList() {
        ArrayList<Display.BlockDisplay> result = new ArrayList<>();
        if (entityPos1 == null || entityPos2 == null) return result;
        AABB selectionArea = new AABB(entityPos1, entityPos2);
        for (Entity entity : serverLevel().getAllEntities()) {
            if (entity instanceof Display.BlockDisplay blockDisplay) {
                Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
                VoxelShape voxelShape = ((BlockDisplayAccessor) blockDisplay).invokeGetBlockState().getShape(level(), BlockPos.ZERO);
                if (voxelShape.isEmpty()) voxelShape = Shapes.block();
                AABB bounds = voxelShape.bounds();
                Vector3f min = new Vector3f();
                Vector3f max = new Vector3f();
                transformation.getMatrix().transformAab((float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, min, max);
                AABB transformedBounds = new AABB(new Vec3(min), new Vec3(max));
                if (selectionArea.contains(transformedBounds.getCenter().add(blockDisplay.position()))) {
                    result.add(blockDisplay);
                }
            }
        }
        return result;
    }

    @Unique
    private void updateSelectedList(List<Display.BlockDisplay> updatedList) {
        for (UUID uuid : selectedBlockDisplays) {
            if (serverLevel().getEntity(uuid) instanceof EntityAccessor accessor) {
                accessor.invokeSetSharedFlag(Entity.FLAG_GLOWING, false);
            }
        }
        selectedBlockDisplays.clear();
        for (Display.BlockDisplay blockDisplay : updatedList) {
            selectedBlockDisplays.add(blockDisplay.getUUID());
            ((EntityAccessor) blockDisplay).invokeSetSharedFlag(Entity.FLAG_GLOWING, true);
        }
    }

    @Inject(method = "doTick", at = @At("HEAD"))
    private void onPlayerTick(CallbackInfo ci) {
        if (editing) return;
        selecting = false;
        if (Tool.SINGLE_ENTITY_SELECTOR.isActive(this)) {
            Display.BlockDisplay selected = BlockDisplaySelector.getBlockDisplay((ServerPlayer) (Object) this, this.level());
            if (selected != null) {
                updateSelectedList(Collections.singletonList(selected));
                selecting = true;
            } else {
                updateSelectedList(Collections.emptyList());
            }
        } else if (Tool.ENTITY_SELECTOR.isPassiveActive(this)) {
            if (entityPos1 != null) {
                if (entityPos2 != null) {
                    Map<String, Component> placeholders = Map.of(
                        "entities", Component.literal(String.valueOf(selectedBlockDisplays.size()))
                    );
                    sendSystemMessage(localized("text.advanced_block_editor.tools.entity_selector.actionbar.selected", placeholders), true);
                    renderSelection(entityPos1, entityPos2);
                } else {
                    sendSystemMessage(localized("text.advanced_block_editor.tools.entity_selector.actionbar.pos2"), true);
                }
            } else {
                sendSystemMessage(localized("text.advanced_block_editor.tools.entity_selector.actionbar.pos1"), true);
            }
            if (Tool.ENTITY_SELECTOR.isActive(this)) {
                Vec3 selectLocation = getSelectLocation((ServerPlayer) (Object) this);
                this.connection.send(new ClientboundLevelParticlesPacket(new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 0.5f), false, selectLocation.x, selectLocation.y, selectLocation.z, 0, 0, 0, 0, 1));
            }
        } else if (Tool.BLOCK_SELECTOR.isPassiveActive(this)) {
            if (blockPos1 != null) {
                if (blockPos2 != null) {
                    AABB selectionArea = new AABB(blockPos1, blockPos2);
                    Map<String, Component> placeholders = Map.of(
                        "blocks", Component.literal(String.valueOf((int) ((selectionArea.getXsize() + 1) * (selectionArea.getYsize() + 1) * (selectionArea.getZsize() + 1))))
                    );
                    sendSystemMessage(localized("text.advanced_block_editor.tools.block_selector.actionbar.selected", placeholders), true);
                    renderSelection(new Vec3(selectionArea.minX, selectionArea.minY, selectionArea.minZ), new Vec3(selectionArea.maxX + 1, selectionArea.maxY + 1, selectionArea.maxZ + 1));
                } else {
                    sendSystemMessage(localized("text.advanced_block_editor.tools.block_selector.actionbar.pos2"), true);
                }
            } else {
                sendSystemMessage(localized("text.advanced_block_editor.tools.block_selector.actionbar.pos1"), true);
            }
            if (Tool.BLOCK_SELECTOR.isActive(this)) {
                BlockHitResult blockHitResult = AdvancedBlockEditorMod.getBlockHitResult(this);
                if (blockHitResult.getType() == HitResult.Type.MISS) {
                    BlockPos pos = blockHitResult.getBlockPos();
                    renderSelection(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerWithOffset(pos, 1, 1, 1), new Vector3f(0.5f, 0.5f, 0.5f), 2);
                }
            }
        }
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void onLeftClick(InteractionHand interactionHand, CallbackInfo ci) {
        if (Tool.ENTITY_SELECTOR.isActive(this)) {
            this.advancedBlockEditor$setEntityPos1(getSelectLocation(this));
            ci.cancel();
        } else if (Tool.BLOCK_SELECTOR.isActive(this)) {
            BlockHitResult blockHitResult = AdvancedBlockEditorMod.getBlockHitResult(this);
            if (blockHitResult.getType() == HitResult.Type.MISS) {
                blockPos1 = BlockPos.containing(getSelectLocation(this));
                ci.cancel();
            }
        }
    }

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void confirmSelection(boolean bl, CallbackInfoReturnable<Boolean> cir) {
        Inventory inventory = getInventory();
        if (Tool.ENTITY_SELECTOR.isActive(this)) {
            if (isShiftKeyDown()) {
                entityPos1 = null;
                entityPos2 = null;
                sendSystemMessage(localized("text.advanced_block_editor.tools.entity_selector.action.reset"));
                this.connection.send(new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, inventory.selected, inventory.getSelected()));
                cir.setReturnValue(false);
            } else {
                if (entityPos1 != null && entityPos2 != null) {
                    List<Display.BlockDisplay> selected = selectedBlockDisplays.stream()
                        .map((uuid) -> serverLevel().getEntity(uuid))
                        .filter(entity -> entity instanceof Display.BlockDisplay)
                        .map(entity -> (Display.BlockDisplay) entity).toList();
                    entityPos1 = null;
                    entityPos2 = null;
                    updateSelectedList(Collections.emptyList());
                    if (!selected.isEmpty()) {
                        new MainGui(new EditingContext((ServerPlayer) (Object) this, selected));
                        cir.setReturnValue(false);
                    }
                }
            }
        } else if (Tool.BLOCK_SELECTOR.isActive(this)) {
            if (isShiftKeyDown()) {
                blockPos1 = null;
                blockPos2 = null;
                sendSystemMessage(localized("text.advanced_block_editor.tools.block_selector.action.reset"));
                this.connection.send(new ClientboundContainerSetSlotPacket(ClientboundContainerSetSlotPacket.PLAYER_INVENTORY, 0, inventory.selected, inventory.getSelected()));
                cir.setReturnValue(false);
            } else {
                if (blockPos1 != null && blockPos2 != null) {
                    BlockDisplayFactory.createFromWorld(1, blockPos1, blockPos2, serverLevel(), Vec3.atLowerCornerOf(getOnPos()), (ServerPlayer) (Object) this);
                    blockPos1 = null;
                    blockPos2 = null;
                    cir.setReturnValue(false);
                }
            }
        }
    }

    @Unique
    private void renderSelection(Vec3 pos1, Vec3 pos2) {
        renderSelection(pos1, pos2, new Vector3f(1, 0, 0), 10);
    }

    @Unique
    private void renderSelection(Vec3 pos1, Vec3 pos2, Vector3f color, int count) {
        Vec3 diff = new Vec3(
            pos1.x - pos2.x,
            pos1.y - pos2.y,
            pos1.z - pos2.z
        );
        sendLine(pos1.x, pos1.y, pos1.z, diff.x, 0, 0, color, count);
        sendLine(pos1.x, pos1.y, pos1.z, 0, 0, diff.z, color, count);
        sendLine(pos1.x, pos1.y, pos2.z, diff.x, 0, 0, color, count);
        sendLine(pos2.x, pos1.y, pos1.z, 0, 0, diff.z, color, count);

        sendLine(pos1.x, pos2.y, pos1.z, diff.x, 0, 0, color, count);
        sendLine(pos1.x, pos2.y, pos1.z, 0, 0, diff.z, color, count);
        sendLine(pos1.x, pos2.y, pos2.z, diff.x, 0, 0, color, count);
        sendLine(pos2.x, pos2.y, pos1.z, 0, 0, diff.z, color, count);

        sendLine(pos1.x, pos1.y, pos1.z, 0, diff.y, 0, color, count);
        sendLine(pos1.x, pos1.y, pos2.z, 0, diff.y, 0, color, count);
        sendLine(pos2.x, pos1.y, pos1.z, 0, diff.y, 0, color, count);
        sendLine(pos2.x, pos1.y, pos2.z, 0, diff.y, 0, color, count);

    }

    @Unique
    private void sendLine(double x, double y, double z, double dx, double dy, double dz, Vector3f color, int count) {
        for (int i = 0; i < count; i++) {
            this.connection.send(
                new ClientboundLevelParticlesPacket(
                    new DustParticleOptions(color, 0.5f), false, x - (dx / count * i), y - (dy / count * i), z - (dz / count * i), 0, 0, 0, 0, 1
                )
            );
        }
    }

    @Override
    public void advancedBlockEditor$setEditing(boolean editing) {
        if (!editing) updateSelectedList(Collections.emptyList());
        this.editing = editing;
    }

    @Override
    public boolean advancedBlockEditor$isEditing() {
        return editing;
    }

    @Override
    public boolean advancedBlockEditor$isSelecting() {
        return selecting;
    }

    @Override
    public void advancedBlockEditor$setEntityPos1(@Nullable Vec3 entityPos1) {
        this.entityPos1 = entityPos1;
        if (entityPos1 != null) updateSelectedList(getSelectedList());
        else updateSelectedList(Collections.emptyList());
    }

    @Override
    public void advancedBlockEditor$setEntityPos2(@Nullable Vec3 entityPos2) {
        this.entityPos2 = entityPos2;
        if (entityPos2 != null) updateSelectedList(getSelectedList());
        else updateSelectedList(Collections.emptyList());
    }

    @Override
    public void advancedBlockEditor$setBlockPos1(BlockPos blockPos1) {
        this.blockPos1 = blockPos1;
    }

    @Override
    public void advancedBlockEditor$setBlockPos2(BlockPos blockPos2) {
        this.blockPos2 = blockPos2;
    }
}
