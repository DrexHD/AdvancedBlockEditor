package me.drex.advancedblockeditor.gui;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.gui.util.Setting;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Locale;
import java.util.function.Function;

import static me.drex.advancedblockeditor.util.TextUtils.gui;
import static me.drex.advancedblockeditor.util.TextUtils.text;

public class RelativeScaleGui extends BaseGui {

    private static final ScaleAxis[] AXES = ScaleAxis.values();
    private ScaleAxis currentAxis = ScaleAxis.ALL;

    public RelativeScaleGui(EditingContext context, int slot) {
        super(context, slot);
        this.rebuildUi();
        this.open();
    }

    @Override
    public void onTick() {
        super.onTick();
        context.player.sendSystemMessage(text("actionbar.relative_scale", context), true);
    }

    @Override
    protected void buildUi() {
        this.setSlot(0, baseElement(currentAxis.display, gui("action.relative_scale." + this.currentAxis.name().toLowerCase(Locale.ROOT), context)).setCallback((x, y, z, c) -> {
                if (this.player.isShiftKeyDown()) {
                    return;
                }
                this.playClickSound();
                this.currentAxis = AXES[(currentAxis.ordinal() + AXES.length + (y.isRight ? 1 : y.isLeft ? -1 : 0)) % AXES.length];
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

    @Override
    protected @Nullable Setting getSetting() {
        return context.relativeScale;
    }

    private void scale(double v) {
        if (this.player.isShiftKeyDown()) {
            return;
        }
        Vector3f direction = currentAxis.axisSupplier.apply(context);

        Vector3f origin = context.getOrigin();
        Vector3f scaleMul = new Vector3f(
            direction.x != 0 ? (float) (direction.x * v) : 1,
            direction.y != 0 ? (float) (direction.y * v) : 1,
            direction.z != 0 ? (float) (direction.z * v) : 1
        );

        for (Display.BlockDisplay blockDisplay : this.context.blockDisplays) {
            Vector3f diff = blockDisplay.position().toVector3f().sub(origin);

            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
            Matrix4f transformationMatrix = transformation.getMatrix();
            transformationMatrix.scale(scaleMul);
            blockDisplay.setPos(
                new Vec3(origin.add(diff.mul(scaleMul), new Vector3f()))
            );
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(transformationMatrix));
        }
        buildUi();
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(RelativeScaleGui::new, this.getSelectedSlot());
    }

    enum ScaleAxis {
        ALL(Items.ORANGE_WOOL, (context) -> new Vector3f(1, 1, 1)),
        LOOK(Items.WHITE_WOOL, (context) -> context.playerLookingDirection.step().absolute()),
        X(Items.RED_WOOL, Direction.Axis.X),
        Y(Items.GREEN_WOOL, Direction.Axis.Y),
        Z(Items.BLUE_WOOL, Direction.Axis.Z);

        private final Item display;
        private final Function<EditingContext, Vector3f> axisSupplier;

        ScaleAxis(Item display, Direction.Axis axisSupplier) {
            this(display, context -> Direction.fromAxisAndDirection(axisSupplier, Direction.AxisDirection.POSITIVE).step());
        }

        ScaleAxis(Item display, Function<EditingContext, Vector3f> axisSupplier) {
            this.display = display;
            this.axisSupplier = axisSupplier;
        }
    }


}
