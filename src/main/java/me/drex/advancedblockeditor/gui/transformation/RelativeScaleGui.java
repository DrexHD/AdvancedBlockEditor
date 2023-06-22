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

public class RelativeScaleGui extends BaseGui {

    private CurrentAxis currentAxis = CurrentAxis.LOOK;
    private Direction playerLookingDirection;

    public RelativeScaleGui(EditingContext context, int slot) {
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
        context.player.sendSystemMessage(text("actionbar.relative_scale", context), true);
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

        this.setSlot(0, baseElement(wool, gui("action.relative_scale." + this.currentAxis.name().toLowerCase(Locale.ROOT), context)/*direction(this.getDirection(true)))*/).setCallback((x, y, z, c) -> {
                    if (this.player.isShiftKeyDown()) {
                        return;
                    }
                    this.playClickSound();
                    // TODO -1 % 3 is not 2 (?)
                    this.currentAxis = CurrentAxis.values()[(this.currentAxis.ordinal() + CurrentAxis.values().length + (y.isRight ? 1 : y.isLeft ? -1 : 0)) % CurrentAxis.values().length];
                    this.buildUi();
                })
        );
        this.setSlot(1, baseElement(Items.RED_CONCRETE, gui("action.relative_scale.negative", context))
                .setCallback((x, y, z, c) -> {
                    this.scale(1 / context.relativeScaleDelta());
                })
        );
        this.setSlot(2, baseElement(Items.LIME_CONCRETE, gui("action.relative_scale.positive", context))
                .setCallback((x, y, z, c) -> {
                    this.scale(context.relativeScaleDelta());
                })
        );
    }

    /*@Override
    public boolean onSelectedSlotChange(int slot) {
        if (this.player.isShiftKeyDown()) {
            var current = this.getSelectedSlot();

            var delta = slot - current;
            this.context.relativeScaleDelta = Mth.clamp(context.relativeScaleDelta + delta, -7, 0);
            this.playSound(SoundEvents.NOTE_BLOCK_HAT, 0.5f, 1f);
            this.player.connection.send(new ClientboundSetCarriedItemPacket(this.selectedSlot));
            this.buildUi();
            return false;
        }

        return super.onSelectedSlotChange(slot);
    }*/

    @Override
    protected @Nullable Setting changeStuff() {
        return context.relativeScale;
    }

    private void scale(double v) {
        if (this.player.isShiftKeyDown()) {
            return;
        }
        Vec3 origin = context.originDisplay.position();
        for (Display.BlockDisplay blockDisplay : this.context.blockDisplays) {
            Vec3 pos = blockDisplay.position();
            Vec3 diff = pos.subtract(origin);

            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());

            Vector3f scale = transformation.getScale();

            // TODO: remove
            // /advancedblockeditor scale -1358 68 -3120 -1355 72 -3120 0.125
            /*if (this.currentAxis.axis != null) {
                blockDisplay.setPos(
                        pos.x + (diff.x * this.currentAxis.axis.choose(v, 0, 0)),
                        pos.y + (diff.y * this.currentAxis.axis.choose(0, v, 0)),
                        pos.z + (diff.z * this.currentAxis.axis.choose(0, 0, v))
                );
            } else {
                double xDiff = diff.x + (diff.x * this.playerLookingDirection.getStepX() * v);
                double yDiff = diff.y + (diff.y * this.playerLookingDirection.getStepY() * v);
                double zDiff = diff.z + (diff.z * this.playerLookingDirection.getStepZ() * v);

                blockDisplay.setPos(
                        pos.add(xDiff, yDiff, zDiff)
                );
            }*/

            blockDisplay.setPos(
                    origin.add(diff.multiply(v, v, v))
            );
            scale.x *= v;
            scale.y *= v;
            scale.z *= v;

            /*if (this.currentAxis.axis != null) {
                scale.x += this.currentAxis.axis.choose(v, 0, 0);
                scale.y += this.currentAxis.axis.choose(0, v, 0);
                scale.z += this.currentAxis.axis.choose(0, 0, v);
            } else {
                scale.x += this.playerLookingDirection.getStepX() * 2;
                scale.y += this.playerLookingDirection.getStepY() * 2;
                scale.z += this.playerLookingDirection.getStepZ() * 2;
            }*/

            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(
                    transformation.getTranslation(),
                    transformation.getLeftRotation(),
                    scale,
                    transformation.getRightRotation()
            ));
        }
        buildUi();
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(RelativeScaleGui::new, this.getSelectedSlot());
    }

    enum CurrentAxis {
        // TODO:
        //ALL(null),
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
