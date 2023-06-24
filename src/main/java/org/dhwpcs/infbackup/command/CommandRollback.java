package org.dhwpcs.infbackup.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.dhwpcs.infbackup.FabricEntrypoint;
import org.dhwpcs.infbackup.LastOperation;
import org.dhwpcs.infbackup.storage.BackupInfo;

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
        switch(entrypoint.config.rollback_method) {
            case SHUTDOWN -> {
                if (!entrypoint.selectedBackups.add(info)) {
                    ctx.getSource().sendMessage(Text.literal("The same backup is already requested to be applied").formatted(Formatting.RED));
                    return 1;
                }
                ctx.getSource().sendMessage(Text.literal("The following backup is chosen").formatted(Formatting.AQUA));
                ctx.getSource().sendMessage(Text.of(info.getRight().toString()));
                ctx.getSource().sendMessage(Text.literal("When the server shuts down, the rollback will be performed.").formatted(Formatting.GREEN));
            }
            case INSTANT -> {
                entrypoint.operationToConfirm.put(ctx.getSource().getName(), new LastOperation.RollbackDirect(id, entrypoint.storage));
                ctx.getSource().sendMessage(Text.literal("Rollback save in slot " + id + ", are you sure?").formatted(Formatting.AQUA));
                ctx.getSource().sendMessage(Text.literal("IF YOU ARE SURE, CLICK HERE")
                        .styled(raw -> raw.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/backup confirm true"))));
                ctx.getSource().sendMessage(Text.literal("IF YOU ARE UNWILLING, CLICK HERE")
                        .styled(raw -> raw.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/backup confirm false"))));
            }
        }
        return 0;
    }
}
