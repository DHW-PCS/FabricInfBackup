package org.dhwpcs.inf_backup.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.storage.BackupInfo;

import java.nio.file.Path;

public class CommandRollback {
    private final FabricEntrypoint entrypoint;

    public CommandRollback(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(LiteralArgumentBuilder.<ServerCommandSource>literal("rollback")
                .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("slot", IntegerArgumentType.integer(0))
                        .executes(this::rollback)));
    }

    private int rollback(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "slot");
        Pair<Path, BackupInfo> info = entrypoint.storage.find(id);
        if (info == null) {
            ctx.getSource().sendMessage(Text.literal("Cannot find the backup in slot " + id).formatted(Formatting.RED));
            return 1;
        }
        if (!entrypoint.selectedBackups.add(info)) {
            ctx.getSource().sendMessage(Text.literal("The same backup is already requested to be applied").formatted(Formatting.RED));
            return 1;
        }
        ctx.getSource().sendMessage(Text.literal("The following backup is chosen").formatted(Formatting.AQUA));
        ctx.getSource().sendMessage(Text.of(info.getRight().toString()));
        ctx.getSource().sendMessage(Text.literal("When the server shuts down, the rollback will be performed.").formatted(Formatting.GREEN));
        return 0;
    }
}
