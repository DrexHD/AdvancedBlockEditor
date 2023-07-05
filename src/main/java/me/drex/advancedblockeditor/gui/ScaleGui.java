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
import org.joml.Vector3f;

import java.util.Locale;
import java.util.function.Function;

import static me.drex.advancedblockeditor.util.TextUtils.gui;
import static me.drex.advancedblockeditor.util.TextUtils.text;

public class ScaleGui extends BaseGui {

    private static final ScaleAxis[] AXES = ScaleAxis.values();

    private ScaleAxis currentAxis = ScaleAxis.ALL;

    public ScaleGui(EditingContext context, int slot) {
        super(context, slot);
        this.rebuildUi();
        this.open();
    }

    @Override
    public void onTick() {
        super.onTick();
        context.player.sendSystemMessage(text("actionbar.scale", context), true);
    }

    @Override
    protected void buildUi() {
        this.setSlot(0, baseElement(currentAxis.display, gui("action.scale." + this.currentAxis.name().toLowerCase(Locale.ROOT), context)).setCallback((x, y, z, c) -> {
                    this.playClickSound();
                    this.currentAxis = AXES[(currentAxis.ordinal() + AXES.length + (y.isRight ? 1 : y.isLeft ? -1 : 0)) % AXES.length];
                    this.buildUi();
                })
        );
        this.setSlot(1, baseElement(Items.RED_CONCRETE, gui("action.scale.negative", context))
                .setCallback(() -> this.scale(true))
        );
        this.setSlot(2, baseElement(Items.LIME_CONCRETE, gui("action.scale.positive", context))
                .setCallback(() -> this.scale(false))
        );
    }

    @Override
    protected @Nullable Setting getSetting() {
        return context.scale;
    }


    private void scale(boolean negative) {
        Vector3f min = context.originDisplay.position().toVector3f();
        Vector3f max = context.originDisplay.position().toVector3f();
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
            Vector3f scale = transformation.getScale();
            blockDisplay.position().toVector3f().min(min, min);
            blockDisplay.position().toVector3f().max(max, max);
            blockDisplay.position().toVector3f().add(scale).min(min, min);
            blockDisplay.position().toVector3f().add(scale).max(max, max);
        }
        Vector3f size = max.sub(min);
        Vector3f direction = currentAxis.axisSupplier.apply(context);
        direction.mul((float) context.scaleDelta());
        if (negative) direction.mul(-1);
        Vector3f factor = size.add(direction, new Vector3f()).div(size);
        if (factor.x == 0 || factor.y == 0 || factor.z == 0) {
            playFailSound();
            return;
        }

        Vec3 origin = context.originDisplay.position();
        for (Display.BlockDisplay blockDisplay : this.context.blockDisplays) {
            Vec3 pos = blockDisplay.position();
            Vec3 diff = pos.subtract(origin);

            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());

            Vector3f scale = transformation.getScale();

            blockDisplay.setPos(
                origin.add(diff.multiply(new Vec3(factor)))
            );
            scale.mul(factor);

            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(
                transformation.getTranslation(),
                transformation.getLeftRotation(),
                scale,
                transformation.getRightRotation()
            ));
        }
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(ScaleGui::new, this.getSelectedSlot());
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
