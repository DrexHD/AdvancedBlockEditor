package me.drex.advancedblockeditor.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.util.BlockDisplayFactory;
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
                                argument("scale", DoubleArgumentType.doubleArg()).executes(context -> {
                                    BlockDisplayFactory.createFromWorld(
                                        DoubleArgumentType.getDouble(context, "scale"),
                                        BlockPosArgument.getLoadedBlockPos(context, "from"),
                                        BlockPosArgument.getLoadedBlockPos(context, "to"),
                                        context.getSource().getLevel(),
                                        context.getSource().getPosition()
                                    );
                                    return 1;
                                })
                        )
                )
        );
    }

}
