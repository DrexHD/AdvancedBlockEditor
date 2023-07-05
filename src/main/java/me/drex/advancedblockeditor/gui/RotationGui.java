package me.drex.advancedblockeditor.gui;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.gui.util.Setting;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static me.drex.advancedblockeditor.util.TextUtils.gui;
import static me.drex.advancedblockeditor.util.TextUtils.text;

public class RotationGui extends BaseGui {

    public RotationGui(EditingContext context, int slot) {
        super(context, slot);
        this.rebuildUi();
        this.open();
    }

    @Override
    public void onTick() {
        super.onTick();
        context.player.sendSystemMessage(text("actionbar.rotation", context), true);
    }

    @Override
    protected void buildUi() {
        this.setSlot(0, baseElement(Items.RED_WOOL, gui("action.rotation.x", context))
                .setCallback(() -> this.rotate(Direction.Axis.X))
        );
        this.setSlot(1, baseElement(Items.LIME_WOOL, gui("action.rotation.y", context))
                .setCallback(() -> this.rotate(Direction.Axis.Y))
        );
        this.setSlot(2, baseElement(Items.BLUE_WOOL, gui("action.rotation.z", context))
                .setCallback(() -> this.rotate(Direction.Axis.Z))
        );
    }

    @Override
    protected @Nullable Setting getSetting() {
        return context.rotation;
    }

    private void rotate(Direction.Axis axis) {
        float radians = (float) context.rotation.getResult();

        if (context.player.isShiftKeyDown()) radians *= -1;
        Vec3 origin = context.originDisplay.position();
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            Vec3 pos = blockDisplay.position();
            Vec3 diff = pos.subtract(origin);
            Matrix4f matrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();

            matrix.setTranslation(diff.toVector3f());
            switch (axis) {
                case X -> matrix.rotateLocalX(radians);
                case Y -> matrix.rotateLocalY(radians);
                case Z -> matrix.rotateLocalZ(radians);
            }
            Vector3f translation = matrix.getTranslation(new Vector3f());
            matrix.setTranslation(0,0,0);
            blockDisplay.setPos(origin.add(new Vec3(translation)));
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(matrix));
        }
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(RotationGui::new, this.getSelectedSlot());
    }
}
