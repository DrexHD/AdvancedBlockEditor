package me.drex.advancedblockeditor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.math.Transformation;
import me.drex.advancedblockeditor.AdvancedBlockEditorMod;
import me.drex.advancedblockeditor.gui.EditingContext;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.mixin.BlockDisplayAccessor;
import me.drex.advancedblockeditor.mixin.DisplayAccessor;
import me.drex.advancedblockeditor.mixin.EntityAccessor;
import me.drex.advancedblockeditor.util.interfaces.EditingPlayer;
import me.drex.message.api.MessageAPI;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class AdvancedBlockEditorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext commandBuildContext) {
        LiteralCommandNode<CommandSourceStack> commandNode = dispatcher.register(
                literal("advancedblockeditor").then(
                        literal("block").then(
                                argument("block", BlockStateArgument.block(commandBuildContext)).executes(AdvancedBlockEditorCommand::execute)
                        )
                ).then(
                        literal("reload").executes(context -> {
                            MessageAPI.reload();
                            return 1;
                        })
                ).then(
                        ScaleCommand.build()
                ).then(
                        literal("select").executes(AdvancedBlockEditorCommand::select)
                )
        );
        dispatcher.register(literal("abe").redirect(commandNode));
    }

    // TODO:
    public static int select(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer serverPlayer = context.getSource().getPlayerOrException();
        AABB selectionArea = new AABB(((EditingPlayer)serverPlayer).getPos1(), ((EditingPlayer)serverPlayer).getPos2());
        ((EditingPlayer)serverPlayer).setPos1(null);
        ((EditingPlayer)serverPlayer).setPos2(null);
        //AABB selectionArea = new AABB(Vec3Argument.getVec3(context, "from"), Vec3Argument.getVec3(context, "to"));
        ServerLevel level = context.getSource().getLevel();
        List<Display.BlockDisplay> blockDisplays = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof Display.BlockDisplay blockDisplay) {
                Transformation transformation = DisplayAccessor.invokeCreateTransformation(((EntityAccessor) blockDisplay).getEntityData());
                VoxelShape voxelShape = ((BlockDisplayAccessor)blockDisplay).invokeGetBlockState().getShape(level, BlockPos.ZERO);
                if (voxelShape.isEmpty()) continue;
                AABB bounds = voxelShape.bounds();
                Vector3f min = new Vector3f();
                Vector3f max = new Vector3f();
                transformation.getMatrix().transformAab((float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, min, max);
                //selectionArea.contains(new Vec3(bounds.minX, bounds.minY, bounds.minZ).add(blockDisplay.position())) && selectionArea.contains(new Vec3(bounds.maxX, bounds.maxY, bounds.maxZ).add(blockDisplay.position()))
                AABB transformedBounds = new AABB(new Vec3(min), new Vec3(max));
                if (selectionArea.contains(transformedBounds.getCenter().add(blockDisplay.position()))) {
                    blockDisplays.add(blockDisplay);
                }
            }
        }
        if (blockDisplays.isEmpty()) {
            context.getSource().sendFailure(Component.literal("none selected"));
            return 0;
        }
        new MainGui(new EditingContext(serverPlayer, blockDisplays), 7);
        return 1;
    }

    public static int execute(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockState blockState = BlockStateArgument.getBlock(context, "block").getState();
        CommandSourceStack source = context.getSource();
        ServerPlayer serverPlayer = source.getPlayerOrException();
        if (((EditingPlayer)serverPlayer).isEditing()) return 0;
        BlockPos blockPos = BlockPos.containing(source.getPosition());
        Display.BlockDisplay blockDisplay = AdvancedBlockEditorMod.creatBlockDisplay(source.getLevel(), blockState, blockPos);
        new MainGui(new EditingContext(serverPlayer, blockDisplay), 7);
        return 1;
    }

}
