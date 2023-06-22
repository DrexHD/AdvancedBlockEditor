package me.drex.advancedblockeditor.util;

import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;

public class BlockDisplaySelector {

    private static final double EPSILON = 0.0000001;
    private static final Vector3f[][] TRIANGLE_VERTICES = {{new Vector3f(1.0f, 1.0f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 0.0f),}, {new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(1.0f, 1.0f, 0.0f),}, {new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(1.0f, 0.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f),}, {new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(0.0f, 1.0f, 1.0f), new Vector3f(0.0f, 0.0f, 1.0f),}, {new Vector3f(0.0f, 1.0f, 1.0f), new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 0.0f),}, {new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, 1.0f, 1.0f),}, {new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(1.0f, 1.0f, 0.0f), new Vector3f(1.0f, 1.0f, 1.0f),}, {new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(1.0f, 0.0f, 1.0f), new Vector3f(1.0f, 0.0f, 0.0f),}, {new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f(1.0f, 0.0f, 1.0f),}, {new Vector3f(1.0f, 0.0f, 1.0f), new Vector3f(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, 0.0f, 0.0f),}, {new Vector3f(1.0f, 1.0f, 1.0f), new Vector3f(1.0f, 1.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f),}, {new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 1.0f, 1.0f), new Vector3f(1.0f, 1.0f, 1.0f),}};

    private static final Vector3f[] DEBUG_COLORS = new Vector3f[]{new Vector3f(1, 0, 0), new Vector3f(0, 1, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, 0), new Vector3f(0, 1, 1), new Vector3f(1, 0, 1), new Vector3f(1, 1, 0), new Vector3f(1, 1, 1),};
    private static final boolean DEBUG = false;
    private static final int MAX_SELECTION_DISTANCE = 100;

    @Nullable
    public static Display.BlockDisplay getBlockDisplay(ServerPlayer player, Level level) {
        Vec3 eyePosition = player.getEyePosition();
        Vec3 viewVector = player.getViewVector(1.0f);
        Vec3 destinationPosition = eyePosition.add(viewVector.x * MAX_SELECTION_DISTANCE, viewVector.y * MAX_SELECTION_DISTANCE, viewVector.z * MAX_SELECTION_DISTANCE);
        double closestDistance = Double.MAX_VALUE;
        Display.BlockDisplay closestEntity = null;
        for (Entity entity : level.getEntities(player, new AABB(player.blockPosition().offset(MAX_SELECTION_DISTANCE, MAX_SELECTION_DISTANCE, MAX_SELECTION_DISTANCE), player.blockPosition().offset(-MAX_SELECTION_DISTANCE, -MAX_SELECTION_DISTANCE, -MAX_SELECTION_DISTANCE)), entity -> entity.getType() == EntityType.BLOCK_DISPLAY)) {
            if (!(entity instanceof Display.BlockDisplay blockDisplay)) continue;
            Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
            Matrix4f transformationMatrix = transformation.getMatrix();

            VoxelShape voxelShape = ((BlockDisplayAccessor)blockDisplay).invokeGetBlockState().getShape(level, BlockPos.ZERO);
            if (voxelShape.isEmpty()) continue;
            AABB bounds = voxelShape.bounds();
            Vec3 displayPos = blockDisplay.position();
            Vec3 localEyePosition = eyePosition.subtract(displayPos);
            Vec3 localDestinationPosition = destinationPosition.subtract(displayPos);
            // Optimize by filtering any display entities, who's transformed AABBs don't intersect
            if (!transformedAABBIntersect(transformationMatrix, bounds, localEyePosition, localDestinationPosition))
                continue;
            List<AABB> aabbs = voxelShape.toAabbs();
            for (int i = 0; i < aabbs.size(); i++) {
                AABB aabb = aabbs.get(i);
                Matrix4f otherMatrix = new Matrix4f();
                otherMatrix.translate((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ);
                otherMatrix.scale((float) aabb.getXsize(), (float) aabb.getYsize(), (float) aabb.getZsize());
                for (Vector3f[] triangle : TRIANGLE_VERTICES) {
                    Vector3f[] transformedTriangle = new Vector3f[3];
                    for (int j = 0; j < triangle.length; j++) {
                        Vector4f point4 = new Vector4f(triangle[j].x, triangle[j].y, triangle[j].z, 1);
                        point4.mul(otherMatrix);
                        point4.mul(transformationMatrix);
                        transformedTriangle[j] = new Vector3f(point4.x, point4.y, point4.z);
                        if (DEBUG) {
                            player.connection.send(new ClientboundLevelParticlesPacket(new DustParticleOptions(DEBUG_COLORS[i % DEBUG_COLORS.length], 0.5f), false, transformedTriangle[j].x + displayPos.x, transformedTriangle[j].y + displayPos.y, transformedTriangle[j].z + displayPos.z, 0, 0, 0, 0, 1));
                        }
                    }
                    Optional<Float> optionalDistance = rayTriangleIntersect(viewVector.toVector3f(), localEyePosition.toVector3f(), transformedTriangle[0], transformedTriangle[1], transformedTriangle[2]);

                    if (optionalDistance.isPresent()) {
                        float distance = optionalDistance.get();
                        if (distance >= 0 && distance < closestDistance) {
                            closestEntity = blockDisplay;
                            closestDistance = distance;
                        }
                    }
                }
            }
        }
        return closestEntity;
    }

    private static boolean transformedAABBIntersect(Matrix4f transformationMatrix, AABB bounds, Vec3 origin, Vec3 destination) {
        Vector3f min = new Vector3f();
        Vector3f max = new Vector3f();
        transformationMatrix.transformAab((float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, min, max);
        AABB transformedBounds = new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
        return transformedBounds.clip(origin, destination).isPresent() || transformedBounds.contains(origin);
    }

    // https://en.wikipedia.org/wiki/M%C3%B6ller%E2%80%93Trumbore_intersection_algorithm
    private static Optional<Float> rayTriangleIntersect(Vector3f ray, Vector3f origin, Vector3f triangleCorner1, Vector3f triangleCorner2, Vector3f triangleCorner3) {
        Vector3f edge1 = triangleCorner2.sub(triangleCorner1, new Vector3f());
        Vector3f edge2 = triangleCorner3.sub(triangleCorner1, new Vector3f());
        Vector3f h = ray.cross(edge2, new Vector3f());
        float a = edge1.dot(h);

        if (a < EPSILON) return Optional.empty();

        float f = (float) (1.0 / a);
        Vector3f s = origin.sub(triangleCorner1, new Vector3f());
        float u = s.dot(h) * f;

        if (u < 0 || u > 1) return Optional.empty();

        Vector3f q = s.cross(edge1, new Vector3f());
        float v = ray.dot(q) * f;

        if (v < 0 || u + v > 1) return Optional.empty();

        return Optional.of(edge2.dot(q) * f);
    }

}
