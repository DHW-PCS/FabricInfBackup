package org.dhwpcs.inf_backup.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.storage.Backup;

import java.util.List;

public class CommandRoot {

    private final FabricEntrypoint entrypoint;
    private final CommandRollback rollback;
    private final CommandList list;
    private final CommandDelete delete;
    private final CommandCreate create;
    private final CommandConfirm confirm;
    private final CommandCancel cancel;

    public CommandRoot(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
        this.rollback = new CommandRollback(entrypoint);
        this.cancel = new CommandCancel(entrypoint);
        this.confirm = new CommandConfirm(entrypoint);
        this.create = new CommandCreate(entrypoint);
        this.list = new CommandList(entrypoint);
        this.delete = new CommandDelete(entrypoint);
    }
    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> root = LiteralArgumentBuilder.<ServerCommandSource>literal("backup")
                .requires(another -> another.hasPermissionLevel(another.getServer().getOpPermissionLevel()))
                .executes(this::printUsage);
        rollback.register(root);
        cancel.register(root);
        confirm.register(root);
        create.register(root);
        list.register(root);
        delete.register(root);
        dispatcher.register(root);
    }

    private int printUsage(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource source = ctx.getSource();
        helpInfo.forEach(it -> source.sendMessage(Text.literal(it)));
        return 0;
    }

    private static final List<String> helpInfo = List.of(
            "Inf-Backup version " + Backup.VERSION,
            "Made by InitAuther97",
            "Commands:",
            " - /backup create <beginPosX> <beginPosY> <endPosX> <endPosY> <description> [<dimension>]",
            "      Used to create a backup. Begin and end pos is the position of the chunk in the world.",
            "      The dimension argument is default to the dimension you are in.",
            " - /backup rollback <slot>",
            "      Add a pending rollback to the wait list. When the server shuts down, it will be applied.",
            "      Obtain the slot by /backup list",
            " - /backup list",
            "      List all the saves created, or to be applied. The number is the slot number.",
            " - /backup cancel <slot>",
            "      Cancel the rollback request in the specified slot in the list to apply.",
            "      Obtain the slot by /backup list",
            " - /backup cancel all",
            "      Cancel all the rollback request.",
            " - /backup delete <slot>",
            "      Delete the save in the specified slot.",
            "      After executing, it requires a confirmation to continue.",
            " - /backup confirm <result: true|false>",
            "      Decide whether to confirm on the result.",
            " - /backup confirm info",
            "      Check out which operation is waiting for confirmation."
    );
}
