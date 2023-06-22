package me.drex.advancedblockeditor.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.gui.EditingContext;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.util.BlockDisplaySelector;
import me.drex.advancedblockeditor.util.interfaces.EditingPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static me.drex.advancedblockeditor.AdvancedBlockEditorMod.getSelectLocation;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements EditingPlayer {

    // TODO: Keep a list of last x selected / used entities, to make it easier to undo mistakes
    @Shadow
    public ServerGamePacketListenerImpl connection;

    @Shadow public abstract ServerLevel serverLevel();

    @Unique
    private final List<Display.BlockDisplay> lastSelectedList = new ArrayList<>();

    @Unique
    private boolean editing = false;

    @Unique
    private boolean selecting = false;

    @Override
    public void setPos1(@Nullable Vec3 pos1) {
        this.pos1 = pos1;
        if (pos1 != null) updateSelectedList(getSelectedList());
    }

    @Override
    public void setPos2(@Nullable Vec3 pos2) {
        this.pos2 = pos2;
        if (pos2 != null) updateSelectedList(getSelectedList());
    }

    @Override
    public @Nullable Vec3 getPos1() {
        return this.pos1;
    }

    @Override
    public @Nullable Vec3 getPos2() {
        return this.pos2;
    }

    @Nullable
    private Vec3 pos1 = null;

    @Nullable
    private Vec3 pos2 = null;

    public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
        super(level, blockPos, f, gameProfile);
    }

    private List<Display.BlockDisplay> getSelectedList() {
        ArrayList<Display.BlockDisplay> result = new ArrayList<>();
        if (pos1 == null || pos2 == null) return result;
        AABB selectionArea = new AABB(pos1, pos2);
        for (Entity entity : serverLevel().getAllEntities()) {
            if (entity instanceof Display.BlockDisplay blockDisplay) {
                Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
                VoxelShape voxelShape = ((BlockDisplayAccessor)blockDisplay).invokeGetBlockState().getShape(level(), BlockPos.ZERO);
                if (voxelShape.isEmpty()) continue;
                AABB bounds = voxelShape.bounds();
                Vector3f min = new Vector3f();
                Vector3f max = new Vector3f();
                transformation.getMatrix().transformAab((float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, min, max);
                //selectionArea.contains(new Vec3(bounds.minX, bounds.minY, bounds.minZ).add(blockDisplay.position())) && selectionArea.contains(new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ).add(blockDisplay.position()))
                AABB transformedBounds = new AABB(new Vec3(min), new Vec3(max));
                if (selectionArea.contains(transformedBounds.getCenter().add(blockDisplay.position()))) {
                    result.add(blockDisplay);
                }
            }
        }
        return result;
    }

    private void updateSelectedList(List<Display.BlockDisplay> updatedList) {
        for (Display.BlockDisplay blockDisplay : lastSelectedList) {
//            blockDisplay.setGlowingTag(false);
            ((EntityAccessor) blockDisplay).invokeSetSharedFlag(Entity.FLAG_GLOWING, false);
        }
        lastSelectedList.clear();
        lastSelectedList.addAll(updatedList);
        for (Display.BlockDisplay blockDisplay : lastSelectedList) {
//            blockDisplay.setGlowingTag(true);
            ((EntityAccessor) blockDisplay).invokeSetSharedFlag(Entity.FLAG_GLOWING, true);

        }
    }

    @Inject(method = "doTick", at = @At("HEAD"))
    private void onPlayerTick(CallbackInfo ci) {
        if (((EditingPlayer) this).isEditing()) return;
        selecting = false;
        ItemStack mainHandItem = this.getMainHandItem();
        if (mainHandItem.is(Items.PRISMARINE_SHARD)) {
            Display.BlockDisplay selected = BlockDisplaySelector.getBlockDisplay((ServerPlayer) (Object) this, this.level());
            if (selected != null) {
                updateSelectedList(Collections.singletonList(selected));
                selecting = true;
            } else {
                updateSelectedList(Collections.emptyList());
            }
        } else if (mainHandItem.is(Items.PRISMARINE_CRYSTALS)) {
            if (pos1 != null) {
                this.connection.send(new ClientboundLevelParticlesPacket(new DustParticleOptions(new Vector3f(1, 0, 0), 0.5f), false, pos1.x, pos1.y, pos1.z, 0, 0, 0, 0, 1));
            }
            if (pos2 != null) {
                this.connection.send(new ClientboundLevelParticlesPacket(new DustParticleOptions(new Vector3f(0, 1, 0), 0.5f), false, pos2.x, pos2.y, pos2.z, 0, 0, 0, 0, 1));
            }
            Vec3 selectLocation = getSelectLocation((ServerPlayer) (Object) this);
            this.connection.send(new ClientboundLevelParticlesPacket(new DustParticleOptions(new Vector3f(0.5f, 0.5f, 0.5f), 0.5f), false, selectLocation.x, selectLocation.y, selectLocation.z, 0, 0, 0, 0, 1));
        } /*else {
            updateSelectedList(Collections.emptyList());
        }*/
    }

    @Inject(method = "swing", at = @At("HEAD"), cancellable = true)
    private void onLeftClick(InteractionHand interactionHand, CallbackInfo ci) {
        if (canSelect()) {
            this.setPos2(getSelectLocation(this));
            ci.cancel();
        }
    }

    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void confirmSelection(boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (canSelect()) {
            if (select()) {
                cir.setReturnValue(false);
            }
        }
    }

    private boolean select() {
        AABB selectionArea = new AABB(this.getPos1(), this.getPos2());
        this.setPos1(null);
        this.setPos2(null);
        //AABB selectionArea = new AABB(Vec3Argument.getVec3(context, "from"), Vec3Argument.getVec3(context, "to"));
        ServerLevel level = this.serverLevel();
        List<Display.BlockDisplay> blockDisplays = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Display.BlockDisplay blockDisplay) {
                Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
                VoxelShape voxelShape = ((BlockDisplayAccessor)blockDisplay).invokeGetBlockState().getShape(level, BlockPos.ZERO);
                if (voxelShape.isEmpty()) continue;
                AABB bounds = voxelShape.bounds();
                Vector3f min = new Vector3f();
                Vector3f max = new Vector3f();
                transformation.getMatrix().transformAab((float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, min, max);
                //selectionArea.contains(new Vec3(bounds.minX, bounds.minY, bounds.minZ).add(blockDisplay.position())) && selectionArea.contains(new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ).add(blockDisplay.position()))
                AABB transformedBounds = new AABB(new Vec3(min), new Vec3(max));
                if (selectionArea.contains(transformedBounds.getCenter().add(blockDisplay.position()))) {
                    blockDisplays.add(blockDisplay);
                }
            }
        }
        if (blockDisplays.isEmpty()) {
            return false;
        }
        new MainGui(new EditingContext((ServerPlayer) (Object) this, blockDisplays), 7);
        return true;
    }

    private boolean canSelect() {
        ItemStack mainHandItem = this.getMainHandItem();
        return !selecting && mainHandItem.is(Items.PRISMARINE_CRYSTALS);
    }

    @Override
    public void setEditing(boolean editing) {
        if (!editing) updateSelectedList(Collections.emptyList());
        this.editing = editing;
    }

    @Override
    public boolean isEditing() {
        return editing;
    }

    @Override
    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
    }

    @Override
    public boolean isSelecting() {
        return selecting;
    }
}
