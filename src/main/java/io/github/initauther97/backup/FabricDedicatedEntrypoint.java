package io.github.initauther97.backup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;

import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class FabricDedicatedEntrypoint implements DedicatedServerModInitializer {

    public final ChunkBackupStorage storage = new ChunkBackupStorage("region_backup");
    public Pair<Path, BackupInfo> info;
    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd__HH_mm_ss");
    @Override
    public void onInitializeServer() {
        try {
            storage.init();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ServerLifecycleEvents.SERVER_STARTED.register(it -> {
            CommandDispatcher<ServerCommandSource> dispatcher = it.getCommandManager().getDispatcher();
            LiteralArgumentBuilder<ServerCommandSource> root = LiteralArgumentBuilder.<ServerCommandSource>literal("backup")
                    .requires(another -> another.hasPermissionLevel(it.getOpPermissionLevel()))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("create")
                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("beginPosX", IntegerArgumentType.integer())
                                    .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("beginPosY",IntegerArgumentType.integer())
                                        .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("endPosX", IntegerArgumentType.integer())
                                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("endPosY",IntegerArgumentType.integer())
                                                    .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("id", StringArgumentType.string())
                                                        .executes(ctx -> {
                                                            ctx.getSource().sendFeedback(Text.of("Begin to backup region file"), true);
                                                            int bgx = IntegerArgumentType.getInteger(ctx, "beginPosX");
                                                            int bgy = IntegerArgumentType.getInteger(ctx, "beginPosY");
                                                            int edx = IntegerArgumentType.getInteger(ctx, "endPosX");
                                                            int edy = IntegerArgumentType.getInteger(ctx, "endPosY");
                                                            String desc = StringArgumentType.getString(ctx, "id");
                                                            ChunkPos begin = new ChunkPos(bgx, bgy);
                                                            ChunkPos end = new ChunkPos(edx, edy);
                                                            ChunkBackup backup = storage.createChunkBackup(desc);
                                                            backup.addRegionIn(begin, end);
                                                            it.saveAll(false, true, true);
                                                            try {
                                                                backup.backup(it.getSavePath(WorldSavePath.ROOT));
                                                            } catch (IOException e) {
                                                                ctx.getSource().sendFeedback(new LiteralText("Failed to backup region file!").formatted(Formatting.RED), true);
                                                                return 1;
                                                            }
                                                            ctx.getSource().sendFeedback(Text.of("Successfully backup chunks"), true);
                                                            return 0;
                                                    })))))))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("rollback")
                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("id", IntegerArgumentType.integer(0))
                                    .executes(ctx -> {
                                        int id = IntegerArgumentType.getInteger(ctx, "id");
                                        info = storage.find(id);
                                        if(info == null) {
                                            ctx.getSource().sendFeedback(new LiteralText("Cannot find the backup with description "+id).formatted(Formatting.RED), true);
                                            return 1;
                                        }
                                        ctx.getSource().sendFeedback(Text.of("The following backup is chosen"), true);
                                        ctx.getSource().sendFeedback(Text.of(info.getRight().toString()), true);
                                        ctx.getSource().sendFeedback(new LiteralText("When the server shuts down, the rollback will be performed.").formatted(Formatting.GREEN), true);
                                        return 0;
                                    })))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                            .executes(ctx -> {
                                ctx.getSource().sendFeedback(Text.of("There are now "+storage.size()+" backups:"), true);
                                storage.forEachIndexed((index,inf) -> {
                                    BackupInfo info = inf.getRight();
                                    ctx.getSource().sendFeedback(Text.of(index + ": " + info.toString()), true);
                                });
                                return 0;
                            }))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("cancel")
                            .executes(ctx -> {
                                if(info != null) {
                                    info = null;
                                    ctx.getSource().sendFeedback(Text.of("The pending rollback request is cancelled."), true);
                                    return 0;
                                }
                                ctx.getSource().sendFeedback(new LiteralText("No rollback request is pending.").formatted(Formatting.RED), true);
                                return 1;
                            }));

            dispatcher.register(root);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(it -> {
            if(info != null) {
                Backup.LOGGER.info("Now begin to rollback chunks");
                Path root = it.getSavePath(WorldSavePath.ROOT);
                ChunkMerger merger;
                try {
                    BackupInfo right = info.getRight();
                    Path left = info.getLeft();
                    merger = new ChunkMerger(left, root.resolve(Backup.REGION_PATH), right.description(), right.begin(), right.end());
                    merger.createBackup(left);
                    try {
                        merger.merge();
                    } catch (RuntimeException e) {
                        Backup.LOGGER.fatal("FAILED TO ROLLBACK CHUNKS!", e);
                        Backup.LOGGER.fatal("The modifies cannot be restored. Please refer to "+ left +" and replace the ones in /region with them.");
                        Backup.LOGGER.fatal("Current rollback failed. However, you can still roll back to the same save when you know where the problem came.");
                        Backup.LOGGER.fatal("Press any key to continue...");
                        try {
                            System.in.read();
                        } catch (IOException ex) {
                            return;
                        }
                    }
                    Backup.LOGGER.info("The restoration is done. Please restart the server.");
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }

            }
        });
    }
}
