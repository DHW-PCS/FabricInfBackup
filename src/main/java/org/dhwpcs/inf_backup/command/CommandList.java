package org.dhwpcs.inf_backup.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.storage.BackupInfo;
import org.dhwpcs.inf_backup.util.Util;

import java.nio.file.Path;
import java.util.function.BiConsumer;

public class CommandList {
    private final FabricEntrypoint entrypoint;

    public CommandList(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }

    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                .executes(this::list));
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        BiConsumer<Integer, Pair<Path, BackupInfo>> printer = (index, inf) -> {
            BackupInfo info = inf.getRight();
            ctx.getSource().sendMessage(Text.of(index + ": " + info.toString()));
        };
        ctx.getSource().sendMessage(Text.of("There are now " + entrypoint.storage.size() + " backups:"));
        entrypoint.storage.forEachIndexed(printer);
        ctx.getSource().sendMessage(Text.of("There are now " + entrypoint.selectedBackups.size() + " backup to be applied:"));
        Util.forEachIndexed(entrypoint.selectedBackups, printer);
        return 0;
    }
}
