package org.dhwpcs.inf_backup.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.util.Util;

public class CommandCancel {
    private final FabricEntrypoint entrypoint;

    public CommandCancel(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(LiteralArgumentBuilder.<ServerCommandSource>literal("cancel")
                .then(LiteralArgumentBuilder.<ServerCommandSource>literal("all")
                        .executes(this::cancelAll))
                .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("slot", IntegerArgumentType.integer(0))
                        .executes(this::cancel)));
    }

    private int cancelAll(CommandContext<ServerCommandSource> ctx) {
        if (!entrypoint.selectedBackups.isEmpty()) {
            entrypoint.selectedBackups.clear();
            ctx.getSource().sendMessage(Text.of("The pending rollback requests are all cancelled."));
            return 0;
        }
        ctx.getSource().sendMessage(Text.literal("No rollback request is pending.").formatted(Formatting.RED));
        return 1;
    }

    private int cancel(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "slot");
        if (!Util.remove(entrypoint.selectedBackups, id)) {
            ctx.getSource().sendMessage(Text.literal("No such rollback request is pending.").formatted(Formatting.RED));
            return 1;
        }
        ctx.getSource().sendMessage(Text.of("The pending rollback request is cancelled."));
        return 0;
    }
}
