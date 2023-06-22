package me.drex.advancedblockeditor.gui.transformation;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.gui.BaseGui;
import me.drex.advancedblockeditor.gui.Setting;
import me.drex.advancedblockeditor.gui.EditingContext;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Locale;

import static me.drex.advancedblockeditor.util.TextUtils.*;

public class ScaleGui extends BaseGui {

    private CurrentAxis currentAxis = CurrentAxis.LOOK;
    private Direction playerLookingDirection;

    public ScaleGui(EditingContext context, int slot) {
        super(context, slot);
        var vec = this.player.getViewVector(0);
        this.playerLookingDirection = Direction.getNearest(vec.x, vec.y, vec.z);
        this.rebuildUi();
        this.open();
    }

    @Override
    public void onTick() {
        if (this.currentAxis.axis == null) {
            var vec = this.player.getViewVector(0);
            var dir = Direction.getNearest(vec.x, vec.y, vec.z);
            if (dir != this.playerLookingDirection) {
                this.playerLookingDirection = dir;
                this.rebuildUi();
            }
        }
        super.onTick();
        context.player.sendSystemMessage(text("actionbar.scale", context), true);
    }

    @Override
    protected void buildUi() {
        var wool = switch (this.currentAxis) {
            //case ALL -> Items.BLACK_WOOL;
            case LOOK -> Items.WHITE_WOOL;
            case X -> Items.RED_WOOL;
            case Y -> Items.GREEN_WOOL;
            case Z -> Items.BLUE_WOOL;
        };

        this.setSlot(0, baseElement(wool, gui("action.scale." + this.currentAxis.name().toLowerCase(Locale.ROOT), context)).setCallback((x, y, z, c) -> {
                    this.playClickSound();
                    // TODO -1 % 3 is not 2 (?)
                    this.currentAxis = CurrentAxis.values()[(this.currentAxis.ordinal() + CurrentAxis.values().length + (y.isRight ? 1 : y.isLeft ? -1 : 0)) % CurrentAxis.values().length];
                    this.buildUi();
                })
        );
        this.setSlot(1, baseElement(Items.RED_CONCRETE, gui("action.scale.negative", context))
                .setCallback(() -> this.scale(-context.scale.getResult()))
        );
        this.setSlot(2, baseElement(Items.LIME_CONCRETE, gui("action.scale.positive", context))
                .setCallback(() -> this.scale(context.scale.getResult()))
        );
        this.setSlot(4, baseElement(Items.WITHER_ROSE, gui("action.scale.reset", context))
                .setCallback(this::reset)
        );
    }

    @Override
    protected @Nullable Setting changeStuff() {
        return context.scale;
    }

    private void reset() {
        for (Display.BlockDisplay blockDisplay : this.context.blockDisplays) {
            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(
                    transformation.getTranslation(),
                    transformation.getLeftRotation(),
                    null,
                    transformation.getRightRotation()
            ));
        }
    }

    private void scale(double v) {
        for (Display.BlockDisplay blockDisplay : this.context.blockDisplays) {
            if (this.currentAxis.axis != null) {
                updateScale(blockDisplay, Direction.fromAxisAndDirection(this.currentAxis.axis, Direction.AxisDirection.POSITIVE), v);
            } else {
                updateScale(blockDisplay, playerLookingDirection, v);
            }
        }
        buildUi();
    }

    private void updateScale(Display.BlockDisplay blockDisplay, Direction lookingDirection, double v) {
        Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());

        Vector3f oldScale = transformation.getScale();
        Vec3 scaleChangeVector = Vec3.atLowerCornerOf(lookingDirection.getNormal()).multiply(v, v, v);
        if (lookingDirection.getAxisDirection() == Direction.AxisDirection.NEGATIVE) {
            Vec3 pos = blockDisplay.position();
            blockDisplay.setPos(
                    pos.add(scaleChangeVector)
            );
            scaleChangeVector = scaleChangeVector.multiply(-1, -1, -1);
        }
        oldScale.add(scaleChangeVector.toVector3f());
        ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(
                transformation.getTranslation(),
                transformation.getLeftRotation(),
                oldScale,
                transformation.getRightRotation()
        ));
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(ScaleGui::new, this.getSelectedSlot());
    }

    enum CurrentAxis {
        LOOK(null),
        X(Direction.Axis.X),
        Y(Direction.Axis.Y),
        Z(Direction.Axis.Z);

        private final Direction.Axis axis;

        CurrentAxis(Direction.Axis axis) {
            this.axis = axis;
        }
    }


}
