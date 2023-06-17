package org.dhwpcs.inf_backup.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.LastOperation;

public class CommandConfirm {
    private final FabricEntrypoint entrypoint;

    public CommandConfirm(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(LiteralArgumentBuilder.<ServerCommandSource>literal("confirm")
                .then(RequiredArgumentBuilder.<ServerCommandSource, Boolean>argument("result", BoolArgumentType.bool())
                        .executes(this::confirm))

                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("info")
                        .executes(this::confirmInfo)));
    }

    private int confirm(CommandContext<ServerCommandSource> ctx) {
        boolean result = BoolArgumentType.getBool(ctx, "result");
        LastOperation operation = entrypoint.operationToConfirm.remove(ctx.getSource().getName());
        if (operation == null) {
            ctx.getSource().sendMessage(Text.literal("You don't have any operation to confirm").formatted(Formatting.RED));
            return 1;
        }
        if (result) {
            if (operation.perform(ctx.getSource())) {
                ctx.getSource().sendMessage(Text.literal("Your last operation was successful.").formatted(Formatting.GREEN));
                return 0;
            } else {
                ctx.getSource().sendMessage(Text.literal("Your last operation was failed.").formatted(Formatting.RED));
                return 1;
            }
        } else {
            entrypoint.operationToConfirm.remove(ctx.getSource().getName());
            ctx.getSource().sendMessage(Text.literal("You have cancelled your last operation.").formatted(Formatting.GREEN));
            return 0;
        }
    }

    private int confirmInfo(CommandContext<ServerCommandSource> ctx) {
        LastOperation operation = entrypoint.operationToConfirm.get(ctx.getSource().getName());
        if (operation == null) {
            ctx.getSource().sendMessage(Text.literal("You don't have any operation to confirm").formatted(Formatting.RED));
            return 1;
        }
        ctx.getSource().sendMessage(Text.of("Your last operation to confirm: " + operation.getInformation()));
        return 0;
    }
}
