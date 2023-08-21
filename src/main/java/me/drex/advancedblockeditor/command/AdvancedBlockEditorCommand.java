package me.drex.advancedblockeditor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.drex.advancedblockeditor.gui.MainGui;
import me.drex.advancedblockeditor.gui.util.EditingContext;
import me.drex.advancedblockeditor.util.BlockDisplayFactory;
import me.drex.advancedblockeditor.util.Tool;
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

    // TODO: Permissions
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> commandNode = dispatcher.register(
                literal("advancedblockeditor").then(
                        literal("reload").executes(context -> {
                            MessageAPI.reload();
                            return 1;
                        })
                ).then(
                        ScaleCommand.build()
                ).then(
                    literal("tools").executes(AdvancedBlockEditorCommand::giveTools)
                )
        );
        dispatcher.register(literal("abe").redirect(commandNode));
    }

    private static int giveTools(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (Tool tool : Tool.values()) {
            tool.give(player);
        }
        return 1;
    }

}
