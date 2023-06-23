package me.drex.advancedblockeditor.gui;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
import me.drex.advancedblockeditor.util.interfaces.EditingPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Brightness;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.Vec3;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

import static me.drex.advancedblockeditor.util.PlaceholderUtils.*;
import static me.drex.advancedblockeditor.util.TextUtils.doubleD;
import static me.drex.advancedblockeditor.util.TextUtils.integer;

public final class EditingContext {

    /**
     * Constant by which to multiply an angular value in radians to obtain an
     * angular value in degrees.
     * Copied from {@link Math#RADIANS_TO_DEGREES}
     */
    private static final double RADIANS_TO_DEGREES = 57.29577951308232;

    public ServerPlayer player;
    public Display.BlockDisplay originDisplay;
    public final List<Display.BlockDisplay> blockDisplays;
    public final List<BaseGui.SwitchEntry> interfaceList = new ArrayList<>();
    public Setting scale = new Setting(0, -10, 10, input -> Math.pow(2, input));
    public Setting relativeScale = new Setting(0, -7, 0, input -> 1 + Math.pow(2, input));
    private static final int rotationMax = 10;
    public Setting rotation = new Setting(8, 0, rotationMax, input -> (Math.PI / Math.pow(2, rotationMax - input)));
    public Setting move = new Setting(0, -10, 10, input -> Math.pow(2, input));
    private boolean placeholdersDirty = true;
    private Map<String, Component> placeholders = null;
    public Direction playerLookingDirection;

    public EditingContext(ServerPlayer player, List<Display.BlockDisplay> blockDisplays) {
        assert !blockDisplays.isEmpty();
        this.player = player;
        this.blockDisplays = blockDisplays;
        updateOriginDisplay();
        ((EditingPlayer) player).setEditing(true);
    }

    public EditingContext(ServerPlayer player, Display.BlockDisplay originDisplay) {
        this(player, Collections.singletonList(originDisplay));
    }

    public double scaleDelta() {
        return scale.getResult();
    }

    public double relativeScaleDelta() {
        return relativeScale.getResult();
    }

    public double moveDelta() {
        return move.getResult();
    }

    public void onTick() {
        // Calculate player look direction
        placeholdersDirty = true;
        var vec = this.player.getViewVector(0);
        var dir = Direction.getNearest(vec.x, vec.y, vec.z);
        if (dir != this.playerLookingDirection) {
            this.playerLookingDirection = dir;
        }
    }

    public boolean hasMultipleDisplays() {
        return blockDisplays.size() > 1;
    }

    public void updateOriginDisplay() {
        Display.BlockDisplay originDisplay = null;
        double value = Double.MAX_VALUE;
        for (Display.BlockDisplay display : blockDisplays) {
            Vec3 position = display.position();
            double currentValue = Math.abs(position.x) * position.x + Math.abs(position.y) * position.y + Math.abs(position.z) * position.z;
            if (currentValue < value) {
                value = currentValue;
                originDisplay = display;
            }
        }
        this.originDisplay = originDisplay;
    }

    public Map<String, Component> placeholders() {
        if (placeholdersDirty) {
            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) originDisplay).getEntityData());
            Matrix4f matrix = transformation.getMatrix();


            Quaternionf axisAngleQuaternion = matrix.getRotation(new AxisAngle4f()).get(new Quaternionf());
            Vector3f rotationVector = axisAngleQuaternion.getEulerAnglesXYZ(new Vector3f());;
            int brightnessOverride = ((DisplayAccessor)originDisplay).invokeGetPackedBrightnessOverride();
            Brightness brightness = brightnessOverride != -1 ? Brightness.unpack(brightnessOverride) : null;

            Vector3f min = originDisplay.position().toVector3f();
            Vector3f max = originDisplay.position().toVector3f();
            for (Display.BlockDisplay blockDisplay : blockDisplays) {
                Transformation t = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
                Vector3f scale = t.getScale();
                blockDisplay.position().toVector3f().min(min, min);
                blockDisplay.position().toVector3f().max(max, max);
                blockDisplay.position().toVector3f().add(scale).min(min, min);
                blockDisplay.position().toVector3f().add(scale).max(max, max);
            }
            Vector3f size = max.sub(min);

            this.placeholders = mergePlaceholders(
                    new HashMap<>() {{
                        put("relative_scale_delta_inverse", doubleD(100 * (1 - (1 / relativeScaleDelta()))));
                        put("relative_scale_delta", doubleD(100 * (relativeScaleDelta() - 1)));
                        put("rotation_delta", doubleD(Math.toDegrees(rotation.getResult())));
                        put("scale_delta", doubleD(scale.getResult()));
                        put("move_delta", doubleD(moveDelta()));
                        put("move_delta_half", doubleD(moveDelta() / 2));
//                        put("rotation_x", doubleD(Math.toDegrees(rotationVector.x)));
//                        put("rotation_y", doubleD(Math.toDegrees(rotationVector.y)));
//                        put("rotation_z", doubleD(Math.toDegrees(rotationVector.z)));
                        put("count", integer(blockDisplays.size()));
                    }},
                    brightness("brightness", brightness),
                    quaternionF("left_rotation", transformation.getLeftRotation()),
                    quaternionF("right_rotation", transformation.getRightRotation()),
                    vector3f("rotation", rotationVector.mul((float) RADIANS_TO_DEGREES)),
                    vector3f("pos", originDisplay.position().toVector3f()),
                    vector3f("scale", size),
                    vector3f("translation", transformation.getTranslation())
            );
            placeholdersDirty = false;
        }
        return placeholders;
    }

    public void close() {
        ((EditingPlayer) player).setEditing(false);
    }


}
