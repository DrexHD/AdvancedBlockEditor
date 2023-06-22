package me.drex.advancedblockeditor.gui.transformation;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.gui.BaseGui;
import me.drex.advancedblockeditor.gui.Setting;
import me.drex.advancedblockeditor.gui.EditingContext;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
import me.drex.advancedblockeditor.util.interfaces.RotatingDisplay;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static java.lang.Math.PI;
import static me.drex.advancedblockeditor.util.TextUtils.gui;
import static me.drex.advancedblockeditor.util.TextUtils.text;

public class YPRRotationGui extends BaseGui {

    private static final double TWO_PI = PI * 2;

    public YPRRotationGui(EditingContext context, int slot) {
        super(context, slot);
        this.rebuildUi();
        this.open();
    }

    @Override
    public void onTick() {
        super.onTick();
        Vec3 position = context.originPos();
        context.player.connection.send(new ClientboundLevelParticlesPacket(new DustParticleOptions(new Vector3f(1, 1, 0), 0.5f), false, position.x, position.y, position.z, 0, 0, 0, 0, 1));
        context.player.sendSystemMessage(text("actionbar.rotation", context), true);
        // TODO:
        /*List<Display.BlockDisplay> blockDisplays = context.blockDisplays;
        for (int i = 0; i < blockDisplays.size(); i++) {
            Display.BlockDisplay blockDisplay = blockDisplays.get(i);
            RotationResult rotationResult = rotate(blockDisplay, Direction.Axis.X);
            VoxelShape voxelShape = blockDisplay.getBlockState().getShape(context.player.level, BlockPos.ZERO);
            if (voxelShape.isEmpty()) continue;
            List<AABB> aabbs = voxelShape.toAabbs();
            Random random = new Random(i);
            Vector3f color = new Vector3f(random.nextFloat(), random.nextFloat(), random.nextFloat());
            for (AABB aabb : aabbs) {
                BlockDisplaySelector.Triangle[] triangles = BlockDisplaySelector.transformedTriangles(aabb, rotationResult.transformationMatrix());
                for (BlockDisplaySelector.Triangle triangle : triangles) {
                    triangle.showParticles(context.player, color, rotationResult.position().toVector3f());
                    //player.connection.send(new ClientboundLevelParticlesPacket(new DustParticleOptions(new Vector3f(1, 0, 0), 0.5f), false, triangle. + displayPos.x, transformedTriangle[j].y + displayPos.y, transformedTriangle[j].z + displayPos.z, 0, 0, 0, 0, 1));
                }
            }
        }*/
    }

    @Override
    protected void buildUi() {
        this.setSlot(0, baseElement(Items.RED_WOOL, gui("action.rotation_ypr.x", context))
                .setCallback(() -> this.rotate(Direction.Axis.X))
        );
        this.setSlot(1, baseElement(Items.LIME_WOOL, gui("action.rotation_ypr.y", context))
                .setCallback(() -> this.rotate(Direction.Axis.Y))
        );
        this.setSlot(2, baseElement(Items.BLUE_WOOL, gui("action.rotation_ypr.z", context))
                .setCallback(() -> this.rotate(Direction.Axis.Z))
        );
        this.setSlot(4, baseElement(Items.WITHER_ROSE, gui("action.rotation_ypr.reset", context))
                .setCallback(this::reset)
        );
    }

    private void reset() {
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            Vec3 untransformedPos = ((RotatingDisplay) blockDisplay).getUntransformedPos();
            Matrix4f originalMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();
            Matrix4f newMatrix = new Matrix4f();
            newMatrix.scale(originalMatrix.getScale(new Vector3f()));
            blockDisplay.setPos(untransformedPos);
            ((RotatingDisplay) blockDisplay).setRotationYPR(Vec3.ZERO);
            ((RotatingDisplay) blockDisplay).setTransformedPosDiff(Vec3.ZERO);
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(newMatrix));
//            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDuration(5);
//            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDelay(0);
        }
    }

    @Override
    protected @Nullable Setting changeStuff() {
        return context.rotation;
    }

    /*
     * Momentanes problem:
     * Transformation / neue Position berücksichtigt nicht die vorherige rotation
     *
     * */

    private RotationResult rotate(Display.BlockDisplay blockDisplay, Direction.Axis axis) {
        float radians = (float) context.rotation.getResult();
        if (context.player.isShiftKeyDown()) radians *= -1;

        Vec3 originPosition = context.originPos();
        Vec3 positionDifference = blockDisplay.position().subtract(originPosition);
        Vector3f positionDifferenceVector3f = positionDifference.toVector3f();
        // TODO: make better
        Matrix4f originalMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();
        Matrix4f resultMatrix = new Matrix4f();
        Matrix4f positionMatrix = new Matrix4f();

        Matrix4f rotationScaleMatrix = new Matrix4f();
        rotationScaleMatrix.scale(originalMatrix.getScale(new Vector3f()));
        //rotationScaleMatrix.translation(positionDifferenceVector3f);

        resultMatrix.scale(originalMatrix.getScale(new Vector3f()));
        positionMatrix.scale(originalMatrix.getScale(new Vector3f()));
        Vec3 rotationYPR = ((RotatingDisplay) blockDisplay).getRotationYPR();

//        positionMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);
//            if (context.player.getMainHandItem().is(Items.DEBUG_STICK)) resultMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);

//            resultMatrix.setTranslation(positionDifference.toVector3f());
        positionMatrix.translation(positionDifferenceVector3f);

        Vec3 rotationDifference = Vec3.ZERO.with(axis, radians);
        positionMatrix.rotateLocalX((float) rotationDifference.x).rotateLocalY((float) rotationDifference.y).rotateLocalZ((float) rotationDifference.z);

        Vector4f updatedPosDifference = new Vector4f(positionDifferenceVector3f.x, positionDifferenceVector3f.y, positionDifferenceVector3f.z, 1).mul(rotationScaleMatrix);

        rotationYPR = wrapRadians0to2Pi(rotationYPR.add(rotationDifference));

        // Set rotation of result matrix
        resultMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);

        Vector3f updatedTranslation = positionMatrix.getTranslation(new Vector3f());

        // TODO: fix for multiple
