package me.drex.advancedblockeditor.gui;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.gui.util.Setting;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
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

        Vector3f origin = context.getOrigin();


        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            Vector3f difference = blockDisplay.position().toVector3f().sub(origin);
            Matrix4f transformationMatrix = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData()).getMatrix();

            transformationMatrix.setTranslation(difference);
            switch (axis) {
                case X -> transformationMatrix.rotateLocalX(radians);
                case Y -> transformationMatrix.rotateLocalY(radians);
                case Z -> transformationMatrix.rotateLocalZ(radians);
            }
            Vector3f translation = transformationMatrix.getTranslation(new Vector3f());
            transformationMatrix.setTranslation(0,0,0);
            blockDisplay.setPos(new Vec3(origin.add(translation, new Vector3f())));
            ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(transformationMatrix));
        }
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(RotationGui::new, this.getSelectedSlot());
    }
}
