package org.dhwpcs.infbackup.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.dhwpcs.infbackup.FabricEntrypoint;
import org.dhwpcs.infbackup.LastOperation;
import org.dhwpcs.infbackup.storage.BackupInfo;

import java.nio.file.Path;

public class CommandDelete {
    private final FabricEntrypoint entrypoint;

    public CommandDelete(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(LiteralArgumentBuilder.<ServerCommandSource>literal("delete")
                .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("slot", IntegerArgumentType.integer(0))
                        .executes(this::delete)));
    }

    private int delete(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        int id = IntegerArgumentType.getInteger(ctx, "slot");
        Pair<Path, BackupInfo> pair = entrypoint.storage.find(id);
        if (pair == null) {
            ctx.getSource().sendFeedback(new LiteralText("Cannot find the backup in slot " + id).formatted(Formatting.RED), false);
            return 1;
        }
        entrypoint.operationToConfirm.put(ctx.getSource().getName(), new LastOperation.DeleteSave(id, entrypoint.storage));
        source.sendFeedback(new LiteralText("Deleting save in slot " + id + ", are you sure?").formatted(Formatting.AQUA), false);
        source.sendFeedback(new LiteralText("IF YOU ARE SURE, CLICK HERE")
                .styled(raw -> raw.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/backup confirm true"))), false);
        source.sendFeedback(new LiteralText("IF YOU ARE UNWILLING, CLICK HERE")
                .styled(raw -> raw.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/backup confirm false"))), false);
        return 0;
    }
}
