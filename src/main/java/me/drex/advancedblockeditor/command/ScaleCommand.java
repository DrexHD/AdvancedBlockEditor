package me.drex.advancedblockeditor.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class ScaleCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        return literal("scale").then(
                argument("from", BlockPosArgument.blockPos()).then(
                        argument("to", BlockPosArgument.blockPos()).then(
                                argument("scale", DoubleArgumentType.doubleArg()).executes(ScaleCommand::execute)
                        )
                )
        );
    }

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // TODO: clean up
        // /execute align xyz run advancedblockeditor scale -1386 67 -3107 -1388 67 -3105 1
        double scale = DoubleArgumentType.getDouble(context, "scale");
        BlockPos from = BlockPosArgument.getLoadedBlockPos(context, "from");
        BoundingBox boundingBox = BoundingBox.fromCorners(from, BlockPosArgument.getLoadedBlockPos(context, "to"));
        CommandSourceStack source = context.getSource();
        ServerLevel serverLevel = source.getLevel();
        Vec3 origin = source.getPosition();
        for (BlockPos blockPos : BlockPos.betweenClosed(boundingBox.minX(), boundingBox.minY(), boundingBox.minZ(), boundingBox.maxX(), boundingBox.maxY(), boundingBox.maxZ())) {
            double dx = (boundingBox.minX() - blockPos.getX()) * scale;
            double dy = (boundingBox.minY() - blockPos.getY()) * scale;
            double dz = (boundingBox.minZ() - blockPos.getZ()) * scale;
            BlockState blockState = serverLevel.getBlockState(blockPos);
            // Check if all surrounding blocks are solid
            if (serverLevel.getBlockState(blockPos.above()).isRedstoneConductor(serverLevel, blockPos.above()) &&
                    serverLevel.getBlockState(blockPos.below()).isRedstoneConductor(serverLevel, blockPos.below()) &&
                    serverLevel.getBlockState(blockPos.north()).isRedstoneConductor(serverLevel, blockPos.north()) &&
                    serverLevel.getBlockState(blockPos.east()).isRedstoneConductor(serverLevel, blockPos.east()) &&
                    serverLevel.getBlockState(blockPos.south()).isRedstoneConductor(serverLevel, blockPos.south()) &&
                    serverLevel.getBlockState(blockPos.west()).isRedstoneConductor(serverLevel, blockPos.west())
            ) continue;
            if (blockState.isAir() || !blockState.getFluidState().isEmpty()) continue;
            EntityType.BLOCK_DISPLAY.spawn(serverLevel, null, blockDisplay -> {
                ((BlockDisplayAccessor)blockDisplay).invokeSetBlockState(blockState);
                blockDisplay.setYRot(0);
                blockDisplay.setXRot(0);
                blockDisplay.moveTo(origin.x - dx, origin.y - dy, origin.z - dz);
                ((DisplayAccessor) blockDisplay).invokeSetTransformation(new Transformation(null, null, new Vector3f((float) scale, (float) scale, (float) scale), null));
                //((DisplayAccessor) blockDisplay).invokeSetViewRange(5);
            }, BlockPos.containing(origin.x - dx, origin.y - dy, origin.z - dz), MobSpawnType.COMMAND, false, false);

        }
        return 1;
    }

}
