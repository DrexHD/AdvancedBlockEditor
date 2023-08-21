package me.drex.advancedblockeditor.gui;

import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.gui.util.Setting;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import static me.drex.advancedblockeditor.util.TextUtils.gui;
import static me.drex.advancedblockeditor.util.TextUtils.text;

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

        this.setSlot(0, baseElement(wool, gui("action.move." + this.currentAxis.name().toLowerCase(Locale.ROOT), context)).setCallback((x, y, z, c) -> {
                    if (this.player.isShiftKeyDown()) {
                        return;
                    }
                    this.playClickSound();
                    this.currentAxis = CurrentAxis.values()[(this.currentAxis.ordinal() + CurrentAxis.values().length + (y.isRight ? 1 : y.isLeft ? -1 : 0)) % CurrentAxis.values().length];
                    this.buildUi();
                })
        );


        this.setSlot(1, baseElement(Items.RED_DYE, gui("action.move.negative.half", context))
                .setCallback((x, y, z, c) -> {
                    this.move(-context.moveDelta() * 0.5);
                })
        );

        this.setSlot(2, baseElement(Items.RED_CONCRETE, gui("action.move.negative.full", context))
                .setCallback((x, y, z, c) -> {
                    this.move(-context.moveDelta());
                })
        );

        this.setSlot(3, baseElement(Items.LIME_CONCRETE, gui("action.move.positive.full", context))
                .setCallback((x, y, z, c) -> {
                    this.move(context.moveDelta());
                })
        );

        this.setSlot(4, baseElement(Items.LIME_DYE, gui("action.move.positive.half", context))
                .setCallback((x, y, z, c) -> {
                    this.move(context.moveDelta() * 0.5);
                })
        );
    }

    @Override
    protected @Nullable Setting getSetting() {
        return context.move;
    }

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
            } else {
                blockDisplay.setPos(
                        pos.x + this.playerLookingDirection.getStepX() * v,
                        pos.y + this.playerLookingDirection.getStepY() * v,
                        pos.z + this.playerLookingDirection.getStepZ() * v
                );
            }
        }
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
