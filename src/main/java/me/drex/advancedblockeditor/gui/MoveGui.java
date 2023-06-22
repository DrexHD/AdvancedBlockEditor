package me.drex.advancedblockeditor.gui;

import me.drex.advancedblockeditor.util.PlaceholderUtils;
import me.drex.advancedblockeditor.util.TextUtils;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static me.drex.advancedblockeditor.util.TextUtils.*;

public class MoveGui extends BaseGui {
    private CurrentAxis currentAxis = CurrentAxis.LOOK;
    private Direction playerLookingDirection;

    public MoveGui(EditingContext context, int slot) {
        super(context, slot);

        var vec = this.player.getViewVector(0);
        this.playerLookingDirection = Direction.getNearest(vec.x, vec.y, vec.z);

        this.rebuildUi();
        this.open();
    }

    @Override
    public void onTick() {
        // TODO:
        if (this.currentAxis.axis == null) {
            var vec = this.player.getViewVector(0);
            var dir = Direction.getNearest(vec.x, vec.y, vec.z);
            if (dir != this.playerLookingDirection) {
                this.playerLookingDirection = dir;
            }
        }
        this.rebuildUi();
        context.player.sendSystemMessage(text("actionbar.move", context), true);
        super.onTick();
    }

    @Override
    protected void buildUi() {
        var wool = switch (this.currentAxis) {
            case LOOK -> Items.WHITE_WOOL;
            case X -> Items.RED_WOOL;
            case Y -> Items.GREEN_WOOL;
            case Z -> Items.BLUE_WOOL;
        };

        this.setSlot(0, baseElement(wool, TextUtils.gui("action.move." + this.currentAxis.name().toLowerCase(Locale.ROOT), context)).setCallback((x, y, z, c) -> {
                    if (this.player.isShiftKeyDown()) {
                        return;
                    }
                    this.playClickSound();
                    this.currentAxis = CurrentAxis.values()[(this.currentAxis.ordinal() + CurrentAxis.values().length + (y.isRight ? 1 : y.isLeft ? -1 : 0)) % CurrentAxis.values().length];
                    this.buildUi();
                })
        );


        this.setSlot(1, baseElement(Items.RED_DYE, TextUtils.gui("action.move.negative.half", context))
                .setCallback((x, y, z, c) -> {
                    this.move(-context.moveDelta() * 0.5);
                })
        );

        this.setSlot(2, baseElement(Items.RED_CONCRETE, TextUtils.gui("action.move.negative.full", context))
                .setCallback((x, y, z, c) -> {
                    this.move(-context.moveDelta());
                })
        );

        this.setSlot(4, baseElement(Items.LIME_CONCRETE, TextUtils.gui("action.move.positive.full", context))
                .setCallback((x, y, z, c) -> {
                    this.move(context.moveDelta());
                })
        );

        this.setSlot(5, baseElement(Items.LIME_DYE, TextUtils.gui("action.move.positive.half", context))
                .setCallback((x, y, z, c) -> {
                    this.move(context.moveDelta() * 0.5);
                })
        );

        /*this.setSlot(6, baseElement(Items.ENDER_PEARL, TextUtils.gui("action.move.teleport"), false)
                .setCallback((x, y, z, c) -> {
                    if (this.player.isShiftKeyDown()) {
                        return;
                    }
                    this.playClickSound();
                    this.context.blockDisplay.setPos(this.player.position());
                })
        );*/
    }

    @Override
    protected @Nullable Setting changeStuff() {
        return context.move;
    }

    private Map<String, Component> placeholders() {
        return PlaceholderUtils.mergePlaceholders(
                new HashMap<>() {{
                    put("direction_positive", direction(getDirection(true)));
                    put("direction_negative", direction(getDirection(false)));
                }},
                context.placeholders()
        );
    }

    private Direction getDirection(boolean positive) {
        if (this.currentAxis.axis == null) {
            return positive ? this.playerLookingDirection : this.playerLookingDirection.getOpposite();
        } else {
            return Direction.fromAxisAndDirection(this.currentAxis.axis, positive ? Direction.AxisDirection.POSITIVE : Direction.AxisDirection.NEGATIVE);
        }
    }

    /*@Override
    public boolean onSelectedSlotChange(int slot) {
        if (this.player.isShiftKeyDown()) {
            var current = this.getSelectedSlot();

            var delta = slot - current;
            this.context.moveDeltaExp = Mth.clamp(context.moveDeltaExp + delta, -10, 10);
            this.playSound(SoundEvents.NOTE_BLOCK_HAT, 0.5f, 1f);
            this.player.connection.send(new ClientboundSetCarriedItemPacket(this.selectedSlot));
            this.buildUi();
            return false;
        }

        return super.onSelectedSlotChange(slot);
    }*/

    private void move(double v) {
        if (this.player.isShiftKeyDown()) {
            return;
        }
        for (Display.BlockDisplay blockDisplay : context.blockDisplays) {
            var pos = blockDisplay.position();
            if (this.currentAxis.axis != null) {
                blockDisplay.setPos(
                        pos.x + this.currentAxis.axis.choose(v, 0, 0),
                        pos.y + this.currentAxis.axis.choose(0, v, 0),
                        pos.z + this.currentAxis.axis.choose(0, 0, v)
                );
                // TODO: make better
                /*((RotatingDisplay)blockDisplay).setUntransformedPos(new Vec3(
                        ((RotatingDisplay)blockDisplay).getUntransformedPos().x + this.currentAxis.axis.choose(v, 0, 0),
                        ((RotatingDisplay)blockDisplay).getUntransformedPos().y + this.currentAxis.axis.choose(0, v, 0),
                        ((RotatingDisplay)blockDisplay).getUntransformedPos().z + this.currentAxis.axis.choose(0, 0, v)
                ));*/
            } else {
                blockDisplay.setPos(
                        pos.x + this.playerLookingDirection.getStepX() * v,
                        pos.y + this.playerLookingDirection.getStepY() * v,
                        pos.z + this.playerLookingDirection.getStepZ() * v
                );
                // TODO: make better
                /*((RotatingDisplay)blockDisplay).setUntransformedPos(new Vec3(
                        ((RotatingDisplay)blockDisplay).getUntransformedPos().x + this.playerLookingDirection.getStepX() * v,
                        ((RotatingDisplay)blockDisplay).getUntransformedPos().y + this.playerLookingDirection.getStepY() * v,
                        ((RotatingDisplay)blockDisplay).getUntransformedPos().z + this.playerLookingDirection.getStepZ() * v
                ));*/
            }
        }
        // TODO:
        /*if (!CommonProtection.canInteractEntity(this.context.player.getEntityWorld(), this.context.armorStand, this.context.player.getGameProfile(), this.context.player)) {
            this.context.armorStand.setPosition(pos);
        }*/
    }

    @Override
    protected SwitchEntry asSwitchableUi() {
        return new SwitchEntry(MoveGui::new, this.getSelectedSlot());
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
