package me.drex.advancedblockeditor.util.interfaces;

import net.minecraft.world.phys.Vec3;

public interface RotatingDisplay {

    void setRotationYPR(Vec3 vec3);
    Vec3 getRotationYPR();

    @Deprecated
    void setUntransformedPos(Vec3 untransformedPos);
    Vec3 getUntransformedPos();

    void setTransformedPosDiff(Vec3 transformedPosDiff);
    Vec3 getTransformedPosDiff();

    void initAdvancedBlock();
    boolean isAdvancedBlock();

}
