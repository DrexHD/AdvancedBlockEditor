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
    protected @Nullable Setting changeStuff() {
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

        // TODO: open other gui instead
        /*for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            if (!((RotatingDisplay) blockDisplay).isAdvancedBlock()) {
                context.player.sendSystemMessage(Component.literal("Found none advanced"));
                return;
            }
        }
        // TODO: also work with translated objects
        float radians = (float) (PI / 8);
        if (context.player.isShiftKeyDown()) radians *= -1;

        Vec3 untransformedOrigin = ((RotatingDisplay) context.originDisplay).getUntransformedPos();
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            // TODO: make better

            Vec3 untransformedPos = ((RotatingDisplay) blockDisplay).getUntransformedPos();
            Vec3 untransformedPositionDifference = untransformedPos.subtract(untransformedOrigin);
            Matrix4f originalMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();
            Matrix4f newMatrix = new Matrix4f();
            newMatrix.scale(originalMatrix.getScale(new Vector3f()));
            newMatrix.setTranslation(untransformedPositionDifference.toVector3f());
            Vec3 rotationYPR = ((RotatingDisplay) blockDisplay).getRotationYPR();

            rotationYPR = wrapRadians0to2Pi(rotationYPR.add(Vec3.ZERO.with(axis, radians)));
            ((RotatingDisplay) blockDisplay).setRotationYPR(rotationYPR);
            newMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);

            Vector3f positionDifference = newMatrix.getTranslation(new Vector3f());
            newMatrix.setTranslation(0, 0, 0);

            blockDisplay.setPos(context.originDisplay.position().add(new Vec3(positionDifference)));
            ((RotatingDisplay) blockDisplay).setTransformedPosDiff(untransformedPos.subtract(blockDisplay.position()));
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(newMatrix));
        }*/
    }

    // TODO: better parameters
    /*private void rotate(boolean x, boolean y, boolean z) {
        // TODO: also work with translated objects
        float radians = (float) (Math.PI / 8);
        if (context.player.isShiftKeyDown()) radians *= -1;

        Vec3 untransformedOrigin = ((RotatingDisplay)context.originDisplay).getUntransformedPos();

        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            // TODO: make better

            Vec3 untransformedPos = ((RotatingDisplay)blockDisplay).getUntransformedPos();
            Vec3 untransformedPositionDifference = untransformedPos.subtract(untransformedOrigin);
            Matrix4f originalMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();
            Matrix4f newMatrix = new Matrix4f();
            newMatrix.scale(originalMatrix.getScale(new Vector3f()));
            newMatrix.setTranslation(untransformedPositionDifference.toVector3f());
            Vec3 rotationYPR = ((RotatingDisplay) blockDisplay).getRotationYPR();
            rotationYPR = rotationYPR.add(x ? radians : 0, y ? radians : 0, z ? radians : 0);
            ((RotatingDisplay) blockDisplay).setRotationYPR(rotationYPR);
            newMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);

            Vector3f positionDifference = newMatrix.getTranslation(new Vector3f());
            newMatrix.setTranslation(0, 0, 0);
            blockDisplay.setPos(context.originDisplay.position().add(new Vec3(positionDifference)));
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(newMatrix));
        }*/
        /*Vec3 origin = context.originDisplay.position();
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            Vec3 pos = blockDisplay.position();
            Vec3 diff = pos.subtract(origin);
            Matrix4f matrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();

            matrix.setTranslation(diff.toVector3f());
            float radians = (float) (Math.PI / 8);
            if (context.player.isShiftKeyDown()) radians *= -1;
            if (x) matrix.rotateLocalX(radians);
            if (y) matrix.rotateLocalY(radians);
            if (z) matrix.rotateLocalZ(radians);
            Vector3f translation = matrix.getTranslation(new Vector3f());
            matrix.setTranslation(0,0,0);
            blockDisplay.setPos(origin.add(new Vec3(translation)));
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(matrix));
        }*/


    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(RotationGui::new, this.getSelectedSlot());
    }
}
