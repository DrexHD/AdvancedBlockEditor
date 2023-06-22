package me.drex.advancedblockeditor.mixin;

import me.drex.advancedblockeditor.AdvancedBlockEditorMod;
import me.drex.advancedblockeditor.util.interfaces.RotatingDisplay;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.drex.advancedblockeditor.AdvancedBlockEditorMod.resource;

@Mixin(Display.class)
public abstract class DisplayMixin extends Entity implements RotatingDisplay {

    // yaw, pitch, roll
    private Vec3 rotationYPR = null;
//    private Vec3 untransformedPos = Vec3.ZERO;
    private Vec3 transformedPosDiff = null;

    private static final String ROTATION_KEY = resource("rotation_ypr").toString();
    private static final String UNTRANSFORMED_POS_KEY = resource("untransformed_pos").toString();
    private static final String TRANSFORMED_POS_DIFF_KEY = resource("transformed_pos_diff").toString();

    public DisplayMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void readRotation(CompoundTag compoundTag, CallbackInfo ci) {
        rotationYPR = loadVec3(compoundTag, ROTATION_KEY);
//        untransformedPos = loadVec3(compoundTag, UNTRANSFORMED_POS_KEY);
        transformedPosDiff = loadVec3(compoundTag, TRANSFORMED_POS_DIFF_KEY);
        /*if (compoundTag.contains(ROTATION_KEY)) {
            ListTag rotationList = compoundTag.getList(ROTATION_KEY, Tag.TAG_DOUBLE);
            rotationYPR = new Vec3(rotationList.getDouble(0),rotationList.getDouble(1),rotationList.getDouble(2));
        }
        if (compoundTag.contains(UNTRANSFORMED_POS_KEY)) {
            ListTag untransformedPosList = compoundTag.getList(UNTRANSFORMED_POS_KEY, Tag.TAG_DOUBLE);
            untransformedPos = new Vec3(untransformedPosList.getDouble(0),untransformedPosList.getDouble(1),untransformedPosList.getDouble(2));
        }
        if (compoundTag.contains(TRANSFORMED_POS_DIFF_KEY)) {
            ListTag transformedPosDiffList = compoundTag.getList(TRANSFORMED_POS_DIFF_KEY, Tag.TAG_DOUBLE);
            transformedPosDiff = new Vec3(transformedPosDiffList.getDouble(0),transformedPosDiffList.getDouble(1),transformedPosDiffList.getDouble(2));
        }*/
    }

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void saveRotation(CompoundTag compoundTag, CallbackInfo ci) {
        saveVec3(compoundTag, ROTATION_KEY, rotationYPR);
//        saveVec3(compoundTag, UNTRANSFORMED_POS_KEY, untransformedPos);
        saveVec3(compoundTag, TRANSFORMED_POS_DIFF_KEY, transformedPosDiff);
//        if (rotationYPR != null) compoundTag.put(ROTATION_KEY, this.newDoubleList(rotationYPR.x, rotationYPR.y, rotationYPR.z));
//        if (untransformedPos != null) compoundTag.put(UNTRANSFORMED_POS_KEY, this.newDoubleList(untransformedPos.x, untransformedPos.y, untransformedPos.z));
//        if (transformedPosDiff != null) compoundTag.put(TRANSFORMED_POS_DIFF_KEY, this.newDoubleList(transformedPosDiff.x, transformedPosDiff.y, transformedPosDiff.z));
    }

    @Nullable
    private Vec3 loadVec3(CompoundTag compoundTag, String key) {
        if (compoundTag.contains(key)) {
            ListTag transformedPosDiffList = compoundTag.getList(TRANSFORMED_POS_DIFF_KEY, Tag.TAG_DOUBLE);
            return new Vec3(transformedPosDiffList.getDouble(0),transformedPosDiffList.getDouble(1),transformedPosDiffList.getDouble(2));
        }
        return null;
    }

    private void saveVec3(CompoundTag compoundTag, String key, Vec3 vec3) {
        if (vec3 != null) compoundTag.put(key, this.newDoubleList(vec3.x, vec3.y, vec3.z));
    }

    @Override
    public void initAdvancedBlock() {
        rotationYPR = Vec3.ZERO;
        transformedPosDiff = Vec3.ZERO;
    }

    @Override
    public boolean isAdvancedBlock() {
        return rotationYPR != null && transformedPosDiff != null;
    }

    @Override
    public void setTransformedPosDiff(Vec3 transformedPosDiff) {
        assert this.transformedPosDiff != null;
        this.transformedPosDiff = transformedPosDiff;
    }

    @Override
    public Vec3 getTransformedPosDiff() {
        return this.transformedPosDiff;
    }

    @Override
    public Vec3 getUntransformedPos() {
        assert this.transformedPosDiff != null;
        return this.position().add(this.transformedPosDiff);
        //return this.untransformedPos;
    }

    @Override
    public void setUntransformedPos(Vec3 untransformedPos) {
//        this.untransformedPos = untransformedPos;
    }

    @Override
    public void setRotationYPR(Vec3 rotationYPR) {
        assert this.rotationYPR != null;
        this.rotationYPR = rotationYPR;
    }

    @Override
    public Vec3 getRotationYPR() {
        return this.rotationYPR;
    }
}
