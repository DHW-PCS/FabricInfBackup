package org.dhwpcs.inf_backup;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.dhwpcs.inf_backup.util.Util;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.dhwpcs.inf_backup.storage.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public class FabricEntrypoint implements ModInitializer {

    public BackupStorage storage;
    public SortedSet<Pair<Path, BackupInfo>> infos = new TreeSet<>(Backup.COMPARATOR);
    public Map<String, LastOperation> operationToConfirm = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(it -> {
            Path saveRoot = it.getSavePath(WorldSavePath.ROOT);
            storage = new BackupStorage(saveRoot.resolve("backup_storage"), saveRoot);
            try {
                storage.init();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            CommandDispatcher<ServerCommandSource> dispatcher = it.getCommandManager().getDispatcher();
            LiteralArgumentBuilder<ServerCommandSource> root = LiteralArgumentBuilder.<ServerCommandSource>literal("backup")
                    .requires(another -> another.hasPermissionLevel(it.getOpPermissionLevel()))
                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("create")
                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("beginPosX", IntegerArgumentType.integer())
                                    .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("beginPosY",IntegerArgumentType.integer())
                                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("endPosX", IntegerArgumentType.integer())
                                                    .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("endPosY",IntegerArgumentType.integer())
                                                            .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("id", StringArgumentType.string())
                                                                    .executes(this::createBackup)

                                                                    .then(RequiredArgumentBuilder.<ServerCommandSource, Identifier>argument("dimension", DimensionArgumentType.dimension())
                                                                            .executes(this::createBackup))))))))

                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("rollback")
                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("id", IntegerArgumentType.integer(0))
                                    .executes(this::rollback)))

                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("list")
                            .executes(this::list))

                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("cancel")
                            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("all")
                                    .executes(this::cancelAll))

                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("id", IntegerArgumentType.integer(0))
                                    .executes(this::cancel)))

                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("delete")
                            .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("id", IntegerArgumentType.integer(0))
                                    .executes(this::delete)))

                    .then(LiteralArgumentBuilder.<ServerCommandSource>literal("confirm")
                            .then(RequiredArgumentBuilder.<ServerCommandSource, Boolean>argument("result", BoolArgumentType.bool())
                                    .executes(this::confirm))

                            .then(LiteralArgumentBuilder.<ServerCommandSource>literal("info")
                                    .executes(this::confirmInfo)));

            dispatcher.register(root);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(it -> {
            operationToConfirm.clear();
            for(Pair<Path, BackupInfo> info: infos) {
                BackupInfo bi = info.getRight();
                List<UUID> entities = new ArrayList<>(bi.entities());
                ServerWorld world = it.getWorld(RegistryKey.of(RegistryKeys.WORLD, bi.dim()));
                if(world == null) {
                    Backup.LOGGER.error("Backup {} is in world {} that is unsupported!", bi.uid(), bi.dim());
                    continue;
                }
                entities.forEach(uid -> {
                    Entity e = world.getEntity(uid);
                    if(e != null) {
                        e.setRemoved(Entity.RemovalReason.KILLED);
                    }
                });
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(it -> {
            boolean errored = false;
            RuntimeException re = new RuntimeException("Exception restoring chunks") {
                @Override
                public Throwable fillInStackTrace() {
                    setStackTrace(Backup.EMPTY_STACK_TRACE);
                    return this;
                }
            };
            for(Pair<Path, BackupInfo> info : infos) {
                Backup.LOGGER.info("Now begin to rollback chunks");
                BackupInfo right = info.getRight();
                Path left = info.getLeft();
                Path root = it.getSavePath(WorldSavePath.ROOT);
                root = DimensionType.getSaveDirectory(RegistryKey.of(RegistryKeys.WORLD, right.dim()), root);
                try(
                    RegionMerger region = new RegionMerger(
                            left.resolve(Backup.REGION_PATH),
                            root.resolve(Backup.REGION_PATH),
                            right.begin(),
                            right.end()
                    );
                    RegionMerger entities = new RegionMerger(
                            left.resolve(Backup.ENTITIES_PATH),
                            root.resolve(Backup.ENTITIES_PATH),
                            right.begin(),
                            right.end()
                    )
                ) {
                    storage.backupRestoration(info);
                    try {
                        CompletableFuture.allOf(region.merge(), entities.merge()).join();
                        Backup.LOGGER.info("The restoration is done. Please restart the server.");
                    } catch (RuntimeException e) {
                        errored = true;
                        Backup.LOGGER.fatal("FAILED TO ROLLBACK CHUNKS!", e);
                        Backup.LOGGER.fatal("The modifies cannot be restored. Please refer to "+ left +" and replace the ones in /region with them.");
                        Backup.LOGGER.fatal("Current rollback failed. However, you can still roll back to the same save when you know where the problem came.");
                        re.addSuppressed(new RuntimeException("failed to restore chunk in backup "+ right, e));
                    }
                } catch (Throwable e) {
                    errored = true;
                    re.addSuppressed(new RuntimeException("failed to restore chunk in backup "+ right, e));
                } finally {
                    try {
                        storage.close();
                    } catch (IOException ignored) {}
                    infos.clear();
                }
            }
            if(errored) {
                re.printStackTrace();
            }
        });
    }

    private int createBackup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        World world;
        Entity entity = source.getEntity();
        try {
            world = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
        } catch (IllegalArgumentException e) {
            if(entity != null) {
                world = entity.getWorld();
            } else {
                ctx.getSource().sendFeedback(() -> Text.literal("Please specify a dimension.").formatted(Formatting.RED), true);
                return 1;
            }
        }
        assert world instanceof ServerWorld;
        ServerWorld sw = (ServerWorld) world;
        source.sendFeedback(() -> Text.literal("Begin to backup region file").formatted(Formatting.AQUA), true);
        int bgx = IntegerArgumentType.getInteger(ctx, "beginPosX");
        int bgy = IntegerArgumentType.getInteger(ctx, "beginPosY");
        int edx = IntegerArgumentType.getInteger(ctx, "endPosX");
        int edy = IntegerArgumentType.getInteger(ctx, "endPosY");
        String desc = StringArgumentType.getString(ctx, "id");
        ChunkPos begin = new ChunkPos(bgx, bgy);
        ChunkPos end = new ChunkPos(edx, edy);
        Identifier key = world.getRegistryKey().getValue();
        ChunkBackup backup = storage.createChunkBackup(key, desc);
        backup.addRegionIn(begin, end);
        world.getServer().saveAll(false, true, true);
        try {
            backup.backup(sw);
        } catch (IOException e) {
            source.sendFeedback(() -> Text.literal("Failed to backup region file!").formatted(Formatting.RED), true);
            return 1;
        }
        source.sendFeedback(() -> Text.literal("Successfully backup chunks").formatted(Formatting.GREEN), true);
        return 0;
    }

    private int rollback(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Pair<Path, BackupInfo> info = storage.find(id);
        if(info == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("Cannot find the backup in slot "+id).formatted(Formatting.RED), true);
            return 1;
        }
        if(!infos.add(info)) {
            ctx.getSource().sendFeedback(() -> Text.literal("The same backup is already requested to be applied").formatted(Formatting.RED), true);
            return 1;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("The following backup is chosen").formatted(Formatting.AQUA), true);
        ctx.getSource().sendFeedback(() -> Text.of(info.getRight().toString()), true);
        ctx.getSource().sendFeedback(() -> Text.literal("When the server shuts down, the rollback will be performed.").formatted(Formatting.GREEN), true);
        return 0;
    }

    private int list(CommandContext<ServerCommandSource> ctx) {
        BiConsumer<Integer, Pair<Path, BackupInfo>> printer = (index,inf) -> {
            BackupInfo info = inf.getRight();
            ctx.getSource().sendFeedback(() -> Text.of(index + ": " + info.toString()), true);
        };
        ctx.getSource().sendFeedback(() -> Text.of("There are now "+storage.size()+" backups:"), true);
        storage.forEachIndexed(printer);
        ctx.getSource().sendFeedback(() -> Text.of("There are now "+infos.size()+" backup to be applied:"), true);
        Util.forEachIndexed(infos, printer);
        return 0;
    }

    private int cancelAll(CommandContext<ServerCommandSource> ctx) {
        if(!infos.isEmpty()) {
            infos.clear();
            ctx.getSource().sendFeedback(() -> Text.of("The pending rollback requests are all cancelled."), true);
            return 0;
        }
        ctx.getSource().sendFeedback(() -> Text.literal("No rollback request is pending.").formatted(Formatting.RED), true);
        return 1;
    }

    private int cancel(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        if(!Util.remove(infos, id)) {
            ctx.getSource().sendFeedback(() -> Text.literal("No such rollback request is pending.").formatted(Formatting.RED), true);
            return 1;
        }
        ctx.getSource().sendFeedback(() -> Text.of("The pending rollback request is cancelled."), true);
        return 0;
    }

    private int delete(CommandContext<ServerCommandSource> ctx) {
        int id = IntegerArgumentType.getInteger(ctx, "id");
        Pair<Path, BackupInfo> pair = storage.find(id);
        if(pair == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("Cannot find the backup in slot "+id).formatted(Formatting.RED), true);
            return 1;
        }
        operationToConfirm.put(ctx.getSource().getName(), new LastOperation.DeleteSave(id, storage));
        ctx.getSource().sendFeedback(() -> Text.literal("Deleting save in slot "+id+", are you sure?").formatted(Formatting.AQUA), true);
        ctx.getSource().sendFeedback(() -> Text.literal("IF YOU ARE SURE, CLICK HERE")
                .styled(raw -> raw.withColor(Formatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/backup confirm true"))), true);
        ctx.getSource().sendFeedback(() -> Text.literal("IF YOU ARE UNWILLING, CLICK HERE")
                .styled(raw -> raw.withColor(Formatting.RED).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/backup confirm false"))), true);
        return 0;
    }

    private int confirm(CommandContext<ServerCommandSource> ctx) {
        boolean result = BoolArgumentType.getBool(ctx, "result");
        LastOperation operation = operationToConfirm.remove(ctx.getSource().getName());
        if(operation == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("You don't have any operation to confirm").formatted(Formatting.RED), true);
            return 1;
        }
        if(result) {
            if(operation.perform(ctx.getSource())) {
                ctx.getSource().sendFeedback(() -> Text.literal("Your last operation was successful.").formatted(Formatting.GREEN), true);
                return 0;
            } else {
                ctx.getSource().sendFeedback(() -> Text.literal("Your last operation was failed.").formatted(Formatting.RED), true);
                return 1;
            }
        } else {
            operationToConfirm.remove(ctx.getSource().getName());
            ctx.getSource().sendFeedback(() -> Text.literal("You have cancelled your last operation.").formatted(Formatting.GREEN), true);
            return 0;
        }
    }

    private int confirmInfo(CommandContext<ServerCommandSource> ctx) {
        LastOperation operation = operationToConfirm.get(ctx.getSource().getName());
        if(operation == null) {
            ctx.getSource().sendFeedback(() -> Text.literal("You don't have any operation to confirm").formatted(Formatting.RED), true);
            return 1;
        }
        ctx.getSource().sendFeedback(() -> Text.of("Your last operation to confirm: "+ operation.getInformation()), true);
        return 0;
    }
}