//        return new RotationResult(originPosition.add(new Vec3(updatedTranslation)), rotationYPR, resultMatrix);
        return new RotationResult(originPosition.add(new Vec3(updatedPosDifference.x, updatedPosDifference.y, updatedPosDifference.z)), rotationYPR, resultMatrix);
//        ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(resultMatrix));
//        ((DisplayAccessor) blockDisplay).invokeSetInterpolationDuration(5);
//        ((DisplayAccessor) blockDisplay).invokeSetInterpolationDelay(0);

    }

    private void rotate(Direction.Axis axis) {

        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            RotationResult rotationResult = rotate(blockDisplay, axis);
            ((RotatingDisplay) blockDisplay).setRotationYPR(rotationResult.rotationYPR());
            blockDisplay.setPos(rotationResult.position());
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(rotationResult.transformationMatrix()));
//            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDuration(5);
//            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDelay(0);
        }
//        context.updateOriginDisplay();

        /*
         * TODO:
         * loop through all
         *
         *
         *
         * */

        /*float radians = (float) context.rotation.getResult();
        if (context.player.isShiftKeyDown()) radians *= -1;

        Vec3 untransformedOriginPos = ((RotatingDisplay) context.originDisplay).getUntransformedPos();

        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            Vec3 untransformedPos = ((RotatingDisplay) blockDisplay).getUntransformedPos();

            Vec3 untransformedPosLocal = untransformedPos.subtract(untransformedOriginPos);
            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
            Vector3f originalScale = transformation.getScale();
            //Vector3f originalTranslation = transformation.getTranslation();
            //Matrix4f originalMatrix = transformation.getMatrix();
            Matrix4f newMatrix = new Matrix4f();
            newMatrix.scale(originalScale);


        }*/

        /*
         * go through all blockdisplay
         * undo their current rotation
         *
         * */


        /*float radians = (float) context.rotation.getResult();
        if (context.player.isShiftKeyDown()) radians *= -1;

//        Vec3 untransformedOrigin = ((RotatingDisplay) context.originDisplay).getUntransformedPos();
        Vec3 originPosition = context.originPos();
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            Vec3 positionDifference = blockDisplay.position().subtract(originPosition);
            // TODO: make better
            Matrix4f originalMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();
            Matrix4f newMatrix = new Matrix4f();
            Matrix4f positionMatrix = new Matrix4f();
            newMatrix.scale(originalMatrix.getScale(new Vector3f()));
            positionMatrix.scale(originalMatrix.getScale(new Vector3f()));
            Vec3 rotationYPR = ((RotatingDisplay) blockDisplay).getRotationYPR();

            positionMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);
//            if (context.player.getMainHandItem().is(Items.DEBUG_STICK)) newMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);

//            newMatrix.setTranslation(positionDifference.toVector3f());
            positionMatrix.setTranslation(positionDifference.toVector3f());

            Vec3 rotationDifference = Vec3.ZERO.with(axis, radians);
            positionMatrix.rotateLocalZ((float) rotationDifference.x).rotateLocalY((float) rotationDifference.y).rotateLocalX((float) rotationDifference.z);
            rotationYPR = wrapRadians0to2Pi(rotationYPR.add(rotationDifference));
            if (context.player.getMainHandItem().is(Items.DEBUG_STICK)) newMatrix.rotateLocalZ((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalX((float) rotationYPR.z);
            ((RotatingDisplay) blockDisplay).setRotationYPR(rotationYPR);

            Vector3f updatedTranslation = positionMatrix.getTranslation(new Vector3f());
            positionMatrix.setTranslation(0, 0, 0);

            // TODO: fix for multiple
            blockDisplay.setPos(originPosition.add(new Vec3(updatedTranslation)));
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(newMatrix));
            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDuration(5);
            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDelay(0);
        }*/


        // TODO: also work with translated objects
        /*float radians = (float) context.rotation.getResult();
        if (context.player.isShiftKeyDown()) radians *= -1;

//        Vec3 untransformedOrigin = ((RotatingDisplay) context.originDisplay).getUntransformedPos();
        Vec3 originPostion = context.originPos();
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            if (*//*context.player.isShiftKeyDown()*//*true) {
                Vec3 positionDifference = blockDisplay.position().subtract(originPostion);
                // TODO: make better
                Matrix4f originalMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();
                Matrix4f newMatrix = new Matrix4f();
                newMatrix.scale(originalMatrix.getScale(new Vector3f()));
                Vec3 rotationYPR = ((RotatingDisplay) blockDisplay).getRotationYPR();
//                newMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);
//                newMatrix.setTranslation(positionDifference.toVector3f());

                Vec3 rotationDifference = Vec3.ZERO.with(axis, radians);

                // Calculate new position
                Matrix4f newPositionMatrix = new Matrix4f();
                newPositionMatrix.scale(originalMatrix.getScale(new Vector3f()));
                if (context.player.getMainHandItem().is(Items.DEBUG_STICK)) newPositionMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);

                rotationYPR = wrapRadians0to2Pi(rotationYPR.add(rotationDifference));
                ((RotatingDisplay) blockDisplay).setRotationYPR(rotationYPR);
                newMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);

//                Vector3f updatedTranslation = newMatrix.getTranslation(new Vector3f());
//                newMatrix.setTranslation(0, 0, 0);


                newPositionMatrix.setTranslation(positionDifference.toVector3f());
                newPositionMatrix.rotateLocalX((float) rotationDifference.x).rotateLocalY((float) rotationDifference.y).rotateLocalZ((float) rotationDifference.z);
                Vector3f updatedTranslation = newPositionMatrix.getTranslation(new Vector3f());


                Matrix4f newNewMatrix = new Matrix4f();
                newNewMatrix.scale(originalMatrix.getScale(new Vector3f()));
                newNewMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);


                // TODO: fix for multiple
                blockDisplay.setPos(originPostion.add(new Vec3(updatedTranslation)));
                ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(newNewMatrix));
                ((DisplayAccessor) blockDisplay).invokeSetInterpolationDuration(5);
                ((DisplayAccessor) blockDisplay).invokeSetInterpolationDelay(0);
            } else {
                Vec3 positionDifference = blockDisplay.position().subtract(originPostion);
                // TODO: make better
                Matrix4f originalMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();
                Matrix4f newMatrix = new Matrix4f();
                newMatrix.scale(originalMatrix.getScale(new Vector3f()));
                Vec3 rotationYPR = ((RotatingDisplay) blockDisplay).getRotationYPR();
                newMatrix.rotateLocalX((float) rotationYPR.x).rotateLocalY((float) rotationYPR.y).rotateLocalZ((float) rotationYPR.z);
                newMatrix.setTranslation(positionDifference.toVector3f());

                Vec3 rotationDifference = Vec3.ZERO.with(axis, radians);
                newMatrix.rotateLocalZ((float) rotationDifference.x).rotateLocalY((float) rotationDifference.y).rotateLocalX((float) rotationDifference.z);
                rotationYPR = wrapRadians0to2Pi(rotationYPR.add(rotationDifference));
                ((RotatingDisplay) blockDisplay).setRotationYPR(rotationYPR);

                Vector3f updatedTranslation = newMatrix.getTranslation(new Vector3f());
                newMatrix.setTranslation(0, 0, 0);

                // TODO: fix for multiple
//            blockDisplay.setPos(originPostion.add(new Vec3(updatedTranslation)));
                ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(newMatrix));
                ((DisplayAccessor) blockDisplay).invokeSetInterpolationDuration(5);
                ((DisplayAccessor) blockDisplay).invokeSetInterpolationDelay(0);
            }
        }*/
        // TODO: also work with translated objects
        /*float radians = (float) context.rotation.getResult();
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

            // TODO: fix for multiple
            blockDisplay.setPos(context.originDisplay.position().add(new Vec3(positionDifference)));
            ((RotatingDisplay) blockDisplay).setTransformedPosDiff(untransformedPos.subtract(blockDisplay.position()));
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(newMatrix));
            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDuration(5);
            ((DisplayAccessor) blockDisplay).invokeSetInterpolationDelay(0);
        }*/
    }

    private static Vec3 wrapRadians0to2Pi(Vec3 vec3) {
        return new Vec3(
                vec3.x >= 0 ? vec3.x % TWO_PI : vec3.x + TWO_PI,
                vec3.y >= 0 ? vec3.y % TWO_PI : vec3.y + TWO_PI,
                vec3.z >= 0 ? vec3.z % TWO_PI : vec3.z + TWO_PI
        );
    }

    private record RotationResult(Vec3 position, Vec3 rotationYPR, Matrix4f transformationMatrix) {
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(YPRRotationGui::new, this.getSelectedSlot());
    }
}
