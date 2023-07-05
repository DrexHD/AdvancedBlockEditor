package me.drex.advancedblockeditor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.drex.advancedblockeditor.AdvancedBlockEditorMod;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.util.interfaces.EditingPlayer;
import me.drex.message.api.MessageAPI;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.block.state.BlockState;

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
                )
        );
        dispatcher.register(literal("abe").redirect(commandNode));
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
