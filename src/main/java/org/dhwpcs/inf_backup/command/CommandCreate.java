package org.dhwpcs.inf_backup.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import org.dhwpcs.inf_backup.FabricEntrypoint;
import org.dhwpcs.inf_backup.storage.ChunkBackup;

import java.io.IOException;

public class CommandCreate {

    private final FabricEntrypoint entrypoint;

    public CommandCreate(FabricEntrypoint entrypoint) {
        this.entrypoint = entrypoint;
    }
    public void register(LiteralArgumentBuilder<ServerCommandSource> root) {
        root.then(LiteralArgumentBuilder.<ServerCommandSource>literal("create")
                .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("beginPosX", IntegerArgumentType.integer())
                        .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("beginPosY", IntegerArgumentType.integer())
                                .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("endPosX", IntegerArgumentType.integer())
                                        .then(RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("endPosY", IntegerArgumentType.integer())
                                                .then(RequiredArgumentBuilder.<ServerCommandSource, String>argument("description", StringArgumentType.string())
                                                        .executes(this::createBackup)

                                                        .then(RequiredArgumentBuilder.<ServerCommandSource, Identifier>argument("dimension", DimensionArgumentType.dimension())
                                                                .executes(this::createBackup))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private int createBackup(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        World world;
        Entity entity = source.getEntity();
        try {
            world = DimensionArgumentType.getDimensionArgument(ctx, "dimension");
        } catch (IllegalArgumentException e) {
            if (entity != null) {
                world = entity.getWorld();
            } else {
                ctx.getSource().sendMessage(Text.literal("Please specify a dimension.").formatted(Formatting.RED));
                return 1;
            }
        }
        assert world instanceof ServerWorld;
        ServerWorld sw = (ServerWorld) world;
        source.sendMessage(Text.literal("Begin to backup region file").formatted(Formatting.AQUA));
        int bgx = IntegerArgumentType.getInteger(ctx, "beginPosX");
        int bgy = IntegerArgumentType.getInteger(ctx, "beginPosY");
        int edx = IntegerArgumentType.getInteger(ctx, "endPosX");
        int edy = IntegerArgumentType.getInteger(ctx, "endPosY");
        String desc = StringArgumentType.getString(ctx, "slot");
        ChunkPos begin = new ChunkPos(bgx, bgy);
        ChunkPos end = new ChunkPos(edx, edy);
        Identifier key = world.getRegistryKey().getValue();
        ChunkBackup backup = entrypoint.storage.createChunkBackup(key, desc);
        backup.addRegionIn(begin, end);
        world.getServer().saveAll(false, true, true);
        try {
            backup.backup(sw);
        } catch (IOException e) {
            source.sendMessage(Text.literal("Failed to backup region file!").formatted(Formatting.RED));
            return 1;
        }
        source.sendMessage(Text.literal("Successfully backup chunks").formatted(Formatting.GREEN));
        return 0;
    }
}
