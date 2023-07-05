package me.drex.advancedblockeditor.util.interfaces;

import net.minecraft.world.phys.Vec3;

public interface EditingPlayer {

    void setEditing(boolean editing);

    boolean isEditing();

    boolean isSelecting();

    void setPos1(Vec3 pos1);

    void setPos2(Vec3 pos2);

}
