package me.drex.advancedblockeditor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.drex.advancedblockeditor.util.Tool;
import me.drex.message.api.MessageAPI;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

public class AdvancedBlockEditorCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> commandNode = dispatcher.register(
            literal("advancedblockeditor")
                .requires(Permissions.require("advancedblockeditor.root", 2))
                .then(
                    literal("reload").executes(context -> {
                            MessageAPI.reload();
                            return 1;
                        })
                        .requires(Permissions.require("advancedblockeditor.reload", 2))
                ).then(
                    ScaleCommand.build()
                ).then(
                    literal("tools")
                        .requires(Permissions.require("advancedblockeditor.tools", 2))
                        .executes(AdvancedBlockEditorCommand::giveTools)
                )
        );
        dispatcher.register(
            literal("abe")
                .requires(Permissions.require("advancedblockeditor.root", 2))
                .redirect(commandNode));
    }

    private static int giveTools(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        for (Tool tool : Tool.values()) {
            tool.give(player);
        }
        return 1;
    }

}
